# Axon Framework 4 → 5 Migration — Learnings

Append-only narrative. One dated entry per surprise, manual fix, or
non-obvious decision. Read on demand only — `progress.md` is the source
of truth for state.

Format per entry:

```
## YYYY-MM-DD — <one-line headline>

- Context: where in the migration this came up.
- Surprise: what was unexpected.
- Resolution: what was done. Link to commit `<sha>` if applicable.
```

---

## 2026-05-09 — Phase 1: `LATEST` resolves to AF4 recipe artifact

- Context: Phase 1 (openrewrite, Path B). Initial run with `recipeArtifactCoordinates=org.axonframework:axon-migration:LATEST`.
- Surprise: Maven resolved `LATEST` to the locally cached `axon-migration:4.13.1` (the AF4 line of the same artifact id), not the AF5 5.x line. Combined with `rewrite-maven-plugin:6.39.0`, this caused a bytecode `VerifyError` (`Type 'UsesType' is not assignable to 'JavaVisitor'`) before any rewrites ran. Spring milestones repo also returned 401 for non-authenticated metadata fetches, which made `LATEST` unable to discover AF5 versions over the wire.
- Resolution: pinned to explicit `5.1.1-SNAPSHOT` per recipe doc — fall-back path. `5.1.0` also failed because the `pom` is gated behind the auth-required `spring-milestones` repo. `5.1.1-SNAPSHOT` resolved successfully via `central.sonatype.com/repository/maven-snapshots/`.
- Takeaway for future runs in this project: always pass an explicit recipe version (recommend `5.1.1-SNAPSHOT` until a fully published 5.x release lands in unauthenticated central). `LATEST` is unsafe here.

## 2026-05-09 — Phase 1: OpenRewrite went past "mechanical" — already produced correct AF5 shape for `CREATE_IF_MISSING`

- Context: Phase 1 (openrewrite, Path B) rewrote `Army`, `Astrologers`, `Calendar`, `Dwelling` to AF5 `@EventSourced(tagKey, idType)`.
- Initial impression (incorrect): "the recipe removed `@CreationPolicy(CREATE_IF_MISSING)` without an AF5 equivalent — behaviour is now lost".
- Correction (verified on Army during Phase 2): the recipe DID produce the AF5 equivalent. Per [aggregate/creation-policy-decision.md](../.claude/skills/axon4-to-axon5-migration/references/aggregate/creation-policy-decision.md), the AF5 shape for `CREATE_IF_MISSING` is "**instance** `@CommandHandler` (NOT static) + no-arg `@EntityCreator`". OpenRewrite produced exactly that:
  - Command handlers stayed instance (not made static).
  - A no-arg constructor annotated `@EntityCreator` was added to the aggregate.
  - `apply(...)` was rewritten to `eventAppender.append(...)`.
  - The `RemoveCreatureFromArmyTest.givenEmptyArmy_...` expectation needed updating: AF4 would throw `AggregateNotFoundException` for a missing aggregate, but AF5 with no-arg `@EntityCreator` materialises an empty entity and runs the instance handler — so the domain rule (`Can remove only present creatures`) fires instead. This is the documented gotcha in the decision-matrix doc.
- Stranded comment: the source still says `// performance downside in comparison to constructor` — was a note about `CREATE_IF_MISSING`'s performance cost on EVERY command. Still loosely accurate (instance handler still re-loads the aggregate) but the original referent is gone. Left in place; can be cleaned up during stabilization.
- Implication: each per-aggregate Phase 2 step is mostly **verification**, not heavy rewriting — confirm the entity-creator pattern matches what AF4's `CreationPolicy` value implied, fix any test expectations that asserted on AF4-only exceptions, and verify scoped tests pass. See Army (commit pending) as the canonical example.
- See: phase-1 commit `1911b46`, phase-2-Army commit `8bf3deb`.

## 2026-05-09 — Phase 2 / Dwelling: NPE-on-null-state fix via explicit domain guard

