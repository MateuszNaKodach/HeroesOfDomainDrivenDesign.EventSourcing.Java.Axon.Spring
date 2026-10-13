# Triage Axon 5 release notes

Axon 5 ships as two products released together with the same version number:

| Repo | Tags | Contains |
|---|---|---|
| `AxonIQ/AxonFramework` | `axon-X.Y.Z`, `axon-X.Y.Z-RCn` | Open-source core: messaging, modelling, event sourcing, Spring/Spring Boot extensions, test fixtures |
| `AxonIQ/axoniq-framework` | `axoniq-X.Y.Z`, `axoniq-X.Y.Z-RCn` | Commercial modules: Axon Server connector, distributed messaging, Postgres engine, DLQ, multi-tenancy, tracing, legacy support, workflows, `axoniq-spring-boot-starter` |

An app on `axoniq-spring-boot-starter` uses both, so read both. Notes of a GA release
usually repeat everything from its RCs; `release_notes.py` lists each item once (under
the first tag that mentions it). RC notes in the commercial repo are sometimes just a
"Full Changelog" link; then the GA notes or the compare view carry the content.

## What is in range

Read every release with `current < version <= target`. A `-SNAPSHOT` baseline is fuzzy:
it may already contain part of the next release. Treat its own release as in range and
verify against the code when an item matters.

## Which items can change behavior without failing a compile

Release notes are written from the framework's point of view ("enhancement", "bug fix").
From the app's point of view, these are the ones to check against your configuration and
code, with the question to ask:

| Category | Why it matters | Question for the app |
|---|---|---|
| New defaults, opt-in/opt-out | Behavior changes on upgrade with zero code changes | Does the app now get a feature it did not have (timeouts, multi-tenancy, local dispatch)? |
| Autoconfiguration, bean registration | Spring may now create, skip, or duplicate beans | Does the app inject by type something that now has several candidates? Does it override a bean the autoconfig now conditions differently? |
| Starter/module dependencies | A dependency turning `optional` disappears from the starter | Does the app use classes from a module it does not declare? (Check the dependency diff) |
| Connector / distribution | Axon Server or other distributed buses change wire behavior | Results, errors, routing, subscription queries across nodes, mixed versions during rolling update |
| Processors, tokens, segments, sequencing | Affects every stored token and event handling order | Custom token store, token-store name, sequencing policies, initial tokens, replay/reset behavior, segment claims |
| Timeouts | New timeouts can cancel long handlers or units of work | Long-running handlers, batch processing, blocking calls in handlers |
| Event store, snapshots, consistency | Storage format and sourcing paths | Custom engines, snapshot stores, DCB vs aggregate-based, append conditions |
| Conversion / serialization | Payloads, results, metadata on the wire and at rest | Custom converters, `resultAs(...)`, `payloadAs(...)`, upcasters, Jackson 2 vs 3 |
| Results, nullability, interceptors, unit of work | Small contract changes show up as runtime errors | `void` handlers, interceptors that send commands, code that checks for `null` |
| Dead letters | Queue schema and processing | DLQ-enabled processors, metadata conversion |
| Multi-tenancy, licensing | Activation by classpath presence, entitlement behavior | Is a tenant module on the classpath? Does the app run without a license in some environment? |
| Breaking, deprecated, removed, renamed | Compile errors or warnings | Grep the app for the API |
| Schema / SQL / transactions | Hibernate `ddl-auto`, isolation levels, autocommit | Schema migrations, ambient transactions, connection usage |

`release_notes.py` flags lines by keyword per category. Keywords over-match on purpose;
read each flagged line and write one verdict.

## Writing verdicts

Keep a short list while reading; it feeds the report directly:

```
- AF #4809 token-store name unset by default: not affecting, app uses the autoconfigured bean named tokenStore.
- AF #4980 handler/UoW timeouts on by default: affecting (behavior change), handlers are short; keep, document.
- AXONIQ #591 starter depends on the Axon Server connector again: fixes an RC1 problem; nothing to do.
```

When a line is ambiguous, open the PR (`gh pr view <n> -R <repo>`) or issue
(`gh issue view <n> -R <repo>`); the description often names the exact property or class.

## Fallbacks

- No `gh`: the script uses the public GitHub API (60 requests per hour unauthenticated;
  set `GITHUB_TOKEN`). Or open `https://github.com/<repo>/releases`.
- Compare two tags: `gh api repos/<repo>/compare/<tagA>...<tagB> --jq '.commits[].commit.message'`
  or `https://github.com/<repo>/compare/<tagA>...<tagB>`.
- The reference guide's release notes pages (per minor version) summarize breaking changes
  in prose; read them for minor updates.
