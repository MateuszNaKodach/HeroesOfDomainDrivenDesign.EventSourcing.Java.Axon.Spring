# Recipe: debug

Triage mode — could be used as an alternative entry point to the phased migration. Source of truth is the current build output after the OpenRewrite recipe and any previous manual or delegated fixes.

Use for post-OpenRewrite build failures, AF4-to-AF5 compile triage, or when the user asks to route migration errors to dedicated recipes.

> Use as the `debug` mode of this skill — alternative to phased mode. The full phased flow lives in [../../SKILL.md](../../SKILL.md).

## Goal

Reach a compiling project by repeatedly:
1. Running main + test compilation,
2. Clustering diagnostics into likely root causes,
3. Delegating one high-leverage cluster to the matching recipe,
4. Rerunning compilation,
5. Rebuilding the queue from fresh evidence.

> Do **NOT** fix diagnostics line-by-line. ONE missing dependency, stale package, or unmigrated construct can create dozens of cascade errors. Find the **root cause group** first.

## Inputs

- target: build-tool target dir (required — defaults to current working directory if it has `pom.xml`)
- profile: optional `migration-<recipe>-<Target>` to scope the compile (only when error evidence indicates the build can't reach the migrated scope without it)

## Preflight

Build is already green? `./mvnw test-compile -DskipTests`. If yes → return Output with skip=true.

## Preconditions

- OpenRewrite has already run for the target project.
- Target is the project being migrated, NOT the AxonFramework5 repository.

## Procedure

### 1. Determine target and build tool

If user didn't name target path, use current working directory when it contains `pom.xml`, `mvnw`, `build.gradle`, `build.gradle.kts`, or `gradlew`. Otherwise ask.

Prefer wrapper scripts when present.

### 2. Run compilation as source of truth

Use main + test compilation by default — fixtures, migrated tests, test support code included without running the test suite.

Maven:

```bash
./mvnw test-compile -DskipTests -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
```

Gradle:

```bash
./gradlew testClasses -x test
```

If project has an active per-recipe migration profile (`migration-<recipe>-<TargetSimpleName>`, e.g. `migration-aggregate-Calendar`) and full compile is blocked by intentionally unmigrated code, retry with that profile **only** when error evidence says the build cannot reach the current migrated scope:

```bash
./mvnw -P migration-<recipe>-<TargetSimpleName> test-compile -DskipTests \
  -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
```

Do **NOT** treat a stale per-recipe profile (one of `migration-*`) as truth. If the profile excludes files needed by current root-cause group, route to maven-migration-profile recipe.

### 3. Normalize diagnostics

For each compiler diagnostic, capture:

- file path + line number,
- module/source set if visible,
- error kind (`cannot find symbol`, incompatible types, missing package, method signature mismatch, missing annotation attribute, dependency/build failure),
- symbol/type/method/package named in the message,
- nearby source context,
- primary vs cascading.

When Maven/Gradle fails **before** Java compilation, classify separately as build-tool, JDK, dependency, or profile-scope. Do NOT route non-Java build failures to construct migration recipes.

### 4. Cluster by root cause

Read all errors. Group by smallest migration action that removes largest cascade. Good clusters share one of:

- same source file or construct,
- same missing AF4 package/type,
- same stale gateway/handler API,
- same test fixture API,
- same configuration API,
- same event-store/storage-engine API,
- same dependency/profile/JDK problem.

**Prefer one root-cause group over many superficial message groups.** Example: `AggregateLifecycle.apply`, `AggregateTestFixture`, missing `@Command`, missing `@EventTag` around the same aggregate → ONE aggregate cluster, not four.

Common shapes:

- **Same missing symbol** — `cannot find symbol: class X` / `package Y does not exist` across many files → ONE root cause.
- **Same call-site shape** — `incompatible types: CommandResult cannot be converted to CompletableFuture` repeated → command gateway not migrated.
- **Same annotation** — `cannot find symbol: @QueryHandler` (AF4 import) → query handler not migrated.
- **Same import** — many files import `org.axonframework.config.Configuration` (AF4) → read-configuration not migrated.

### 5. Pick one cluster and route

Priority:

1. build/JDK/dependency/profile issues that prevent meaningful Java diagnostics,
2. dependency or migration-profile gaps that hide intended target scope,
3. construct roots that create many cascades,
4. production sources before test sources when both fail independently,
5. test fixtures tied to an already-migrated construct,
6. isolated stale imports / obvious leftovers.

Pick **one cluster, one recipe**. Run on highest-leverage file in the cluster.

| Cluster shape | Route to |
|---|---|
| `cannot find symbol: @QueryHandler` (AF4 import) | [../query-handler/query-handler.md](../query-handler/query-handler.md) |
| `cannot find symbol: @Aggregate`, `@AggregateRoot`, `@AggregateIdentifier`, `@TargetAggregateIdentifier` | [../aggregate/aggregate.md](../aggregate/aggregate.md) |
| `cannot find symbol: @ProcessingGroup`, `@EventHandler` (AF4) | [../event-processor/event-processor.md](../event-processor/event-processor.md) |
| `incompatible types: CommandResult cannot be converted to CompletableFuture` | [../command-gateway/command-gateway.md](../command-gateway/command-gateway.md) |
| `cannot find symbol: ResponseType`, `multipleInstancesOf` | [../query-gateway/query-gateway.md](../query-gateway/query-gateway.md) |
| `cannot find symbol: org.axonframework.config.Configuration`, `EventProcessingConfiguration` | [../read-configuration/read-configuration.md](../read-configuration/read-configuration.md) |
| `cannot find symbol: ConfigurerModule`, `DefaultConfigurer.defaultConfiguration`, `EventProcessingConfigurer` | [../write-configuration/write-configuration.md](../write-configuration/write-configuration.md) |
| `cannot find symbol: JpaEventStorageEngine`, `EmbeddedEventStore` | [../event-storage-engine/event-storage-engine.md](../event-storage-engine/event-storage-engine.md) |
| Active `migration-<recipe>-<Target>` profile includes/excludes wrong, javac compiles unrelated transitive files, profile cannot reach migrated scope | [../maven-profile/maven-profile.md](../maven-profile/maven-profile.md) |

### 6. Rerun compilation

```bash
./mvnw test-compile -DskipTests -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
```

If error count dropped significantly, the cluster was correct — repeat from step 3 with fresh evidence.

If error count unchanged or grew, the recipe didn't reach the actual root cause. Re-cluster with fresh evidence; the original cluster was masking a deeper issue.

### 7. Stop conditions

- Build is green → done.
- Next cluster is blocked on missing product intent or unsupported AF5 feature → stop with concrete diagnosis. Name the missing migration path, unsupported feature, or product decision needed.
- Only manual/out-of-scope errors remain → record in `learnings.md`, defer.
- Cluster picks repeatedly without progress → escalate to user via `AskUserQuestion`:
  - **Show me the diagnostic dump** — surface raw error output.
  - **Skip these errors and stash them** — defer to stabilization; orchestrator records the deferral.
  - **Stop debugging** — exit recipe.

## End condition

Build is green for the chosen scope:
- Main + test compile OK without any `migration-*` profile, OR
- The user accepts a known-deferred subset (orchestrator records the deferral via a decision-only commit).

## Output

- target: <build-tool target dir>
- decisions: per cluster the recipe handled
    - cluster: <signature>
    - routed-to: <recipe>
    - defer: <reason> (only when applicable)
- needs-user-decision: <true | false> (true when the loop hit a "no progress" stop condition)
- needs-user-decision-reason: <text> (e.g. "two clusters in a row picked the same recipe with no error-count drop")
- notes: optional free text — the orchestrator copies any non-obvious lessons here into `learnings.md`

## Limited direct fixes

Directly edit only when no sibling recipe owns the construct AND the fix is small, mechanical, and verified by compilation. Allowed:

- stale imports left behind after a sibling migration,
- obvious package/class rename leftovers not covered by any sibling recipe,
- small dependency, plugin, profile, or wrapper-command fixes,
- deleting an unused import after a delegated fix.

NEVER directly rewrite a construct that has a dedicated sibling recipe. Aggregate, gateway, handler, configuration reader/writer, event processor, storage engine, or migration profile issue → delegate.

For manual or unsupported work, stop with a concrete diagnosis instead of guessing.

## Learnings — surface non-obvious fixes

The orchestrator (NOT this recipe) writes `learnings.md`. After a non-obvious delegated or direct fix, surface a concise dated entry in Output `notes`:

- root-cause cluster,
- delegated recipe or direct-fix reason,
- files/classes affected,
- verification command and result,
- generic lesson for later clusters.

Do NOT surface routine import cleanup unless it prevented a misleading cascade.

## Report format

When presenting triage or handoff:

```markdown
## Compile Command
`<command>` -> `<green|failed|blocked>`

## Queue
1. `<group id>` -> `<route>`
   Root cause: `<short hypothesis>`
   Evidence: `<files/classes + representative diagnostics>`
   Action: `<delegate to recipe X on class/file Y>` or `<small direct fix>`
   Verify: `<command>`

## Current Action
Delegating `<group id>` to `<recipe>` because `<reason>`.

## Blocked / Manual
- `<cluster>`: `<why no sibling recipe/direct fix applies>`

## Learnings Consulted
- `<target learnings entries or sibling examples used>`
```

Keep queue complete enough for the human to understand the plan, but fix only the current highest-leverage group before rerunning compilation.

## Dry-check examples

- `cannot find symbol: variable AggregateLifecycle` / `AggregateLifecycle.apply(...)` in an aggregate → aggregate recipe.
- `package org.axonframework.test.aggregate does not exist` / `AggregateTestFixture` → aggregate recipe.
- `package org.axonframework.commandhandling.gateway does not exist` in REST controller → command-gateway recipe.
- `ResponseTypes` / AF4 `QueryGateway` dispatch in input adapter → query-gateway recipe.
- `package org.axonframework.queryhandling does not exist` for `@QueryHandler` → query-handler recipe.
- `ConfigurerModule` / `EventProcessingConfigurer` no longer resolves in config class → write-configuration recipe.
- `JpaEventStorageEngine` / `EmbeddedEventStore` wiring fails → event-storage-engine recipe.
- Maven compiles unrelated broken files under an active `migration-*` profile → maven-profile recipe.

## Anti-patterns

- Fixing one error line at a time without clustering — wastes context.
- Ignoring active `migration-*` profile(s) when present — runs unrelated AF4 code into the compile.
- Running `./mvnw verify` instead of `test-compile` — runtime errors hide compile-time root causes.
- Editing files outside the cluster's scope to "tidy up" — defeats clustering.
- Routing non-Java build failures (JDK, dependency resolution, plugin errors) to construct migration recipes — those need build-tool fixes first.
- Directly rewriting a construct that has a dedicated sibling recipe instead of delegating.
