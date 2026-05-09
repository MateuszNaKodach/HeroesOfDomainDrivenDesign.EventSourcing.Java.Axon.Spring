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

- **Current Migration Phase:** `Migration Phase #1 — openrewrite (one-shot)`
- **Phase status:** pending
- **Next action (one sentence):** Apply OpenRewrite Path B (Axoniq Commercial) recipe to upgrade Axon 4.13.1 → AF5 dependencies and bulk-port mechanical patterns.
- **Exact recipe:** `openrewrite` with `license-decision=axoniq-commercial`
- **Exact verification command:** _none for Phase 1 — bulk recipe; `./mvnw -DskipTests test-compile` may surface what still needs per-construct migration in phases 2–9 (failures expected)._
- **Awaiting user input?** no
- **Working-tree expectation at resume time:** clean — last commit is the INIT commit. The user's WIP under `.claude/skills/axon4-to-axon5-migration/...` is unrelated to migration commits and must NOT be staged by the orchestrator.
- **Last commit recorded by orchestrator:** _will be filled by INIT commit_

---

## Project metadata

- **Target project:** `/Users/mateusznowak/GitRepos/MateuszNaKodach/HeroesOfDomainDrivenDesign.EventSourcing.Java.Axon.Spring`
- **Started:** 2026-05-09
- **Last updated:** 2026-05-09
- **Active branch:** `af5-migration/test1`
- **Build tool:** Maven (single module)
- **Starting Axon version:** 4.13.1 (`axon-spring-boot-starter`)

---

## Pinned user decisions

Frozen for the run. A fresh session must respect these without re-asking.

- **License target:** `axoniq-commercial` — set at INIT 2026-05-09
- **Recipe scope (openrewrite):** top-level (single-module project)
- **Unsupported features detected at INIT:** none (no sagas, no deadline-manager)
- **Per-feature decision:** n/a
- **Commit cadence:** per-item (default)
- **Storage-engine path:** _set when Phase 9 reached_

---

## Phase status

Legend: `pending` · `in-progress` · `awaiting-checkpoint` · `complete` · `paused` · `skipped`

| # | Recipe | Mode | Status | Items done / total | Last commit |
|---|---|---|---|---|---|
| 1 | openrewrite | one-shot | pending | — | — |
| 2 | aggregate | iterative | pending | 0 / 5 | — |
| 3 | event-processor | iterative | pending | 0 / 5 | — |
| 4 | command-gateway | iterative | pending | 0 / 6 | — |
| 5 | query-gateway | iterative | pending | 0 / 2 | — |
| 6 | query-handler | iterative | pending | 0 / 2 | — |
| 7 | read-configuration | iterative | pending | 0 / 1 | — |
| 8 | write-configuration | iterative | skipped | 0 / 0 (none discovered) | — |
| 9 | event-storage-engine | one-shot | pending | — | — |
| — | stabilization | — | pending | — | — |

> When a phase enters `in-progress`, refresh its detailed section below
> with the current FQ-class enumeration. Don't rely on a fresh session
> re-running discovery — files may move mid-migration.

---

## Per-phase plan

### Migration Phase #1 — openrewrite

- **Recipe(s) run:** _to be filled — Path B (Axoniq Commercial)_
- **Resolved version:** _to be filled by recipe (latest AF5 commercial line)_
- **Diff stat summary:** _to be filled_
- **Behavior changes flagged:** _to be filled_
- **Commit:** _pending_

### Migration Phase #2 — aggregate

| # | FQ aggregate | FQ test | Status | Commit |
|---|---|---|---|---|
| 1 | `com.dddheroes.heroesofddd.armies.write.Army` | `com.dddheroes.heroesofddd.armies.write.ArmyTest` | pending | — |
| 2 | `com.dddheroes.heroesofddd.astrologers.write.Astrologers` | `com.dddheroes.heroesofddd.astrologers.write.AstrologersTest` | pending | — |
| 3 | `com.dddheroes.heroesofddd.calendar.write.Calendar` | `com.dddheroes.heroesofddd.calendar.write.CalendarTest` | pending | — |
| 4 | `com.dddheroes.heroesofddd.creaturerecruitment.write.Dwelling` | `com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingTest` | pending | — |
| 5 | `com.dddheroes.heroesofddd.resourcespool.write.ResourcesPool` | `com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolTest` | pending | — |

**Status legend:** `pending` · `in-progress` · `done` · `deferred: <reason>` · `blocked: <reason>`

**Verify command template (per item):**
```bash
./mvnw test -P migration-aggregate-<AggregateSimpleName> \
  -Dtest='<FQTestClass>' -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false
```

### Migration Phase #3 — event-processor

