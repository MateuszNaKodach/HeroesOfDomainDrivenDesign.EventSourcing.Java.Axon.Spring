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

- **Current Migration Phase:** `Migration Phase #6 — query-handler (iterative)` — 1/2 done.
- **Phase status:** Phase 6 in-progress (item 1 done, item 2 pending).
- **Next action (one sentence):** Migrate the second `@QueryHandler` class `com.dddheroes.heroesofddd.creaturerecruitment.read.getalldwellings.GetAllDwellingsQueryHandler` (dual-natured — `@EventHandler` shape already migrated in Phase 3; query-handler recipe finishes the unit).
- **Exact recipe:** `query-handler` with `target=com.dddheroes.heroesofddd.creaturerecruitment.read.getalldwellings.GetAllDwellingsQueryHandler`
- **Exact verification command:** `./mvnw -P migration-query-handler-GetAllDwellingsQueryHandler clean test-compile -DskipTests -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false`
- **Awaiting user input?** no
- **Working-tree expectation at resume time:** clean — last migration commit is Phase 6 / item 1 (GetDwellingByIdQueryHandler).
- **Last commit recorded by orchestrator:** _this commit_ — `refactor(af5-migration): migrate query-handler GetDwellingByIdQueryHandler to AF5 (Migration Phase #6)`

### Phase 6 progress

| # | Class | Notes | Result |
|---|---|---|---|
| 1 | GetDwellingByIdQueryHandler | Single-natured `@QueryHandler` — already on AF5 import, no `MetaData`/`@MetaDataValue`/`UnitOfWork` params | recipe-pre-migrated by Phase 1 OpenRewrite — no code change. Added scoped profile, clean compile passes. |

### Phase 5 summary (all 2 query-gateway callers done)

| # | Caller | Shape | Commit |
|---|---|---|---|
| 1 | GetDwellingByIdRestApi | MVC controller — recipe-pre-migrated by OpenRewrite (no code change, profile only) | `b73a4f8` |
| 2 | GetAllDwellingsMcp | MCP synchronous resource handler — replaced bare `.get()` with `.orTimeout(30, SECONDS).join()` per recipe step 7 | _this commit_ |

**Pattern recap.** OpenRewrite already migrated the bulk: AF5 `QueryGateway` import, typed `query(payload, R.class)` overload, no `ResponseTypes` wrapper, no named queries (handlers use plain `@QueryHandler`). Substantive AF5 work was confined to item 2 only: a synchronous MCP resource callback was using bare `.get()` (preflight item 6 violation), now `.orTimeout(30, TimeUnit.SECONDS).join()` — `CompletionException` is unchecked and the existing `catch (Exception)` keeps matching it.

**Combined scoped compile across both Phase 5 profiles passes:**
```bash
./mvnw -P migration-query-gateway-GetDwellingByIdRestApi,migration-query-gateway-GetAllDwellingsMcp \
  clean test-compile -DskipTests -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
```

### Phase 4 summary (all 6 command-gateway callers done)

