---
name: axon4-to-axon5-migration
description: >-
  Migrate a Spring Boot + Axon Framework 4 project to Axon Framework 5.
  Three modes: (1) **phased** (default) — orchestrate the migration phase by
  phase, persistence in `<target>/.axon4-to-axon5-migration/progress.md`,
  resume from that file; (2) **debug** — drive triage from `mvn` compile
  errors, route clusters to recipes; (3) **single** — pass a file path or FQ
  class name, the orchestrator auto-routes via the routing table and runs ONE
  recipe. Behavior preserved: no DCB, no new patterns, legacy event storage
  preserved. Use whenever the user asks to migrate AF4 → AF5, resume an
  in-progress migration, run one specific item, or unblock a failing build.
argument-hint: "[<file path or FQ class> | debug | phased]"
---

# AF4 → AF5 migration — orchestrator + plugin recipes

ONE skill. Orchestrator owns: mode dispatch, the routing table, progress
mechanics, commit emission. Recipes are phase-unaware plugins with a fixed
five-section interface plus an optional subagent block. Source of truth is
**`progress.md`** under `<target>/.axon4-to-axon5-migration/` — read on
every invocation, rewritten before every commit, committed alongside the
code change it documents.

## Mode dispatch — `$ARGUMENTS`

```yaml
phased:                         # default — runs when $ARGUMENTS empty or "phased"
  trigger: "(no args) | 'phased'"
  flow: PHASE_LOOP from current state in progress.md
  resume: read progress.md ▶︎ RESUME HERE block on every entry

debug:
  trigger: "'debug'"
  flow: DEBUG_LOOP — driven by mvn output, route clusters to recipes
  use-when: full project compile failing, or alternative to phased order

single:
  trigger: "<file path or FQ class name>"
  flow: RUN_ONE — auto-route to recipe via routing table, run once, commit, stop
  recipe: NOT specified by user — orchestrator picks via the routing table
```

Ambiguous → AskUserQuestion. Never auto-pick "phased" when the user clearly
named a file but the path is wrong; surface the error.

## Goal

> Fully compiling, green-test codebase on AF5, **same architecture as AF4**.
> No DCB. No new patterns. Legacy event storage preserved.
> `./mvnw clean verify` green at the end of stabilization.

### 🚨 Data migration is out of scope — code only

This skill rewrites **code**: bean wiring, imports, annotations, handler
shapes, test fixtures, processor config, and (Path A only) AF5 schema
SQL that operates on rows already present in the AF4 table.

It does **NOT**:

- copy or transform event-store rows between stores (Mongo → AS,
  Mongo → relational, JDBC → JPA, …);
- copy or rebuild token-store rows;
- delete, rewrite, or re-read existing snapshot rows;
- export, import, or replay any persisted state.

Every `move-to-*` option in any recipe's `not-supported.md` is a
**code-rewrite choice**, not a data-migration offer. The user owns and
runs every data move out-of-band, on a non-prod copy first, with row
counts verified. If the user has not planned a data move, recipes must
prefer `pause-migration` / `accept-stays-af4` over a `move-to-*` path.

Intermediate phases may leave the project non-compiling — by design.
Per-recipe `migration-<recipe>-<Target>` Maven profiles keep verification
scoped (one profile per migrated item — atomic, parallelizable, individually
removable). See [references/maven-profile/maven-profile.md](references/maven-profile/maven-profile.md).

## Routing table

Single source of truth for: phase order, per-recipe discovery, single-file
auto-routing, INIT detection of unsupported features.

### Column key

| Column | Purpose |
|---|---|
| **Recipe** | Folder name under `references/`. Also the recipe id used in commit bodies. |
| **Mode** | `iterative` (one item at a time) · `one-shot` (no item iteration) · `not-supported` (stop and message) · `triage` (top-level mode, not a phase) · `utility` (invoked transitively) |
| **Phase** | Migration phase number `1–9`, or `n/a` for non-phase rows |
| **Discovery** | Grep pattern used to find candidates. `n/a` for one-shot or non-phase rows. |
| **Condition** | Plain-language predicate — "when does this recipe apply?" |
| **Notes** | Per-recipe extras: `exclude-when` greps, sub-paths, license-decision, the `not-supported-message`, keyword aliases, file-include globs. Free-form. |

### Table

