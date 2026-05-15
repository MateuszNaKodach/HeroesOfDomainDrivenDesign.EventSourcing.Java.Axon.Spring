# Axon Framework 4 → 5 Migration — Learnings

Append-only. One dated entry per surprise, manual fix, or non-obvious decision.
Read on demand — `progress.md` is the source of truth for state.

```
## YYYY-MM-DD — <one-line headline>
**Context:** where in the migration this came up.
**Surprise:** what was unexpected.
**Resolution:** what was done. Link to commit `<sha>` if applicable.
```

---

## 2026-05-15 — `@SequencingPolicy` annotation is in `core.annotation`, not `core.sequencing.annotation`
**Context:** Items 6-10 (event-processor recipe) — adding `@SequencingPolicy` to all processor classes.
**Surprise:** The recipe docs referenced `org.axonframework.messaging.core.sequencing.annotation.SequencingPolicy` but the actual package in the 5.1.1-SNAPSHOT jar is `org.axonframework.messaging.core.annotation.SequencingPolicy`. All 4 files written with the wrong import failed to compile.
**Resolution:** Fixed via `sed` in bulk across 4 files (commit `badbc32`). Correct import: `import org.axonframework.messaging.core.annotation.SequencingPolicy;`

## 2026-05-15 — `EventMessage` is non-generic in AF5
**Context:** Maintenance module (`EventStreamsRestApi`) — replacing AF4 `DomainEventMessage<?>` with AF5 equivalent.
**Surprise:** In AF4, `EventMessage<T>` carried a type parameter. In AF5, `EventMessage` is a plain non-generic interface (extends `Message`). Using `EventMessage<?>` or `EventMessage<T>` causes a compile error: *"type EventMessage does not take parameters"*.
**Resolution:** Changed return type and variable types to raw `EventMessage` (commit `badbc32`).

## 2026-05-15 — `Repository<ID, E>` requires 2 type args; ID from `@EventSourced(idType=...)`
**Context:** Item 21 (interceptors recipe) — `PaidCommandInterceptor` held a `Repository<ResourcesPool>` field.
**Surprise:** AF5 `Repository` interface is `Repository<ID, E>` with two type parameters. The `ID` type comes from the aggregate's `@EventSourced(idType = ResourcesPoolId.class)` annotation, not from a raw String. `Repository<ResourcesPool>` fails to compile.
**Resolution:** Dropped the `Repository` field entirely. Switched to `CommandDispatcher.forContext(context).send(withdrawResources)` dispatched within the interceptor's own `ProcessingContext` (commit `badbc32`). This is cleaner AF5 idiom: interceptors dispatch commands, not manipulate repositories directly.

## 2026-05-15 — `MessageHandlerInterceptorChain.proceed()` requires `(message, context)` arguments
**Context:** Item 21 (interceptors recipe) — `PaidCommandInterceptor.interceptOnHandle`.
**Surprise:** AF4's `interceptorChain.proceed()` took no arguments. AF5's `chain.proceed(M, ProcessingContext)` requires the message and context forwarded explicitly.
**Resolution:** Changed `chain.proceed()` → `chain.proceed(message, context)` (commit `e4353c7`).

## 2026-05-15 — `CommandDispatcher.forContext(ProcessingContext)` — interceptor-safe command dispatch
**Context:** Item 21 (interceptors recipe) — `PaidCommandInterceptor` needs to dispatch `WithdrawResources` before proceeding.
**Surprise:** No direct AF5 equivalent of the AF4 `repository.loadOrCreate(id, factory).execute(entity -> ...)` pattern for use inside an interceptor. `loadOrCreate` in AF5 only takes `(ID, ProcessingContext)` with no factory, and `ManagedEntity` has no `execute` method.
**Resolution:** `CommandDispatcher.forContext(context)` provides a `CommandDispatcher` bound to the current processing context. Dispatching `WithdrawResources` through it runs within the same unit of work. `WithdrawResources` implements `Command` but has no registered cost resolver, so the interceptor does not recurse (commit `badbc32`).

