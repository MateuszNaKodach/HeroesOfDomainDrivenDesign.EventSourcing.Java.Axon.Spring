# Local build of the fix without clobbering releases

Use this for step 6. Goal: the fixed module (and anything it needs) is in the local repository
under a new snapshot version, and the released version there is byte-identical to before.

## Pick the snapshot version

Next patch plus `-SNAPSHOT` on top of the released tag you branched from: `5.4.0` becomes
`5.4.1-SNAPSHOT`. It must differ from every released version and from anything the user may
have installed for other work (check `ls ~/.m2/repository/<group/path>/<artifactId>/`). If that
version already exists locally from unrelated work, use a distinct qualifier such as
`5.4.1-<slug>-SNAPSHOT`.

## Override the version by build style

**Maven, CI-friendly versions** (root POM has `<version>${revision}</version>` and a
`<revision>` property, usually with the flatten plugin). AxonFramework and axoniq-framework use
this style. Pass the override on every invocation:

```bash
./mvnw -Drevision=5.4.1-SNAPSHOT -pl <module/path> -am install -DskipTests
./mvnw -Drevision=5.4.1-SNAPSHOT -pl <module/path> test
```

**Maven, literal versions:** change the version in the worktree only (it is a throwaway branch),
never in the user's checkout:

```bash
./mvnw versions:set -DnewVersion=5.4.1-SNAPSHOT -DgenerateBackupPoms=false
./mvnw -pl <module/path> -am install -DskipTests
```

Keep this version change out of the commit you will propose upstream (separate commit you drop,
or `git checkout -- '**/pom.xml'` before committing the fix).

**Gradle:** most builds read `version` from `gradle.properties`; override with
`-Pversion=5.4.1-SNAPSHOT` (or edit it in the worktree) and run
`./gradlew :<module>:publishToMavenLocal`.

## Build only what is needed

`-pl <module> -am` builds the module plus its in-reactor dependencies (parents, BOM-less
siblings). Check which in-reactor modules the fixed module depends on at runtime: they get the
snapshot version too and must be installed. Modules outside the reactor (for example the open
source framework used by a commercial module) stay on their released version and come from the
remote repository. Install the full reactor only if `-am` misses something the app needs.

Run the module's tests with the same override before installing, if you did not already in step 5.

## Read test results you can trust

- Check `docker info` first. Many modules have Testcontainers-based tests; with the daemon down
  they error with "Could not find a valid Docker environment", which looks like a real failure.
- `verify -pl <module> -am` also runs the tests of every in-reactor dependency and stops at the
  first failing one, before the module you care about. Its `target/surefire-reports` are then
  left over from an earlier run. Delete them before the run and count the results afterwards.
- Do not narrow with a package-wide `-Dtest=<package>.**`: it also selects `*IT` classes that the
  default build leaves to an integration-test profile.
- Compare the test count with an earlier run of the same module (axoniq-framework's
  `axon-server-connector`: 315). A lower count means tests were skipped, not that they passed.

## Guard the released artifacts

```bash
scripts/m2-guard.sh snapshot <groupId> <artifactId> <released-version>
# install ...
scripts/m2-guard.sh verify   <groupId> <artifactId> <released-version>
```

Guard every artifact the install touches (the fixed module and each sibling built by `-am`).
`verify` compares SHA-256 and modification time of every file in the released version
directory and prints `OK` or the differences. If it reports a difference, stop: re-download the
released version (`rm -r` that version directory and resolve it again) and find out why the
override did not apply.

## Consumer side

The consuming app pins only the fixed artifact(s) to the snapshot version; see
`app-validation.md`. If the app imports a framework BOM, the BOM itself was not installed as a
snapshot, so pin the artifact directly in `dependencyManagement` instead of bumping the BOM.
