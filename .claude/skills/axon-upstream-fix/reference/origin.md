# Owning repository and origin of a framework bug

Use this for steps 2 and 3 of the skill. Goal: name the repo, module, introducing commit, PR,
first affected tag, last good tag, and status on `main`, with evidence.

## Map the class to a repository

| Package / artifact | Repository | Notes |
|---|---|---|
| `org.axonframework.*` (groupId `org.axonframework`, `org.axonframework.extensions.*`) | `AxonIQ/AxonFramework` | Open source. Tags `axon-X.Y.Z`. Spring Boot support lives under `extensions/spring`. |
| `io.axoniq.framework.*` (groupId `io.axoniq.framework`) | `AxonIQ/axoniq-framework` | Commercial; needs access. Tags `axoniq-X.Y.Z`. Axon Server connector, distributed messaging, tracing, multitenancy, legacy, Spring Boot starter. |
| `io.axoniq.axonserver.connector.*` (artifact `axonserver-connector-java`) | `AxonIQ/axonserver-connector-java` | Low-level gRPC client, separate release line. |
| Other `org.axonframework.extensions.*` not in the main repo | the extension's own repo | Check the artifact's POM `<scm>` element. |

When unsure, read the artifact's POM in `~/.m2` (`<scm>` and `<url>`), or the `META-INF/MANIFEST.MF`
of the jar.

Find the module inside the repo: search for the file path from the sources jar.

```bash
gh search code "class <SimpleClassName>" --repo <owner/repo> --json path -q '.[].path'
# or in a local clone:
git -C <clone> ls-files | grep '/<SimpleClassName>.java$'
```

## Read the exact code the app runs

```bash
./mvnw -q dependency:get -Dartifact=<groupId>:<artifactId>:<version>:jar:sources
unzip -p ~/.m2/repository/<group/path>/<artifactId>/<version>/<artifactId>-<version>-sources.jar \
  '<package/path>/<Class>.java' | less
```

Reason from this file, not from `main` and not from memory. Note the exact lines involved.

## Find the introducing change

```bash
R=<owner/repo>; P=<path/in/repo/Class.java>
# history of the file
gh api "repos/$R/commits?path=$P&per_page=20" \
  --jq '.[] | .sha[0:10]+" "+.commit.committer.date+" "+(.commit.message|split("\n")[0])'
# commits between last good and first bad tag that touch the file
gh api "repos/$R/compare/<good-tag>...<bad-tag>" --jq '.files[].filename' | grep -F "$P"
# the PR that brought a commit in
gh api "repos/$R/commits/<sha>/pulls" --jq '.[] | "#\(.number) \(.title) merged=\(.merged_at)"'
# is a commit contained in a tag? ("ahead" or "identical" = contained, "diverged"/"behind" = not)
gh api "repos/$R/compare/<sha>...<tag>" --jq .status
# current state on main
gh api "repos/$R/contents/$P?ref=main" -H "Accept: application/vnd.github.raw" | sed -n '<from>,<to>p'
```

Large compares are truncated at 300 files by the API; when that happens, use a local clone:
`git log --oneline <good-tag>..<bad-tag> -- <path>`.

Also check the paired repo: a commercial change is often a follow-up to an open-source PR (or the
other way around). Look for a linked PR with a matching title in the other repo and cite both.

Record:

- introducing commit (full hash) and its PR,
- related PR in the paired repo, if any,
- last good tag and first affected tag (check patch tags of the previous minor too),
- whether `main` and any maintenance branches still have it.

## Search for existing reports

```bash
gh search issues --repo <owner/repo> "<distinctive error text>" --json number,title,state
gh search prs    --repo <owner/repo> "<Class or method name>"    --json number,title,state
```

Link anything relevant in the report. Do not open, comment on, or react to issues or PRs unless
the user asks.
