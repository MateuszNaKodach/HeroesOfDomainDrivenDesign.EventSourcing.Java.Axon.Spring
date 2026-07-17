# 📊 Observability — distributed tracing

The app can emit distributed traces to either **Elastic APM** or **Jaeger** via OpenTelemetry. Tracing is **off by default** and activated by one of two Spring profiles, each paired with its own Docker Compose overlay.

> 👉 Back to the [main README](../README.md).

## Table of contents

1. [Backends — pick one](#backends--pick-one)
2. [Stack components](#stack-components)
3. [Run with Elastic APM](#run-with-elastic-apm)
4. [Run with Jaeger](#run-with-jaeger)
5. [Run without tracing (default)](#run-without-tracing-default)
6. [Ports & URLs](#ports--urls)
7. [Exploring traces in Kibana — guided tour](#exploring-traces-in-kibana--guided-tour)
8. [Same traces in Jaeger](#same-traces-in-jaeger)
9. [Useful filters](#useful-filters)
10. [JDBC / SQL spans](#jdbc--sql-spans)
11. [R2DBC / SQL spans](#r2dbc--sql-spans)
12. [gRPC spans (Axon Server connector)](#grpc-spans-axon-server-connector)
13. [Why Axon splits work across multiple traces](#why-axon-splits-work-across-multiple-traces)

## Backends — pick one

| Backend         | Spring profile             | Compose overlay                                  | Footprint    | UI                                  | When to use                                    |
|-----------------|----------------------------|--------------------------------------------------|--------------|-------------------------------------|------------------------------------------------|
| **Elastic APM** | `observability-elastic`    | `docker-compose.observability-elastic.yaml`      | ~2 GB RAM    | Kibana APM (rich, service map, KQL) | Production-realistic UX, queries, dashboards   |
| **Jaeger**      | `observability-jaeger`     | `docker-compose.observability-jaeger.yaml`       | ~150 MB RAM  | Jaeger UI (simple waterfall)        | Quick spin-up, demos, CI, constrained machines |

The two profiles are **alternatives** — pick the backend you want for a given session. The base `observability` profile (sampling, OTLP/HTTP transport, Axon `OpenTelemetrySpanFactory` wiring) is inherited by both via Spring's `spring.profiles.group`, so switching backends is a one-line change.

## Stack components

| Component | Role |
|---|---|
| [`axon-tracing-opentelemetry`](https://docs.axoniq.io/axon-framework-reference/4.13/monitoring/tracing/) | Instruments command/event/query handlers, aggregates, repositories, event store |
| [`micrometer-tracing-bridge-otel`](https://docs.spring.io/spring-boot/reference/actuator/tracing.html) | Spring Boot's official bridge from Micrometer Observation to OpenTelemetry |
| [`opentelemetry-exporter-otlp`](https://opentelemetry.io/docs/specs/otlp/) | Pushes traces over OTLP/HTTP to the chosen backend |
| [`datasource-micrometer-spring-boot`](https://github.com/jdbc-observations/datasource-micrometer) | Wraps the HikariCP `DataSource` so each JDBC connection/query becomes a child span carrying the SQL text and bind parameters. Runtime gate: `jdbc.datasource-proxy.enabled` |
| [`r2dbc-proxy`](https://github.com/r2dbc/r2dbc-proxy) | Activates Spring Boot's own `R2dbcObservationAutoConfiguration`, which wraps the reactive `ConnectionFactory` so each read-model query becomes a Micrometer Observation → child span. The parent is resolved from the Reactor Context captured at subscription. Runtime gate: `management.observations.enable.r2dbc` |
| [`opentelemetry-grpc-1.6`](https://opentelemetry.io/docs/languages/java/instrumentation/) | Client interceptor on the Axon Server connector channel, producing gRPC client spans (only under an `axonserver` profile) |
| [Elastic APM 9.x](https://www.elastic.co/observability/application-performance-monitoring) | Receives OTLP, stores in Elasticsearch, visualizes in Kibana |
| [Jaeger 2.x](https://www.jaegertracing.io/) | Receives OTLP directly, in-memory storage, lightweight UI |

Activation surface:
- Base profile **`observability`** — common tracing config (sampling, transport, Axon span factory bean). Never activated directly.
- Profile **`observability-elastic`** — base + Elastic APM endpoint
- Profile **`observability-jaeger`** — base + Jaeger endpoint
- Profile groups in `application.yaml` make the two child profiles automatically include the base.

Instrumentation JARs (JDBC, R2DBC, gRPC) are always on the classpath; each feature is gated at **runtime** only:
the `observability` profile plus the feature's enabled property (`jdbc.datasource-proxy.enabled`,
`management.observations.enable.r2dbc`, or `axon.axonserver.enabled`) must be active before spans appear.
Normal runs (no observability profile) carry no per-query observations.

## Run with Elastic APM

1. Start the base stack + Elastic observability stack:
   ```bash
   docker compose -f docker-compose.yaml -f docker-compose.observability-elastic.yaml up -d
   ```
   Wait ~60s for Elasticsearch and Kibana to be ready.

2. Run the app with the `observability-elastic` profile:
   ```bash
   SPRING_PROFILES_ACTIVE=observability-elastic ./mvnw spring-boot:run
   ```
   Or: `./mvnw spring-boot:run -Dspring-boot.run.profiles=observability-elastic`.

3. Generate some traffic via Swagger UI at [http://localhost:3773/swagger-ui/index.html](http://localhost:3773/swagger-ui/index.html).

4. Open Kibana APM — see [the guided tour below](#exploring-traces-in-kibana--guided-tour).

## Run with Jaeger

1. Start the base stack + Jaeger:
   ```bash
   docker compose -f docker-compose.yaml -f docker-compose.observability-jaeger.yaml up -d
   ```
   Jaeger v2 starts in seconds — UI is ready almost immediately.

2. Run the app with the `observability-jaeger` profile:
   ```bash
   SPRING_PROFILES_ACTIVE=observability-jaeger ./mvnw spring-boot:run
   ```
   Or: `./mvnw spring-boot:run -Dspring-boot.run.profiles=observability-jaeger`.

3. Generate some traffic via Swagger UI at [http://localhost:3773/swagger-ui/index.html](http://localhost:3773/swagger-ui/index.html).

4. Open Jaeger UI at [http://localhost:16686](http://localhost:16686) — see [Same traces in Jaeger](#same-traces-in-jaeger) below.

## Run without tracing (default)

```bash
docker compose up -d
./mvnw spring-boot:run
```

No `observability-*` profile = no traces emitted, no extra containers needed. Useful for daily development.

## Ports & URLs

| Service                              | Port  | URL                                                                                | Used by profile           |
|--------------------------------------|-------|------------------------------------------------------------------------------------|---------------------------|
| Kibana (APM UI)                      | 5601  | [http://localhost:5601](http://localhost:5601)                                     | `observability-elastic`   |
| Kibana → APM → Services              |       | [http://localhost:5601/app/apm/services](http://localhost:5601/app/apm/services)   | `observability-elastic`   |
| Kibana → APM → Traces                |       | [http://localhost:5601/app/apm/traces](http://localhost:5601/app/apm/traces)       | `observability-elastic`   |
| Elasticsearch                        | 9200  | [http://localhost:9200](http://localhost:9200)                                     | `observability-elastic`   |
| Elastic APM Server (OTLP receiver)   | 8200  | [http://localhost:8200/v1/traces](http://localhost:8200/v1/traces)                 | `observability-elastic`   |
| Jaeger UI                            | 16686 | [http://localhost:16686](http://localhost:16686)                                   | `observability-jaeger`    |
| Jaeger OTLP receiver (HTTP)          | 4318  | [http://localhost:4318/v1/traces](http://localhost:4318/v1/traces)                 | `observability-jaeger`    |
| Jaeger OTLP receiver (gRPC)          | 4317  | grpc://localhost:4317                                                              | `observability-jaeger`    |

## Exploring traces in Kibana — guided tour

A quick walkthrough of what you can see and where to click. Examples below were captured after running the full chain `BuildDwelling → IncreaseAvailableCreatures → RecruitCreature → GetDwellingById` through Swagger UI with the `observability-elastic` profile active.

### 1. Service inventory

[Kibana → ☰ → Observability → APM → Services](http://localhost:5601/app/apm/services) — landing page lists every service emitting traces. After firing a few requests you should see `heroesofddd` with average latency, throughput, and error rate.

![Service inventory](images/observability/service-inventory.png)

### 2. Transactions

Click `heroesofddd` → **Transactions** tab. Each row is a distinct "entry point" — both HTTP endpoints (auto-instrumented by Spring Web) and Axon's async boundaries (each command/event/query handler is a top-level transaction because Axon hops across the gRPC bus and async event processors).

![Transactions](images/observability/transactions.png)

### 3. Traces — the full list

[Kibana → APM → Traces](http://localhost:5601/app/apm/traces) shows every individual trace tree.

For this project you'll see roots like:
- `http put /games/{gameId}/dwellings/{dwellingId}` — HTTP root
- `CommandBus.handleDistributedCommand(RecruitCreature)` — command handling on the aggregate side after the gRPC hop to Axon Server
- `StreamingEventProcessor.process(CreatureRecruited)` — projector / automation
- `CommandBus.handleCommand(AddCreatureToArmy)` — command emitted by the `WhenCreatureRecruitedThenAddToArmy` automation
- `QueryBus.processQueryMessage(GetDwellingById)` — query side

![Traces list](images/observability/traces.png)

### 4. Trace waterfall — Axon internals visible

Click any `CommandBus.handleCommand(...)` trace and Kibana renders a waterfall like this — the full call path of the Axon command handler, including aggregate loading and event publication:

![Axon trace waterfall](images/observability/axon-waterfall.png)

What you're looking at:

```
✓ CommandBus.handleDistributedCommand(RecruitCreature)     22 ms     ← gRPC server side
  └─ CommandBus.dispatchCommand(RecruitCreature)           22 ms
     └─ CommandBus.handleCommand(RecruitCreature)          22 ms
        ├─ Repository.load                                 10 ms     ← event sourcing
        │  ├─ Repository.obtainLock                        41 μs
        │  └─ Repository.initializeState(Dwelling)          1.0 ms   ← rehydrate aggregate
        ├─ Dwelling.decide(RecruitCreature)                 3.2 ms   ← AGGREGATE business logic
        ├─ EventBus.publishEvent(CreatureRecruited)        29 μs
        └─ EventBus.commitEvents                            5.8 ms   ← persist to event store
```

This is *exactly* the layered shape from Event Sourcing theory — repository → aggregate → event publication — rendered as data, not as a diagram in a slide.

### 5. Span attributes — gameId, playerId, message metadata

Click any Axon span (e.g. `CommandBus.handleCommand(RecruitCreature)`) → **Metadata** tab. The flyout shows OpenTelemetry attributes — including correlation data injected by `GameConfiguration.gameDataProvider`:

![Span attributes](images/observability/span-attributes.png)

```
labels.axon_metadata_gameId    = scenario-1                  ← from gameDataProvider
labels.axon_metadata_playerId  = player-1                    ← from gameDataProvider
labels.axon_message_id         = 2201ae5d-3871-45b2-a661-...
labels.axon_message_name       = com.dddheroes…RecruitCreature
labels.axon_message_type       = GrpcBackedCommandMessage
labels.axon_payload_type       = com.dddheroes…RecruitCreature
```

This is the practical payoff: filtering traces by `labels.axon_metadata_gameId : "scenario-1"` in the Kibana search bar isolates every span — across every aggregate, processor and projector — that participated in one game session.

## Same traces in Jaeger

With `observability-jaeger` active, the **same** trace data is produced — Jaeger just renders it differently:

1. Open [http://localhost:16686](http://localhost:16686).
2. **Service** dropdown → pick `heroesofddd`.
3. **Operation** dropdown → e.g. `CommandBus.handleCommand(RecruitCreature)`.
4. Click **Find Traces** → see the same waterfall tree (`Repository.load`, `Dwelling.decide`, `EventBus.publishEvent`, …).
5. Click any span → "Tags" panel shows the same OTel attributes as Kibana labels, with dot-notation: `axon.message.id`, `axon.message.name`, `axon.metadata.gameId`, `axon.metadata.playerId`, etc.

Trade-offs vs Kibana APM:
- ✅ **Lighter** — one container, instant startup, no Elasticsearch index management
- ✅ **Simpler** — direct trace search by service / operation / tag / duration
- ❌ **No service map** — Kibana shows topology between services; Jaeger v2 OSS doesn't
- ❌ **No KQL** — Jaeger uses a simpler tag-equality search (`tag: axon.metadata.gameId=scenario-1`) rather than full KQL
- ❌ **No persistence by default** — all-in-one stores traces in memory; restart loses them
- ❌ **No metrics/logs correlation** — Kibana correlates traces with the rest of the Elastic stack

## Useful filters

### Kibana (KQL)

Paste into the Kibana search bar (KQL) at the top of any APM page:

| Goal | KQL |
|---|---|
| Traces for one game | `labels.axon_metadata_gameId : "scenario-1"` |
| Only command handlers | `transaction.name : "CommandBus.handleCommand*"` |
| Only one aggregate's decisions | `span.name : "Dwelling.decide(*)"` |
| Only automation reactions | `span.name : "*Processor.react(*)"` |
| Only event publications | `span.name : "EventBus.publishEvent(*)"` |

### Jaeger (Tags field)

In the Jaeger UI search form, the **Tags** field accepts space-separated `key=value` pairs:

| Goal | Tag query |
|---|---|
| Traces for one game | `axon.metadata.gameId=scenario-1` |
| One specific player's traces | `axon.metadata.playerId=player-1` |
| Combine | `axon.metadata.gameId=scenario-1 axon.metadata.playerId=player-1` |

(Operation-level filtering — e.g. "only `Dwelling.decide` spans" — is done via the **Operation** dropdown, not the Tags field.)

## JDBC / SQL spans

Under the `observability` profile the app also instruments the JDBC layer via
[`datasource-micrometer-spring-boot`](https://github.com/jdbc-observations/datasource-micrometer).
It wraps the autoconfigured HikariCP `DataSource` in a proxy that emits a Micrometer Observation
per JDBC connection and query, so **every SQL statement becomes its own child span** nested under
the Axon span that triggered it:

- Axon **JPA event store** reads/appends and snapshot access → SQL spans under the event-store /
  aggregate-load spans.
- **Read-model projections** (`DwellingReadModel`, `BuiltDwellingReadModel`) → INSERT/UPDATE/SELECT
  spans under the projection event-handler spans.

Each query span carries the SQL text; `jdbc.datasource-proxy.include-parameter-values: true` also
attaches the bind-parameter values. This is instrumented purely at the `DataSource` layer — no
domain or infrastructure code changes — so all pooled SQL is captured automatically.

Gating: the proxy is **disabled by default** (`jdbc.datasource-proxy.enabled: false` in
`application.yaml`) and turned on only by the `observability` profile
(`application-observability.yaml`), matching the rest of the tracing setup — normal runs are
unaffected.

> ⚠️ Bind-parameter values can expose data. This is intentional here (local/dev tracing, off by
> default). Before enabling in any shared environment, revisit `include-parameter-values`.

## R2DBC / SQL spans

The reactive read models use Spring Data R2DBC, so JDBC instrumentation does not see their queries. R2DBC
tracing rides on Spring Boot's own [`R2dbcObservationAutoConfiguration`](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/actuate/autoconfigure/r2dbc/R2dbcObservationAutoConfiguration.html),
activated by [`r2dbc-proxy`](https://github.com/r2dbc/r2dbc-proxy) on the classpath: it wraps the reactive
`ConnectionFactory` with `ObservationProxyExecutionListener`, so each query becomes a Micrometer Observation
(`r2dbc.query...`) flowing through the same `micrometer-tracing-bridge-otel` → OTLP pipeline as everything else.

```bash
SPRING_PROFILES_ACTIVE=observability-jaeger ./mvnw spring-boot:run
```

Every reactive SELECT, INSERT, and UPDATE issued by the read-model repositories becomes a database-client
span under the WebFlux request or Axon projection/query-handler span that initiated it. Crucially, the
listener resolves the parent from the **Reactor Context captured at subscription** (not from a thread-local
read at query time), so queries whose I/O completes on the Postgres driver's shared Netty event-loop threads
still nest correctly — the raw-OpenTelemetry `R2dbcTelemetry` wrapper used previously read
`Context.current()` on those threads and produced orphaned root spans instead.

Gating: `management.observations.enable.r2dbc` is `false` by default (`application.yaml`) and turned on by
the `observability` profile, which also sets `management.observations.r2dbc.include-parameter-values: true`
(same caveat as the JDBC bind-parameter values above). Normal runs therefore remain unchanged.

## gRPC spans (Axon Server connector)

The only gRPC in this app is the connection to **Axon Server** (via `axonserver-connector-java`),
which is **disabled by default** — the app runs on the JPA event store. gRPC spans therefore only
appear when you run under an Axon Server profile *and* the `observability` profile, e.g.:

```bash
SPRING_PROFILES_ACTIVE=observability-jaeger,axonserver-dcb ./mvnw spring-boot:run
```

Instrumentation is an OpenTelemetry gRPC `ClientInterceptor` (`opentelemetry-grpc-1.6`) registered
on the connector channel through Axon 5's `ManagedChannelCustomizer` hook (see
`GrpcTracingConfiguration`). Spans carry the standard gRPC attributes (`rpc.system=grpc`,
`rpc.service`, `rpc.method`, `rpc.grpc.status_code`) and flow through the same OTLP pipeline as the
Axon/HTTP/JDBC spans. It is built from the shared `OpenTelemetry` SDK bean, so no extra export
wiring is needed.


> ℹ️ Axon Server traffic is dominated by **long-lived bidirectional streams** (command / query /
> event / control channels). gRPC client instrumentation opens **one span per RPC**, so a streaming
> call produces a *single span that lasts the whole stream lifetime* (often the app's lifetime) —
> not one span per message. These spans are most useful for connection/stream lifecycle and error
> visibility; **per-message** command/event/query tracing is already provided by the framework's
> distributed tracing (`axoniq-distributed-messaging`). Expect a few very long-duration gRPC spans
> in the trace list — that is normal for streaming RPCs.

## Why Axon splits work across multiple traces

You'll notice that an HTTP request often produces *two or three* separate trace trees rather than one giant tree. That's expected. Axon hops over async boundaries that don't preserve OpenTelemetry context: the gRPC call to Axon Server (server-side `handleDistributedCommand` starts a new root) and the asynchronous event processors (each `process(Event)` is its own root). Inside one boundary, however, the tree is complete — as the waterfall above shows. Behavior is identical in both Kibana APM and Jaeger.

To stitch sessions together end-to-end, use the `axon_metadata_gameId` (Kibana) / `axon.metadata.gameId` (Jaeger) tag filter described above.
