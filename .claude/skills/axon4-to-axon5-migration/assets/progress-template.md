# Axon Framework 4 → 5 Migration — Progress

> Single source of truth for this project's migration. A fresh session
> with **zero prior context** must be able to read this file alone and
> resume exactly where the previous session stopped.
>
> **Update protocol:** rewrite the relevant section, THEN commit. Never
> split "did the work" and "wrote progress.md" across commits.

## Goal

Fully compiling, green-test codebase on AF5, **same architecture as AF4**:
- No DCB. Aggregate-centric model retained.
- No new patterns. Sagas / projections / messages unchanged in shape.
- Legacy event storage preserved — `AggregateBased…EventStorageEngine` reads
  existing event log.
- `./mvnw clean verify` green at end of stabilization.

Intermediate phases will leave the project non-compiling — by design.
Per-recipe `migration-<recipe>-<Target>` Maven profiles keep verification
scoped through phases 2–8. Stabilization drops all `migration-*` profiles.

> Manual work is sometimes unavoidable. Out-of-scope features, custom
> subclasses, bespoke config may need user judgment. Orchestrator records
> `blocked` / `deferred-to-stabilization` and keeps moving.

---

## ▶︎ RESUME HERE — read this first

The single block a fresh session needs to make the next move. Keep
**current** and **concrete** (FQ class names, exact commands).

- **Current Migration Phase:** _e.g. `Migration Phase #2 — aggregate (iterative)`_
- **Phase status:** _pending / in-progress / awaiting-checkpoint / paused / complete_
- **Next action (one sentence):** _e.g. "Migrate aggregate `org.example.Faculty`."_
- **Exact recipe:** _e.g. `aggregate` with `target=org.example.Faculty`_
- **Exact verification command:**
  ```bash
  ./mvnw -f <target>/pom.xml test -P migration-aggregate-Faculty \
    -Dtest='org.example.FacultyTest' -DfailIfNoTests=false \
    -Dsurefire.failIfNoSpecifiedTests=false
  ```
- **Awaiting user input?** _yes (and the question) / no_
- **Working-tree expectation at resume time:** _clean — last commit `<sha>` is the previous item. If dirty → previous session crashed mid-step._
- **Last commit recorded by orchestrator:** `<short-sha>` — `<commit subject>`

---

## Project metadata

- **Target project:** `<absolute-path>`
- **Started:** `<YYYY-MM-DD>`
- **Last updated:** `<YYYY-MM-DD HH:MM>`
- **Active branch:** `<branch-name>`
- **Build tool:** _Maven / Gradle (Maven only is fully automated)_

---

## Pinned user decisions

Frozen for the run. A fresh session must respect these without re-asking.

- **License target:** _free-af5 / axoniq-commercial — set at INIT_
- **Recipe scope (openrewrite):** _top-level / per-module subset (list)_
- **Unsupported features detected at INIT:** _none / list (saga, deadline-manager, …)_
- **Per-feature decision:** _one line per feature: `<feature>: accept-stays-af4 / pause / remove-first`_
- **Commit cadence:** _per-item (default) / per-phase squashed / no auto-commits_
- **Storage-engine path:** _A (JPA) / B (Axon Server) / C (non-Spring) — set when reached_

---

## Phase status

Legend: `pending` · `in-progress` · `awaiting-checkpoint` · `complete` · `paused` · `skipped`

| # | Recipe | Mode | Status | Items done / total | Last commit |
|---|---|---|---|---|---|
| 1 | openrewrite | one-shot | pending | — | — |
| 2 | aggregate | iterative | pending | 0 / ? | — |
| 3 | event-processor | iterative | pending | 0 / ? | — |
| 4 | command-gateway | iterative | pending | 0 / ? | — |
| 5 | query-gateway | iterative | pending | 0 / ? | — |
| 6 | query-handler | iterative | pending | 0 / ? | — |
| 7 | read-configuration | iterative | pending | 0 / ? | — |
| 8 | write-configuration | iterative | pending | 0 / ? | — |
| 9 | event-storage-engine | one-shot | pending | — | — |
| — | stabilization | — | pending | — | — |

> When a phase enters `in-progress`, fill its detailed section below with the
> **complete enumerated plan** (every FQ class). Don't rely on a fresh session
> re-running discovery — files may move mid-migration.

---

## Per-phase plan

### Migration Phase #1 — openrewrite

- **Recipe(s) run:** _e.g. `org.axonframework.migration.UpgradeAxon4ToAxoniq5`_
- **Resolved version:** _e.g. `5.1.0`_
- **Diff stat summary:** _N files changed_
- **Behavior changes flagged:** _verbatim_
- **Commit:** `<short-sha>` — `chore(af5-migration): apply OpenRewrite recipe …`

### Migration Phase #2 — aggregate

| # | FQ aggregate | FQ test | Status | Commit |
|---|---|---|---|---|
| 1 | `org.example.Faculty` | `org.example.FacultyTest` | pending | — |

**Status legend:** `pending` · `in-progress` · `done` · `deferred: <reason>` · `blocked: <reason>`

**Verify command template (per item):**
```bash
./mvnw -f <target>/pom.xml test -P migration-aggregate-<AggregateSimpleName> \
  -Dtest='<FQTestClass>' -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false
```

**Combined across migrated items:** `-P migration-aggregate-Faculty,migration-aggregate-Calendar,…` (Maven merges `<includes>`).

### Migration Phase #3 — event-processor

| # | FQ class | FQ test | Status | Commit |
|---|---|---|---|---|
| 1 | … | … | pending | — |

### Migration Phase #4 — command-gateway

| # | FQ class | FQ test | Status | Commit |
|---|---|---|---|---|
| 1 | … | … | pending | — |

### Migration Phase #5 — query-gateway

| # | FQ class | FQ test | Status | Commit |
|---|---|---|---|---|
| 1 | … | … | pending | — |

### Migration Phase #6 — query-handler

| # | FQ class | FQ test | Status | Commit |
|---|---|---|---|---|
| 1 | … | … | pending | — |

### Migration Phase #7 — read-configuration

| # | FQ class | FQ test | Status | Commit |
|---|---|---|---|---|
| 1 | … | … | pending | — |

### Migration Phase #8 — write-configuration

| # | FQ class | FQ test | Status | Commit |
|---|---|---|---|---|
| 1 | … | … | pending | — |

### Migration Phase #9 — event-storage-engine

- **Path chosen:** _A (JPA) / B (Axon Server) / C (non-Spring)_
- **Evidence:** _AF4 beans observed + dependencies_
- **Configuration class touched:** _FQ class — bean(s) replaced_
- **SQL migration script:** _path under `sql/` (Path A) or "n/a"_
- **SQL applied to build's database?** _no (pending) / yes — date_
- **Commit:** `<short-sha>` — `feat(af5-migration): wire AggregateBased…EventStorageEngine`

### Stabilization

- **Pre-flight (Phase #9 SQL applied if applicable):** _yes / no / n/a_
- **`./mvnw clean verify` first run:** _PASS / FAIL — module(s) failing_
- **Outstanding compile errors:**
  - `<FQ class>` — `<one-line cause>` — `pending` / `fix in <sha>`
- **Outstanding test failures:**
  - `<FQ test method>` — `<one-line cause>` — `pending` / `fix in <sha>`
- **Deferred items folded forward:** _list_
- **Behavior changes confirmed by user:** _list_
