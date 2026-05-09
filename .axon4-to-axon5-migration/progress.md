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

- **Current Migration Phase:** `Migration Phase #2 — aggregate (iterative)`
- **Phase status:** pending (Phase 1 complete)
- **Next action (one sentence):** Migrate aggregate `com.dddheroes.heroesofddd.armies.write.Army` (the OpenRewrite recipe in Phase 1 already converted it to `@EventSourced` + `EventAppender` — Phase 2's job is per-aggregate verification, restoring `CREATE_IF_MISSING` semantics where needed via AF5's new entity-creation model, and seeding the per-target Maven profile).
- **Exact recipe:** `aggregate` with `target=com.dddheroes.heroesofddd.armies.write.Army`
- **Exact verification command:**
  ```bash
  ./mvnw test -P migration-aggregate-Army \
    -Dtest='com.dddheroes.heroesofddd.armies.write.ArmyTest' \
    -DfailIfNoTests=false \
    -Dsurefire.failIfNoSpecifiedTests=false
  ```
  (Profile `migration-aggregate-Army` will be seeded by the recipe — see [maven-profile/maven-profile.md](../.claude/skills/axon4-to-axon5-migration/references/maven-profile/maven-profile.md).)
- **Awaiting user input?** no
- **Working-tree expectation at resume time:** clean — last migration commit is Phase 1 (`chore(af5-migration): apply OpenRewrite recipe UpgradeAxon4ToAxoniq5@5.1.1-SNAPSHOT (Migration Phase #1)`). The user's WIP under `.claude/skills/axon4-to-axon5-migration/...` is unrelated and must NOT be staged by the orchestrator.
- **Last commit recorded by orchestrator:** `2e10065` — `chore(af5-migration): initialize migration` (Phase 1 commit SHA recorded in next item's commit per chicken-and-egg rule)

---

## Project metadata

- **Target project:** `/Users/mateusznowak/GitRepos/MateuszNaKodach/HeroesOfDomainDrivenDesign.EventSourcing.Java.Axon.Spring`
- **Started:** 2026-05-09
- **Last updated:** 2026-05-09 (Phase 1 complete)
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
| 1 | openrewrite | one-shot | complete | n/a | _this commit (see `git log --grep='Migration Phase #1'`)_ |
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

- **Recipe(s) run:** `org.axonframework.migration.UpgradeAxon4ToAxoniq5` (Path B — Axoniq Commercial). Composes the free leg first (`UpgradeAxon4ToAxon5` → `UpgradeSpringBoot_3_5` → `Axon4ToAxon5Messaging` → `Axon4ToAxon5Modelling` → `Axon4ToAxon5Test`) then layers commercial-only rewrites (`Axon4ToAxoniq5Testcontainer`, BOM/starter swaps to `io.axoniq.framework`).
- **Resolved version:** `axon-migration:5.1.1-SNAPSHOT` (LATEST resolved to AF4 4.13.1 from local cache, incompatible with current `rewrite-maven-plugin:6.39.0` — fell back to explicit SNAPSHOT). OpenRewrite plugin: `6.39.0`.
- **Diff stat summary:** 75 files modified (1 pom.xml, 1 application.yaml, 73 .java). `git diff --stat` ≈ 624 insertions / 479 deletions.
- **Behavior changes flagged:**
  - `pom.xml`: starter switched `org.axonframework:axon-spring-boot-starter:4.13.1` → `io.axoniq.framework:axoniq-spring-boot-starter:5.1.1-SNAPSHOT`. Added explicit `org.axonframework:axon-eventsourcing` and `axon-modelling` 5.1.1-SNAPSHOT. Added `io.axoniq.framework:axoniq-testcontainer:5.1.1-SNAPSHOT` (test). Spring Boot 3.5.4 → 3.5.14, springdoc 2.8.5 → 2.8.17.
  - `<java.version>` left at 23 (recipe did not bump to 25 in this run; active JDK is 25 so no immediate impact).
  - **`@CreationPolicy(CREATE_IF_MISSING)` removed** from 4 aggregates (Army, Astrologers, Calendar, Dwelling). AF5's `@EventSourced` reframes creation; Phase 2 will restore equivalent semantics per aggregate (likely via AF5 entity-creator pattern). Stranded comments in source still reference `CREATE_IF_MISSING`.
  - Aggregates rewritten to `@EventSourced(tagKey="...", idType=...)`, command handlers gain `EventAppender eventAppender` parameter, `apply(...)` calls become `eventAppender.append(...)`. Imports moved: `org.axonframework.eventsourcing.annotation.EventSourcingHandler`, `org.axonframework.messaging.commandhandling.annotation.CommandHandler`, `org.axonframework.extension.spring.stereotype.EventSourced`.
  - `AggregateTestFixture` → `AxonTestFixture` (`org.axonframework.test.fixture.AxonTestFixture`); fixture API rewritten via `MigrateAxonTestFixtureFluentApi`.
  - `org.axonframework.test.server.AxonServerContainer` → `io.axoniq.framework.testcontainer.AxonServerContainer`.
  - Several events (e.g. `CreatureAddedToArmy`, `WeekSymbolProclaimed`, `DayStarted`, `DwellingBuilt`, `CreatureRecruited`, `AvailableCreaturesChanged`, `ResourcesDeposited`, `ResourcesWithdrawn`) modified — likely added/changed annotations for AF5 event metadata.
  - `application.yaml` modified (7 lines) — likely processor/storage config keys renamed.
  - assertj-core dependency cleaned (BOM-managed).
  - `axon.version` property updated 4.13.1 → 5.1.1-SNAPSHOT but the new deps hardcode 5.1.1-SNAPSHOT directly; property reuse can be re-tightened later.
- **Estimate from OpenRewrite log:** "time saved: 26h 16m".
- **Commit:** _this commit_ (subject: `chore(af5-migration): apply OpenRewrite recipe UpgradeAxon4ToAxoniq5@5.1.1-SNAPSHOT (Migration Phase #1)`)

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
