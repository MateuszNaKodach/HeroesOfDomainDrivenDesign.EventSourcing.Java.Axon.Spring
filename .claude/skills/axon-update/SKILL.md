---
name: axon-update
description: >-
  Update an Axon Framework 5 / Axoniq Framework 5 app to a newer patch or minor release
  and prove it behaves the same on the data it already has. Applies the version
  playbooks (known checks per release line), triages release notes, bumps, compares
  against a baseline, root-causes failures to the upstream commit, runs an old-data
  compatibility check, writes an upgrade report, and writes new findings back into the
  playbooks. Use when asked to update, upgrade or bump Axon 5.x / Axoniq 5.x (RCs and
  SNAPSHOT-to-release included), whether a new 5.x release breaks the app, or for an
  Axon upgrade report. Not for Axon 4 to 5, which is a migration.
---

# axon-update

Move an Axon 5 app to a newer 5.x release so the user only answers one question: the
**plan**. Everything known about a release line lives in its **playbook**. The run applies the
playbooks, finds anything new, and leaves the playbooks richer for the next update.

Done means a defensible answer: *does the app behave as before, on its existing data, and if
not, why, since which upstream commit, and what did we do about it?*

## Ground rules

- **Behavior first.** A forced code change keeps the previous observable behavior, and the
  report says what changed and why. Improvements the update did not force are follow-ups.
- **Evidence from sources.** Every regression gets the exact error, the call sites, the cause
  in the sources jars of both versions, and the upstream commit and PR.
- **Upstream stays the user's call.** Put the analysis in the report; open issues, PRs or
  comments only on request. The `axon-upstream-fix` skill handles a framework fix.
- **Own your sandbox.** Own worktree, containers and databases; clean them up, or say what is
  still running.

## Resources

```bash
AXU="$(pwd)/.claude/skills/axon-update"    # absolute, so commands also work from the baseline worktree
```

| Resource | Use it for |
|---|---|
| `playbooks/<A>-to-<B>.md` | Known checks of each release line; `reference/playbooks.md` defines the format, selection and writing back |
| `scripts/detect.py` | Build tool, Axon coordinates and the properties driving them, latest releases, storage modes per profile |
| `scripts/release_notes.py` | Downloads and triages the notes of both repos between two versions |
| `scripts/dep-list.sh` | Resolved runtime dependencies, for diffing |
| `scripts/compat-lib.sh` | Start/stop jars, SQL, token dumps, state snapshots, log diff |
| `reference/release-notes.md` | Which notes can change behavior silently |
| `reference/root-cause.md` | From a stack trace to the upstream commit; workaround patterns |
| `reference/compat-recipe.md` | The old-data compatibility run per storage mode, Axon Server setup |
| `assets/upgrade-notes-template.md` | The report skeleton |

Keep logs, jars and snapshots in a scratch directory outside the repo, `$SCRATCH` below.

## Workflow

Keep a checklist (the todo tool, or `$SCRATCH/checklist.md`) with these steps, plus one item per
check ID of the selected playbooks.
Each item ends with a status: `done`, `decided: <choice>`, `n/a: <reason>`, `deferred: step <n>`
(on the way), or `failed: <link to regression>`.

### 1. Detect

