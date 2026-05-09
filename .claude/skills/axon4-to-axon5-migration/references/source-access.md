# Source access — where AF4 / AF5 sources resolve

Recipes consult framework source as a **fallback** when migration docs and the recipe's transformation tables leave a real gap.

## Local clone (preferred)

If a local AxonFramework5 clone exists, point recipes at:

```
<af5-clone>/
├── messaging/src/main/java/org/axonframework/messaging/        # core messaging
├── eventsourcing/src/main/java/org/axonframework/eventsourcing/ # event sourcing
├── modelling/src/main/java/org/axonframework/modelling/         # entities
├── test/src/main/java/org/axonframework/test/                   # AxonTestFixture
├── integration-tests/                                           # working examples
├── examples/                                                    # standalone example projects
│   └── university-java-springboot-4/                            # Spring-on-AF5 reference
└── docs/reference-guide/modules/migration/pages/                # migration docs
    ├── paths/index.adoc                                         # import & package changes table
    ├── paths/aggregates/                                        # aggregate migration
    ├── paths/projectors-event-processors.adoc                   # event processors + gateway/dispatcher rule
    └── paths/test-fixtures.html                                 # AxonTestFixture mapping
```

## Sources jar (fallback)

When no local clone, use Maven dependency sources:

```bash
./mvnw dependency:sources -DincludeArtifactIds=axon-messaging,axon-eventsourcing,axon-modelling,axon-test
```

## Public docs (read-only — recipes don't fetch at runtime)

- AF5 reference: <https://docs.axoniq.io/axon-framework-reference/5.1/>
- Migration paths: <https://docs.axoniq.io/axon-framework-reference/5.1/migration/>
- Test fixtures: <https://docs.axoniq.io/axon-framework-reference/5.1/migration/paths/test-fixtures.html>

## Key entry points by recipe

### Aggregate
- `eventsourcing/src/main/java/org/axonframework/eventsourcing/annotation/EventSourcedEntity.java`
- `eventsourcing/src/main/java/org/axonframework/eventsourcing/annotation/EventTag.java`
- `eventsourcing/src/main/java/org/axonframework/eventsourcing/annotation/EventSourcingHandler.java`
- `eventsourcing/src/main/java/org/axonframework/eventsourcing/annotation/reflection/EntityCreator.java`
- `messaging/src/main/java/org/axonframework/messaging/eventhandling/gateway/EventAppender.java`
- `modelling/src/main/java/org/axonframework/modelling/annotation/TargetEntityId.java`
- `modelling/src/main/java/org/axonframework/modelling/annotation/InjectEntity.java`
- `modelling/src/main/java/org/axonframework/modelling/entity/annotation/EntityMember.java`
- `test/src/main/java/org/axonframework/test/fixture/AxonTestFixture.java`

### CommandGateway
- `messaging/src/main/java/org/axonframework/messaging/commandhandling/gateway/CommandGateway.java`
- `messaging/src/main/java/org/axonframework/messaging/commandhandling/gateway/CommandResult.java`
- `messaging/src/main/java/org/axonframework/messaging/commandhandling/gateway/CommandDispatcher.java`
- `messaging/src/main/java/org/axonframework/messaging/core/Metadata.java`

### QueryGateway
- `messaging/src/main/java/org/axonframework/messaging/queryhandling/gateway/QueryGateway.java`
- `messaging/src/main/java/org/axonframework/messaging/queryhandling/SubscriptionQueryResponse.java`
- `messaging/src/main/java/org/axonframework/messaging/queryhandling/annotation/Query.java`

### EventProcessor
- `messaging/src/main/java/org/axonframework/messaging/eventhandling/annotation/EventHandler.java`
- `messaging/src/main/java/org/axonframework/messaging/core/annotation/Namespace.java`
- `messaging/src/main/java/org/axonframework/messaging/core/annotation/MetadataValue.java`
- `messaging/src/main/java/org/axonframework/messaging/eventhandling/replay/annotation/DisallowReplay.java`

### Configuration (read/write)
- `messaging/src/main/java/org/axonframework/common/configuration/AxonConfiguration.java`
- `messaging/src/main/java/org/axonframework/common/configuration/Configuration.java`
- `messaging/src/main/java/org/axonframework/configuration/ApplicationConfigurer.java`
- `messaging/src/main/java/org/axonframework/configuration/MessagingConfigurer.java`
- `modelling/src/main/java/org/axonframework/modelling/configuration/ModellingConfigurer.java`
- `eventsourcing/src/main/java/org/axonframework/eventsourcing/configuration/EventSourcingConfigurer.java`
- `messaging/src/main/java/org/axonframework/configuration/ComponentRegistry.java`
- `messaging/src/main/java/org/axonframework/configuration/ConfigurationEnhancer.java`

### EventStorageEngine
- `eventsourcing/src/main/java/org/axonframework/eventsourcing/eventstore/EventStorageEngine.java`
- `eventsourcing-jpa/src/main/java/org/axonframework/eventsourcing/eventstore/jpa/AggregateBasedJpaEventStorageEngine.java`
- (Axoniq commercial) `axonserver-connector/.../AggregateBasedAxonServerEventStorageEngine.java`

## Rule

Treat source-fallback as a signal that the migration docs are incomplete for this case. After consulting source, append a learnings entry so the next migration of the same shape doesn't need the source dive.
