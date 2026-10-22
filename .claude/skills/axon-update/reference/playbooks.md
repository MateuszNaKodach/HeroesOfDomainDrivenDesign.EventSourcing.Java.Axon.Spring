# Playbooks

A **playbook** is the known knowledge of one release line: every check an app needs when it
lands on that line. It turns an update from research into a checklist. Each run reads them, and
each run that learns something writes it back.

## Selection

A file `playbooks/<A>-to-<B>.md` covers updates landing on any `B.x`: from `A.x`, from a
snapshot of either line, and between versions of `B.x` (for example `5.4.0` to `5.4.1`).

For a hop from line `C` to line `T`, select the playbook of every line after `C` up to and
including `T`. For a hop within `T`, select only `T`'s. Apply them in line order. Bump once to
the target, unless a check has `Kind` `stop`.

A line without a playbook makes the hop **uncharted**. Run the workflow anyway: the release
notes and the baseline comparison find what a playbook would have listed. Then write the
missing playbook (see "Writing back").

## File format

```markdown
---
line: 5.4                      # the release line this playbook lands on
from: 5.3                      # the line it comes from
status: verified               # verified: applied to a real app; draft: built from notes and PRs only
verified-on: <app kind, storage modes, date>
---

# 5.3 to 5.4

<One line: what makes this hop special.>

## Checks

| ID | Kind | Applies when | Detect | Action |
|---|---|---|---|---|
```

Columns:

- **ID:** a stable kebab-case name. Reports and later playbooks cite it.
- **Kind:** one of the kinds below.
- **Applies when:** the version condition, written against current (`cur`) and target (`tgt`),
  for example `tgt in 5.3.2..5.4.0` or `cur < 5.4.1 <= tgt`, plus any app condition. No condition
  means every hop onto the line.
- **Detect:** how to find it in the app: exact greps, properties, beans, classes or profiles. A
  check that matches nothing is `n/a` in the plan, with the reason, and gets no assertion later.
  A Detect that needs built trees or a running app (a log line, a dependency diff) shows as
  `deferred: step <n>` in the plan.
- **Action:** what to do on a match, with upstream references (PR, commit). Name a reference
  implementation when one exists. A row taken from release notes, not yet applied to an app,
  starts with `unverified:`; the first run that applies it drops the prefix or corrects it.

Kinds:

- `auto`: an exact, bounded, idempotent edit. The plan lists it, and it runs after the one
  confirmation.
- `decide`: needs the user's choice. The plan gives the options and a recommendation, and the
  user answers all of them in the same confirmation.
- `verify`: a test, canary or compatibility-run check. A failure becomes a regression in the
  report.
- `note`: no code action. It goes into the report, under behavior changes or under items
  checked and not affecting the app.
- `stop`: the bump must pause at an intermediate version (for example to migrate data first).

State facts as versions, not dates ("fixed in 5.4.1", not "not released yet"): a date goes stale
the day the release ships. Keep one meaning in one row. A problem that spans releases gets one
row, with the range in **Applies when**. The row that removes a workaround is separate, so each
row stays a single action.

## Writing back

At the end of every run, update the selected playbooks:

- every new finding becomes a row: a regression, a behavior change, a deprecation, or a trap
  that cost time;
- a row the run proved wrong gets corrected, not appended to;
- a `draft` playbook that this run applied becomes `verified`, with `verified-on` filled in;
- an uncharted hop gets its playbook created from the report.

Keep app-specific facts out of a playbook. A row says what to look for in any app. The
report in the app's repository says what this app had.