- Context: Phase 2 / Dwelling. `RecruitCreatureTest.givenNotBuiltDwellingWhenRecruitCreatureThenException` fed an empty event stream, then sent `RecruitCreature`.
- Surprise: AF4 threw `AggregateNotFoundException` (the test author commented "exception is not from domain, AggregateNotFoundException is meaningless"). AF5 with no-arg `@EntityCreator` materialises an empty Dwelling instead — `dwellingId`, `creatureId`, `availableCreatures` are all `null`. The first rule constructed in `decide(RecruitCreature, EventAppender)` is `RecruitCreaturesNotExceedAvailableCreatures(creatureId, availableCreatures, ...)`, whose `isViolated()` calls `dwellingCreatureId.equals(...)` — NPE on null `dwellingCreatureId` (the `creatureId` field of the empty Dwelling), not a domain exception.
- Resolution: per [creation-policy-decision.md](../.claude/skills/axon4-to-axon5-migration/references/aggregate/creation-policy-decision.md) "NPE on null state", added an explicit domain-level guard at the top of the recruit handler:
  ```java
  new OnlyBuiltDwellingCanHaveAvailableCreatures(dwellingId).verify();
  ```
  This reuses the existing rule already used by `IncreaseAvailableCreatures`. Message ("Only built dwelling can have available creatures") is slightly broader than "Only built dwelling can recruit creatures" but matches the actual condition (`dwellingId == null`). Test expectation updated to match. Net effect: AF4's meaningless `AggregateNotFoundException` becomes a clear domain rule violation — improvement, not just preservation.
- Same `IncreaseAvailableCreaturesTest.givenNotBuildDwellingWhenIncreaseAvailableCreaturesThenException` was already covered by the existing `OnlyBuiltDwellingCanHaveAvailableCreatures(dwellingId).verify()` call at the top of the increase handler — only the test expectation needed updating.
- See: phase-2-Dwelling commit (this commit).

## 2026-05-09 — Phase 2 / Dwelling: snapshotting accepted as dropped

- Context: Phase 2 / Dwelling. Dwelling was the only aggregate with snapshotting in AF4 (`@Aggregate(snapshotTriggerDefinition = "dwellingSnapshotTrigger")`).
- Surprise: OpenRewrite (Phase 1) silently dropped the attribute and left a `// TODO #LLM: reconfigure snapshot trigger` comment. AF5's `@EventSourced` does not yet expose a snapshotting API — per recipe `not-supported.md` B1 this is a blocker requiring an `accept-drop / pause-migration / remove-feature-first` decision.
- Resolution: pinned `snapshotting: accept-drop` (project is small enough that snapshot rebuild on full replay is acceptable). The TODO comment stays; existing snapshot rows in storage are NOT touched (data migration is out of scope of this skill — user owns that decision). Re-introduce snapshotting once AF5 ships the API.
- The `public` field declarations in `Dwelling.java` (`public DwellingId dwellingId; // needs to be public for snapshotting`) are no longer strictly required — could be re-tightened to `private` during stabilization. Left as-is for now to keep this commit minimal.
- See: progress.md "Snapshotting (Dwelling)" pinned decision; phase-2-Dwelling commit (this commit).

## 2026-05-09 — Phase 3 / DwellingReadModelProjector: pure projector — only Step 7 (sequencing-policy) was non-OpenRewrite work