| Recipe | Mode | Phase | Discovery (grep) | Condition | Notes |
|---|---|---|---|---|---|
| `openrewrite` | one-shot | 1 | n/a | Target depends on `org.axonframework.*` and not yet on AF5 BOM | License decision (free-af5 / axoniq) drives Path A vs Path B. Aliases: openrewrite, recipes, bulk. |
| `aggregate` | iterative | 2 | `@Aggregate\b\|@AggregateRoot\b` (`*.java`, `*.kt`) | Class with `@Aggregate`/`@AggregateRoot` AND `@EventSourcingHandler` methods | Variant detection in Procedure: simple / multi-entity / polymorphic. Aliases: aggregate, event-sourced-aggregate, entity. |
| `event-processor` | iterative | 3 | `@ProcessingGroup\|org\.axonframework\.eventhandling\.EventHandler` | Class with `@EventHandler` methods (typically `@ProcessingGroup`) | Aliases: event-processor, eventhandler, projector, projection. |
| `command-gateway` | iterative | 4 | `org\.axonframework\.commandhandling\.gateway\.CommandGateway` | Top-of-chain caller injecting `CommandGateway`, NOT a message handler | exclude-when: `@EventHandler\|@CommandHandler\|@QueryHandler\|@MessageHandlerInterceptor`. Three return-shape paths (MVC / scheduler / reactive). Aliases: command-gateway, commanddispatch, controller-command. |
| `query-gateway` | iterative | 5 | `org\.axonframework\.queryhandling\.QueryGateway` | Top-of-chain caller injecting `QueryGateway`, NOT a message handler | exclude-when: `@EventHandler\|@CommandHandler\|@QueryHandler`. Aliases: query-gateway, controller-query. |
| `query-handler` | iterative | 6 | `org\.axonframework\.queryhandling\.QueryHandler` | Class with `@QueryHandler` methods | Aliases: query-handler, queryhandler. |
| `read-configuration` | iterative | 7 | `org\.axonframework\.config\.(Configuration\|EventProcessingConfiguration)` | Class injecting `Configuration` / `EventProcessingConfiguration` | Aliases: read-configuration, readconfig, configuration-reader. |
| `write-configuration` | iterative | 8 | `@Bean.*(Configurer\|ConfigurerModule\|EventProcessingConfigurer)\|DefaultConfigurer\.defaultConfiguration` | `@Configuration` class with `Configurer` / `ConfigurerModule` / `EventProcessingConfigurer` beans | Aliases: write-configuration, writeconfig, configurer, configurer-module. |
| `event-storage-engine` | one-shot | 9 | n/a (one-shot bean swap) | Project declares `EventStorageEngine` / `EmbeddedEventStore` / `AxonServerEventStore` bean | Three sub-paths: A (JPA), B (Axon Server), C (non-Spring). Aliases: event-storage-engine, storage-engine, eventstore. |
| `saga` | not-supported | n/a | `@Saga\b\|@SagaEventHandler\|@StartSaga\|@EndSaga\|SagaConfigurer` | Project uses AF4 sagas | Detected at INIT. not-supported-message: "AF5 reframes long-running coordination as process-managers; no automatic rewrite. Currently not supported by this skill. Workflow support is on the Axoniq roadmap — contact Axoniq." |
| `deadline-manager` | not-supported | n/a | `@DeadlineHandler\|DeadlineManager\|DeadlineMessage` | Project uses AF4 `DeadlineManager` | not-supported-message: "Predominantly used with sagas (also unsupported). No direct AF5 successor. Contact Axoniq for roadmap." |
| `debug` | triage | n/a | n/a (driven by `mvn` errors) | Build failing under no migration profile, or alternative to phased order | Top-level mode, NOT a phase. Routes clusters to other recipes — sibling-link exemption applies. Aliases: debug, triage, compile-errors. |
| `maven-profile` | utility | n/a | n/a | Any iterative recipe needs to seed/extend its per-target profile | Invoked transitively by every iterative recipe. Reference: [assets/maven-profile-snippet.xml](assets/maven-profile-snippet.xml). Aliases: maven-profile, migration-profile. |

Adding a phase = appending one row with the next `Phase` integer + a recipe
folder. Adding an unsupported-feature stop = appending a `not-supported` row.

### Single-file routing (auto-assign recipe)

User passes a file path or FQ class. Orchestrator does NOT ask "which
recipe?" — it inspects the file and matches the routing table:

