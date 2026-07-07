# Axon Framework 4 — Distributed Tracing Baseline

This document is the human-readable companion to `JaegerTracingIntegrationTest`. It records the **exact
OpenTelemetry spans and attributes** that **Axon Framework 4.13.1** (`axon-tracing-opentelemetry`) emits for
a representative command → event → projection → query flow in this application.

**Why it exists:** this app is being migrated to **Axon Framework 5**. Re-running the test after the
migration turns any change in tracing output (renamed operations, dropped spans, changed span kinds,
renamed/added/removed attributes, broken context propagation) into a concrete, reviewable diff.

> Keep this file in sync with the constants in `JaegerTracingIntegrationTest`. The test is the source of
> truth that is actually enforced; this file explains it.

## Stack under test

| Component | Image | Role |
|-----------|-------|------|
| Axon Server | `axoniq/axonserver:latest` (Testcontainers) | event store + command/query/event routing (distributed buses) |
| PostgreSQL | `postgres:latest` (Testcontainers) | JPA read-model store + token store |
| Jaeger | `jaegertracing/jaeger:2.5.0` (Testcontainers) | OTLP/HTTP backend (4318) + query API (16686) |

JDBC/JPA tracing is provided by **`net.ttddyy.observation:datasource-micrometer-spring-boot`** (`1.4.1`, the
Spring Boot 3.x line; 2.x targets Spring Boot 4), which instruments the `DataSource` through the Micrometer
Observation API so database spans flow through the same Micrometer → OpenTelemetry → OTLP pipeline (no
OpenTelemetry agent needed). The proxy is **gated by `jdbc.datasource-proxy.enabled`**: `false` in the base
`application.yaml` and `true` in `application-observability.yaml`, so it isn't even created in the default
runtime and turns on only under the `observability` profile — matching the rest of the tracing setup. Bind
parameter values are captured (`jdbc.datasource-proxy.include-parameter-values: true`).

> This mirrors the configuration used on the Axon 5 side (`axon5/tracing-v1`), so the JDBC tracing behaviour
> is directly comparable across the migration.

gRPC tracing (Axon Server connector) is provided by **`io.opentelemetry.instrumentation:opentelemetry-grpc-1.6`**
(`2.15.0-alpha`, matching the `opentelemetry-api` 1.49.0 managed by the Spring Boot 3.5 BOM). A gRPC
`ClientInterceptor` is registered on the Axon Server channel via Axon 4's `ManagedChannelCustomizer`
(`GrpcTracingConfiguration`), gated to the `observability` profile **and** `axon.axonserver.enabled=true`. Also
mirrors the Axon 5 side (the only difference is the `ManagedChannelCustomizer` package:
`org.axonframework.axonserver.connector` on Axon 4 vs `io.axoniq.framework...` on Axon 5).

Profiles: `observability, observability-jaeger, axonserver`. Sampling `1.0`, OTLP transport HTTP,
service name `heroesofddd`.

> **Note on `management.tracing.enabled`:** the production `observability` profile
> (`application-observability.yaml`) already sets this to `true`, and that value wins at runtime. Spring Boot,
> however, injects a high-precedence `test` property source under `@SpringBootTest` that forces it back to
> `false` — so the test re-enables it via `@DynamicPropertySource` (which outranks the `test` source).
> This is a test-only concern; no production profile change is needed.

## The flow (driven over HTTP)

1. `PUT /games/{gameId}/dwellings/{dwellingId}` → `BuildDwelling` → `DwellingBuilt`
2. `PUT /games/{gameId}/dwellings/{dwellingId}/available-creatures-increases` → `IncreaseAvailableCreatures` → `AvailableCreaturesChanged`
3. `PUT /games/{gameId}/dwellings/{dwellingId}/creature-recruitments` → `RecruitCreature` → `CreatureRecruited`
   → async automation → `AddCreatureToArmy` (Army aggregate) → `CreatureAddedToArmy`
4. `GET /games/{gameId}/dwellings/{dwellingId}` → `GetDwellingById` query

## Span naming convention (Axon 4)

`OpenTelemetrySpanFactory` names spans **`<operation>(<MessageName>)`**:
- `<MessageName>` is the **command/query name** (FQCN by default) or the **event payload simple name**.
- Emitted under the instrumentation scope **`axon-framework`** (the tracer name from `TracingConfiguration`).
- Because **Axon Server** is used, command/query buses are **distributed** → both local
  (`dispatchCommand`/`handleCommand`) **and** distributed (`dispatchDistributedCommand`/`handleDistributedCommand`)
  spans appear.

