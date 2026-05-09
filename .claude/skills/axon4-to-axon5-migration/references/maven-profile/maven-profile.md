# Recipe: Maven `migration-*` profiles (per-recipe, scoped)

Adds (or extends) a **per-recipe, per-target** Maven profile in the target project that scopes `maven-compiler-plugin` compilation and tests to ONE migration unit. Each recipe call gets its own profile so migrations are atomic, parallelizable, and individually removable.

**Construct-agnostic.** Aggregates are one example; same pattern works for any AF4→AF5 recipe.

## Goal

A `<profile id="migration-<recipe>-<SimpleTargetName>">` exists in the relevant `pom.xml`:
- Self-contained: carries its own AF5 dep declarations (versioned via a shared root property) AND its own `<includes>` / `<testIncludes>`.
- Independent of every other `migration-*` profile in the same pom.

Profile id grammar:

```
migration-<recipe-id>-<SimpleTargetName>
```

| Recipe | Profile id format | Example |
|---|---|---|
| aggregate | `migration-aggregate-<Aggregate>` | `migration-aggregate-Calendar` |
| event-processor | `migration-event-processor-<Processor>` | `migration-event-processor-WhenCreatureRecruitedThenAddToArmy` |
| command-gateway | `migration-command-gateway-<Class>` | `migration-command-gateway-BuildDwellingRestApi` |
| query-gateway | `migration-query-gateway-<Class>` | `migration-query-gateway-DwellingQueryRestApi` |
| query-handler | `migration-query-handler-<Class>` | `migration-query-handler-DwellingProjection` |
| read-configuration | `migration-read-config-<Class>` | `migration-read-config-EventProcessorAdmin` |
| write-configuration | `migration-write-config-<Class>` | `migration-write-config-AxonConfig` |
| event-storage-engine | `migration-event-store-<Bean>` | `migration-event-store-AxonConfig` |

`<SimpleTargetName>` = simple class name only (no package). Java identifier chars only — strip generics, anonymous suffixes, etc.

## Inputs

- recipe: invoking recipe id (required — `aggregate`, `event-processor`, …)
- target: simple class name of the migrated target (required)
- include: list of repo-relative file paths to add to the profile's `<includes>` / `<testIncludes>` (required; recipe is the authority on which files it touched)

## Preflight