## 2026-05-15 — `exceptionallyCompose` rejects `CompletableFuture<? extends Message>` lambda return
**Context:** Item 8 (event-processor recipe) — `WhenCreatureRecruitedThenAddToArmyProcessor` compensation chain.
**Surprise:** `getResultMessage()` returns `CompletableFuture<? extends Message>`. Java's `exceptionallyCompose` infers `CompletionStage<T>` where `T = Message`, but the wildcard capture `? extends Message` is not assignable to `Message`, causing a compile error.
**Resolution:** Use `.resultAs(Message.class)` instead of `.getResultMessage()` in the `exceptionallyCompose` lambda — returns `CompletableFuture<Message>` with no wildcard (commit `badbc32`).

## 2026-05-15 — `EventStore.readEvents(String)` removed; replaced by `EventStorageEngine.source(SourcingCondition)`
**Context:** Maintenance module (`EventStreamsRestApi`) — debug loop after all 22 queue items done.
**Surprise:** AF5 `EventStore` no longer has `readEvents(String aggregateId)`. Reading events per aggregate now requires `EventStorageEngine.source(SourcingCondition.conditionFor(EventCriteria.havingTags(...)))`. The tag-based filtering semantics differ from the AF4 aggregate-identifier lookup — exact tag key/value matching may need manual tuning for the maintenance endpoint.
**Resolution:** Injected `EventStorageEngine` (instead of `EventStore`), iterated `MessageStream` with `while (stream.hasNextAvailable())` (commit `badbc32`). Runtime behavior of the `/maintenance/event-store/streams/{streamId}/events` endpoint may need further adjustment to match the correct tag key for a given aggregate type.

## 2026-05-15 — `EventProcessingConfiguration` + `TrackingEventProcessor` removed; maintenance module needed full rewrite
**Context:** Maintenance module (`StreamProcessorsOperations`) — debug loop.
**Surprise:** AF4's `EventProcessingConfiguration.eventProcessorByProcessingGroup(name, TrackingEventProcessor.class)` central registry is gone. `TrackingEventProcessor` is replaced by `StreamingEventProcessor`. `TokenStore.fetchSegments(String)` now requires `ProcessingContext`, making it unusable inside a plain `@Transactional` method.
**Resolution:** Injected `ApplicationContext` and looked up `StreamingEventProcessor` beans by `ep.name()`. Replaced `TokenStore`-based progress tracking with `StreamingEventProcessor.processingStatus()` (returns `Map<Integer, EventTrackerStatus>` with `getCurrentPosition()` / `getResetPosition()`). `reset` now uses async `shutdown().join()` / `resetTokens().join()` / `start().join()` (commit `badbc32`).

## 2026-05-15 — `gameIdSequencingPolicy` @Bean in `GameConfiguration` is now orphaned
**Context:** Finalize step — after all processors migrated to `@SequencingPolicy` annotations.
**Surprise:** The `gameIdSequencingPolicy` `SequencingPolicy<EventMessage>` bean in `GameConfiguration` was referenced by AF4 YAML processor config. After migration, all processors use `@SequencingPolicy(type = MetadataSequencingPolicy.class, ...)` annotations directly — no YAML-wired sequencing policy remains. The bean is now unused.
**Resolution:** Left in place (safe — Spring simply registers an unused bean). Can be removed in a follow-up cleanup PR.

## 2026-05-15 — JPA schema change required before runtime — `domain_event_entry` → `aggregate_event_entry`
**Context:** Item 22 (event-store recipe) — `AggregateBasedJpaEventStorageEngine` registered.
**Surprise:** The AF5 JPA engine uses a different table name (`aggregate_event_entry`) than AF4's (`domain_event_entry`). The application will start cleanly (the `@Bean` is correct) but fail at the first command or replay until the database schema is updated.
**Resolution:** Noted as out-of-band task. User must apply the DDL migration (`domain_event_entry` → `aggregate_event_entry`) on a non-production copy first before deploying AF5. See `event-store.adoc` for the full schema diff.
