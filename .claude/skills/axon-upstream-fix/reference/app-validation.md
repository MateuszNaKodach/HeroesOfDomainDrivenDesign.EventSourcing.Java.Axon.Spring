# Validating the fix in the application

Use this for step 7. Goal: show that the app passes its full suite with the workaround removed
and only the fixed artifact changed, and that it fails again without the fix.

## 1. Child branch / worktree

```bash
git -C <app-checkout> worktree add -b <user>/<slug>-check <path>/<app-check-dir> <commit-with-workaround>
git -C <path>/<app-check-dir> log -1 --oneline     # confirm the base
```

Base it on the commit that contains the workaround and the target framework version (usually the
tip of the upgrade branch). Never edit the user's working branch. Worktree tools sometimes create
new worktrees from the repository's default branch; confirm the base before changing anything.

## 2. Pin only the fixed artifact

Maven, in the app's `pom.xml`:

```xml
<dependencyManagement>
  <dependencies>
    <!-- Local build of the upstream fix (not published). Validation only, do not merge. -->
    <dependency>
      <groupId>io.axoniq.framework</groupId>
      <artifactId>axon-server-connector</artifactId>
      <version>5.4.1-SNAPSHOT</version>
    </dependency>
  </dependencies>
</dependencyManagement>
```

Gradle (Kotlin DSL):

```kotlin
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "io.axoniq.framework" && requested.name == "axon-server-connector") {
            useVersion("5.4.1-SNAPSHOT") // local build of the upstream fix, validation only
        }
    }
}
repositories { mavenLocal() } // first, so the local snapshot wins
```

Do not bump the app's shared framework version property: the other artifacts do not exist as
the snapshot version, and changing them would mean the fix is no longer the only difference.

Watch the transitive siblings: a snapshot built with `-am` declares its in-reactor dependencies at
the snapshot version too (for example `axon-server-connector` pulls `axoniq-distributed-messaging`
5.4.1-SNAPSHOT). Maven then resolves those siblings to the snapshot as well. Pin each such sibling
back to the released version in the same `dependencyManagement` block, so the resolved list shows
exactly the fixed artifact(s) as SNAPSHOT:

```bash
./mvnw -nsu -q dependency:list -DincludeScope=runtime -DoutputFile=/dev/stdout | grep SNAPSHOT
```

Check resolution:

```bash
./mvnw -nsu dependency:tree -Dincludes=<groupId>      # Maven
./gradlew dependencies --configuration runtimeClasspath --offline | grep <artifactId>   # Gradle
```

Exactly the fixed artifact(s) should show the snapshot version.

## 3. Remove the workaround

Delete the workaround class/config (preferred, proves the fix stands alone), or switch it off
through its toggle. If you delete it, also remove now-unused imports or properties it relied on.

## 4. Full suite

```bash
./mvnw -nsu clean test          # Maven: -nsu keeps the local snapshot
./gradlew test --offline        # Gradle
```

Compare with the baseline totals of the app with the workaround in place. Expect the same
number of tests and no failures. A difference in test count means something did not run
(for example a Testcontainers-based class skipped): find out why.

## 5. Negative control

1. Change the pin back to the released version (workaround still removed).
2. Run only the test classes that cover the bug. They must fail with the original error from
   step 1 of the skill. Keep the excerpt for the report.
3. Restore the snapshot pin and run the same classes again: green.

If the negative control does not fail, the tests do not cover the bug (or the bug is not where
you think). Do not claim the fix is validated until this is resolved.

## 6. Optional: compatibility rerun

If the bug lives in a storage mode, token handling or a distributed bus, rerun the relevant part
of the `axon-update` compatibility recipe (`../../axon-update/reference/compat-recipe.md`) with the
snapshot pin: old data written by the released version, then the app with the fix on the same
stores. For wire-format fixes, consider a mixed run (one node on the released version, one on the
fix) when the app can run two instances; if it cannot, state the mixes as untested.

## 7. Commit locally

Commit the pin and the workaround removal on the child branch with a message that says it is a
validation branch for the upstream fix. Do not push it and do not merge it: the app keeps its
workaround until a release contains the fix.
