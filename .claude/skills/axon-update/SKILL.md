---
name: axon-update
description: >-
  Update an Axon Framework 5 / Axoniq Framework 5 application to a newer PATCH or
  MINOR release (for example 5.3.2 to 5.3.3, or 5.3.x to 5.4.0) and prove it still
  behaves the same: read every release note in between (both the AxonFramework and
  axoniq-framework repos), bump the versions, compare tests, deprecations, dependencies
  and logs against a baseline worktree, root-cause each failure down to the upstream
  commit, apply minimal documented workarounds, and run an "old data, new version"
  compatibility check against the app's real event store, token store and Axon Server
  contexts in Docker. Always ends with a written upgrade report in the repo. Use this
  skill whenever the user asks to update, upgrade, bump or move an app to a newer Axon
  5.x / Axoniq 5.x version (including RCs and SNAPSHOT-to-release moves), asks whether
  a new Axon 5 release breaks their app, wants to check event processor / token /
  event-store compatibility after an Axon version change, or wants an upgrade report,
  even if they only say "bump axon". Not for Axon Framework 4 to 5 (that is a migration).
---

# axon-update

Update an Axon Framework 5 (AF5) / Axoniq Framework 5 application between patch or
minor releases, verify that nothing changed for the app and its stored data, and write
down everything that did change.

The goal is not "it compiles". The goal is a defensible answer to: *does the app
behave exactly as before, on the data it already has, and if not, why, since which
upstream commit, and what did we do about it?* Every step below produces evidence for
the report.

## Scope check (do this first)

Read the current and target versions (step 1). Then:

- **Major move (4.x to 5.x):** stop. That is a migration, not an update: package
  renames, aggregate-to-entity model, new configuration API. Point the user to the Axon
  migration tooling (the OpenRewrite `UpgradeAxon4ToAxoniq5`-style recipes and the Axon
  migration paths in the reference guide) instead of this skill.
- **Patch or minor within 5.x** (including `-RCn`, `-SNAPSHOT` to release, release to
  RC): continue.
- **Downgrade:** continue, but treat every "new" release note as something being
  removed.

## Ground rules

- **Behavior first.** If the update forces a code change, the change must keep the
  previous observable behavior, and the report must say what changed and why. Do not
  "improve" things the update did not force (record them as follow-ups instead).
- **Evidence over guesses.** Every regression gets: before vs after behavior, the
  exact error, affected call sites, the root cause in framework sources, and the
  upstream commit/PR that introduced it. Read the sources jar; do not reason from
  memory about framework internals.
- **Do not open upstream issues, PRs or comments** unless the user explicitly asks.
  Capture the analysis in the report so the user can decide.
- **Do not touch shared state you did not create.** Use your own worktree, your own
  containers and databases, and clean them up (or tell the user what is left running).
- Write plain ASCII punctuation in code, comments, commits and the report unless the
  repo clearly does otherwise.

## Bundled resources

Set an absolute variable to this skill's directory (adjust the path to wherever the skill lives),
so commands also work from the baseline worktree:

```bash
AXU="$(pwd)/.claude/skills/axon-update"
```

| Resource | Use it for |
|---|---|
| `scripts/detect.py` | Step 1: build tool, Axon artifacts and versions, latest versions on Maven Central, storage/processor configuration per profile, engine/token/snapshot classes in code |
| `scripts/release_notes.py` | Step 2: download and triage release notes of both repos between two versions |
| `scripts/dep-list.sh` | Step 4: resolved runtime dependency list (Maven or Gradle) for diffing |
| `scripts/compat-lib.sh` | Step 7: start/stop app jars, health wait, SQL, token dump, state snapshots and diffs, log scan |
| `reference/release-notes.md` | How to triage notes: which items can change behavior silently |
| `reference/root-cause.md` | Finding the upstream commit, sources jars, workaround patterns |
| `reference/compat-recipe.md` | The "old data, new version" procedure per storage mode, Axon Server setup |
| `reference/example-5.3-to-5.4.md` | A complete worked example (one real app); read when unsure what "done" looks like |
| `assets/upgrade-notes-template.md` | The skeleton of the upgrade notes written in step 8 (mandatory) |

Keep a scratch directory outside the repo for logs, notes, jars and snapshots (the
session scratchpad if there is one, otherwise a temp dir). Call it `$SCRATCH` below.

## Workflow

### 1. Detect the current state

```bash
python3 $AXU/scripts/detect.py          # from the project root; --offline skips Maven Central
```

It prints the build tool, every Axon/Axoniq coordinate with its declared version (or
the property/catalog entry that drives it), the latest releases on Maven Central, and
a per-profile view of storage-related configuration plus the engine, token-store,
snapshot-store and dead-letter classes the code wires itself.