```
1. content = read(file)
2. for each row where Mode in {iterative, one-shot}, sorted by Phase ascending:
     - if row.Discovery matches content
       AND row.Notes.exclude-when does NOT match → row wins
3. if multiple match (e.g. write-configuration class also declares storage-engine bean):
     - pick the row with the lowest Phase
     - ask user via AskUserQuestion only if user wants to override
```

User says **what** to migrate (the file). The skill picks **how** via the table.

## Recipe interface contract

Every `references/<recipe>/<recipe>.md` MUST declare these five sections,
parsed by heading. Order is flexible — orchestrator parses by heading
match, not position. The template below shows one common arrangement
(End condition / Output near the top works as a contract header before
the steps; near the bottom works as a closer after the steps):

```
# Recipe: <name>

## Inputs
- target: <FQ class | file path | none>  (required | optional)
- <other recipe-specific args, each typed and required/optional flagged>

## Preflight
- quick "already migrated?" check → return Output with skip=true

## Procedure
- main flow as numbered pseudo-code; conditions explicit, parameters typed
- references each ### Path subsection by condition

### Path A — <name>
#### Condition
- when this path applies (Spring Boot / aggregate variant / dependency present)
#### Steps
- pseudo-code: numbered, conditions explicit, parameters typed

### Path B — <name>
#### Condition
- ...
#### Steps
- ...

## End condition
- objective, machine-checkable

## Output
- target: <FQ name | file path | "n/a">
- decisions: [<list of choices made — what feeds the commit body>]
- needs-user-decision: <true | false>      # internal flag for orchestrator branching
- needs-user-decision-reason: <string>     # only when needs-user-decision=true
- notes: <optional free text>
- <recipe-specific extras (parsed by orchestrator, NOT persisted in commit body)>
```

