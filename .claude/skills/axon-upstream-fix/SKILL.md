---
name: axon-upstream-fix
description: >-
  Fix an Axon Framework 5 / Axoniq Framework 5 bug at its source, using the application
  that hit it as the reproduction and validation harness: capture the exact symptom from
  the app, find the owning repo (AxonIQ/AxonFramework or AxonIQ/axoniq-framework) and the
  commit/PR/tag that introduced it, fix it test-first (RED with the same error as the app,
  then GREEN) in an isolated worktree based on the released tag, install it locally as a
  -SNAPSHOT without overwriting released jars, prove the app works without its workaround
  (plus a negative control), write a fix report, and prepare an upstream PR (pushing only
  after explicit user confirmation). Use this skill whenever a framework-level defect shows
  up in an Axon 5 app: after axon-update finds a regression, when a stack trace points into
  org.axonframework.* or io.axoniq.framework.*, when the user says "fix it upstream",
  "fix this in the framework", "patch the connector", "build a local snapshot with the fix",
  "try the app against my framework fix", or "prepare a PR for Axon", even if they do not
  name this skill. Not for bugs in the app's own code, and not for AF4 to AF5 migration.
---

# axon-upstream-fix

Turn a framework bug seen in an application into a tested upstream fix. The application is
the harness: it reproduces the bug before the fix, and proves the fix (and that nothing else
broke) after it.

The deliverable is not "a patch that compiles". It is a chain of evidence a framework
maintainer can trust: *this exact symptom in a real app, caused by this commit, reproduced by
this failing test, fixed by this minimal change, verified in the app with the workaround
removed, and shown to fail again without the fix.*

## When to use, and when to stop

Enter from any of:

- the `axon-update` skill (`../axon-update/SKILL.md`), step "Root-cause every failure", once a
  regression is pinned to a framework commit and the app has a workaround;
- a failing app test or production symptom whose stack trace and root cause sit in
  `org.axonframework.*` or `io.axoniq.framework.*` code.

Stop and say so when:

- the root cause is in the app (wrong usage, config). Fix the app instead.
- the app is on Axon Framework 4, or the question is AF4 to AF5. That is a migration.
- the owning repository is not reachable (commercial repo without access). Report the root
  cause and the proposed patch as text and let the user route it.

## Ground rules

- **Evidence before edits.** Reproduce from the app first and keep the exact exception text.
  The framework test must fail with the same error, otherwise it tests something else.
- **Only the fix differs.** Base the framework worktree on the exact released tag the app uses,
  not on `main`, so app validation compares like with like. Retarget to the PR branch later.
- **Never clobber released artifacts.** Install the fix under a new `-SNAPSHOT` version and
  verify the released version in the local repository is byte-identical afterwards. A silently
  replaced `5.4.0` jar poisons every other project on the machine.
- **Never touch the user's working copies.** New worktrees for the framework fix and for the
  app validation. Do not reuse a dirty checkout.
- **Nothing outward-facing without an explicit yes.** `git push`, `gh pr create`, issues and
  comments are visible to others and hard to take back. Prepare everything locally, then ask.
  Never open issues unless asked.
- Follow the user's commit rules (attribution, message style) and the target repo's
  contribution rules (`CONTRIBUTING*`, `AGENTS.md`, `CLAUDE.md`, PR template).

## Bundled resources

| File | Read it when |
|---|---|
| `reference/origin.md` | Step 2 and 3: mapping a class to its repo, finding the introducing commit/PR/tags, searching existing issues/PRs |
| `reference/local-build.md` | Step 6: version override per build style, `-pl -am`, guarding `~/.m2`, consumer pinning |
| `reference/app-validation.md` | Step 7: child worktree, pinning only the fixed artifact, negative control, compatibility rerun |
| `reference/pr.md` | Step 9: choosing the target branch, porting the fix, PR body, the confirmation gate |
| `reference/example-void-command-result.md` | A complete worked example (Axon Server connector, 5.4.0). Example only, not a template for other bugs |
| `assets/fix-report-template.md` | Step 8: the report skeleton, fill every section |
| `scripts/m2-guard.sh` | Step 6: snapshot and verify checksums of released artifacts around the local install |

Related skill: `axon-update` (`../axon-update/SKILL.md`). Its `../axon-update/reference/root-cause.md` covers
reading sources jars and workaround patterns, and its `../axon-update/reference/compat-recipe.md` is the
"old data, new version" recipe reused in step 7.

## Workflow

Keep a scratch notes file while working. Steps 1 to 3 feed the report; nothing is lost if the
session is interrupted.

### 1. Capture the symptom from the app

