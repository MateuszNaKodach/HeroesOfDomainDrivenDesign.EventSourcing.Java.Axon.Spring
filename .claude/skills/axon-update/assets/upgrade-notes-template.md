# Upgrade notes: Axon Framework / Axoniq Framework <current> to <target>

Upgrade date: <YYYY-MM-DD>. Artifacts: <which groups, where from (Maven Central, internal repo)>.
<One or two sentences on anything that changed about how the app is built, for example
"no longer depends on locally built snapshots".>

Sources: release notes of <list the AxonFramework and axoniq-framework tags read, with links>,
and the <target> sources jars.

## Result

| Step                                     | <current> (baseline) | <target>, no code changes | <target> + fixes |
|------------------------------------------|----------------------|---------------------------|------------------|
| Compile (main + test)                    |                      |                           |                  |
| New deprecation warnings                 | -                    |                           |                  |
| Tests (<total>)                          |                      |                           |                  |
| Compatibility run, <mode 1, default>     | -                    |                           |                  |
| Compatibility run, <mode 2>              | -                    |                           |                  |

Changes in this repository:

- `<build file>`: <version properties> set to `<target>`.
- `<file>`: <what and why, one line each; "none" if the bump was enough>.

Runtime dependency changes (resolved runtime classpath, old vs new): <versions that moved,
artifacts added or removed; "same set, versions only" when that is the case>.

## Regressions

<One subsection per regression. Write "None found." when there are none.>

### Regression <n>: <short description> (<worked around | fixed in app | open>)

What worked before: <behavior on <current>, as the caller saw it>.

What happens on <target>:

```
<exact exception / error message, trimmed to the relevant frames>
```

Affected: <modes/profiles, call sites (classes, endpoints), failing tests>.

Runtime effect: <for example "the command is applied but the caller gets an error";
what that means for clients>.

Cause: <the framework code path, class and method, in the target version, compared with
the old version>.

Origin:

- <repo> commit `<full or 10+ char sha>` "<title>" (<date>), PR <repo>#<n>.
- <related upstream change it follows, if any>.
- First affected release: <tag>. Not in: <earlier tags checked>. On `main`: <yes/no, as of date>.

Fix or workaround: <what was changed, where, why this shape (central, removable),
how to switch it off (property), when to remove it>.

## Fixed between <RC or intermediate version> and <target> (no action needed)

<Problems seen in an earlier attempt or RC that are gone in the target, each with the
upstream PR/commit. Delete the section if not applicable.>

## Behavior changes without code changes

<Changes that do not fail anything but differ from before: new defaults, new log
messages, licensing, performance hints. For each: what changed, upstream reference,
why it is acceptable for this app or how to configure it back.>

## Deprecations

<New deprecation warnings: API, replacement, where used, decision (kept / migrated) and why.
"None." if none.>

## Release-note items checked and not affecting this app

<One line per relevant flagged item: item (repo #PR) and why it does not affect the app.>

## Compatibility run (old data, new version)

Procedure: <one paragraph: old jar wrote data through the API, stopped, new jar on the same
stores; infrastructure and versions used, including the Axon Server image version; one
database/context per mode>.

| Check                                                     | <mode 1> | <mode 2> | <mode 3> |
|-----------------------------------------------------------|----------|----------|----------|
| Tokens unchanged at startup                               |          |          |          |
| No reprocessing (events, read models, automations)        |          |          |          |
| Segments re-claimed, processors continue                  |          |          |          |
| Entities from old events enforce rules                    |          |          |          |
| New events appended to old streams                        |          |          |          |
| Snapshots over old and new events                         |          |          |          |
| Automations / dead letters                                |          |          |          |
| Subscription queries                                      |          |          |          |
| Schema diff                                               |          |          |          |
| Rollback (old version on new data)                        |          |          |          |

<Notes: anything observed that is not a pass/fail, for example status codes identical on
both versions.>

## Not tested / open risks

<Mixed-version rolling update analysis, modes not covered, flows the API cannot reach,
anything else the reader should not assume was verified.>