Run `python3 $AXU/scripts/detect.py` from the project root. Note the current version (`cur`),
the target (`tgt`: the user's, or the latest release), the version properties, the storage
modes and their default, and the git commit. Check that the working tree is clean (the skill's
own folder aside). When a newer patch of the target line exists, put it in the plan as an option:
it may carry fixes for the playbook's `decide` rows. A changed target reruns step 3 on the new
range.

- A 4.x to 5.x move is a migration: stop and point to the Axon migration tooling.
- A downgrade runs the same workflow, with every note read in reverse.
- A local `-SNAPSHOT` baseline may need artifacts that exist only in `~/.m2`: build it with
  `-nsu` (Gradle `--offline`). Date it by its jar's timestamp in `~/.m2` to know which releases it
  already contains. Its failures can predate the target: reproduce a failure on the latest
  *released* version of the old line before naming the first affected release.

Find what earlier updates left behind: `docs/UPGRADE-*.md`, `*Workaround*` classes,
`application.workarounds.*` properties, and `*Canary*` tests. Each of them is a check of this run.

### 2. Select playbooks

Run `ls $AXU/playbooks` and select by the rule in `reference/playbooks.md`. A line without a
playbook makes the hop **uncharted**: the steps stay the same, and step 10 creates the playbook.

### 3. Read the release notes

```bash
python3 $AXU/scripts/release_notes.py --from <cur> --to <tgt> --out $SCRATCH/notes
```

Read `TRIAGE.md` fully and skim the raw notes (`reference/release-notes.md` says what to look
for). An item is relevant when it changes a default, a contract, a wire or stored format, or an
API in a category `detect.py` shows the app uses. A relevant item that no playbook row covers is a
**new finding**: give it a provisional check ID and treat it like a playbook row.

### 4. Plan and confirm (the one question)

Run every check's **Detect** against the app. Then show the plan:

```text
Axon update <cur> -> <tgt>   playbooks: <files> (<status>)   uncharted: <lines or none>
Automatic:   <ID>: <file:line> <edit>                        (one line each)
Your call:   <ID>: <options>, recommended <x> because <why>
Will verify: <ID>: <how>                                     (tests, canary, compatibility run)
New findings: <provisional ID>: <note item> -> <kind, action>
Notes:       <ID>: <one line>
Not applicable: <IDs> (with reasons)
Needs: Docker for <modes>; <n> full test runs; compatibility run of <n> modes
```

Ask once, for the plan and every "Your call" answer together. If the user asked for a plan
only, stop here.

### 5. Baseline

```bash
git worktree add --detach $SCRATCH/baseline HEAD
(cd $SCRATCH/baseline && ./mvnw -B test > $SCRATCH/baseline-test.log 2>&1)   # or ./gradlew test
```

Record the totals. A red baseline stops the run: tell the user. Check `docker info` first when
tests use Testcontainers. Keep the worktree: it builds the old jar for step 8.

### 6. Bump and apply

1. Apply the `auto` checks and the confirmed `decide` choices.
2. Compile main and tests with deprecations visible in both trees, and diff the warnings
   (Maven: `-Dmaven.compiler.showDeprecation=true -Dmaven.compiler.showWarnings=true`; Gradle:
   `options.deprecation = true`). Each new deprecation is reported with its replacement, from
   the `@deprecated` Javadoc.
3. Diff `dep-list.sh` output between the trees. Watch for artifacts added or removed, not only
   version moves.
4. Run the full suite and compare its totals with the baseline. Diff the WARN/ERROR lines
   (`axc_logdiff` in `compat-lib.sh`). A new warning is often the only sign of a changed
   default.

### 7. Root-cause every failure

Follow `reference/root-cause.md`. The fix is one of:

- the app relied on something that legitimately changed: adapt the app;
- a framework regression: one central, removable, property-switchable workaround, with the
  upstream commit and its removal condition in the Javadoc, plus a **canary** test that expects
  the upstream failure with the workaround off. The canary fails once a release fixes the bug,
  which tells the next update to remove the workaround.

Also note what the failure means at runtime: a call that fails after its side effects were
committed invites duplicate retries. Each regression becomes a playbook row in step 10.

### 8. Old data, new version

Follow `reference/compat-recipe.md` for every storage mode, the default first, with a separate
database and Axon Server context per mode. The old jar writes realistic data through the app's
own API. The new jar must continue on it: tokens unchanged, nothing reprocessed, entities
sourced from old events still enforce their rules, processors advance. Run the `verify` checks
here. For several nodes on Axon Server, test the mixed-version case or state "not tested" with
your analysis.

### 9. Report

Copy `assets/upgrade-notes-template.md` to `docs/UPGRADE-AXON-<tgt>.md`, or to the repo's own
docs convention. Fill every section, and write "none" where nothing applies. The check table
lists every check ID with its status. Link upstream PR numbers (commit hashes change on rebase
and squash). The step is done when `grep -n '<' docs/UPGRADE-AXON-<tgt>.md` shows no placeholder
left.

### 10. Write back

Update the selected playbooks by the "Writing back" rules in `reference/playbooks.md`: new rows,
corrected rows, status `verified`, a new playbook for an uncharted line. These edits are a
separate commit, because they help every app and not just this one. If the skill lives outside
this repo, offer the playbook change to its owners.

### 11. Finish

- Run the full suite on the final code.
- Remove the baseline worktree, stop app processes, remove your containers.
- Commit when asked or when the repo says so: the bump together with any workaround it needs
  (every commit green), then the report, then the playbook edits.
- Summarize in chat, leading with the answer (does it work, and what did it take): test totals
  against the baseline, each regression with its origin and workaround, the behavior changes
  kept, the compatibility result per mode, untested risks, the report path, and the playbook
  rows added.

## Traps

- **RC versus GA.** RC problems may be gone in GA, and new ones may appear. Verify against the
  exact target.
- **"Worked before" may cover one API only.** Compare old and new behavior on every API the app
  calls on the failing path (see `reference/root-cause.md`).
- **Starter content.** A dependency that turns `optional` drops out of a starter silently; the
  dependency diff catches it.
- **Ambiguous beans.** New named components of an existing type can break by-type injection in
  Spring. Check whether a later patch fixed it before working around it.
