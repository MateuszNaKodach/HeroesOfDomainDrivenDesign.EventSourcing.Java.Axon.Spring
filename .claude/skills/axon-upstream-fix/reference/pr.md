# Preparing the upstream PR

Use this for step 9. Everything here is local until the user explicitly confirms the push.

## 1. Choose the target branch

```bash
git -C <framework-clone> fetch origin
git -C <framework-clone> branch -r | grep -E 'origin/(main|master|axon-[0-9.]+\.x|axoniq-[0-9.]+\.x|[0-9.]+\.x)$'
# how did previous patch releases get their fixes?
gh pr list --repo <owner/repo> --state merged --search "base:<maintenance-branch>" --limit 10
git -C <framework-clone> log --oneline <previous-patch-tag>..<next-patch-tag> | head -30
```

- A maintenance branch for the affected minor (for example `axon-5.4.x` or `axoniq-5.4.x`) exists
  and recent patch fixes target it: target it. The fix usually gets merged forward to `main` by
  the maintainers; mention that `main` is affected too.
- No maintenance branch, and patch releases are cut from `main` or from tags: target `main`.
- Unclear: say so when asking the user, with the evidence, and let them choose.

## 2. Port the fix

```bash
git -C <framework-clone> worktree add -b <user>/<slug> <path>/<pr-dir> origin/<target-branch>
git -C <path>/<pr-dir> cherry-pick <fix-commit>
```

Drop any local-only commits (version overrides, debugging). Resolve conflicts against the target
branch's current code; the defect may look different on `main`. Re-run the module's tests on this
branch (no version override needed unless you also install it). If the target branch already
fixed the bug differently, stop and report that instead of opening a duplicate.

## 3. Draft the PR

Use the repo's PR template if there is one (`.github/PULL_REQUEST_TEMPLATE*`). Build the body from
the fix report:

- **Title:** what the fix achieves, short (for example "Send no payload for empty command results
  over Axon Server").
- **Problem:** the user-visible symptom with the exact exception, and the impact.
- **Cause:** the introducing commit and PR, with the precise mechanism.
- **Fix:** what changed and why this shape (for example both sides of a wire format, and which
  version mixes that covers).
- **Tests:** new and changed tests, and that they failed before the fix.
- **Validation:** verified in an application with its workaround removed, with negative control.
- **Compatibility / open questions:** mixes that stay broken, edge cases accepted.
- Link related issues/PRs found in the origin search.

Follow the user's attribution rules in commits and PR text.

## 4. Confirmation gate

Before any `git push` or `gh pr create`, ask the user explicitly and show:

- target repository and base branch,
- head branch name and the commits it contains (`git log --oneline origin/<base>..HEAD`),
- PR title and full body,
- draft or ready (default: draft).

Push and create the PR only after a clear yes. Then report the PR URL. Never open issues, add
labels, request reviewers or comment unless asked.

```bash
git -C <path>/<pr-dir> push -u origin <user>/<slug>
gh pr create --repo <owner/repo> --base <target-branch> --head <user>/<slug> --draft \
  --title "<title>" --body-file <body.md>
```

## 5. Optional review

If the environment provides review tooling for the Axon repositories (for example a PR review
skill), offer to run it on the branch before or after opening the PR. The workflow does not
depend on it.