| # | Caller | Shape | Commit |
|---|---|---|---|
| 1 | BuildDwellingRestApi | MVC controller, single `.send(cmd, metadata)` return | `69a686a` (also bundled the user's `.claude/skills/...` WIP — see note below) |
| 2 | BuildDwellingMcp | MCP `Tools` adapter, `.thenApply(...).exceptionally(...)` chain | `18c618f` |
| 3 | IncreaseAvailableCreaturesRestApi | MVC controller, single `.send(cmd, metadata)` return | `f4f31b4` |
| 4 | IncreaseAvailableCreaturesMcp | MCP `Tools` adapter, `.thenApply(...).exceptionally(...)` chain | `0c34152` |
| 5 | RecruitCreatureRestApi | MVC controller, single `.send(cmd, metadata)` return | `d1c4590` |
| 6 | RecruitCreatureMcp | MCP `Tools` adapter, `.thenApply(...).exceptionally(...)` chain | _this commit_ |

**Pattern recap.** OpenRewrite (Phase 1) had already done all the bulk:
- `CommandGateway` import switched to AF5 location (`org.axonframework.messaging.commandhandling.gateway.CommandGateway`).
- `GameMetaData` already returns AF5 `org.axonframework.messaging.core.Metadata`.

The single substantive AF5 change per caller was the dispatch return shape: AF4 `commandGateway.send(cmd, metadata)` returned `CompletableFuture<Void>`, but AF5 returns `CommandResult` — not assignable to `CompletableFuture`. Recipe Step 3/4 fix is `.resultAs(Void.class)`:

- **MVC controllers (3):** appended `.resultAs(Void.class)` directly to the `return commandGateway.send(...)` line.
- **MCP adapters (3):** inserted `.resultAs(Void.class)` between `.send(...)` and `.thenApply(...)`.

`CommandGateway` field, constructor, and parameter kept in all 6 — top-of-chain callers (REST / MCP request → first cause, no active `ProcessingContext`) are exactly the case AF5 javadoc earmarks for `CommandGateway` (NOT `CommandDispatcher`).

**Combined scoped compile across all 6 profiles passes:**
```bash
./mvnw -P migration-command-gateway-BuildDwellingRestApi,migration-command-gateway-BuildDwellingMcp,migration-command-gateway-IncreaseAvailableCreaturesRestApi,migration-command-gateway-IncreaseAvailableCreaturesMcp,migration-command-gateway-RecruitCreatureRestApi,migration-command-gateway-RecruitCreatureMcp \
  test-compile -DskipTests -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
```

**Note on commit `69a686a`** (BuildDwellingRestApi): the orchestrator used `git add <paths>` followed by `git commit` (no paths) which committed everything in the index — sweeping the user's pre-existing `.claude/skills/axon4-to-axon5-migration/...` WIP (the migration skill itself) into the migration commit (49 files changed). Subsequent items 2–6 used `git commit <explicit paths>` to stay scoped to 3 files each. Net effect: the skill files are now committed, but bundled with item 1 instead of as a standalone commit. User can `git reset --soft 69a686a^` and re-split if desired.

### Phase 3 summary (all 5 event-processors done)

| # | Processor | Commit |
|---|---|---|
| 1 | DwellingReadModelProjector (pure projector) | `cba9cc2` |
| 2 | GetAllDwellingsQueryHandler (`@EventHandler` shape only — `@QueryHandler` deferred to Phase 6) | `df2a674` |
| 3 | WhenCreatureRecruitedThenAddToArmyProcessor (try/catch compensation) | `d5ca747` |
| 4 | WhenWeekStartedThenProclaimWeekSymbolProcessor (multi-DI, conditional dispatch inverted) | `b08748b` |
| 5 | WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreaturesProcessor (loop dispatch + projector) | _this commit_ |

**Pattern recap.** OpenRewrite (Phase 1) had already done the bulk: `@ProcessingGroup` → `@Namespace`, `@EventHandler` / `@DisallowReplay` / `@MetadataValue` import moves, `CommandGateway` field → `CommandDispatcher` parameter, `sendAndWait(...)` → `send(...).getResultMessage()` (where applicable), constructor + field cleanup, return type `void` → `CompletableFuture<?>`. The only manual work per processor was recipe Step 7 — moving `axon.eventhandling.processors.<group>.sequencing-policy: gameIdSequencingPolicy` from YAML onto each class as `@SequencingPolicy(type = MetadataSequencingPolicy.class, parameters = GameMetaData.GAME_ID_KEY)`. Items 4 and 5 also needed branch-shape adjustments (early-return inversion / `CompletableFuture.allOf(...)` loop pattern).

**Pinned for stabilization:**
- `gameIdSequencingPolicy` `@Bean` in `GameConfiguration.java` is now obsolete — all 5 processors are annotated. Bean can be deleted in Phase 8 (`write-configuration`) or stabilization.
- All E2E `@SpringBootTest` tests for these processors are deferred — they use AF4 shapes (`eventGateway.publish(DomainEventMessage)`, `GenericDomainEventMessage`, `verify(commandGateway).sendAndWait(...)`) that don't exist in AF5. Test rewrite is broader than the per-processor recipe scope.
- Combined scoped compile across all 5 profiles passes:
  ```bash
  ./mvnw -P migration-event-processor-DwellingReadModelProjector,migration-event-processor-GetAllDwellingsQueryHandler,migration-event-processor-WhenCreatureRecruitedThenAddToArmy,migration-event-processor-WhenWeekStartedThenProclaimWeekSymbol,migration-event-processor-WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreatures \
    test-compile -DskipTests -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
  ```

### Phase 2 summary (all 5 aggregates done)

| # | Aggregate | Tests | Commit |
|---|---|---|---|
| 1 | Army | 8 | `8bf3deb` |
| 2 | Astrologers | 3 | `2bcc939` |
| 3 | Calendar | 8 | `1d96fdf` |
| 4 | Dwelling | 22 | `8647307` |
| 5 | ResourcesPool | 5 | `37985b9` |

Total: 46 aggregate-level tests passing under per-target Maven profiles. All five aggregates use the same shape: `@EventSourced(tagKey, idType)`, instance command handlers (`CREATE_IF_MISSING` semantics), no-arg `@EntityCreator`, `eventAppender.append(...)` instead of `apply(...)`.

Recurring fixes per aggregate:
- Add `@AfterEach tearDown() { fixture.stop(); }` to the test base.
- Replace `AggregateNotFoundException` test expectations with the rule that fires when AF5 materialises an empty entity (typically `OnlyBuilt…CanHaveAvailableCreatures` / `CanOnlyFinishCurrentDay` / `Can remove only present creatures` / `Cannot withdraw more than deposited resources`).
- For the Dwelling recruit handler, an explicit domain guard was added to avoid NPE on null state — see learnings.

Pinned: `snapshotting (Dwelling) = accept-drop`.

### Pattern observed on Army (re-use for other aggregates)

OpenRewrite (Phase 1) was exhaustive — for an aggregate with `CREATE_IF_MISSING` semantics it already produced the correct AF5 shape: instance `@CommandHandler` + no-arg `@EntityCreator`, `apply(...)` → `eventAppender.append(...)`, `@EventSourced(tagKey, idType)`, commands annotated `@Command` + `@TargetEntityId`, events annotated `@Event` + `@EventTag(key)`, test fixture migrated to `AxonTestFixture.with(EventSourcingConfigurer...)`.

What Phase 2 still has to do per aggregate:

1. Add `@AfterEach tearDown() { fixture.stop(); }` to the test base class (recipe T.2 — OpenRewrite skipped this).
2. Update any test that expected `AggregateNotFoundException` against an empty/missing aggregate — AF5 with no-arg `@EntityCreator` materialises an empty entity, so the domain rule fires instead. Replace expectation with the actual domain exception.
3. Seed `<profile id="migration-aggregate-<Name>">` in pom.xml — `<includes>` for the aggregate + commands + events + domain helpers, `<testIncludes>` for the test base + scenario tests, plus the `jackson-annotations:2.21` pin in `<dependencyManagement>` to make `AxonTestFixture` work under Spring Boot 3.5.x.
4. Run scoped verify: `./mvnw -P <profile> test -Dtest='<FQ tests>' -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false`.

---

## Project metadata

- **Target project:** `/Users/mateusznowak/GitRepos/MateuszNaKodach/HeroesOfDomainDrivenDesign.EventSourcing.Java.Axon.Spring`
- **Started:** 2026-05-09
- **Last updated:** 2026-05-09 (Phase 2 complete — all 5 aggregates done)
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
- **Snapshotting (Dwelling):** `accept-drop` — AF4 had `snapshotTriggerDefinition = "dwellingSnapshotTrigger"`; AF5 `@EventSourced` exposes no equivalent yet. OpenRewrite dropped it; recipe `not-supported.md` B1 confirms `accept-drop` is allowed. Decision frozen 2026-05-09 during Phase 2 / Dwelling. The `TODO #LLM` comment in `Dwelling.java` documents the deferral; existing snapshot rows in storage are NOT touched (data migration is out of scope per the skill's contract).
- **Commit cadence:** per-item (default; user later confirmed autonomous mode — proceed without per-item AskUserQuestion checkpoints, surface only on real blockers).
- **Storage-engine path:** _set when Phase 9 reached_

---

## Phase status

Legend: `pending` · `in-progress` · `awaiting-checkpoint` · `complete` · `paused` · `skipped`

| # | Recipe | Mode | Status | Items done / total | Last commit |
|---|---|---|---|---|---|
| 1 | openrewrite | one-shot | complete | n/a | `1911b46` |
| 2 | aggregate | iterative | complete | 5 / 5 | `37985b9` |
| 3 | event-processor | iterative | complete | 5 / 5 | _this commit (WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreatures)_ |
| 4 | command-gateway | iterative | complete | 6 / 6 | _this commit (RecruitCreatureMcp)_ |
| 5 | query-gateway | iterative | complete | 2 / 2 | _this commit (GetAllDwellingsMcp)_ |
| 6 | query-handler | iterative | in-progress | 1 / 2 | _this commit (GetDwellingByIdQueryHandler)_ |
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
| 1 | `com.dddheroes.heroesofddd.armies.write.Army` | `com.dddheroes.heroesofddd.armies.write.ArmyTest` (base) + `AddCreatureToArmyTest`, `RemoveCreatureFromArmyTest` (8 tests) | done | `8bf3deb` |
| 2 | `com.dddheroes.heroesofddd.astrologers.write.Astrologers` | `com.dddheroes.heroesofddd.astrologers.write.AstrologersTest` (base) + `proclaimweeksymbol.ProclaimWeekSymbolTest` (3 tests) | done | `2bcc939` |
| 3 | `com.dddheroes.heroesofddd.calendar.write.Calendar` | `com.dddheroes.heroesofddd.calendar.write.CalendarTest` (base) + `startday.StartDayTest`, `finishday.FinishDayTest` (8 tests) | done | `1d96fdf` |
| 4 | `com.dddheroes.heroesofddd.creaturerecruitment.write.Dwelling` | `DwellingTest` (base) + `DwellingIdTest`, `builddwelling.BuildDwellingTest`, `changeavailablecreatures.IncreaseAvailableCreaturesTest`, `recruitcreature.RecruitCreatureTest` (22 tests) — `RecruitCreatureRequiresResourcesTest` deferred to Phase 4/9 (E2E) | done | `8647307` |
| 5 | `com.dddheroes.heroesofddd.resourcespool.write.ResourcesPool` | `ResourcesPoolTest` (base) + `deposit.DepositResourcesTest`, `withdraw.WithdrawResourcesTest` (5 tests) | done | _this commit_ |

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
| 1 | `com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModelProjector` | `com.dddheroes.heroesofddd.creaturerecruitment.read.getdwellingbyid.GetDwellingByIdTest` (E2E — deferred to stabilization) | done | `cba9cc2` |
| 2 | `com.dddheroes.heroesofddd.creaturerecruitment.read.getalldwellings.GetAllDwellingsQueryHandler` (dual-natured: `@EventHandler` + `@QueryHandler`) | `com.dddheroes.heroesofddd.creaturerecruitment.read.getalldwellings.GetAllDwellingsTest` (E2E — deferred to stabilization) | done (`@EventHandler` only — `@QueryHandler` deferred to Phase 6) | `df2a674` |
| 3 | `com.dddheroes.heroesofddd.creaturerecruitment.automation.WhenCreatureRecruitedThenAddToArmyProcessor` | `com.dddheroes.heroesofddd.creaturerecruitment.automation.WhenCreatureRecruitedThenAddToArmyTest` (E2E `@SpringBootTest`, broken; deferred to stabilization) | done | `d5ca747` |
| 4 | `com.dddheroes.heroesofddd.astrologers.automation.whenweekstartedthenproclaimweeksymbol.WhenWeekStartedThenProclaimWeekSymbolProcessor` | `com.dddheroes.heroesofddd.astrologers.automation.whenweekstartedthenproclaimweeksymbol.WhenWeekStartedThenProclaimWeekSymbolTest` (E2E `@SpringBootTest`; deferred to stabilization) | done | `b08748b` |
| 5 | `com.dddheroes.heroesofddd.astrologers.automation.whenweeksymbolproclaimedthenincreasedwellingavailablecreatures.WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreaturesProcessor` | `com.dddheroes.heroesofddd.astrologers.automation.whenweeksymbolproclaimedthenincreasedwellingavailablecreatures.WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreaturesTest` (E2E `@SpringBootTest`; deferred to stabilization) | done | _this commit_ |

### Migration Phase #4 — command-gateway

After exclude-when filter (rows whose file also has `@EventHandler` / `@CommandHandler` / `@QueryHandler` / `@MessageHandlerInterceptor` are excluded — those become Phase 3 work).

| # | FQ class | FQ test | Status | Commit |
|---|---|---|---|---|
| 1 | `com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.BuildDwellingRestApi` | `com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.BuildDwellingTest` (E2E — deferred to stabilization) | done | _this commit_ |
| 2 | `com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.BuildDwellingMcp` | _none direct_ | done | _this commit_ |
| 3 | `com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.IncreaseAvailableCreaturesRestApi` | `com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.IncreaseAvailableCreaturesTest` (E2E — deferred to stabilization) | done | _this commit_ |
| 4 | `com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.IncreaseAvailableCreaturesMcp` | _none direct_ | done | _this commit_ |
| 5 | `com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.RecruitCreatureRestApi` | `com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.RecruitCreatureTest` (E2E — deferred to stabilization) | done | _this commit_ |
| 6 | `com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.RecruitCreatureMcp` | _none direct_ | done | _this commit_ |

### Migration Phase #5 — query-gateway

| # | FQ class | FQ test | Status | Commit |
|---|---|---|---|---|
| 1 | `com.dddheroes.heroesofddd.creaturerecruitment.read.getdwellingbyid.GetDwellingByIdRestApi` | `com.dddheroes.heroesofddd.creaturerecruitment.read.getdwellingbyid.GetDwellingByIdTest` (E2E — deferred to stabilization) | done (recipe-pre-migrated by OpenRewrite — only added scoped profile) | _this commit_ |
| 2 | `com.dddheroes.heroesofddd.creaturerecruitment.read.getalldwellings.GetAllDwellingsMcp` | _none direct_ | done (replaced bare `.get()` with `.orTimeout(30, SECONDS).join()` — recipe step 7 sync framework callback) | _this commit_ |

### Migration Phase #6 — query-handler

| # | FQ class | FQ test | Status | Commit |
|---|---|---|---|---|
| 1 | `com.dddheroes.heroesofddd.creaturerecruitment.read.getdwellingbyid.GetDwellingByIdQueryHandler` | `com.dddheroes.heroesofddd.creaturerecruitment.read.getdwellingbyid.GetDwellingByIdTest` (E2E — deferred to stabilization) | done (recipe-pre-migrated by OpenRewrite — only added scoped profile) | _this commit_ |
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