- Context: Phase 3 / event-processor item #1 — `com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModelProjector`.
- Surprise: OpenRewrite (Phase 1) had already done everything except recipe Step 7 — `@ProcessingGroup` → `@Namespace`, `@EventHandler` / `@ResetHandler` / `@MetadataValue` imports, etc. Pure projector (no `CommandGateway` field), so recipe steps 5–6 (CommandDispatcher + sendAndWait→send) didn't apply. The only manual work was moving the AF4 `axon.eventhandling.processors.ReadModel_Dwelling.sequencing-policy: gameIdSequencingPolicy` YAML key onto the class as `@SequencingPolicy(type = MetadataSequencingPolicy.class, parameters = GameMetaData.GAME_ID_KEY)` (the AF4 bean was a metadata-keyed lookup, so `MetadataSequencingPolicy` is the AF5 equivalent — no custom impl needed).
- Resolution: added the annotation, deleted the YAML key + its TODO comment for `ReadModel_Dwelling` only. The `gameIdSequencingPolicy` `@Bean` in `GameConfiguration` is still referenced by 4 other processor groups in YAML — leave the bean intact; the write-configuration recipe (Phase 8 / stabilization) deletes it once all 5 groups have been annotated.
- Caveat: namespace `ReadModel_Dwelling` is shared with `WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreaturesProcessor` (Phase 3 item #5). Both classes will need the same `@SequencingPolicy` annotation; migrating one without the other leaves a partial wiring — fine until item #5 also lands.
- Verification: `./mvnw -P migration-event-processor-DwellingReadModelProjector test-compile` is clean. No scoped projector test exists (only `GetDwellingByIdTest` which is `@SpringBootTest` E2E) — `<testIncludes>` deliberately matches nothing; functional verification deferred to stabilization.
- See: phase-3-DwellingReadModelProjector commit (this commit).

## 2026-05-09 — Phase 3 / WhenWeekSymbolProclaimed: AF5 `Message` is NOT generic

- Context: Phase 3 / event-processor item 5 — loop-dispatch handler. Per recipe Step 6 "Loop / multiple dispatches → `CompletableFuture.allOf(...)`". Helper method type signature initially set to `CompletableFuture<? extends Message<?>>` per recipe wording.
- Surprise: compile error "The type Message is not generic; it cannot be parameterized with arguments <?>". Verified by `javap` against `axon-messaging-5.1.1-SNAPSHOT.jar`: `org.axonframework.messaging.core.Message` is declared as `public interface Message` (no type parameter). The recipe's pseudocode (`commandResult.getResultMessage()` returns `CompletableFuture<? extends Message>`) is correct; only the documentation hint "`CompletableFuture<? extends Message<?>>`" was misleading.
- Resolution: changed helper return type to `CompletableFuture<? extends Message>` (no `<?>`). Rest of the loop-pattern compiles cleanly: `repository.findAllByGameId(...).stream().filter(...).map(d -> helper(dispatcher, d, ...)).toArray(CompletableFuture[]::new)` → `CompletableFuture.allOf(futures)`.
- Future-proofing for this skill: recipe `event-processor.md` should be tightened to use `CompletableFuture<? extends Message>` (without `<?>`) in any documented pseudocode.
- See: phase-3-WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreatures commit (this commit).

## 2026-05-09 — Phase 7 / StreamProcessorsOperations: AF5 `TokenStore` API forces `progressOf` deferral

- Context: Phase 7 / read-configuration item 1. The class injected both `EventProcessingConfiguration` and `TokenStore` (AF4 shape — diagnostic helper that read segment positions to compute replay progress).
- Surprise: AF5 `org.axonframework.messaging.eventhandling.processing.streaming.token.store.TokenStore` rewrites every operation as `CompletableFuture<...>` AND requires a `ProcessingContext` parameter on every method (`fetchSegments(String, ProcessingContext)`, `fetchToken(String, int, ProcessingContext)`, …). There is no public "no-context" or default `ProcessingContext` instance for diagnostic callers running outside a unit-of-work — the read-configuration recipe explicitly says to drop the redundant `TokenStore` field, but the recipe does not address callers that *use* `fetchSegments` / `fetchToken` directly.
- Resolution: migrated `reset(...)` per recipe (switched field to `AxonConfiguration`, looked up `StreamingEventProcessor` via `getModuleConfiguration("EventProcessor[" + name + "]").flatMap(m -> m.getOptionalComponent(StreamingEventProcessor.class))`, replaced `shutDown()` / `resetTokens()` / `start()` with `.orTimeout(30, SECONDS).join()`-bridged async calls). Stubbed `progressOf(...)` body to throw `UnsupportedOperationException` with a `TODO #LLM` comment pointing at `StreamingEventProcessor.processingStatus()` (per-segment `EventTrackerStatus` map, returned synchronously) as the AF5-native replacement. `progressOf` is unused outside this file and `Progress` record is preserved verbatim — admin path can be revived in stabilization.
- Takeaway: the recipe step 6 "drop redundant `TokenStore` field" only fits classes whose use of `TokenStore` is the AF4 implicit "looked up by name" pattern. Direct diagnostic readers of `fetchSegments` / `fetchToken` need a per-callsite redesign; flag them instead of attempting an automated port.
- See: phase-7-StreamProcessorsOperations commit (this commit).

## 2026-05-09 — Phase 9 / event-storage-engine: no bean swap needed; deliverable is SQL only

- Context: Phase 9 / event-storage-engine (one-shot). Preflight greps for `MongoEventStorageEngine`, `JdbcEventStorageEngine`, `EmbeddedEventStore`, `JpaEventStorageEngine`, `AxonServerEventStore`, custom `EventStorageEngine` subclasses, `axon-mongo` — all empty.
- Surprise: nothing to swap on the Java side. AF4 wiring relied entirely on the starter's auto-config (`axon-spring-boot-starter` registered `JpaEventStorageEngine` automatically because `axon.axonserver.enabled=false` and JPA + PostgreSQL are on the classpath). Phase 1 OpenRewrite swapped the starter coordinate to `io.axoniq.framework:axoniq-spring-boot-starter:5.1.1-SNAPSHOT`, and AF5's `JpaEventStoreAutoConfiguration` now registers `AggregateBasedJpaEventStorageEngine` automatically under the same conditions (EntityManagerFactory + PlatformTransactionManager present, no explicit `EventStorageEngine` bean). Path A — code unchanged.
- Resolution:
  - **Path picked: A — JPA via auto-config.** No `@Bean EventStorageEngine` exists in the project, so there is nothing to delete and nothing to add. Auto-config wins.
  - Path A.2 (custom enhancer) skipped — AF4 did not tune `batchSize` / `gapTimeout` / `persistenceExceptionResolver`.
  - Path A.3 (custom `Serializer` → `Converter`) — `SerializationConfiguration` declares Jackson `Module` beans (NOT custom `org.axonframework.serialization.Serializer` subclasses); these are picked up by Spring's `ObjectMapper` autoconfig and remain valid under AF5's Jackson-backed `Converter`. No serializer ports flagged. B4 does not fire.
  - Path A.4 (schema migration) — emitted `01-rename-domain-to-aggregate-event-entry.sql` under `.axon4-to-axon5-migration/sql/`, PostgreSQL-flavoured. Renames `domain_event_entry` → `aggregate_event_entry`, renames all 7 columns, tightens nullability on `version` / `aggregate_sequence_number` / `identifier`, drops `NOT NULL` on `aggregate_identifier` (DCB-friendly), creates the `aggregate-event-global-index-sequence` and seeds it past the current max `global_index`, and adds the unique `(aggregate_identifier, aggregate_sequence_number)` index. **NOT executed by the orchestrator** — user runs out-of-band on a non-prod copy first, verifying row counts before/after. No Flyway directory exists in this project, so the SQL lives under the skill's state dir; user can move it into a Flyway changeset if desired.
  - Path A.5 (entity scan) — no explicit `LocalContainerEntityManagerFactoryBean` / `packagesToScan` / `@EntityScan` in the project; Spring Boot's default entity scan plus AF5's `@RegisterDefaultEntities` will pick up `org.axonframework.eventsourcing.eventstore.jpa.AggregateEventEntry` from the framework JAR. No code change required.
  - **Hibernate `ddl-auto: update` caveat:** if the AF5 app starts before the SQL runs, Hibernate will auto-create an empty `aggregate_event_entry` next to the AF4 `domain_event_entry` (two tables, AF4 events orphaned, AF5 events going to a fresh empty table). The SQL header documents this. User must apply SQL **before** first AF5 boot against an existing DB. For development against a fresh Testcontainers PostgreSQL the SQL is unnecessary — Hibernate creates the AF5 table directly.
- Stabilization carry-over (NOT addressed in Phase 9):
  - **`com.dddheroes.heroesofddd.maintenance.read.geteventstream.EventStreamsRestApi`** uses AF4 `EventStore.readEvents(streamId).asStream()` — `EventStore.readEvents(String)` is an AF4 aggregate-stream API not present on AF5's `EventStorageEngine`/`EventStore`. The class will not compile under AF5. Out of scope for this recipe (one-shot bean swap, not handler ports). To address in stabilization: rewrite to `eventStorageEngine.source(SourcingCondition.conditionFor(...))` or expose via `EventSink`/`EventConverter` per AF5 API. Tracked as a known compile error to surface during stabilization.
- Decision recorded in pinned-decisions: `Storage-engine path: A — JPA via auto-config (no bean swap, SQL emitted only)`.
- See: phase-9-event-storage-engine commit (this commit).
