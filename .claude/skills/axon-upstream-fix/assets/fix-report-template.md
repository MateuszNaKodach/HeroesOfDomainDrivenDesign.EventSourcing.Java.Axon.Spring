# <Short title of the defect> - upstream fix report

Date: <yyyy-mm-dd>. Status: <local only | PR draft opened: URL>.

## Summary

<Three sentences: the symptom in the app, the root cause and introducing change, the fix and how
it was validated.>

## Symptom in the application

- Application / module: <name, framework versions where it works and where it fails>
- Failing tests: <Class#method, profiles/properties needed>
- Exception (verbatim):

```
<exception, framework frames, root Caused by>
```

- Impact: <what the user or system observes beyond the exception>
- Workaround in the app: <class/config, toggle or how to remove it>

## Origin

| Item | Value |
|---|---|
| Repository / module | <owner/repo>, <module path> |
| Defective code | <Class#method, lines in the released sources> |
| Introducing commit | <full hash> "<subject>" |
| Pull request | <#number title>; related in paired repo: <#number or n/a> |
| Last good tag | <tag> |
| First affected tag | <tag> |
| Still on main / maintenance branches | <yes/no per branch, how checked> |
| Existing issue or PR | <links or "none found (searched: ...)"> |

## Root cause

<The precise mechanism, both sides if it is a wire format or contract between components.>

## Fix

- Worktree / branch: <path>, <branch> (based on <released tag>)
- Commit: <hash> "<subject>" (local only)
- Diff: <files, +/- lines>
- What changed and why this shape: <...>
- Compatibility matrix (for distributed or persisted formats):

| Sender | Receiver | Before fix | After fix |
|---|---|---|---|
| <old> | <new> | <ok/broken> | <ok/broken> |

## Tests

- New / changed tests: <names; note any test that asserted the buggy behavior>
- RED (before the fix):

```
<failure excerpt showing the same error as the app>
```

- GREEN: <new tests totals>, module suite <totals>
- Integration test: <added / not added and why>

## Local build

- Command: <exact command with the version override>
- Installed artifacts: <paths>
- Released artifacts unchanged: <how verified, result>

## Validation in the application

- Child worktree / branch: <path>, <branch>, base <commit>
- Pin: <artifact -> snapshot version>; other framework artifacts: <released version>
- Workaround: <deleted | disabled via toggle>
- Dependency resolution: <excerpt showing only the fixed artifact on the snapshot>
- Full suite: <totals> (baseline with workaround: <totals>)
- Negative control (released version, no workaround): <failing tests and error excerpt>
- Re-green with the snapshot: <totals>
- Compatibility rerun: <done for which storage mode / not done and why>

## Open questions and accepted edge cases

- <...>

## Proposed PR

- Repository / base branch: <owner/repo> <branch> (why this branch)
- Head branch: <branch>, commits: <list>
- Title: <title>
- Body:

```
<PR body following the repo template>
```

Pushing and opening the PR require the user's explicit confirmation.

## Hand-back to the application

- Keep the workaround until: <release containing the fix>
- To remove it then: <delete class/config, drop pin, rerun suite>
