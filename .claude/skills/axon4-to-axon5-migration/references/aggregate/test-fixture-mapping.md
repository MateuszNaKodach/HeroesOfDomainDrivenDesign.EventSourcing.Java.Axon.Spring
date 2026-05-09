# Test fixture mapping (AF4 → AF5)

Applied after the production-code migration. Authoritative reference:
<https://docs.axoniq.io/axon-framework-reference/5.1/migration/paths/test-fixtures.html>

## Fixture replacement

| AF4 | AF5 |
|---|---|
| `org.axonframework.test.aggregate.AggregateTestFixture` | `org.axonframework.test.fixture.AxonTestFixture` |
| `org.axonframework.test.aggregate.FixtureConfiguration` | (removed — `AxonTestFixture` has no plain registration methods; build it from an `ApplicationConfigurer`) |
| `new AggregateTestFixture<>(GiftCard.class)` | `AxonTestFixture.with(EventSourcingConfigurer.create().registerEntity(EventSourcedEntityModule.autodetected(IdType.class, GiftCard.class)))` |
| Fixture field type `AggregateTestFixture<?>` | `AxonTestFixture` |
| (no explicit teardown) | `@AfterEach fixture.stop();` |

The minimal first-step configurer for a single entity is:

```java
EventSourcingConfigurer.create()
                       .registerEntity(EventSourcedEntityModule.autodetected(<IdType>.class, <Aggregate>.class))
```

By default, the fixture's `Customization(integrationEnabled=false)` already
disables Axon Server / Postgres enhancers, so a plain
`AxonTestFixture.with(configurer)` is enough for unit tests.

## Fluent API mapping

| AF4 | AF5 |
|---|---|
| `fixture.given(events…)` | `fixture.given().events(events…)` (or `.event(e)` for single) |
| `fixture.given(List<?> events)` | `fixture.given().events(list)` — but prefer `.given().noPriorActivity()` when the list is empty |
| `fixture.givenCommands(c…)` | `fixture.given().command(c)` |
| `fixture.givenNoPriorActivity()` | `fixture.given().noPriorActivity()` |
| `.when(cmd)` | `.when().command(cmd)` |
| `.when(cmd, metadata)` | `.when().command(cmd, metadata)` |
| `.expectEvents(events…)` | `.then().events(events…)` |
| `.expectNoEvents()` | `.then().noEvents()` |
| `.expectException(Cls.class)` | `.then().exception(Cls.class)` |
| `.expectException(Cls.class).expectExceptionMessage(msg)` | `.then().exception(Cls.class, msg)` |
| `.expectSuccessfulHandlerExecution()` | `.then().success()` |
| `.expectResultMessagePayload(p)` | `.then().resultMessagePayload(p)` |
| `.expectEventsMatching(matcher)` | `.then().eventsSatisfy(consumer)` or `.eventsMatch(predicate)` |

## Gotchas

### `EventMessage` accessors are record-style

Inside `eventsSatisfy(events -> { ... })` lambdas (or any other place that
handles a raw `EventMessage`), use `events.get(0).payload()` and
`events.get(0).metaData()` — **NOT** AF4's JavaBean-style `getPayload()` /
`getMetaData()`. The AF4 names do not exist on the AF5 `EventMessage` interface
and produce `cannot find symbol: method getPayload()` compile errors.

### `AggregateNotFoundException` is no longer thrown for instance handlers

With a no-arg `@EntityCreator`, the framework always materializes an empty
entity, so the handler runs and any domain rule against empty state surfaces
instead. Tests that asserted `AggregateNotFoundException` in AF4 must be updated
to expect the actual domain exception (e.g. a domain-rule violation message).
Add a comment noting the semantic shift.

### NPE as the "actual exception"

If the `@CommandHandler` body calls a method on a null entity-state field (e.g.
`null.equals(...)` when a field that is only set by an `@EventSourcingHandler`
was never initialised), it throws `NullPointerException` rather than a
meaningful domain exception. Two options:

1. Use `Exception.class` as the expected type and add a `// TODO` comment that
   the domain model should add an explicit "entity not yet initialised" guard.
2. Add the guard in the production code (preferred) and assert the new domain
   exception.

### `EntityAlreadyExistsForCreationalCommandHandlerException`

Thrown by static (creational) handlers (`org.axonframework.modelling.entity`)
when the entity already exists. If you see this in a test that should succeed
on existing entities, the handler shouldn't be `static` — re-check the
`CreationPolicy` migration in `creation-policy-decision.md`.

### `NoClassDefFoundError` from Jackson when `AxonTestFixture.with(...)` runs

Symptom: fixture initialization throws something like:

```
NoClassDefFoundError: com/fasterxml/jackson/annotation/JsonSerializeAs
NoClassDefFoundError: tools/jackson/databind/json/JsonMapper$Builder
```

Compile is clean; failure surfaces only at fixture build time.

Cause: AF5's `axon-test` (transitively `axon-conversion`) uses **Jackson 3** (`tools.jackson:*`). Jackson 3 still resolves `com.fasterxml.jackson.annotation.*` from the legacy `jackson-annotations` artifact and demands version **2.21+**. Spring Boot 3.5.x pins `jackson-annotations:2.19.x` via its BOM — which Jackson 3 can't load.

Fix: pin `jackson-annotations` 2.21 (or import the Jackson 3 BOM) in the per-recipe `migration-aggregate-<Target>` profile's `<dependencyManagement>`. Full options are in [../maven-profile/maven-profile.md](../maven-profile/maven-profile.md) under "Known issue: Jackson 3 ↔ Spring Boot 3.5.x" — apply that whenever the profile pulls `axon-test` (so any aggregate test fixture). Ignore on Spring Boot < 3.5.x — bumping to 2.21 is still safe.
