# Root-cause a failure after an Axon update

The report needs, per regression: before vs after behavior, the exact error, affected
call sites, the root cause in framework code, the upstream commit and PR, the first
affected release, whether `main` still has it, and the workaround. This file is the
procedure to get each of those.

## 1. Read the failure

- Maven: `target/surefire-reports/<TestClass>.txt` (and `-output.txt` for logs).
- Gradle: `build/test-results/test/TEST-<TestClass>.xml` or `build/reports/tests/test/`.
- Keep only frames from `org.axonframework`, `io.axoniq` and the app to see the path.
- Confirm it passes on the baseline worktree (same test, old version). If it also fails
  there, it is not caused by the update.

## 2. Read the framework code of both versions

Download sources jars and unpack the classes on the stack into a scratch dir:

```bash
./mvnw -q dependency:get -Dartifact=<group>:<artifact>:<version>:jar:sources
unzip -o -q ~/.m2/repository/<group-path>/<artifact>/<version>/<artifact>-<version>-sources.jar -d $SCRATCH/src-<version>
```

(Gradle: the IDE's "download sources", or the same `dependency:get` with a Maven wrapper
from any project, or fetch the file from GitHub at the tag.)

Read the failing frames in the new version, then the same file in the old one. For a
file in a GitHub repo at a tag:

```bash
gh api "repos/<repo>/contents/<path>?ref=<tag>" -H "Accept: application/vnd.github.raw"
```

Describe the old behavior from that code, not from the test that used to pass. "It worked
before" may only hold for the one API the failing test uses. Follow the value to every API on
the same path that the app calls, in each version. Keep "no message" apart from "a message
with a `null` payload". In 5.3.0/5.3.1 a void command over Axon Server gave no result message
at all: `resultAs(Void.class)` returned `null`, but `onSuccess` threw inside `whenComplete` and
was silently skipped. A table that says only "returned `null`" hides that.

## 3. Find the upstream change

```bash
# commits that touched the file, newest first
gh api "repos/<repo>/commits?path=<path>&per_page=10" \
  --jq '.[] | .sha[0:10] + " " + .commit.committer.date + " " + (.commit.message|split("\n")[0])'

# the PR that merged a commit
gh api "repos/<repo>/commits/<sha>/pulls" --jq '.[] | "#\(.number) \(.title) merged \(.merged_at)"'

# is the commit in a tag? "ahead" or "identical" = included, "diverged"/"behind" = not
gh api "repos/<repo>/compare/<sha>...<tag>" --jq .status

# is it still on main (same check against main), and did anything change since?
gh api "repos/<repo>/compare/<old-tag>...<new-tag>" --jq '.files[].filename' | grep <FileName>
```

Check the previous line's patch tags too (for example the latest `5.3.x`) to state the
first affected release precisely. Changes in the commercial repo often follow a change in
the open-source repo (a contract change in core, adapted in a connector); link both.

## 4. Prove it

Run the smallest experiment that confirms the cause, then revert it. For example change
one call site to avoid the suspect path and run the one failing test. Prefer a temporary
edit you revert with `git checkout -- <file>` over reasoning alone.

Also check the runtime effect, not only the test: did the operation still commit its side
effects even though the caller saw an error? That turns a failure into a data problem
(retries apply changes twice) and belongs in the report.

## 5. Decide the fix

| Situation | Fix |
|---|---|
| The app relied on undocumented or changed behavior that the release notes announce | Adapt the app to the new contract |
| Framework regression, many call sites affected | One central workaround: dispatch/handler interceptor, decorator, bean override, or configuration enhancer |
| Framework regression, one call site | Local change with a comment pointing to the upstream commit |
| Changed default the app does not want | Set the property back explicitly, with a comment saying why |
| Problem already fixed in a later patch | Prefer the later patch as the target, if the user agrees |

A good workaround:

- restores exactly the previous behavior and nothing more;
- lives in one place, named after what it works around;
- has Javadoc (or comments) with the symptom, the upstream commit/PR, and "remove when";
- is active only where the problem exists (for example `@ConditionalOnProperty` on the
  Axon Server switch);
- can be switched off with a property, so the next update can check whether it is still
  needed;
- is covered by the tests that failed before.

Do not open upstream issues or PRs. Write the analysis into the report; the user decides.

## 6. Re-verify

Run the previously failing tests, then the full suite. Compare WARN/ERROR log lines with
the baseline again; a workaround should not add new warnings.

## Patterns worth knowing in Axon 5

- Dispatch interceptors (`MessageDispatchInterceptor`) see the result stream:
  `chain.proceed(message, context).mapMessage(...)` can rewrite a command result before
  the gateway converts it. Spring Boot registers any `MessageDispatchInterceptor` bean of
  the right message type automatically.
- Named components of a type that used to be unique make Spring by-type injection
  ambiguous. Resolving through `AxonConfiguration#getComponent(Type.class)` returns the
  unnamed default.
- `context.component(...)` throws in bare or mocked contexts; constructor injection is
  more robust for workarounds that must also work in tests.