## Span catalog (operation names)

### Axon framework spans (`EXPECTED_AXON_FRAMEWORK_SPANS`)

**CommandBus** — for each of `BuildDwelling`, `IncreaseAvailableCreatures`, `RecruitCreature`, `AddCreatureToArmy`:
- `CommandBus.dispatchCommand(<Cmd>)` — local dispatch, kind `INTERNAL`
- `CommandBus.dispatchDistributedCommand(<Cmd>)` — Axon Server dispatch
- `CommandBus.handleCommand(<Cmd>)` — local handling, kind `CONSUMER`
- `CommandBus.handleDistributedCommand(<Cmd>)` — Axon Server handling

**EventBus**
- `EventBus.publishEvent(DwellingBuilt | AvailableCreaturesChanged | CreatureRecruited | CreatureAddedToArmy)` — kind `PRODUCER`
- `EventBus.commitEvents` — internal commit

**QueryBus**
- `QueryBus.query(GetDwellingById)` — local dispatch
- `QueryBus.queryDistributed(GetDwellingById)` — Axon Server dispatch
- `QueryBus.processQueryMessage(GetDwellingById)` — handling, kind `CONSUMER`
- `QueryBus.processQueryResponse(GetDwellingById)` — internal

**Repository** (event-sourced aggregate loading)
- `Repository.load`
- `Repository.obtainLock`
- `Repository.initializeState(Dwelling)`

**Event processors** (re-reading the events)
- `StreamingEventProcessor.process(DwellingBuilt | AvailableCreaturesChanged | CreatureRecruited)` — pooled/streaming processors
- `EventProcessor.process(DwellingBuilt)` — the subscribing `Read_GetAllDwellings_QueryCache` processor

### `@MessageHandler` invocation spans (`EXPECTED_HANDLER_SPANS`)

Wrapped by `TracingHandlerEnhancerDefinition` as `<DeclaringClass>.<method>(<ParamSimpleTypes>)`:
- `Dwelling.decide(BuildDwelling | IncreaseAvailableCreatures | RecruitCreature)`
- `Army.decide(AddCreatureToArmy)`
- `DwellingReadModelProjector.on(DwellingBuilt,String)` · `on(AvailableCreaturesChanged)` · `on(CreatureRecruited)`
- `WhenCreatureRecruitedThenAddToArmyProcessor.react(CreatureRecruited,String,String)`
- `WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreaturesProcessor.on(DwellingBuilt,String)`
- `GetAllDwellingsQueryHandler.evolve(DwellingBuilt,String)`
- `GetDwellingByIdQueryHandler.handle(GetDwellingById)`

> **Baseline fact:** aggregate `@EventSourcingHandler` replays produce **no** spans, because
> `axon.tracing.show-event-sourcing-handlers=false` in `application.yaml`.

### HTTP server spans (Spring/Micrometer, not Axon) (`EXPECTED_HTTP_SERVER_SPANS`)
- `http put /games/{gameId}/dwellings/{dwellingId}`
- `http put /games/{gameId}/dwellings/{dwellingId}/available-creatures-increases`
- `http put /games/{gameId}/dwellings/{dwellingId}/creature-recruitments`
- `http get /games/{gameId}/dwellings/{dwellingId}`

### JPA/JDBC spans (datasource-micrometer, scope `org.springframework.boot`) (`EXPECTED_JPA_SPANS`)
- `connection` — JDBC connection acquisition
- `query` — SQL statement execution; SQL text on the `jdbc.query[0]` tag
- `result-set` — result-set traversal

> Asserted by **presence** only — the number of DB spans and the exact SQL vary per run, so they are not
> policed exhaustively. Query-span attributes checked: `span.kind=client`,
> `jdbc.datasource.driver=org.postgresql.Driver`, and a non-empty `jdbc.query[0]` (the SQL). Other tags seen:
> `jdbc.datasource.name`, `jdbc.datasource.pool` (e.g. `HikariPool-1`), `peer.service`. Bind-parameter values
> **are** captured (`jdbc.datasource-proxy.include-parameter-values: true`).
>
> ⚠️ Bind-parameter values can expose data. Intentional here (local/dev tracing, off by default). Revisit
> `include-parameter-values` before enabling in any shared environment.

### gRPC spans — Axon Server connector (scope `io.opentelemetry.grpc-1.6`) (`EXPECTED_GRPC_SPANS`)

