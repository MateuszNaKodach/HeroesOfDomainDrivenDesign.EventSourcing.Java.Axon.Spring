# Recipe: Aggregate-centric `EventStorageEngine` wiring

Atomic migration of the **event store backend**: pick the right AF5 `EventStorageEngine` implementation, wire it up, emit the SQL schema migration (JPA path).

AF4 used `EmbeddedEventStore` + `JpaEventStorageEngine` / `JdbcEventStorageEngine` / Axon Server backed engine. AF5 collapses to a single bean of type `org.axonframework.eventsourcing.eventstore.EventStorageEngine`.

This is a one-shot recipe (Migration Phase #9) — no item iteration; one bean swap.

## Goal

ONE bean of type `EventStorageEngine`, picked from:
- `AggregateBasedJpaEventStorageEngine` (JPA path, Path A) — **schema migration required**.
- `AggregateBasedAxonServerEventStorageEngine` (Axon Server path, Path B) — explicit `@Bean` overrides the autoconfigured DCB-flat engine.
- `componentRegistry(...)` registration on `EventSourcingConfigurer` (non-Spring path, Path C).

Reads existing aggregate-keyed event log — preserves legacy storage. NO data rewrite.

## Inputs

- target: FQ name of the configuration class that today declares the `EventStorageEngine` bean (required)
- path: `A` | `B` | `C` (optional — derived from inspection; user picks via AskUserQuestion when ambiguous)

## Subagent guidelines

- subagent_type: general-purpose
- isolation: worktree
  # Bean swap can ripple through autoconfigure imports — worktree gives easy rollback if
  # the path picked turns out wrong (e.g. user wanted Path B but Path A was picked first).
- prompt-framing: |
  This is a one-shot bean swap. NO test runs as part of the recipe — runtime verification
  happens during stabilization once the user applies the SQL. The recipe's job is: pick
  the right AF5 engine, replace the bean, emit the SQL on Path A, surface custom-Serializer
  ports to the orchestrator.
- parallelism: single

## Preflight

1. **Read [not-supported.md](not-supported.md) first** — run every Detection grep listed there. If any blocker fires, follow that file's `AskUserQuestion` flow and apply its "Effect on Procedure" before doing anything else. Recipe must NOT proceed past Preflight while a blocker is unresolved.
2. Project already declares a single `EventStorageEngine` bean of an `AggregateBased*` type?
3. No leftover `JpaEventStorageEngine` / `JdbcEventStorageEngine` / `EmbeddedEventStore` references?
4. Compile clean?
5. If 2–4 all yes AND no blocker fired → return Output with skip=true.

## Decision tree — which AF5 engine?

Inspect AF4 wiring before changing anything.

| AF4 wiring observed | AF5 target | Notes |
|---|---|---|
| `JpaEventStorageEngine.builder()…build()` (often inside `EmbeddedEventStore`) | `AggregateBasedJpaEventStorageEngine` | **Schema migration required** — Path A. |
| `AxonServerEventStore` / `AxonServer*EventStore` autoconfigured by `axon-spring-boot-starter` | `AggregateBasedAxonServerEventStorageEngine` | Axoniq connector's `AxonServerConfigurationEnhancer` auto-registers `AxonServerEventStorageEngine` (DCB-flat), **NOT** the aggregate-based variant. To preserve aggregate-keyed event-log semantics, declare explicit `@Bean EventStorageEngine` returning `new AggregateBasedAxonServerEventStorageEngine(...)` — Path B. |
| `JdbcEventStorageEngine.builder()…build()` | *No drop-in equivalent in AF5 yet* | **Blocker — see [not-supported.md](not-supported.md) B2.** User picks JPA / Axon Server / defer. |
| `MongoEventStorageEngine` (from `axon-mongo` extension) | *No AF5 release of `axon-mongo`* | **Blocker — see [not-supported.md](not-supported.md) B1.** User picks Axon Server / JPA / pause / accept-stays-af4. |
| Custom `EventStorageEngine` subclass | Reimplement on top of `AggregateBased*EventStorageEngine` | **Blocker — see [not-supported.md](not-supported.md) B3.** Out of scope for one atomic invocation. |

### Detection (run from target root)

```bash
grep -RnE 'JpaEventStorageEngine|JdbcEventStorageEngine|MongoEventStorageEngine|EmbeddedEventStore|AxonServerEventStore' \
     --include='*.java' --include='*.kt' src 2>/dev/null

# Mongo extension signals:
grep -RnE 'org\.axonframework\.extensions\.mongo|axon-mongo' \
     --include='*.java' --include='*.kt' --include='pom.xml' --include='*.gradle*' . 2>/dev/null

# Spring Boot starter signals:
grep -RnE 'axon-server-connector|axoniq-spring-boot-starter|axon-spring-boot-starter' pom.xml */pom.xml 2>/dev/null
```

If project depends on `axoniq-spring-boot-starter` (or pre-migration `axon-spring-boot-starter` plus `axon-server-connector`), Path B is almost always the right answer even if `JpaEventStorageEngine` bean co-exists.

After inspection, ask user with `AskUserQuestion` to confirm path. Default to inspection's recommendation; mark it `(Recommended)`.

### Blockers

Full detection greps + `AskUserQuestion` flows + Output decision keys live in [not-supported.md](not-supported.md). Run that file's checks during Preflight; this section just summarizes which rows of the decision tree route there.

| Decision-tree row | Blocker | File |
|---|---|---|
| `JdbcEventStorageEngine.builder()…build()` | B2 | [not-supported.md](not-supported.md) |
| `MongoEventStorageEngine` | B1 | [not-supported.md](not-supported.md) |
| Custom `EventStorageEngine` subclass | B3 | [not-supported.md](not-supported.md) |
| Custom `Serializer` (soft) | B4 | [not-supported.md](not-supported.md) |

## FQN cheat sheet

### AF4 — remove

| Element | FQN |
|---|---|
| `EmbeddedEventStore` | `org.axonframework.eventsourcing.eventstore.EmbeddedEventStore` |
| `JpaEventStorageEngine` | `org.axonframework.eventsourcing.eventstore.jpa.JpaEventStorageEngine` |
| `JdbcEventStorageEngine` | `org.axonframework.eventsourcing.eventstore.jdbc.JdbcEventStorageEngine` |
| `AxonServerEventStore` | `org.axonframework.axonserver.connector.event.axon.AxonServerEventStore` |
| `MongoEventStorageEngine` | `org.axonframework.extensions.mongo.eventsourcing.eventstore.MongoEventStorageEngine` |

### AF5 — add

| Element | FQN |
|---|---|
| `EventStorageEngine` (interface) | `org.axonframework.eventsourcing.eventstore.EventStorageEngine` |
| `AggregateBasedJpaEventStorageEngine` | `org.axonframework.eventsourcing.eventstore.jpa.AggregateBasedJpaEventStorageEngine` |
| `AggregateBasedAxonServerEventStorageEngine` | `io.axoniq.framework.axonserver.connector.event.AggregateBasedAxonServerEventStorageEngine` (ships in `io.axoniq.framework:axon-server-connector`, free) |
| `AggregateBasedJpaEventStorageEngineConfiguration` | `org.axonframework.eventsourcing.eventstore.jpa.AggregateBasedJpaEventStorageEngineConfiguration` |
| `AggregateEventEntry` (JPA entity) | `org.axonframework.eventsourcing.eventstore.jpa.AggregateEventEntry` |
| `JpaTransactionalExecutorProvider` | `org.axonframework.messaging.core.unitofwork.transaction.jpa.JpaTransactionalExecutorProvider` |
| `EventConverter` | `org.axonframework.messaging.eventhandling.conversion.EventConverter` |
| `Converter` | `org.axonframework.conversion.Converter` |
| `EventSourcingConfigurer` | `org.axonframework.eventsourcing.configuration.EventSourcingConfigurer` |
| `ComponentDefinition` | `org.axonframework.common.configuration.ComponentDefinition` |
| `ConfigurationEnhancer` | `org.axonframework.common.configuration.ConfigurationEnhancer` |

> AF5 replaced `Serializer` with `Converter` / `EventConverter`. New storage engines take an `EventConverter`, not a `Serializer`. If AF4 wired a `Serializer` bean as a constructor arg to the engine, that wiring is gone — `EventConverter` is resolved from the configuration registry.

### Path A — Spring Boot + JPA

#### Condition

- Spring Boot project AND AF4 wiring observed via `JpaEventStorageEngine.builder()…build()` (typically inside an `EmbeddedEventStore`).
- ALSO the fallback target when AF4 used `JdbcEventStorageEngine` and the user picks "move to JPA + Hibernate over same DB."

#### Steps

##### A.1. Drop AF4 wiring; let autoconfig win when possible

If project declared its own `EventStore` / `EventStorageEngine` `@Bean`, **delete those bean methods**. AF5 `JpaEventStoreAutoConfiguration` registers `AggregateBasedJpaEventStorageEngine` automatically when an `EntityManagerFactory` and `PlatformTransactionManager` are present and no other `EventStorageEngine`/`EventStore` bean exists.

Also delete the `EmbeddedEventStore` bean — AF5 wires the event store from the engine alone.

##### A.2. Override defaults — register a `ConfigurationEnhancer`

Only when AF4 wiring tuned `batchSize`, `gapTimeout`, `lowestGlobalSequence`, `maxGapOffset`, `gapCleaningThreshold`, or the `PersistenceExceptionResolver`. Mirror the framework's `AggregateBasedJpaEventStorageEngineConfigurationEnhancer`:

```java
@Bean
public ConfigurationEnhancer aggregateBasedJpaEventStorageEngineCustomization(
        EntityManagerFactory entityManagerFactory,
        PersistenceExceptionResolver persistenceExceptionResolver) {

    return registry -> {
        UnaryOperator<AggregateBasedJpaEventStorageEngineConfiguration> configurer = config ->
                config.batchSize(200)
                      .gapTimeout(60_000)
                      .persistenceExceptionResolver(persistenceExceptionResolver);

        ComponentDefinition<EventStorageEngine> definition =
                ComponentDefinition.ofType(EventStorageEngine.class)
                                   .withBuilder(configuration ->
                                           new AggregateBasedJpaEventStorageEngine(
                                                   new JpaTransactionalExecutorProvider(entityManagerFactory),
                                                   configuration.getComponent(EventConverter.class),
                                                   configurer))
                                   .onShutdown(Phase.INBOUND_EVENT_CONNECTORS, ese -> {
                                       if (ese instanceof AggregateBasedJpaEventStorageEngine engine) {
                                           engine.close();
                                       }
                                   });

        registry.registerIfNotPresent(definition, SearchScope.ALL);
    };
}
```

Default knobs are usually fine — only introduce the enhancer if AF4 explicitly tuned them.

##### A.3. Custom `Serializer` → `Converter`

Different SPI; flag for user. The recipe does NOT auto-port a custom `Serializer`. AF5 ships matching default converters for Jackson/XStream — most projects work without changes. Subclassed serializers / custom `RevisionResolver` / `ContentTypeConverter` need redesign.

##### A.4. Schema migration — rename `domain_event_entry` → `aggregate_event_entry`

AF5 uses a different table with renamed columns and a stricter sequence generator. The framework cannot read AF4 events out of the old table. **Migrate data, not just schema.**

> 🚨 **This skill does NOT migrate event data.** The SQL emitted below is a **column/table rename** that operates on rows already present in the AF4 `domain_event_entry` table — same DB, same payload bytes. It is **not** a Mongo/JDBC/anywhere → JPA data export. If the user picked `move-to-jpa` from the Mongo or JDBC blocker, the new AF5 table will be **empty until the user runs their own out-of-band data migration**. Verify row counts before/after on a non-prod copy.

Full diff (per `axon-5/api-changes/10-stored-format-changes.md`):

| AF4 column (`domain_event_entry`) | AF5 column (`aggregate_event_entry`) | Constraint change |
|---|---|---|
| `event_identifier` | `identifier` | now `NOT NULL` |
| `payload_type` | `type` | — |
| `payload_revision` | `version` | now **NOT NULL** (was optional) |
| `time_stamp` | `timestamp` | — |
| `type` | `aggregate_type` | — |
| `sequence_number` | `aggregate_sequence_number` | now **NOT NULL** |
| `meta_data` | `metadata` | — |
| `payload` | `payload` | max length cap of 10_000 **removed** |
| `aggregate_identifier` | `aggregate_identifier` | now **optional** (DCB-friendly) |
| (table-name index) | unique index on `(aggregate_identifier, aggregate_sequence_number)` | — |
| `@GeneratedValue` (default) | sequence `aggregate-event-global-index-sequence`, allocation size **1** | — |

SQL shape (vendor-specific tweaks needed):

```sql
-- AF4 → AF5: rename domain_event_entry → aggregate_event_entry
-- Run inside a transaction. Verify counts before and after.

ALTER TABLE domain_event_entry RENAME TO aggregate_event_entry;

ALTER TABLE aggregate_event_entry RENAME COLUMN event_identifier     TO identifier;
ALTER TABLE aggregate_event_entry RENAME COLUMN payload_type         TO type;
ALTER TABLE aggregate_event_entry RENAME COLUMN payload_revision     TO version;
ALTER TABLE aggregate_event_entry RENAME COLUMN time_stamp           TO timestamp;
ALTER TABLE aggregate_event_entry RENAME COLUMN type                 TO aggregate_type;
ALTER TABLE aggregate_event_entry RENAME COLUMN sequence_number      TO aggregate_sequence_number;
ALTER TABLE aggregate_event_entry RENAME COLUMN meta_data            TO metadata;

-- AF4 allowed null payload_revision; AF5 requires it.
UPDATE aggregate_event_entry SET version = '0' WHERE version IS NULL;
ALTER TABLE aggregate_event_entry ALTER COLUMN version SET NOT NULL;

-- aggregate_sequence_number now NOT NULL.
ALTER TABLE aggregate_event_entry ALTER COLUMN aggregate_sequence_number SET NOT NULL;

-- Drop AF4 length cap on payload / metadata (vendor-specific — adjust).
-- e.g. PostgreSQL: ALTER TABLE … ALTER COLUMN payload  TYPE bytea;
--                  ALTER TABLE … ALTER COLUMN metadata TYPE bytea;

-- Sequence generator with allocation size 1.
CREATE SEQUENCE IF NOT EXISTS "aggregate-event-global-index-sequence" INCREMENT BY 1 MINVALUE 1;
-- Seed past highest existing global_index, vendor-specifically.

-- Unique index on (aggregate_identifier, aggregate_sequence_number).
CREATE UNIQUE INDEX IF NOT EXISTS aggregate_event_entry_aggidx
    ON aggregate_event_entry (aggregate_identifier, aggregate_sequence_number);
```

Place under `<target>/.axon4-to-axon5-migration/sql/01-rename-domain-to-aggregate-event-entry.sql`. If project uses Flyway/Liquibase, emit `V<NN>__af5_aggregate_event_entry.sql` (or changeset) under the project's existing migration directory instead.

`payload`/`metadata` `byte[]` content does NOT need rewriting — converter-encoded blobs round-trip provided same converter is configured. Surface to user so they don't double-migrate.

The orchestrator does **NOT** run the SQL. User runs it on a controlled environment.

> ⚠️ **Don't tie SQL run to bean swap.** Ship SQL on a quiet window, verify renamed table healthy, then ship AF5 bean change. Single deploy that flips both is hard to roll back.

##### A.5. Entity scan

`AggregateEventEntry` is a `@Entity` from the framework JAR. Spring Boot autoconfig registers it via `@RegisterDefaultEntities` — no `persistence.xml` change needed. **But** if the project uses an explicit `LocalContainerEntityManagerFactoryBean` with fixed `packagesToScan`, **add `org.axonframework.eventsourcing.eventstore.jpa`** to that list. Don't copy `AggregateEventEntry` into the project; custom `DomainEventEntry` subclasses are out of scope.

### Path B — Spring Boot + Axon Server

#### Condition

- Project depends on `axoniq-spring-boot-starter` (or pre-migration `axon-spring-boot-starter` plus `axon-server-connector`).
- AF4 wiring uses `AxonServerEventStore` autoconfigured by the starter — even if `JpaEventStorageEngine` is also declared, Path B is almost always correct here.

#### Steps

##### B.0. What the connector autoconfig actually does

Verified on `axon-server-connector:5.x` (`AxonServerConfigurationEnhancer`, ServiceLoader-discovered):

- Registers `EventStorageEngine` via `AxonServerEventStorageEngineFactory.constructForContext(...)` → constructs **`AxonServerEventStorageEngine`** (DCB-flat).
- `AggregateBasedAxonServerEventStorageEngine` ships in the same JAR but has **no factory** and is **not auto-registered** — opt-in.
- Connector calls `registerIfNotPresent`, so any earlier-registered or Spring-bean-registered `EventStorageEngine` wins.

Choose based on the migration goal, not just on Axon Server presence:

| Goal | Engine | Path |
|---|---|---|
| Preserve AF4 aggregate-keyed event log on AF5 (orchestrator default — "legacy storage preserved") | `AggregateBasedAxonServerEventStorageEngine` | **B-aggregate** — explicit `@Bean`. |
| Migrate to AF5 DCB semantics (flat event log, no aggregate routing) | `AxonServerEventStorageEngine` | **B-DCB** — let autoconfig win. |

If unsure, ask user. Aggregate preservation is the safe default; DCB migration is a separate, larger initiative.

##### B-aggregate.1. Declare explicit `@Bean EventStorageEngine`

Spring registers the bean as a component before the connector's enhancer runs `registerIfNotPresent`, so the aggregate-based engine wins:

```java
@Bean
public EventStorageEngine storageEngine(AxonServerConnectionManager connectionManager,
                                        EventConverter eventConverter) {
    // getConnection() = default context. Use getConnection(String) for non-default.
    return new AggregateBasedAxonServerEventStorageEngine(
            connectionManager.getConnection(),
            eventConverter
    );
}
```

Imports:

```java
import io.axoniq.framework.axonserver.connector.api.AxonServerConnectionManager;
import io.axoniq.framework.axonserver.connector.event.AggregateBasedAxonServerEventStorageEngine;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.messaging.eventhandling.conversion.EventConverter;
```

If project also declared an AF4 `@Bean EventStore` / `@Bean EventStorageEngine`, **delete those** — two beans of `EventStorageEngine` = startup failure.

##### B-DCB.1. Remove AF4 storage-engine wiring

Delete any AF4 `@Bean EventStore` / `@Bean EventStorageEngine`. With no Spring bean of that type, the connector's `AxonServerConfigurationEnhancer` auto-registers `AxonServerEventStorageEngine` (DCB-flat). Adding your own bean prevents autoconfig.

##### B.2. Confirm connector dependency

```xml
<dependency>
    <groupId>io.axoniq.framework</groupId>
    <artifactId>axoniq-spring-boot-starter</artifactId>
</dependency>
```

(The OpenRewrite recipe should already have done this swap. Verify.)

##### B.3. NO SQL

Axon Server stores its own events.

##### B.4. Caveat — converter wiring

If AF4 explicitly wired a `Serializer` bean (e.g. Jackson) to the storage engine, port to a `Converter`/`EventConverter` bean. Without one, AF5 falls back to default converter, which may not round-trip events serialized by a customized AF4 Jackson serializer. Surface to user.

### Path C — Non-Spring (programmatic Configuration API)

#### Condition

- Project does NOT depend on Spring Boot starter (`axoniq-spring-boot-starter` / `axon-spring-boot-starter`). Wiring lives in code via `EventSourcingConfigurer.create()` / `componentRegistry(...)`.

#### Steps

JPA variant:

```java
EventSourcingConfigurer configurer = EventSourcingConfigurer.create();

configurer.componentRegistry(registry ->
    registry.registerComponent(
        EventStorageEngine.class,
        configuration -> new AggregateBasedJpaEventStorageEngine(
            new JpaTransactionalExecutorProvider(entityManagerFactory),
            configuration.getComponent(EventConverter.class),
            cfg -> cfg.batchSize(100)
        )
    )
);
```

Axon Server variant:

```java
configurer.componentRegistry(registry ->
    registry.registerComponent(
        EventStorageEngine.class,
        configuration -> new AggregateBasedAxonServerEventStorageEngine(
            configuration.getComponent(AxonServerConnection.class),
            configuration.getComponent(EventConverter.class)
        )
    )
);
```

For Axon Server in non-Spring setups, the connector's enhancer registers the DCB-flat engine when on the classpath — explicit registration is the override path for aggregate preservation.

JPA path: same SQL migration as A.4.

## Procedure

1. Locate target.
   - if Inputs.target set → use it
   - else → first match of the AF4 storage-engine greps below
2. Inspect AF4 wiring (greps above + dependency check). Classify into one of the rows of the decision tree (Path A / B / C / blocker).
3. Pick path (Preflight has already run [not-supported.md](not-supported.md); use the recorded decisions):
   - JPA observed (or blocker prompt picked JPA) → Path A
   - Axon Server observed (or blocker prompt picked Axon Server) → Path B
   - non-Spring (programmatic Configuration API) → Path C
   - any blocker resolved with `pause-migration` / `accept-stays-af4` / `defer-until-af5-jdbc` / `surface-and-defer` → Output with `needs-user-decision=true`, exit (no bean swap)
4. Run path Steps (see ### Path A / ### Path B / ### Path C below).
5. Surface custom-`Serializer` ports in Output `notes` (orchestrator records to `learnings.md`).
6. Verify against ## End condition.
7. Emit ## Output. Orchestrator commits — on Path A, the SQL script is included in the same commit (artifact is part of the migration even though applied separately).

## End condition

1. Configuration class compiles cleanly under the per-recipe `migration-event-store-<BeanSimpleName>` profile (`test-compile`).
2. Path A only: SQL migration script emitted under `<target>/.axon4-to-axon5-migration/sql/` (or project's existing migration dir).
3. Custom `Serializer` → `Converter` ports surfaced to the orchestrator in Output `notes` (orchestrator records to `learnings.md`).

> Runtime verification of the storage engine belongs to stabilization (after user has applied the SQL on the build's database).

## Output

- target: <FQ config class>
- decisions:
    - path: <A | B | C>
    - bean-replaced: <bean name>
    - sql-emitted: <path under sql/> (Path A only)
    - serializer-ports-flagged: <list | "none">      # B4 (soft blocker)
    - mongo-event-store: <none | move-to-axon-server | move-to-jpa | pause-migration | accept-stays-af4>   # B1
    - jdbc-event-store: <none | move-to-jpa | move-to-axon-server | defer-until-af5-jdbc>                  # B2
    - custom-storage-engine-subclass: <none | surface-and-defer | pause-migration>                          # B3
- needs-user-decision: false
- notes: optional free text (e.g. "JdbcEventStorageEngine present — surfaced to user, no AF5 path; user picked Path A (move to JPA)")

## Caveats

- **Don't migrate data and code in the same commit.** Partial deploy leaves the system unable to read its own events. Split: SQL on quiet window → verify table healthy → ship bean change.
- **Two `EventStorageEngine` beans = startup failure.** AF5 autoconfig backs off only when no other bean is present. Remove leftover AF4 `@Bean EventStore` / `@Bean EventStorageEngine`.
- **Payload/metadata length cap removal is vendor-specific.** Postgres `bytea` (no cap to drop); MySQL/Oracle have explicit column-type changes. No one-size-fits-all `ALTER COLUMN`.
- **JDBC has no AF5 drop-in yet.** If AF4 used `JdbcEventStorageEngine`, stop and ask user to pick JPA or Axon Server. Don't write a custom AF5 JDBC engine inside a migration run.
- **Mongo has no AF5 release at all.** `MongoEventStorageEngine` and `axon-mongo-spring-boot-autoconfigure` pull AF4 transitives. Run the Mongo blocker prompt before any bean swap; never silently swap to a JPA/Axon Server engine over Mongo data — the event log itself needs out-of-band migration.
- **Custom `Serializer` ≠ `Converter`.** Jackson/XStream defaults port automatically. Subclassed serializers / custom `RevisionResolver` / `ContentTypeConverter` are not one-line ports — surface to stabilization.
- **`AggregateEventEntry` table comes from the framework JAR.** Don't copy it. Custom `DomainEventEntry` subclasses = custom storage-engine subclass — out of scope.
- **The OpenRewrite recipe does NOT handle the schema migration.** Recipes rewrite Java code; database stays on AF4 schema until you run the SQL emitted here.

## Verify (against End condition)

```bash
./mvnw -f <target>/pom.xml -P migration-event-store-<BeanSimpleName> test-compile -DskipTests \
  -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
```

This recipe usually adds zero new tests. Runtime verification belongs to stabilization, after the SQL has been applied to the build's database.
