# Verification — scoped compile / test patterns

Shared across all recipes. Each recipe references this file for HOW to verify; the recipe's End condition decides WHAT to verify.

## Per-recipe `migration-<recipe>-<Target>` Maven profiles

`maven-compiler-plugin`'s `includes`/`excludes` are NOT exposed as `-D` user properties. The supported way to scope compilation is **POM `<configuration>`** wrapped in a Maven profile.

Each recipe call seeds ITS OWN profile (e.g. `migration-aggregate-Faculty`) — migrations are atomic, parallelizable, and individually removable. [maven-profile/maven-profile.md](maven-profile/maven-profile.md) seeds / extends the matching per-recipe profile. Run it:
- Every recipe call (recipe drives it).
- Re-invoked if the same item's include list grows.

Each per-recipe profile is idempotent — re-running augments rather than replaces. Combined verification activates multiple profiles at once: `-P migration-aggregate-Faculty,migration-aggregate-Calendar,…` (Maven merges `<includes>`).

## Standard scoped verify (iterative recipes)

```bash
./mvnw -f <target>/pom.xml test -P migration-aggregate-<AggregateSimpleName> \
  -Dtest='<FQTestClass1>,<FQTestClass2>' \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Combined across multiple migrated items: `-P migration-aggregate-Faculty,migration-aggregate-Calendar,migration-event-processor-FacultyProjector,…`.

### Why both `failIfNoTests=false` AND `surefire.failIfNoSpecifiedTests=false`

- `-DfailIfNoTests=false` — for plain `surefire:test`. Prevents fail on modules with no tests.
- `-Dsurefire.failIfNoSpecifiedTests=false` — for the explicit `-Dtest=…` filter. Prevents `No tests matching pattern "…" were executed!` on modules where the pattern matches nothing.

Multi-module reactor (`-pl <a>,<b>`) needs **both**.

## Compile-only check (one-shot recipes, fast iteration)

```bash
./mvnw -f <target>/pom.xml -P migration-event-storage-engine-<Target> test-compile \
  -DskipTests \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Use when the recipe only needs "does it compile" (e.g. event-storage-engine wiring). Runtime verification belongs to stabilization (after the SQL has been applied to the build's database).

## Full verify (stabilization only)

```bash
./mvnw -f <target>/pom.xml clean verify
```

NO `-P migration-*`. NO `-Dtest` filter. This is the goal-state assertion — green here = migration done. Stabilization drops all `migration-*` profiles.

## When to drop the `-P migration-*` flag

Once the surrounding code already compiles cleanly (typically near the end of the iterative phases or during stabilization), no per-recipe profile is needed:

```bash
./mvnw -f <target>/pom.xml test -Dtest='<FQTestClass>' -DfailIfNoTests=false
```

## Per-file vs per-package includes — pick per case

In a per-recipe `migration-<recipe>-<Target>` profile's `<includes>`:

- **Per-file** (precise, verbose): `com/example/foo/Bar.java`. Use when scattered across packages.
- **Per-package** (concise, forward-friendly): `com/example/foo/**/*.java`. Use when most changes cluster in a few packages and more files coming there.

⚠️ **Watch the wildcard**: `com/example/write/**/*.java` pulls in every file in that package — including unrelated `*Mcp.java`, `*RestApi.java` files that may still use AF4 APIs. If those fail to compile, switch to explicit per-file includes.

## RunTests.java — fallback JUnit Platform launcher

When `junit-platform-console-launcher` is not reliably published for your JUnit version (e.g. 1.12.x), use the embedded launcher in `scripts/RunTests.java`:

```bash
javac --release <N> -d <out>/runner -cp <test-classpath>:<launcher-jar> scripts/RunTests.java
java -cp <out>/runner:<out>/test:<out>/main:<test-classpath>:<launcher-jar> \
     RunTests <FQTestClass1> [<FQTestClass2> ...]
```

`<launcher-jar>` = `~/.m2/repository/org/junit/platform/junit-platform-launcher/<v>/junit-platform-launcher-<v>.jar`.

Exit code 0 = green. Exit code 1 = at least one test failed.

## Pre-flight check — has the work already been done?

Before invoking ANY recipe's procedure, run the recipe's preflight. Pattern:

1. Check compilation problems on the target file(s) (use `mcp__ide__getDiagnostics` if available; else scoped `mvn -P migration-<recipe>-<Target> test-compile`).
2. If zero compilation problems AND tests exist, run them with the standard scoped verify above.
3. If green → STOP. Ask user via `AskUserQuestion`:
   - **Skip** — treat as already migrated, move on.
   - **Deep verify** — diff current source against AF4 baseline (`git log` / `git show`) to confirm nothing was silently lost (dropped `snapshotTriggerDefinition`, missing `@EventTag`, lost `@CreationPolicy` semantics).
4. Only proceed to procedure if user picks **Deep verify**, or step 1/2 reported failures.

> Running a full procedure against an already-migrated class wastes context and risks double-edits. Skip is the most efficient outcome when an earlier pass (OpenRewrite, prior session, …) already migrated this class.

## End-condition shape (every recipe defines its own)

A recipe's End condition is a concrete check — not "feels migrated". Examples:

- **Aggregate**: (1) all aggregate tests pass via `AxonTestFixture`; (2) no compile errors in the aggregate, its commands, its events, and its primary test class.
- **EventProcessor**: (1) the projector's tests pass; (2) no compile errors in the projector class.
- **CommandGateway**: (1) no compile errors in the controller class; (2) verify decided by user (often integration test).

Recipes state their End condition in their first section. Verify against it before declaring done.