An OpenTelemetry gRPC `ClientInterceptor` (`opentelemetry-grpc-1.6`) is registered on the Axon Server
connection channel via Axon 4's `ManagedChannelCustomizer` hook (see `GrpcTracingConfiguration`), gated to the
`observability` profile **and** `axon.axonserver.enabled=true`. Spans are named `<rpc.service>/<rpc.method>`:

- `io.axoniq.axonserver.grpc.command.CommandService/Dispatch` — command sent to Axon Server
- `io.axoniq.axonserver.grpc.query.QueryService/Query` — `GetDwellingById`
- `io.axoniq.axonserver.grpc.event.EventStore/AppendEvent` — events appended
- `io.axoniq.axonserver.grpc.event.EventStore/ListAggregateEvents` — aggregate loading

Attributes on each: `rpc.system=grpc`, `rpc.service`, `rpc.method`, `rpc.grpc.status_code` (`0` = OK),
`span.kind=client`, plus `server.address`/`server.port`, `network.peer.*`.

> Asserted by **presence** only (the `Dispatch` span's attributes are checked exactly). Axon Server traffic is
> dominated by **long-lived bidirectional streams** (command/query/event/control channels); gRPC opens one span
> per RPC, so a streaming call yields a *single span lasting the whole connection* — it does not end during the
> test and is therefore not asserted. Only the **unary** RPCs above end and export. Per-message tracing is
> already covered by the framework's distributed tracing (`axon-tracing-opentelemetry`).

## Span attributes (tags)

### Attribute key sets per span category

| Span | Attribute keys |
|------|----------------|
| `CommandBus.dispatchCommand(*)` | `axon_message_id`, `axon_message_name`, `axon_message_type`, `axon_metadata_gameId`, `axon_metadata_playerId`, `axon_metadata_traceparent`, `axon_payload_type`, `otel.scope.name`, `span.kind` |
| `CommandBus.handleCommand(*)` | *(same as dispatch)* |
| `EventBus.publishEvent(*)` | `axon_aggregate_identifier`, `axon_message_id`, `axon_message_type`, `axon_metadata_correlationId`, `axon_metadata_gameId`, `axon_metadata_playerId`, `axon_metadata_traceId`, `axon_payload_type`, `otel.scope.name`, `span.kind` |
| `StreamingEventProcessor.process(*)` | *(publish keys)* **+** `axon_metadata_traceparent` |
| `QueryBus.processQueryMessage(*)` | `axon_message_id`, `axon_message_name`, `axon_message_type`, `axon_metadata_traceparent`, `axon_payload_type`, `otel.scope.name`, `span.kind` |
| `<Class>.<handler>(*)` (handler-enhancer) | `otel.scope.name`, `span.kind` only |
| `http put …` (Spring) | `exception`, `http.url`, `method`, `otel.scope.name` (=`org.springframework.boot`), `otel.scope.version`, `outcome`, `span.kind`, `status`, `uri` |

> **Baseline asymmetries worth noting for the migration:**
> - `publishEvent` has `correlationId`/`traceId` (Axon causation metadata) but **no** `traceparent`;
>   the async `process` span **does** carry `traceparent` (injected on the dispatch path).
> - The query side has **no** `axon_metadata_gameId` — `GetDwellingById` is dispatched without game metadata.
> - Handler-enhancer spans carry **no** message attributes (only scope + kind).

### Attribute values (key invariants)

- `otel.scope.name = axon-framework` on **every** Axon framework span.
- `span.kind`: dispatch (local) = `internal`, handling = `consumer`, publish = `producer`, handler-enhancer = `internal`, HTTP = `server`.
- `axon_message_type`: `GrpcBackedCommandMessage` (command, via Axon Server), `GenericDomainEventMessage` (published event),
  `GenericTrackedDomainEventMessage` (event in a streaming processor), `GrpcBackedQueryMessage` (query).
- `axon_message_name` / `axon_payload_type`: fully-qualified class name of the command/query/event payload.
- `axon_aggregate_identifier`: `Dwelling:<dwellingId>` on the dwelling's domain-event spans.
- **Causation:** the recruit event's `axon_metadata_correlationId` and `axon_metadata_traceId` both equal the
  originating `RecruitCreature` command's `axon_message_id`.
- **W3C propagation:** `axon_metadata_traceparent` matches `00-<32hex>-<16hex>-<2hex>`, and the async
  `StreamingEventProcessor.process(CreatureRecruited)` span shares the **same trace-id** as the dispatch span.

## When migrating to Axon 5

Re-run `JaegerTracingIntegrationTest`. Expected, intentional changes (e.g. renamed operations or attributes
in Axon 5) should be reflected by updating the constants in the test **and** this document in the same commit,
so the diff documents the behavioural change.
