# Recipe: openrewrite

Drives the published `org.axonframework:axon-migration` OpenRewrite recipes against the target project. Bulk mechanical leg of AF4 → AF5 — package renames, FQN moves, Maven coordinate swaps, BOM swaps, dependency version bumps, Java compiler-target bump.

> **The post-run state is expected to be non-compiling.** OpenRewrite runs the *first* mechanical step. The remaining AF4→AF5 surface (handler shapes, async dispatch, configuration model, aggregate model) is judgment-driven and lives in the per-construct recipes. Compile errors after this recipe are the work the per-construct recipes exist to do, scoped iteratively via [../maven-profile/maven-profile.md](../maven-profile/maven-profile.md).

## Goal

Bulk mechanical rewrites applied:
- Java compiler target bumped (Spring Boot 4 / AF5 minimum).
- Spring Boot upgraded to 3.5.x (or untouched if already ≥3.5 / 4.x).
- AF4 package renames inside `org.axonframework.*` (and `io.axoniq.framework.*` for commercial).
- Maven coordinates / BOM swapped to AF5.

## Inputs

- target: project root path (required)
- license: `free-af5` | `axoniq-commercial` (optional — asked at runtime if absent; recommended driven from signal scan)
- scope: `top-level` | `per-module-subset:<list>` (optional — defaults to `top-level`)

## Preflight

1. Project already at AF5? Check `pom.xml` BOM / dep versions.
2. Recent OpenRewrite run already committed? Check `git log --oneline | grep af5-migration | head -5`.
3. If both clean → return Output with skip=true. AskUserQuestion only when user explicitly wants to re-run with a different recipe choice.

## Subagent guidelines

- subagent_type: general-purpose
- isolation: worktree
  # Bulk transform touches many files; worktree gives easy rollback if a recipe choice
  # turns out wrong (e.g. user wanted free but ran commercial).
- prompt-framing: |
  Run an OpenRewrite bulk migration. The user has already chosen license and scope
  (passed in inputs). Print the Maven invocation verbatim BEFORE running. Do not
  attempt to fix compile errors after the run — those belong to per-construct recipes.
- parallelism: single

## Procedure

1. Validate target.
   - path exists, has `pom.xml` or `build.gradle*`, is a git repo
   - refuse if path is the AxonFramework repo itself (`git remote get-url origin` matches)
2. Pre-flight clean tree check.
   - dirty → AskUserQuestion: stash / commit-first / abort
3. Detect build tool.
   - `pom.xml` only → Maven; continue
   - `build.gradle*` only → bail with clear message; emit Output decisions=[bail: gradle-not-supported]
   - both → ask user which to drive
4. Verify `JAVA_HOME` can compile target (see Step 3.5 details below).
5. Inspect free-vs-commercial signals (see Step 4a).
6. Decide recipe path.
   - strong commercial signal → Path B (axoniq-commercial)
   - no commercial signal → Path A (free-af5)
   - weak / mixed → AskUserQuestion (Path B recommended-default)
