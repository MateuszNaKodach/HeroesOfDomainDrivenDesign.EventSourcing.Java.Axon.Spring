# Commit message template

Conventional commits, short subject, free-form body. State lives in
`progress.md`, not in the commit body.

## Subject line

```
<type>(af5-migration): <one-line description> (Migration Phase #<N>)
```

- `<type>`: `chore` (one-shot, no behavior change), `refactor` (iterative
  migration of one item), `feat` (storage-engine wiring), `fix` (a
  stabilization fix), `docs` (decision-only commits — no code change).
- Subject ≤ 70 chars when possible.
- "Migration Phase #<N>" suffix is optional but recommended for code-changing
  commits — it lets a reader scan `git log --oneline` and see progress
  phase by phase.

## Body

Free-form prose. Include the verification command used and anything a
reviewer needs. Keep brief — detail belongs in `progress.md` /
`learnings.md`.

```
Verified via:
  ./mvnw -f <target>/pom.xml test -P <profile-id> \
    -Dtest='<FQTestClass>' \
    -DfailIfNoTests=false \
    -Dsurefire.failIfNoSpecifiedTests=false

<one or two short lines on the path / variant / decisions, if non-obvious>
```

## Hard rules

- One commit per item — never bundle multiple migrations.
- Stage explicit paths. NEVER `git add -A` / `git add .`.
- Include the matching `progress.md` rewrite in the same commit (rewrite
  the relevant sections before staging).
- Include `learnings.md` if a non-obvious lesson surfaced.
- Never push, never amend, never `--no-verify`.
- Commit on the user's current branch — never on `main` / `master`.
- No co-author attribution lines (per project convention).
