# Commit cadence — full rules

Migrations are easier to review, bisect, and roll back when each unit of progress is its own commit. Always on user's current branch (never `main`), never pushed (user controls remote).

The commit message template (subject line + body shape) lives in [../assets/commit-message-template.md](../assets/commit-message-template.md). This file covers cadence rules: when to commit and what to include.

## When to commit

One commit per item. Map a recipe outcome to a commit subject like this:

| Recipe outcome | Subject line |
|---|---|
| `init` (license + unsupported-feature decisions captured) | `chore(af5-migration): record migration init decisions` |
| `openrewrite` finished | `chore(af5-migration): apply OpenRewrite recipe <recipe-name>@<version> (Migration Phase #1)` |
| One `aggregate` migrated | `refactor(af5-migration): migrate aggregate <SimpleClassName> to AF5 (Migration Phase #2)` |
| One `event-processor` migrated | `refactor(af5-migration): migrate event handler <SimpleClassName> to AF5 (Migration Phase #3)` |
| One `command-gateway` migrated | `refactor(af5-migration): migrate command dispatch in <SimpleClassName> to AF5 (Migration Phase #4)` |
| One `query-gateway` migrated | `refactor(af5-migration): migrate query dispatch in <SimpleClassName> to AF5 (Migration Phase #5)` |
| One `query-handler` migrated | `refactor(af5-migration): migrate query handler <SimpleClassName> to AF5 (Migration Phase #6)` |
| One `read-configuration` migrated | `refactor(af5-migration): migrate configuration reader <SimpleClassName> to AF5 (Migration Phase #7)` |
| One `write-configuration` migrated | `refactor(af5-migration): migrate configuration <SimpleClassName> to AF5 (Migration Phase #8)` |
| `event-storage-engine` wired | `feat(af5-migration): wire AggregateBased{Jpa,AxonServer}EventStorageEngine (Migration Phase #9)` (include the SQL script in same commit on Path A) |
| Stabilization fix | `fix(af5-migration): <one-line description>` — one commit per logical fix, NOT one big bundle |
| Decision-only (defer / unsupported-feature accept) | `docs(af5-migration): record decision on <recipe>/<target>` (no code change; the decision is recorded in `progress.md` Pinned-decisions and/or `learnings.md`) |
| Plan committed at start of iterative phase | `chore(af5-migration): plan Migration Phase #<N>` |

The "Migration Phase #N" suffix is recommended for code-changing commits — it lets a reader scan `git log --oneline` and see migration progress phase by phase.

Always include matching `progress.md` / `learnings.md` / `sql/*` updates in the **same commit** as the code change they document — bookkeeping and work belong together.

## Body shape

Free-form prose first (verification command used, anything a reviewer needs). No structured block required. See [../assets/commit-message-template.md](../assets/commit-message-template.md) for the template.

Decision-only commits (defer, unsupported-feature accepts) record the choice in `progress.md` Pinned-decisions and/or a `learnings.md` entry — that's the audit trail, not the commit body.

## Persistence checklist (run before every commit)

- [ ] **▶︎ RESUME HERE block** in `progress.md` points at the *next* unit (not the one just finished). One-sentence Next action, exact recipe, exact verify command.
- [ ] **Phase status table** row updated.
- [ ] **Per-phase plan table** row for just-finished item shows status `done` / `deferred: <reason>` and a commit SHA (or `<pending>` placeholder if back-filling).
- [ ] If non-obvious lesson surfaced: `learnings.md` has dated entry.

A fresh session reading `progress.md` after this commit must pick up the next action with zero clarifying questions. Update `progress.md` before staging; the commit must include both code and the matching `progress.md` rewrite.

## Commit command shape

Use a heredoc to keep messages clean and respect user's git identity:

```bash
git -C <target> add <changed-files> .axon4-to-axon5-migration/
git -C <target> commit -m "$(cat <<'EOF'
refactor(af5-migration): migrate aggregate Faculty to AF5 (Migration Phase #2)

Verified via:
  ./mvnw test -P migration-aggregate-Faculty -Dtest='org.example.FacultyTest' -DfailIfNoTests=false

Spring Boot path; simple variant; creation-policy NEVER; test-fixture migrated via AggregateTestFixture mapping.
EOF
)"
```

## NEVER

- `git add -A` / `git add .` — risks staging unrelated WIP or secrets.
- `git commit --amend` — each migration step is its own historical record. If a step needs a follow-up fix, commit it on top.
- `git push` — user pushes when ready. Orchestrator does not.
- `--no-verify` — if a pre-commit hook fails, surface to user and let them decide.
- Co-author attribution lines (`Co-Authored-By: Claude` etc.) — per project convention, do not add.

## Dirty working tree mid-phase

If `git -C <target> status --porcelain` shows files the orchestrator did not touch, the orchestrator's resume protocol prompts via `AskUserQuestion`. See SKILL.md "Mid-phase dirty tree" — three options: stage-only-migration-files / let-user-clean-up / skip-this-commit.

Don't silently sweep user's WIP into a migration commit.