Decide the target version (the user's, or the latest release). Note which version
properties must move together: AF (`org.axonframework*`) and Axoniq
(`io.axoniq.framework*`) share version numbers in 5.x and are released together; keep
them on the same version unless the user says otherwise. Check the git status is
clean and note the current commit.

If the current version is a `-SNAPSHOT` built locally (for example from a feature
branch), the build may depend on artifacts that only exist in the local Maven
repository. Do not run with `-U` / `--refresh-dependencies` in the baseline, it can
replace them with remote snapshots. After moving to a release this caveat is gone; say
so in the report.

### 2. Read every release note in between

```bash
python3 $AXU/scripts/release_notes.py --from <current> --to <target> --out $SCRATCH/notes
```

It downloads the notes of every `axon-X.Y.Z` (AxonFramework) and `axoniq-X.Y.Z`
(axoniq-framework) release after `<current>` up to and including `<target>`, RCs
included, drops dependency-bump noise, and writes `TRIAGE.md` with the items that
match behavior-sensitive keywords. Read `TRIAGE.md` fully, then skim the raw notes
for anything the keywords missed. `reference/release-notes.md` explains what to look
for and why; read it the first time.

For each flagged item, check the app: does it use the feature, override the bean,
set the property, rely on the old default? Write a one-line verdict per relevant item
(affects / does not affect, and why). These verdicts become the "checked and not
affecting" and "behavior changes" sections of the report.

When an item is unclear, open the linked issue/PR (`gh issue view`, `gh pr view`).

### 3. Baseline worktree

Create a detached worktree of the current commit outside the repo and run the full
test suite there before changing anything:

```bash
git worktree add --detach $SCRATCH/baseline HEAD
(cd $SCRATCH/baseline && ./mvnw -B test > $SCRATCH/baseline-test.log 2>&1)   # or ./gradlew test
# on a locally built -SNAPSHOT: ./mvnw -nsu ... (Gradle: --offline) so local artifacts are kept
```

Record the totals (tests run, failures, errors, skipped). If the baseline is already
red, stop and tell the user; an update cannot be judged against a broken baseline.
Tests often need Docker (Testcontainers); if the daemon is down, start it or ask.
Keep this worktree: it builds the "old" jar for step 7. Remove it at the end with
`git worktree remove --force`.

### 4. Bump, compile, compare

1. Change only the version declarations found in step 1 (Maven properties or BOM
   import, Gradle version catalog or `ext`/`extra` properties). Keep all Axon/Axoniq
   coordinates on one version.
2. Compile main and test sources with deprecation warnings visible, in both trees, and
   diff the warning lists. New deprecations go into the report with the replacement
   API (read the `@deprecated` Javadoc in the sources jar).
   - Maven: `./mvnw -B clean test-compile -Dmaven.compiler.showDeprecation=true -Dmaven.compiler.showWarnings=true`
   - Gradle: temporarily set `options.deprecation = true` on `tasks.withType(JavaCompile)`
     (Kotlin reports deprecations as warnings by default), then `./gradlew clean testClasses`
3. Diff the resolved runtime dependencies:
   ```bash
   bash $AXU/scripts/dep-list.sh $SCRATCH/deps-new.txt
   (cd $SCRATCH/baseline && bash $AXU/scripts/dep-list.sh $SCRATCH/deps-old.txt)
   diff $SCRATCH/deps-old.txt $SCRATCH/deps-new.txt
   ```
   Look for artifacts that appeared or disappeared (a starter that stopped pulling a
   connector, a new module that enables a feature by default), not just version moves.
4. Run the full test suite, save the log as `$SCRATCH/new-test.log`, compare totals with
   the baseline.
5. Diff WARN/ERROR log lines between the two test logs (normalize timestamps, PIDs,
   UUIDs and numbers first: source `$AXU/scripts/compat-lib.sh` and run
   `axc_logdiff $SCRATCH/baseline-test.log $SCRATCH/new-test.log`). New warnings are often the only sign of a changed default.

### 5. Root-cause every failure

For each failing test or new error, follow `reference/root-cause.md`:

1. Get the stack trace from the surefire/Gradle reports.
2. Read the framework code at the failing frames in the sources jar of the new
   version, and compare with the old version.
3. Find the upstream change: history of the file on GitHub, `gh api repos/<repo>/compare/<old-tag>...<new-tag>`,
   the PR that merged it, whether it is in earlier patch tags of the previous line
   (to know the first affected release), and whether `main` still has it.
4. Prove the cause with a minimal experiment (for example temporarily change one call
   site, run one test, revert).
5. Decide the fix:
   - The app relied on something that legitimately changed: adapt the app.
   - Framework regression: add the smallest workaround that restores the old behavior.
     Prefer **one central, removable, switchable** workaround (an interceptor,
     decorator, bean override, or property) over editing many call sites; document in
     its Javadoc the upstream commit, the symptom, and when to delete it; guard it with
     a property so the user can switch it off to check a future version.
6. Re-run the affected tests, then the full suite.

Also check what the failure means at runtime, not just in tests: a "failed" call that
still committed its side effects is worse than a clean failure (clients retry). Say so
in the report when that is the case.

### 6. Review behavior changes that do not fail tests

Go through the step 2 verdicts that say "affects". Typical silent changes: new
defaults switched on (timeouts, multi-tenancy, local dispatch shortcuts), autoconfig
that now registers or no longer registers a bean, changed bean naming or uniqueness,
token store or sequencing defaults, licensing/entitlement behavior, new log warnings.
Decide per item: keep (document), or configure back to the old behavior (document
why). Do not change behavior the user did not ask to change without saying so.

### 7. Old data, new version (compatibility run)

Tests start from empty stores. Production does not. Follow
`reference/compat-recipe.md`:

1. Inspect the app to list every storage mode it supports and which one is the
   default (event storage engine, Axon Server aggregate-based vs DCB context, token
   store, snapshot store, dead-letter queues, processor types), from `detect.py`
   output plus reading the configuration classes. Test every mode the app can run in;
   the default first.
2. For each mode: start the infrastructure in Docker, using a separate database (and
   Axon Server context or container) per mode so token types never mix.
3. Run the **old** jar, create realistic data through the app's own API (commands
   that touch several entities, trigger automations/sagas, feed projections), then
   snapshot tokens, events, read models and the DB schema with `compat-lib.sh`.