1. Profile `<profile id="migration-<recipe>-<Target>">` already exists in the relevant `pom.xml`?
2. If yes AND its `<includes>` covers the current changed-set for this target → return Output with skip=true.
3. If yes BUT includes are stale → augment (don't replace).

## Why per-recipe profiles, not one shared profile

Earlier iteration of this skill used a single `<profile id="migration">` that every recipe appended `<include>` entries to. Three problems pushed us off that:

1. **Sequential bottleneck.** Two parallel migrations writing the same `<includes>` block race on file edits.
2. **No clean rollback.** A failed migration leaves stale `<include>` entries mixed in with successful ones — you can't drop one without grepping for everything it added.
3. **Bisect-hostile.** The shared profile keeps mutating, so `git bisect` across migration commits sees an unstable scope.

Per-recipe profiles fix all three: each migration is one self-contained block; deleting the block reverses the change; concurrent migrations touch different XML elements.

The cost — duplicated AF5 dep declarations across many profiles — is real but cheap to mitigate via the shared `${axon5.version}` property below.

## Project-level setup (one-time, before the first recipe runs)

Add ONE root-level `<properties>` entry pinning the AF5 version. Every per-recipe profile references it.

```xml
<properties>
    <axon5.version>5.1.1-SNAPSHOT</axon5.version>
    <!-- existing properties -->
</properties>
```

Use `5.1.1-SNAPSHOT` (or the AF5 version the user has chosen) — never `LATEST`, never bare. If the project already pulls anything from `org.axonframework`, keep its existing AF4 version property as-is (e.g. `<axon.version>`); the AF4 deps stay on it. The `<axon5.version>` property is for the migration profiles only.

`scripts/ensure_axon5_version_property.py` adds the property if missing (idempotent).

## When to run

- **First** recipe call per target → create a new `migration-<recipe>-<Target>` profile.
- **Re-running** the same recipe on the same target → augment the existing profile's `<includes>` (transitive references, helper files surfaced by compile errors).
- **Re-running** for a *different* target → create a new profile, do not touch sibling profiles.
- **End of migration** (post-stabilization, project compiles cleanly): user may delete all `migration-*` profiles, or keep them as historical record. Don't delete for them.

Idempotent: re-running for the same target augments, never replaces.

## Procedure

### 1. Locate target `pom.xml`

Default: project root. For multi-module reactors, the profile goes in the module(s) the recipe touched. The parent pom doesn't help — Maven scopes per-module.

### 2. Determine the migration scope for THIS recipe call

Two inputs:

- **Recipe identity** — passed by the calling recipe (e.g. `aggregate`, `event-processor`, …).
- **Target simple name** — the class the recipe just migrated (e.g. `Calendar`).

Together they produce the profile id: `migration-<recipe>-<Target>`.

Then collect the changed-files set for THIS recipe call. Three sources:

```bash
git -C <target> diff --name-only HEAD -- '*.java'
git -C <target> diff --cached --name-only -- '*.java'
git -C <target> ls-files --others --exclude-standard -- '*.java'
```

Filter to files this recipe owns (the target class + its commands + its events + its tests + the helper files it surfaced). The recipe is the authority on which files it touched — it should pass the list explicitly when invoking this profile recipe.

If a sibling `migration-*` profile already commits files and they appear in this set, **leave them alone** — they belong to the previous migration, not this one. Per-recipe profiles do not share `<include>` entries.

### 3. Map changed files to compiler include patterns

Strip the `src/main/java/` or `src/test/java/` prefix:

```
com/example/calendar/write/Calendar.java
com/example/calendar/events/DayStarted.java
```

**Default to per-file `<include>` entries** for per-recipe profiles. Per-package wildcards (`com/example/calendar/**/*.java`) are tempting but pull in non-migration files (`*Mcp.java`, `*RestApi.java`, AF4-only helpers) and those will fail compile under the AF5 deps in this profile. Per-file is verbose but precise — and the profile is short-lived anyway.

### 4. Add or update the per-recipe profile

If `<profile id="migration-<recipe>-<Target>">` does NOT exist, append the block below as sibling of `<build>` (creating `<profiles>` parent if needed). If it exists, merge the new patterns into its `<includes>` / `<testIncludes>`, deduplicating.

```xml
<profiles>
    <profile>
        <id>migration-aggregate-Calendar</id>
        <dependencyManagement>
            <dependencies>
                <!-- AF5 deps the recipe needs. List only what the recipe pulls in. -->
                <dependency>
                    <groupId>org.axonframework</groupId>
                    <artifactId>axon-modelling</artifactId>
                    <version>${axon5.version}</version>
                </dependency>
                <dependency>
                    <groupId>org.axonframework</groupId>
                    <artifactId>axon-eventsourcing</artifactId>
                    <version>${axon5.version}</version>
                </dependency>
                <dependency>
                    <groupId>org.axonframework</groupId>
                    <artifactId>axon-test</artifactId>
                    <version>${axon5.version}</version>
                </dependency>
                <!-- Add recipe-specific extras here (e.g. Spring extension for @EventSourced):
                <dependency>
                    <groupId>org.axonframework.extensions.spring</groupId>
                    <artifactId>axon-spring</artifactId>
                    <version>${axon5.version}</version>
                </dependency>
                -->
            </dependencies>
        </dependencyManagement>
        <dependencies>
            <dependency><groupId>org.axonframework</groupId><artifactId>axon-modelling</artifactId></dependency>
            <dependency><groupId>org.axonframework</groupId><artifactId>axon-eventsourcing</artifactId></dependency>
            <dependency><groupId>org.axonframework</groupId><artifactId>axon-test</artifactId><scope>test</scope></dependency>
        </dependencies>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <configuration>
                        <includes>
                            <include>com/example/calendar/write/Calendar.java</include>
                            <include>com/example/calendar/write/CalendarId.java</include>
                            <include>com/example/calendar/events/DayStarted.java</include>
                            <!-- … one per main-source file from step 3 -->
                        </includes>
                        <testIncludes>
                            <testInclude>com/example/calendar/write/CalendarTest.java</testInclude>
                            <!-- … one per test-source file from step 3 -->
                        </testIncludes>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

#### Recipe-to-deps cheat sheet

| Recipe | AF5 deps the profile usually needs |
|---|---|
| aggregate (Spring) | `axon-modelling`, `axon-eventsourcing`, `axon-test`, `axon-spring` extension |
| aggregate (non-Spring) | `axon-modelling`, `axon-eventsourcing`, `axon-test` |
| event-processor | `axon-messaging`, `axon-modelling`, `axon-test` (+ project's existing axon-messaging coords) |
| command-gateway | `axon-messaging` |
| query-gateway | `axon-messaging` |
| query-handler | `axon-messaging`, `axon-test` |
| read-configuration | `axon-configuration`, `axon-messaging` |
| write-configuration | `axon-configuration`, `axon-modelling`, `axon-eventsourcing` |
| event-storage-engine | `axon-eventsourcing`, plus path-specific (`axon-eventsourcing-jpa`, `axon-server-connector`, …) |

Add only what the recipe's migrated files actually `import`. Trim later if surefire output shows unused deps.

#### Known issue: Jackson 3 ↔ Spring Boot 3.5.x

AF5's `axon-test` (and `axon-conversion` transitively) depends on **Jackson 3** (`tools.jackson:*`). Jackson 3 still resolves `com.fasterxml.jackson.annotation.*` annotations from the legacy `jackson-annotations` artifact and requires version **2.21+**. Spring Boot 3.5.x pins `jackson-annotations:2.19.x` via its BOM. Without intervention, `AxonTestFixture.with(...)` fails at runtime with errors like:

```
NoClassDefFoundError: com/fasterxml/jackson/annotation/JsonSerializeAs
NoClassDefFoundError: tools/jackson/databind/json/JsonMapper$Builder
```

This surfaces only when the fixture initializes — compile passes fine — so it looks like a project bug, not a dep mismatch.

**Fix (pick one):**

**Option A — pin `jackson-annotations` 2.21 in the per-recipe profile's `<dependencyManagement>`** (minimal):

```xml
<dependencyManagement>
    <dependencies>
        <!-- … AF5 deps … -->
        <!-- Jackson 3 (used by AF5 axon-conversion) requires jackson-annotations 2.21+ -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-annotations</artifactId>
            <version>2.21</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**Option B — import the Jackson 3 BOM** (defends against any other Jackson 3 transitive surprise):

```xml
<dependencyManagement>
    <dependencies>
        <!-- … AF5 deps … -->
        <dependency>
            <groupId>tools.jackson</groupId>
            <artifactId>jackson-bom</artifactId>
            <version>3.1.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Default to **Option A** when the recipe's only Jackson interaction is via `AxonTestFixture` — the pin is small and explicit. Reach for **Option B** when the project also uses Jackson directly (REST controllers, custom serializers, `tools.jackson.databind.*`) so the BOM keeps everything aligned.

Apply this in the `<dependencyManagement>` of any per-recipe profile that pulls in `axon-test` (anything that runs an `AxonTestFixture`). Production-side recipes that don't touch the test fixture are unaffected — their deps don't pull Jackson 3 into the runtime classpath.

If the project is on Spring Boot 3.4.x or earlier (jackson-annotations < 2.19): same pin still works; bumping to 2.21 doesn't break older Spring Boot consumers.

### 5. Verify

Compile-only check first:

```bash
./mvnw -f <target>/pom.xml -P<profile-id> test-compile -DskipTests \
  -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
```

Then scoped test run:

```bash
./mvnw -f <target>/pom.xml -P<profile-id> test \
  -Dtest='<FQTestClasses>' \
  -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
```

If it fails on missing symbols **outside** the recipe's intended scope, the include list is too narrow — add the missing files. If it fails on legitimate AF4→AF5 work, that's the per-construct recipe's job to fix; the profile is doing what it should.

**Transitive references leak.** Per Apache Maven FAQ, `javac` still pulls in classes referenced from kept files even if they are not listed in `<includes>`. If `KeptClass.java` imports `BrokenClass.java`, you must either add `BrokenClass.java` to the includes or migrate it. The compiler error names the exact file to add.

### 6. Combined verification (multiple migrations)

After several recipes have run, verify combined behaviour by activating multiple profiles in one command:

```bash
./mvnw -f <target>/pom.xml \
  -P migration-aggregate-Calendar,migration-aggregate-Army,migration-event-processor-WhenCreatureRecruited \
  test -Dtest='com.example.calendar.write.CalendarTest,com.example.army.write.ArmyTest' \
  -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
```

Maven merges `<includes>` from all activated profiles. This is how phased mode verifies the cumulative migrated set across iterative recipes without needing a single mega-profile.

If two profiles add the same `<include>` → harmless (Maven dedupes).
If two profiles disagree on an AF5 dep version → use the same `${axon5.version}` everywhere; pinning is project-level, not per-profile.

### 7. Multi-module reactor

For multi-module Maven, the profile goes in **each module the recipe touched**. Add to all relevant modules' `pom.xml`. Run from the reactor root with `-pl <module-list>` to scope:

```bash
./mvnw -f <target>/pom.xml -P migration-aggregate-Calendar -pl module-a,module-b test \
  -Dtest='<FQ>' -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
```

**Multi-module flag pair.** When `-pl` includes modules that don't contain a class matching the `-Dtest=…` pattern, surefire fails with `No tests matching pattern "…" were executed!` for empty modules. Always pass **both** `-DfailIfNoTests=false` (for plain `surefire:test`) AND `-Dsurefire.failIfNoSpecifiedTests=false` (for the explicit `-Dtest=…` filter).

### 8. End-of-migration cleanup (optional, post-stabilization)

After stabilization succeeds (`./mvnw clean verify` green without any `migration-*` profile):

- **Option A — keep the profiles** as a historical record of what was migrated when. Especially useful for projects that migrate over weeks; a profile per migrated unit is a built-in audit trail.
- **Option B — delete them.** Drop every `migration-*` `<profile>` block, move the AF5 deps into the main `<dependencies>` (now that the whole project is on AF5), and delete `<axon5.version>` if the project no longer needs the AF4/AF5 split.

Default: keep until the user explicitly asks to clean up. Don't delete for them.

## End condition

1. `pom.xml` has the per-recipe profile present and well-formed.
2. `./mvnw -P<profile-id> test -Dtest='<FQTestClasses>' -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false` succeeds (or fails on legitimate AF4→AF5 work that the *recipe* must fix, never on missing-symbol from outside the profile's includes).
3. Other `migration-*` profiles for already-completed migrations remain untouched.

## Output

- target: <pom.xml path>
- decisions:
    - profile-id: migration-<recipe>-<target>
    - includes-added: [<patterns>]
    - testIncludes-added: [<patterns>]
- needs-user-decision: false
- notes: optional (e.g. "Jackson 3 pin added under dependencyManagement")

> Utility recipe — invoked transitively by every iterative recipe, NOT a phase. Profile setup is bundled into the calling recipe's commit — never a standalone commit.

## NEVER

- Add `<excludes>` to fix compile errors — defeats the purpose; if includes are too broad, narrow them.
- Activate any `migration-*` profile in CI by default — they're migration-time scaffolds, not production scope.
- Mutate someone else's profile to add files for your recipe. Each recipe owns exactly one profile per target.
- Use `LATEST` or unpinned versions in profile deps. Pin via `${axon5.version}`.
- Delete a sibling profile while running. Each migration is independent; cleanup is the user's call at end-of-stabilization.

## Scripts

- `scripts/ensure_axon5_version_property.py <pom.xml> <version>` — idempotently add `<axon5.version>` to root `<properties>`.
- `scripts/upsert_migration_profile.py <pom.xml> --recipe <name> --target <Simple> --include <relpath>...` — create or augment a per-recipe profile (handles `<include>` / `<testInclude>` partitioning by `src/main/java/` vs `src/test/java/` prefix).

If scripts aren't present, do the edits by hand following the structure above. Either way the result must be the same: one self-contained profile per `<recipe, target>` pair.