Optional sections recipes may keep: `## Goal`, `## In scope` / `## Out of
scope`, `## FQN cheat sheet`, `## Caveats`, `## Examples`, `## Reference
index` (links INTO the recipe's own folder only).

### Optional `not-supported.md` sibling file

Recipes that have AF5-blocking inputs (no portable target, missing AF5
release, removed SPI) put detection + `AskUserQuestion` flows in a
sibling `references/<recipe>/not-supported.md`. Each blocker entry gives:

- **Why blocker** — one paragraph.
- **Detection** — exact grep / inspection.
- **AskUserQuestion** — verbatim option labels.
- **Output decision key** — added to the recipe's Output `decisions`.
- **Effect on Procedure** — proceed / redirect path / exit with `needs-user-decision=true`.

When a `not-supported.md` exists, the recipe's `## Preflight` MUST list
"Read [not-supported.md] first — run every Detection grep" as its first
step. Recipe must NOT proceed past Preflight while a blocker is
unresolved. The orchestrator records each `decisions.<key>` from the
recipe's Output exactly as the recipe emits it.

This pattern replaces former top-level `not-supported`-mode rows in the
routing table for blockers that are scoped to a specific recipe (e.g.
Mongo on `event-storage-engine` / `event-processor`, snapshotting on
`aggregate`). Top-level `not-supported` rows stay only for project-wide
features detected at INIT (e.g. `saga`).

### Optional `## Subagent guidelines`

Declares how the orchestrator should spawn a subagent for this recipe. If
absent, the orchestrator uses the default (`general-purpose`, no isolation,
no framing). Shape:

```
## Subagent guidelines

- subagent_type: <general-purpose | Explore | Plan | claude-code-guide>
  # which subagent the orchestrator passes to the Agent tool

- isolation: <none | worktree>
  # use "worktree" for broad sweeping edits with easy rollback

- prompt-framing: |
  # paragraph(s) the orchestrator prepends to the recipe-execution prompt

- parallelism: <single | per-item>
  # "per-item" lets the orchestrator fan out one subagent per discovered candidate
```

The orchestrator NEVER invents subagent types not listed in the available
agents — `general-purpose` is the safe default.

### Forbidden in any recipe (lint enforced)

- migration-phase numbering in any free-form ("phase 1", "Phase 2",
  "(phase 9)") — except the explicit form **"Migration Phase #N"**
- references to sibling recipes (no `../command-gateway/…`)
- references to `progress.md` / `learnings.md` / `index.md` / orchestrator
  semantics

The framework class `org.axonframework.common.lifecycle.Phase` is
exempt — recipes refer to it as `Phase.<NAME>` or via FQN, never as a
migration phase number.

The `debug` recipe is **exempt from the no-sibling-links rule** — its
routing IS its job. The `maven-profile` utility recipe is also OK to
link from inside any iterative recipe, since it's invoked transitively
to seed/extend the per-target profile.

## Procedure form (pseudo-code with explicit conditions)

Every recipe's `## Procedure` is the **main flow**. Branches go to `### Path
A / B / …` subsections, each gated by a `#### Condition`. The procedure is
concise pseudo-code:

```
## Procedure

1. Locate target.
   - if Inputs.target set → use it
   - else → first match of <discovery grep>
2. Detect variant.
   - has @AggregateMember field? → variant=multi-entity
   - has subclass annotated @Aggregate? → variant=polymorphic
   - else → variant=simple
3. Run class-level transformation (steps 3a–3n below).
4. Pick path:
   - depends on axoniq-spring-boot-starter? → Path A (Spring Boot)
   - else → Path B (non-Spring)
5. Run path Steps (see ### Path A / ### Path B).
6. Run test fixture migration if test class exists.
7. Verify against ## End condition.
8. Emit ## Output.
```

## Source of truth = progress.md

State lives in `<target>/.axon4-to-axon5-migration/progress.md`. The
orchestrator reads it on every invocation, rewrites the relevant sections
before every commit, and stages it together with the code change. A fresh
session must be able to resume from this file alone.

Conventional commit messages — no structured body block. See
[references/commit-cadence.md](references/commit-cadence.md) for the full
subject-line table and [assets/commit-message-template.md](assets/commit-message-template.md)
for the template.

### Persistence invariant

Every state change ends with: **`progress.md` rewritten + commit including
it**. Never mutate the working tree without a corresponding `progress.md`
update in the same commit.

### Persistence checklist (run before every commit)

- [ ] **▶︎ RESUME HERE** points at the *next* unit (not the one just done).
      Has Next action, exact recipe, exact verify command.
- [ ] **Phase status table** row updated: `Items done / total`, `Last commit`.
- [ ] **Per-phase plan table** row for just-finished item: status `done`
      (or `blocked` / `deferred-to-stabilization`), commit SHA.
- [ ] If non-obvious lesson: `learnings.md` has new dated entry.

A fresh session reading `progress.md` after this commit must pick up the next
action with NO clarifying questions about state.

### Resume protocol

When `progress.md` exists:

1. Read it top-to-bottom. **▶︎ RESUME HERE** block tells the exact next move.
2. Sanity-check working tree:
   ```bash
   git -C <target> rev-parse --short HEAD
   git -C <target> status --porcelain
   ```
   - HEAD matches recorded last commit + tree clean → proceed.
   - HEAD ahead (user committed manually) → surface, trust user.
   - Tree dirty → previous session crashed. AskUserQuestion: inspect diff /
     reset to last commit (destructive — only with explicit user OK) /
     continue from dirty (treat in-progress edits as yours to finish).
3. Read **Pinned user decisions** block — license target, commit cadence,
   storage path, unsupported-feature acceptance — already answered. Do **NOT**
   re-prompt.
4. Read `learnings.md` only on demand (full file may not fit in context for
   long-running migrations — pull entries when relevant).
5. Confirm in 1–2 sentences with user via AskUserQuestion ("Resuming at
   phase X. Next: Y. Continue?").
6. Trust `progress.md`. If user says it's wrong, fix together, commit fix,
   then continue.

### Mid-phase dirty tree (user WIP detected)

If `git status --porcelain` shows files the orchestrator did NOT touch
(user-side WIP that crept in), pause and AskUserQuestion:

- `Stage and commit only the migration files I touched` *(Recommended)* —
  `git add` with explicit paths.
- `Let me handle the working tree first` — pause, let user clean up, resume.
- `Skip this commit` — record skip in `progress.md`, continue without committing.

Don't silently sweep user WIP into a migration commit.

### Encourage `/clear` between units

After every commit on a non-trivial unit (the whole of Migration Phase #1,
every 2–3 migrated items, or anything large):

> "Committed `<sha>`. Working tree clean, `progress.md` up to date. Safe
> point to `/clear` if you'd like — when you come back, just invoke this
> skill again and I'll resume."

Phase boundaries especially. Don't insist; user decides.

## State directory — `<target>/.axon4-to-axon5-migration/`

```
<target>/.axon4-to-axon5-migration/
├── index.md       # short README — points at progress.md and learnings.md.
├── progress.md    # SINGLE SOURCE OF TRUTH for migration state.
│                  #   Rewritten before every commit, committed with the code.
│                  #   Sections: Goal / ▶︎ RESUME HERE / Pinned decisions /
│                  #             Phase status / Per-phase plan.
├── learnings.md   # Append-only narrative. Surprises, manual fixes, decisions.
└── sql/           # event-storage-engine Path A DDL.
```

Templates: [assets/index-template.md](assets/index-template.md),
[assets/progress-template.md](assets/progress-template.md),
[assets/learnings-template.md](assets/learnings-template.md).

`.axon4-to-axon5-migration/` is committed alongside the migration changes —
every migration commit includes a `progress.md` rewrite.

## Orchestrator pseudocode

```
ORCHESTRATE:
  1. mode = parse($ARGUMENTS)
  2. resolve target dir; validate (exists, has pom.xml, is git repo,
                                   not the AxonFramework repo itself)
  3. dispatch on mode:
       - single → RUN_ONE
       - debug  → DEBUG_LOOP
       - phased → PHASED

PHASED:
  1. if progress.md missing → INIT (create from template, run init steps)
  2. State = read progress.md
  3. handle dirty tree (see Resume protocol)
  4. PHASE_LOOP

PHASE_LOOP:
  1. row = next routing-table row where Mode in {iterative, one-shot}
                                      AND State.phase_done(row.Phase) == false
  2. none left → DONE
  3. if row.Mode == one-shot → items = [None]
     else                    → items = discover(row.Discovery)
  4. items = items minus State.deferred_or_unsupported
  5. ITEM_LOOP for this phase
  6. AskUserQuestion checkpoint (continue / iterate phase / pause). Goto 1.

ITEM_LOOP:
  1. item = next pending in items
  2. none → return to PHASE_LOOP
  3. Output = EXECUTE_RECIPE(row.Recipe, build_inputs(item, row))
  4. if Output.skip (preflight: already-migrated) → goto 1, no commit
  5. if Output.needs-user-decision:
       AskUserQuestion: fix / defer / stop
       - fix   → user resolves, re-run from step 3
       - defer → record decision in progress.md (status=deferred,
                 reason=<text>); commit progress.md only; goto 1
       - stop  → HALT
  6. update progress.md (RESUME HERE → next item, phase status row,
                         per-phase plan row with commit SHA)
  7. commit_per_item — stage explicit paths (touched code + progress.md +
                       learnings.md if dirty); conventional message
  8. suggest /clear. Goto 1.

EXECUTE_RECIPE(recipe, inputs):
  1. validate inputs against recipe ## Inputs
  2. run recipe ## Preflight; if already-migrated → return Output with skip=true
  3. SPAWN_SUBAGENT(recipe, inputs) → Output     # see below
  4. verify recipe ## End condition against subagent's reported state:
     - green   → Output.needs-user-decision = false
     - blocker → Output.needs-user-decision = true, reason = <text>
  5. return Output

SPAWN_SUBAGENT(recipe, inputs):
  1. parse recipe ## Subagent guidelines (optional). Defaults if absent:
       subagent_type = "general-purpose"
       isolation = none
       prompt-framing = ""
       parallelism = single
  2. if subagent spawning unavailable (nested context, tool denied)
     → run inline in main loop, log "fallback=inline" in Output.notes
  3. else build prompt:
       <prompt-framing>
       Read references/<recipe>/<recipe>.md and execute its ## Procedure.
       Inputs: <inputs serialized>
       When done, return a filled ## Output block (target, decisions,
       needs-user-decision, notes).
       Do NOT commit — the orchestrator owns commits.
  4. invoke Agent tool with subagent_type, prompt, isolation
  5. parse the Output block from subagent's response, validate, return

INIT (first phased run):
  1. mkdir -p <target>/.axon4-to-axon5-migration/
  2. seed progress.md from assets/progress-template.md, learnings.md from
     assets/learnings-template.md, index.md from assets/index-template.md
  3. for each row in routing table where Mode == not-supported:
       hits = run row.Discovery
       if hits → AskUserQuestion: accept-stays-af4 / pause / remove-feature-first
                 record decision in progress.md Pinned-decisions block
                 (e.g. "saga: accept-stays-af4")
                 append narrative line to learnings.md
  4. license target via AskUserQuestion: free-af5 / axoniq-commercial
     record in progress.md Pinned-decisions
  5. commit init record:
       chore(af5-migration): initialize migration
       (stages progress.md, learnings.md, index.md)

RUN_ONE (single mode):
  1. row = route(arg) via routing table (auto, no user prompt)
  2. inputs = build_inputs(arg, row)
  3. Output = EXECUTE_RECIPE(row.Recipe, inputs)
  4. update progress.md (if it exists), commit_per_item
  5. suggest /clear. STOP.

DEBUG_LOOP:
  1. run ./mvnw test-compile under no migration profile
  2. cluster errors (see references/debug/debug.md)
  3. for the highest-leverage cluster:
       row = route(cluster.shape) via routing table
       Output = EXECUTE_RECIPE(row.Recipe, build_inputs(cluster, row))
       update progress.md (if it exists), commit_per_item
  4. rerun compile; if dropped → goto 2.
                    if unchanged → AskUserQuestion: surface / skip-defer / stop.
  5. all green → DONE.
```

`commit_per_item` stages explicit paths (touched code + progress.md +
learnings.md if dirty). Conventional commit message — no structured event
block. Never `git add -A`.

## Commit & verification

- Commit cadence rules: [references/commit-cadence.md](references/commit-cadence.md).
- Commit message template: [assets/commit-message-template.md](assets/commit-message-template.md).
- Verification rules (mvn flags, multi-module reactor, scoped vs full):
  [references/verification.md](references/verification.md).

One commit per item. Stage explicit paths only. Never push, amend, or
`--no-verify`. Commit on the user's current branch — never on `main` /
`master`.

## Anti-patterns — don't

- Skipping the human checkpoint between phases.
- Running `mvn verify` after Migration Phase #1 (it WILL fail — expected
  until stabilization).
- Letting `progress.md` drift behind reality. Always rewrite the relevant
  section, then commit code + `progress.md` together — never split work and
  bookkeeping across commits.
- Re-running OpenRewrite to "fix" what a per-construct recipe couldn't
  handle.
- Spawning a subagent to invoke a code-mutating recipe **when the recipe
  itself uses AskUserQuestion** — those prompts must reach the main
  conversation. The orchestrator wraps EXECUTE_RECIPE in a subagent only
  when the recipe declares `## Subagent guidelines` and its Procedure does
  not gate on user input.
- Treating Gradle like Maven — `openrewrite` bails on Gradle today;
  verification commands assume Maven. Surface upfront for Gradle projects.
- Editing files outside the recipe's scope to "clean up" — keep diffs
  atomic.
- `git add -A` / `git push` / `git commit --amend` / `--no-verify`.

## Reference index

Shared (loaded on demand):
- [references/verification.md](references/verification.md) — mvn flags, reactor rules.
- [references/commit-cadence.md](references/commit-cadence.md) — commit rules per recipe kind.
- [references/source-access.md](references/source-access.md) — where AF4/AF5 sources resolve locally.

Recipes (one per concept):
- [references/openrewrite/openrewrite.md](references/openrewrite/openrewrite.md)
- [references/aggregate/aggregate.md](references/aggregate/aggregate.md)
- [references/event-processor/event-processor.md](references/event-processor/event-processor.md)
- [references/command-gateway/command-gateway.md](references/command-gateway/command-gateway.md)
- [references/query-gateway/query-gateway.md](references/query-gateway/query-gateway.md)
- [references/query-handler/query-handler.md](references/query-handler/query-handler.md)
- [references/read-configuration/read-configuration.md](references/read-configuration/read-configuration.md)
- [references/write-configuration/write-configuration.md](references/write-configuration/write-configuration.md)
- [references/event-storage-engine/event-storage-engine.md](references/event-storage-engine/event-storage-engine.md)
- [references/maven-profile/maven-profile.md](references/maven-profile/maven-profile.md)
- [references/debug/debug.md](references/debug/debug.md)

Unsupported-feature stops:
- [references/saga/saga.md](references/saga/saga.md)
- [references/deadline-manager/deadline-manager.md](references/deadline-manager/deadline-manager.md)

Per-recipe addenda are linked from inside each recipe's own folder. Topic
files do NOT link sideways to other topics — shared content lives at
the top of `references/` or in `assets/`.