4. Stop it. Run the **new** jar on the same stores. Verify: tokens unchanged and
   re-claimed, nothing reprocessed, entities sourced from old events still enforce
   their rules and accept new events, snapshots, processors advance, subscription
   queries, dead letters, schema unchanged.
5. Optionally roll back: old jar on data written by the new one.
6. For distributed buses (Axon Server), think about mixed old/new nodes during a
   rolling update. If you did not test it, say "not tested" and give your analysis.

### 8. Write the report (mandatory)

Copy `assets/upgrade-notes-template.md` to the repo, by default
`docs/UPGRADE-AXON-<target>.md` (follow the repo's docs convention if it has one, and
create `docs/` if missing). Fill every section; delete a section only when it truly does
not apply, and say "none" rather than leaving placeholders. Put commit hashes, PR
numbers and exact error messages in; they are what makes the report useful later.
Check that no placeholder text remains (`grep -n '<' docs/UPGRADE-AXON-*.md` and review
the hits).

### 9. Finish

- Run the full suite one last time on the final code.
- Clean up: remove the baseline worktree, stop app processes, remove containers you
  created, or list what is still running and how to stop it.
- Commit only if the user asked (or the repo's instructions say so). Use small commits
  that explain why and keep every commit green: the version bump together with any
  workaround it requires, the report separately. Follow the repo's and user's rules on
  commit attribution.
- Summarize in chat: final test totals vs baseline, each regression with its upstream
  origin and the workaround, behavior changes kept, the compatibility result per
  storage mode, untested risks, and the report path. Lead with the answer (does it
  work, what did it take).

## Gotchas seen in practice

- **RC versus GA.** Problems seen on an RC may be fixed in the GA (and new ones may
  appear). Re-verify everything against the exact target version.
- **Ambiguous beans after an update.** New named components of an existing type (for
  example per-bus `UnitOfWorkFactory` variants) can make by-type injection ambiguous
  in Spring. Check whether a later patch fixed it before working around it.
- **Starter content changes.** A dependency turning `optional` in an autoconfigure
  module silently drops it from the starter; the dependency diff catches this.
- **Stale local Docker images.** `image:latest` resolves to whatever was pulled last;
  inspect it (`docker image inspect`) before claiming which server version you tested.
- **Axon Server contexts.** An unlicensed node cannot create extra contexts; a fresh
  dev-mode container initialized with `POST /v2/cluster/init?dcb=true` gives a DCB
  `default` context. Some server versions ignore the flag; check with
  `GET /v1/public/context` or the server log.
- **Token type mixing.** Tokens written by a JPA/JDBC event store are not readable
  against Axon Server and vice versa. Never reuse one database across modes.
- **Postgres token column.** The JPA token store maps `token` to an `oid` large
  object; read it with `lo_get(token)`. `compat-lib.sh` handles both `oid` and `bytea`.
- **zsh word splitting.** A command stored in a variable (`PG="docker exec ..."`) does
  not split in zsh. Use shell functions (as `compat-lib.sh` does).
- **In-memory snapshot stores** are empty after a restart, which is good for the
  compatibility run: the new version must source entities fully from old events.
