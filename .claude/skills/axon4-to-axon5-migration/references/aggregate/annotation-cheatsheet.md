# Annotation / class FQN cheatsheet

Use this as the source of truth for imports while migrating an aggregate.
Never guess imports — copy from here.

## AF4 — remove these

| Element | FQN |
|---|---|
| `@TargetAggregateIdentifier` | `org.axonframework.modelling.command.TargetAggregateIdentifier` |
| `@AggregateIdentifier` | `org.axonframework.modelling.command.AggregateIdentifier` |
| `@CreationPolicy` | `org.axonframework.modelling.command.CreationPolicy` |
| `AggregateCreationPolicy` (enum) | `org.axonframework.modelling.command.AggregateCreationPolicy` |
| `@CommandHandler` | `org.axonframework.commandhandling.CommandHandler` |
| `@EventSourcingHandler` | `org.axonframework.eventsourcing.EventSourcingHandler` |
| `AggregateLifecycle.apply(...)` | `org.axonframework.modelling.command.AggregateLifecycle` |
| `@Aggregate` (Spring) | `org.axonframework.spring.stereotype.Aggregate` |
| `@AggregateRoot` | `org.axonframework.modelling.command.AggregateRoot` |
| `@AggregateMember` | `org.axonframework.modelling.command.AggregateMember` |
| `@EntityId` (child entity id) | `org.axonframework.modelling.command.EntityId` |
| `@Revision` | `org.axonframework.serialization.Revision` |
| `@RoutingKey` | `org.axonframework.commandhandling.RoutingKey` |
| `AggregateTestFixture` | `org.axonframework.test.aggregate.AggregateTestFixture` |
| `FixtureConfiguration` | `org.axonframework.test.aggregate.FixtureConfiguration` |
| `AggregateNotFoundException` | `org.axonframework.modelling.command.AggregateNotFoundException` |

## AF5 — add these if needed

| Element | FQN |
|---|---|
| `@TargetEntityId` | `org.axonframework.modelling.annotation.TargetEntityId` |
| `@EventTag` | `org.axonframework.eventsourcing.annotation.EventTag` |
| `@CommandHandler` | `org.axonframework.messaging.commandhandling.annotation.CommandHandler` |
| `@Command` | `org.axonframework.messaging.commandhandling.annotation.Command` |
| `@EventSourcingHandler` | `org.axonframework.eventsourcing.annotation.EventSourcingHandler` |
| `@Event` | `org.axonframework.messaging.eventhandling.annotation.Event` |
| `EventAppender` | `org.axonframework.messaging.eventhandling.gateway.EventAppender` |
| `@EntityCreator` | `org.axonframework.eventsourcing.annotation.reflection.EntityCreator` |
| `@InjectEntity` (NOT `@InjectState`) | `org.axonframework.modelling.annotation.InjectEntity` |
| `@InjectEntityId` | `org.axonframework.modelling.annotation.InjectEntityId` |
| `@EventSourced` (Spring) | `org.axonframework.extension.spring.stereotype.EventSourced` |
| `@EventSourcedEntity` (core) | `org.axonframework.eventsourcing.annotation.EventSourcedEntity` |
| `@EntityMember` (child entities) | `org.axonframework.modelling.entity.annotation.EntityMember` |
| `AxonTestFixture` | `org.axonframework.test.fixture.AxonTestFixture` |
| `EventSourcingConfigurer` | `org.axonframework.eventsourcing.configuration.EventSourcingConfigurer` |
| `EventSourcedEntityModule` | `org.axonframework.eventsourcing.configuration.EventSourcedEntityModule` |

## Direct AF4 → AF5 replacement table

| AF4 | AF5 |
|---|---|
| `@AggregateRoot` / `@Aggregate` (core) | `@EventSourcedEntity` |
| `@Aggregate` (Spring) | `@EventSourced` |
| `@AggregateIdentifier` (field) | *removed* — id field stays plain; identity expressed via `@EventTag` on events + entity `tagKey` |
| `@TargetAggregateIdentifier` (command field) | `@TargetEntityId` |
| `@CommandHandler` (constructor for creation) | `static @CommandHandler` method + `@EntityCreator` on a separate constructor |
| `@CommandHandler` (instance, update) | `@CommandHandler` instance method, with `EventAppender` parameter |
| `@EventSourcingHandler` | `@EventSourcingHandler` (annotation kept; package moved) |
| `AggregateLifecycle.apply(event)` | `eventAppender.append(event)` (parameter-injected `EventAppender`) |
| `@CreationPolicy(ALWAYS)` | `static` `@CommandHandler` |
| `@CreationPolicy(CREATE_IF_MISSING)` | instance `@CommandHandler` + no-arg `@EntityCreator` |
| `@CreationPolicy(NEVER)` (default) | instance `@CommandHandler` (default) |
| `@AggregateMember` (child entities) | `@EntityMember(routingKey = "<childIdProperty>")` |
| `@EntityId` (on child) | not required; child id usually managed via constructor/sourcing handler |
| `AggregateTestFixture<T>` | `AxonTestFixture` (built from an `ApplicationConfigurer`) |
| `FixtureConfiguration<T>` | `AxonTestFixture` |
| `@Revision("x")` (event) | `@Event(version = "x")` |
| `@RoutingKey` (on command field) | `@Command(routingKey = "<propertyName>")` on the command class |

## Pitfalls

- **`@InjectState` does not exist.** The reference docs sometimes show this name. Always use `@InjectEntity` (`org.axonframework.modelling.annotation.InjectEntity`).
- **`AggregateNotFoundException` is not the AF5 equivalent of "no entity yet"** — see `test-fixture-mapping.md` for the AF5 semantic shift.
- **`@EntityMember` does not support `Map<K, V>`** — only `List<V>`. See `multi-entity-migration.md`.