Write down, verbatim:

- the exception class, message and the framework frames of the stack trace (down to the
  first app frame), plus the root `Caused by`;
- the failing tests (class and method) and the profiles/properties they need (for example an
  Axon Server profile, a DCB context, a specific event store);
- framework versions where it works and where it fails (from the build file and the resolved
  dependency list, not from memory);
- the observable impact beyond the exception (for example "the command was applied but the
  caller got an error", "the processor stalls", "events are re-delivered");
- the app's workaround, if any: the class/config, and its on/off switch. If there is no
  switch, note how to remove it (needed for step 7).

Reproduce once more right before moving on (single test class is enough) so the symptom is
fresh and confirmed on the current versions.

### 2. Find the owning repository and a local clone

Map the defective class to its repo and module (`reference/origin.md` has the table). In short:
`org.axonframework.*` is AxonIQ/AxonFramework, `io.axoniq.framework.*` is
AxonIQ/axoniq-framework (commercial, access needed). Find an existing local clone (ask the user
for the path if it is not obvious) and fetch tags. Clone only if there is none and the user
agrees on the location.

### 3. Find the origin

Read the sources jar of the exact version the app uses, then locate the introducing change:
history of the file, compare between the last good and first bad tag, the PR behind the commit,
and whether the defect is still on `main` (and on any maintenance branch). Search for an existing
issue or PR on the same symptom and link it if found. Do not open or comment on anything.
Commands are in `reference/origin.md`.

Write down: introducing commit hash, PR number and title, first affected tag, last good tag,
status on `main`. This is the heart of the PR description later.

Treat "first affected" and "what older versions do" as hypotheses until a released artifact has
shown them. Code reading of one layer can mislead: the cause may sit in another repo or layer
(for example the OSS command bus changing what the commercial connector receives), may have been
backported into an earlier patch line, and the app's own baseline may be a SNAPSHOT that is
neither the old nor the new release. Prove it empirically: run same-version pairs of the
released versions around the suspected change, and capture what the real released code produces
(see step 5).

### 4. Isolated framework worktree on the released tag

```bash
git -C <framework-clone> fetch --tags origin
git -C <framework-clone> worktree add -b <user>/fix-<short-slug> <path>/<fix-dir> <released-tag>
```

Base it on the tag the app runs (for example `axoniq-5.4.0` or `axon-5.4.0`). Read the repo's
`AGENTS.md`/`CLAUDE.md`/`CONTRIBUTING*` and the module's existing tests to match style before
writing code.

If a multi-agent or worktree orchestration tool is available, steps 4 to 6 (framework) and the
setup of step 7 (app child worktree) can run in parallel by two workers; the app validation run
itself has to wait for the local install. This is optional; the workflow works sequentially.

### 5. Test-first fix

1. **RED.** Add the smallest test that reproduces the defect, at the lowest level that can show
   it: a unit test next to the defective class first. Add an integration test only if the module
   already has cheap infrastructure for it. Run it and keep the failure output: it must show the
   same error as the app (same exception and root cause), or a direct precursor of it. If an
   existing test asserts the buggy behavior, change it and say so in the report.
2. **Cover the compatibility matrix** when the fix touches serialization, a wire protocol, stored
   data or tokens. For distributed components (connectors, buses, event stores), test both
   directions: old sender with new receiver and new sender with old receiver (rolling upgrades),
   plus the already-released buggy format if it is in the wild. Fixing only one side usually
   leaves one mix broken; document which mixes stay broken.
   Pin what released nodes really exchange as **golden samples captured from the released
   artifacts** (run their real code once, store the bytes as test resources with a README on how
   they were produced). Hand-built "old format" fixtures encode your assumption, not the truth.
   Prefer a fix that keeps the released wire format and only makes the receiving side tolerant:
   a patch release then adds no new shape that not yet upgraded nodes must understand.
3. **GREEN.** Minimal change in the defective class. Keep Javadoc truthful. No unrelated
   refactoring, formatting or dependency changes: they make the fix hard to review and to
   backport.
4. Run the whole module test suite, not only the new tests.
5. Commit locally on the fix branch with a message that explains why (symptom, cause, why this
   change), following the user's attribution rules. Do not push.

### 6. Local install without clobbering releases

Install the fixed module (and what it needs, `-pl <module> -am`) under a new snapshot version,
for example `5.4.1-SNAPSHOT` for a fix on top of `5.4.0`. How to override the version depends on
the build (CI-friendly `${revision}`, `versions:set`, Gradle property); see
`reference/local-build.md`. Guard the released artifacts (the script path is relative to this
skill's directory; call it by absolute path from the framework worktree):

```bash
scripts/m2-guard.sh snapshot <groupId> <artifactId> <released-version>   # before installing
# ... install with the overridden version ...
scripts/m2-guard.sh verify   <groupId> <artifactId> <released-version>   # after: must print OK
```

Record the installed artifact paths for the report.

### 7. Validate in the app (without the workaround, plus a negative control)

Details and pinning snippets: `reference/app-validation.md`. In short:

1. Create a child branch/worktree of the app from the commit that has the workaround (not the
   user's working branch; check the new worktree really is on that commit, some tools base new
   worktrees on the default branch).
2. Pin **only** the fixed artifact(s) to the local snapshot through dependency management. Keep
   every other framework artifact on the released version, so the fix is the only difference.
3. Remove the workaround (delete it, or switch it off if it has a toggle and you also want to
   prove the toggle works).
4. Build offline-for-snapshots (`./mvnw -nsu`, Gradle `--offline`) so the local snapshot is used
   and not replaced by a remote one. Confirm resolution with the dependency tree.
5. Run the full app test suite. Expect the baseline totals with the workaround (no regressions).
6. **Negative control:** pin the fixed artifact back to the released version, workaround still
   removed, run the tests that cover the bug. They must fail with the original error. Restore the
   snapshot pin and re-run them green. Without this, a green run proves little.
7. Optionally rerun the axon-update compatibility recipe for the storage mode the bug lives in
   (`../axon-update/reference/compat-recipe.md`).
8. Commit the pin and workaround removal on the child branch, locally only. It is evidence, not
   something to merge.

### 8. Write the fix report

Fill `assets/fix-report-template.md` and save it next to the fix (for example
`<framework-worktree>/REPORT-<slug>.md`, uncommitted) and, if useful, a copy in the app child
worktree. Every section matters; write "n/a" with a reason instead of dropping one. The report's
"Proposed PR" section is the draft for step 9.

### 9. Prepare the upstream PR (ask before pushing)

Follow `reference/pr.md`:

1. Decide the target branch from how previous patch fixes were merged. If the maintenance
   branch only receives `[Backport ...]` PRs or a backport workflow exists (axoniq-framework),
   target `main` and backport after the merge; otherwise a maintenance branch such as
   `axon-5.4.x` if it exists, else `main`.
2. Port the fix onto a branch from that target (cherry-pick or rebase), resolve conflicts, rerun
   the module tests there.
3. Draft title and body from the report, following the repo's PR template.
4. **Stop and ask the user** for explicit confirmation, showing: target repo and branch, branch
   name, commit list, PR title and body. Only after a clear yes: push and open the PR, as a draft
   unless the user says otherwise. Never open issues unless asked.
5. If review tooling is available in the environment (for example a PR review skill for the
   Axon repos), offer it; do not depend on it.

### 10. Hand back to the app

The app keeps its workaround until a released version contains the fix. Update the app's
upgrade report (or add a note next to the workaround) with: upstream PR link or branch, the
expected release, and how to remove the workaround then (delete class/config, drop the
snapshot pin, rerun the suite). Do not merge the validation child branch.

Close with a short chat summary: root cause and origin commit, the fix in one sentence, RED
and GREEN evidence, app validation and negative control results, where the report and branches
are, and what is waiting for the user's decision (push/PR).

## Gotchas

- **Wrong premise about older versions.** A SNAPSHOT baseline or code reading of one layer can make
  a long-standing bug look like a new regression (or the reverse). Confirm with released artifacts
  and golden samples before writing the PR description or choosing which side to change.
- **Workaround too broad.** An app workaround that matches on data alone (for example "empty
  bytes") can also rewrite legitimate results (an empty String serializes to empty bytes too).
  Match on the type as well, and probe it with real mixed versions.

- **New worktree on the wrong base.** Worktree tools may create branches from the default branch
  instead of the current one. Check `git log -1` in every new worktree before editing.
- **`-U` / remote snapshot refresh** replaces locally installed snapshots with remote ones of the
  same version. Build the app with `-nsu` while validating.
- **BOM-managed versions.** Pinning one artifact may pull a sibling at the snapshot version
  through its POM. Check the dependency tree and install the siblings too (`-am`) if needed.
- **Existing tests that encode the bug.** A test asserting the buggy wire format or behavior will
  turn red after the fix; update it deliberately and call it out in the PR.
- **Commercial repo access.** Without access to AxonIQ/axoniq-framework you can still capture the
  root cause from the sources jar and propose the patch as text.
- **Generated or flattened POMs.** CI-friendly versions need the flatten plugin to run; pass the
  version override on every Maven invocation, not only the install.