7. Run path Steps (see ### Path A or ### Path B).
8. Verify against ## End condition.
9. Emit ## Output.

### Step 3.5 — Verify `JAVA_HOME` can compile the target

`rewrite-maven-plugin:run` binds to `process-test-classes` → **project must compile under active `JAVA_HOME` before recipe runs**. The recipe also bumps the project's `<java.version>` (currently to 25 via `UpgradeJavaVersion`), so post-recipe `./mvnw` calls need an even-newer JDK.

```bash
grep -E '<java\.version>|<release>|<source>|<target>|<maven\.compiler\.' <target>/pom.xml
echo "$JAVA_HOME"; java -version 2>&1
```

Decision rules:
- Active `java -version` **lower than** project's `<java.version>` → build fails with `release version N not supported` before the recipe runs. Ask user via `AskUserQuestion` to pick installed JDK high enough (run `/usr/libexec/java_home -V` on macOS) and re-invoke prefixed with `JAVA_HOME=<picked>`, **or** install newer JDK first and pause.
- Active JDK fine today but lower than what `UpgradeJavaVersion` will bump to → warn that *post-recipe* `./mvnw` calls need newer JDK on `JAVA_HOME`. Don't block; record for the final summary.
- `JAVA_HOME` unset → JDK that resolves via shell `java -version` is what Maven picks up. Same rules.

This step exists because the failure is **silent and misleading** — the OpenRewrite plugin reports a Maven compile failure that looks like a project bug, when `JAVA_HOME` is the culprit.

### Step 4a — Inspect free-vs-commercial signals

```bash
# AF4 dependency footprint — narrow to dependency declarations.
grep -RE 'org\.axonframework' --include='pom.xml' --include='build.gradle*' <target>

# Source-level signals for commercial-only features.
grep -RE 'AxonServer|DistributedCommandBus|SequencedDeadLetterQueue|DeadLetter' \
     --include='*.java' --include='*.kt' <target>/src 2>/dev/null
```

Classify into three buckets:

- **Strong commercial signal** — dependency on `axon-server-connector`, `axon-distributed-commandbus-*`; references to `AxonServerConfiguration`, `DistributedCommandBus`, `SequencedDeadLetterQueue` from `org.axonframework.eventhandling.deadletter.*`, or `DeadLetter` types explicitly handled.
- **Weak commercial signal** — Spring Boot autoconfig wiring an Axon Server profile but no source references; commented-out DLQ snippets; test-only references.
- **No commercial signal** — only core `axon-messaging` / `axon-modelling` / `axon-eventsourcing` / Spring Boot starter deps, no source references to dropped features.

Record bucket — drives recipe recommendation in path selection.

### Path A — UpgradeAxon4ToAxon5 (free)

#### Condition

- License input is `free-af5`, OR step 4a found **no commercial signal** AND user picked free.

#### Steps

1. Recipe id: `org.axonframework.migration.UpgradeAxon4ToAxon5`.
2. Scope: free AF5 (Apache 2.0). Bumps Java compiler target, upgrades Spring Boot to 3.5.x, renames inside `org.axonframework.*`, bumps Maven coordinates, swaps BOM.
3. Construct + run the Maven invocation (see "Maven invocation template" below).
4. Capture resolved artifact version from Maven log (not `LATEST` literal).
5. Continue to step 6 (Report) below.

> **Eligibility:** Target app does NOT use Axon Server, sequenced DLQ, or `DistributedCommandBus`. Those features were dropped from free AF5.

### Path B — UpgradeAxon4ToAxoniq5 (commercial)

#### Condition

- License input is `axoniq-commercial`, OR step 4a found **strong commercial signal**, OR user explicitly picked Axoniq commercial.

#### Steps

1. Recipe id: `org.axonframework.migration.UpgradeAxon4ToAxoniq5`.
2. Scope: commercial Axoniq AF5 (`io.axoniq.framework.*`). Composes the free leg first, then layers commercial-only rewrites: Axon Server connector, DLQ, distributed messaging, BOM swap to `axoniq-framework-bom`, Spring Boot starter swap to `axoniq-spring-boot-starter`.
3. Construct + run the Maven invocation (see "Maven invocation template" below).
4. Capture resolved artifact version from Maven log.
5. Continue to step 6 (Report) below.

> **Eligibility:** Target app uses any of: Axon Server connector, sequenced DLQ, `DistributedCommandBus`. Recommended default if unsure — the free recipe alone leaves those projects non-compiling.

### Maven invocation template (used by Path A and Path B)

Both paths use the same invocation; only `recipe1,recipe2,…` differ.

Both top-level recipes include `org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_5`. Projects already at Spring Boot 3.5.x or later (including 4.x) are unaffected — OpenRewrite never downgrades version numbers, and source-level sub-recipes are idempotent against already-migrated code.

Per-module recipes (Group A free, Group B commercial) are listed in the published `migration/README.md` of the recipes JAR. Use them when a top-level run is overkill — small isolated module, or only one slice (messaging, Spring Boot starter) needs migrating.

### Step 5 — Construct + run the OpenRewrite invocation

Maven invocation template:

```bash
mvn -U -f <target>/pom.xml \
    org.openrewrite.maven:rewrite-maven-plugin:run \
    -Drewrite.recipeArtifactCoordinates=org.axonframework:axon-migration:5.1.1-SNAPSHOT \
    -Drewrite.activeRecipes=<recipe1>,<recipe2>,...
```

Pitfalls and rules:
- `-U` forces fresh resolution of recipe artifact (migration JAR ships frequent fixes). Keep it.
- `-Drewrite.activeRecipes` is comma-separated. **Pitfall**: legacy unprefixed form (`-DactiveRecipes=...`) is silently ignored on `rewrite-maven-plugin` 6.x — build returns `BUILD SUCCESS` with `Using active recipe(s) []` in the log and rewrites nothing. Always use the `rewrite.` prefix.
- `LATEST` for `recipeArtifactCoordinates` is reproducible **per run, not across days**. Recipes are deterministic and bug fixes land continually. Capture resolved version from Maven log so user can pin it later.
- If `LATEST` fails to resolve → fall back to explicit current release (e.g. `5.1.0` — confirm against `migration/pom.xml`'s `${revision}` if in doubt).
- Run from target dir or with `-f <target>/pom.xml`. Never `cd` into target if user works from another directory.
- **Print the command verbatim before executing.** It's destructive against the working tree; user should see what's about to happen even though the clean-tree step confirmed the baseline.

### Step 6 — Report what changed

```bash
git -C <target> status --short
git -C <target> diff --stat
```

Surface:
- Recipe(s) ran + resolved artifact version (read from Maven log — not `LATEST` literal).
- File-count summary (added / modified) from `git diff --stat`.
- Top 3 hot-spot modules / paths.
- **Set expectation explicitly: project is not expected to compile yet.** Don't run `mvn compile` / `mvn test` to "verify" the recipe — they fail by design until per-construct recipes run.
- **Behavior-change watch**: scan OpenRewrite output and rewritten files for removed parameters, dropped methods, signature changes. Tell user about everything that may need their attention.
- If `JAVA_HOME` step flagged a future JDK bump → remind user `JAVA_HOME` must move to that JDK before next `./mvnw` call.

### Step 7 — STOP

DO NOT run `mvn compile` / `mvn verify`. The orchestrator owns the commit and the next-step checkpoint with the user.

## End condition

1. OpenRewrite recipe completed without runtime error.
2. `git diff --stat` shows the expected set of mechanical rewrites for the chosen path.
3. The Output `decisions` list captures `recipe-name`, resolved version, license, and scope.

## Output

- target: <target project root>
- decisions:
    - license: <free-af5 | axoniq-commercial>
    - recipe-name: <UpgradeAxon4ToAxon5 | UpgradeAxon4ToAxoniq5>
    - resolved-version: <e.g. 5.1.0>
    - scope: <top-level | per-module:[...]>
    - behavior-changes-flagged: <yes | no>
- needs-user-decision: false
- notes: <list of behavior-change warnings if any; "" if none>

> Subject line for the orchestrator's commit (chosen by orchestrator based on Output):
> `chore(af5-migration): apply OpenRewrite recipe <recipe-name>@<version> (Migration Phase #1)`

## Caveats

- **Don't run inside the AxonFramework repo.** Recipes are for consumer projects — running against the framework rewrites framework source. Step 1 enforces this.
- **`LATEST` is reproducible per run, not across days.** Capture resolved version for deterministic re-runs.
- **Free vs commercial is a license choice, not a version bump.** Both top-level recipes target the same release line; picking one doesn't lock into a different upgrade cadence later.
- **Spring Boot upgrade is safe on already-modern projects.** `UpgradeSpringBoot_3_5` is a no-op when project is already on 3.5.x or newer (incl. 4.x). OpenRewrite never downgrades.
- **Compile failures after recipe are expected, not a problem to chase.** This recipe runs *only* the first mechanical step. Async-everywhere handler shapes, new configuration model, new aggregate model, and most judgment-driven rewrites are intentionally out of scope and leave project non-compiling until per-construct skills run.
- **Gradle targets are out of scope today.** Step 3 hands off to user with instructions rather than emitting partial Gradle config.
- **`-DactiveRecipes` (no `rewrite.` prefix) silently does nothing** on `rewrite-maven-plugin` 6.x. Always use `-Drewrite.activeRecipes`.

## Reference docs

- `migration/README.md` (in `org.axonframework:axon-migration` JAR) — full per-module recipe inventory (Group A free, Group B commercial), Maven invocation patterns, wrapper-recipe pattern for customizing `targetVersion` on Java compiler bump.
- `docs/reference-guide/modules/migration/pages/paths/index.adoc` — human-language migration paths the recipes implement (import & package changes table).