| # | FQ class | FQ test | Status | Commit |
|---|---|---|---|---|
| 1 | `com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModelProjector` | `com.dddheroes.heroesofddd.creaturerecruitment.read.getdwellingbyid.GetDwellingByIdTest` (covers projector indirectly) | pending | — |
| 2 | `com.dddheroes.heroesofddd.creaturerecruitment.read.getalldwellings.GetAllDwellingsQueryHandler` | `com.dddheroes.heroesofddd.creaturerecruitment.read.getalldwellings.GetAllDwellingsTest` | pending | — |
| 3 | `com.dddheroes.heroesofddd.creaturerecruitment.automation.WhenCreatureRecruitedThenAddToArmyProcessor` | `com.dddheroes.heroesofddd.creaturerecruitment.automation.WhenCreatureRecruitedThenAddToArmyTest` | pending | — |
| 4 | `com.dddheroes.heroesofddd.astrologers.automation.whenweekstartedthenproclaimweeksymbol.WhenWeekStartedThenProclaimWeekSymbolProcessor` | `com.dddheroes.heroesofddd.astrologers.automation.whenweekstartedthenproclaimweeksymbol.WhenWeekStartedThenProclaimWeekSymbolTest` | pending | — |
| 5 | `com.dddheroes.heroesofddd.astrologers.automation.whenweeksymbolproclaimedthenincreasedwellingavailablecreatures.WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreaturesProcessor` | `com.dddheroes.heroesofddd.astrologers.automation.whenweeksymbolproclaimedthenincreasedwellingavailablecreatures.WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreaturesTest` | pending | — |

### Migration Phase #4 — command-gateway

After exclude-when filter (rows whose file also has `@EventHandler` / `@CommandHandler` / `@QueryHandler` / `@MessageHandlerInterceptor` are excluded — those become Phase 3 work).

| # | FQ class | FQ test | Status | Commit |
|---|---|---|---|---|
| 1 | `com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.BuildDwellingRestApi` | `com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.BuildDwellingTest` (E2E) | pending | — |
| 2 | `com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.BuildDwellingMcp` | _none direct_ | pending | — |
| 3 | `com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.IncreaseAvailableCreaturesRestApi` | `com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.IncreaseAvailableCreaturesTest` | pending | — |
| 4 | `com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.IncreaseAvailableCreaturesMcp` | _none direct_ | pending | — |
| 5 | `com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.RecruitCreatureRestApi` | `com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.RecruitCreatureTest` | pending | — |
| 6 | `com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.RecruitCreatureMcp` | _none direct_ | pending | — |

### Migration Phase #5 — query-gateway

| # | FQ class | FQ test | Status | Commit |
|---|---|---|---|---|
| 1 | `com.dddheroes.heroesofddd.creaturerecruitment.read.getdwellingbyid.GetDwellingByIdRestApi` | `com.dddheroes.heroesofddd.creaturerecruitment.read.getdwellingbyid.GetDwellingByIdTest` | pending | — |
| 2 | `com.dddheroes.heroesofddd.creaturerecruitment.read.getalldwellings.GetAllDwellingsMcp` | _none direct_ | pending | — |

### Migration Phase #6 — query-handler

| # | FQ class | FQ test | Status | Commit |
|---|---|---|---|---|
| 1 | `com.dddheroes.heroesofddd.creaturerecruitment.read.getdwellingbyid.GetDwellingByIdQueryHandler` | `com.dddheroes.heroesofddd.creaturerecruitment.read.getdwellingbyid.GetDwellingByIdTest` | pending | — |
| 2 | `com.dddheroes.heroesofddd.creaturerecruitment.read.getalldwellings.GetAllDwellingsQueryHandler` | `com.dddheroes.heroesofddd.creaturerecruitment.read.getalldwellings.GetAllDwellingsTest` | pending | — |

### Migration Phase #7 — read-configuration

| # | FQ class | FQ test | Status | Commit |
|---|---|---|---|---|
| 1 | `com.dddheroes.heroesofddd.maintenance.write.resetprocessor.StreamProcessorsOperations` | _none direct_ | pending | — |

### Migration Phase #8 — write-configuration

_No `@Configuration` beans returning `Configurer` / `ConfigurerModule` / `EventProcessingConfigurer` were detected. Phase will be skipped unless new candidates appear after Phase 1._

### Migration Phase #9 — event-storage-engine

- **Path chosen:** _to be set — likely Path A (JPA event store via `axon-spring-boot-starter` + PostgreSQL)_
- **Evidence:** `axon-spring-boot-starter` 4.13.1 + JPA + PostgreSQL Testcontainers; no explicit `EventStorageEngine` / `EmbeddedEventStore` / `AxonServerEventStore` bean declared in code (auto-config provides JPA event store).
- **Configuration class touched:** _to be filled_
- **SQL migration script:** _to be generated under `sql/` if Path A_
- **SQL applied to build's database?** no
- **Commit:** pending

### Stabilization

- **Pre-flight (Phase #9 SQL applied if applicable):** pending
- **`./mvnw clean verify` first run:** pending
- **Outstanding compile errors:** _to be filled_
- **Outstanding test failures:** _to be filled_
- **Deferred items folded forward:** _to be filled_
- **Behavior changes confirmed by user:** _to be filled_
