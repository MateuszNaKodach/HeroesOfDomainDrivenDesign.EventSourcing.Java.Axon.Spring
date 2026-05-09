# Recipe: Aggregate → `@EventSourced` / `@EventSourcedEntity`

Atomic migration of ONE aggregate class and its surrounding commands, events, child entities, and primary test class.

## Goal

The aggregate (and its commands, events, and primary test class) compile and behave on AF5 APIs:
- `@Aggregate` (Spring) → `@EventSourced`; `@AggregateRoot` (core) → `@EventSourcedEntity`.
- Identity expressed via `@EventTag` on events + entity `tagKey` (no more `@AggregateIdentifier`).
- `AggregateLifecycle.apply(...)` → `EventAppender` parameter on `@CommandHandler`.
- `@CreationPolicy` → AF5 handler shape (static for ALWAYS, instance for CREATE_IF_MISSING / NEVER).
- `@AggregateMember` → `@EntityMember` (multi-entity addendum).
- `AggregateTestFixture` → `AxonTestFixture` with full fluent API mapping.

## Inputs

- target: FQ aggregate class name (required)
- target_test: FQ test class name (optional — auto-discovered as `<target>Test` if absent)

## End condition (verify BEFORE declaring done)

1. **Aggregate test class passes** if it exists in AF4 using `AggregateTestFixture` — now using `AxonTestFixture`. Scoped run (see [../verification.md](../verification.md)):
   ```bash
   ./mvnw -f <target>/pom.xml test -P migration-aggregate-<AggregateSimpleName> \
     -Dtest='<FQTestClass>' -DfailIfNoTests=false \
     -Dsurefire.failIfNoSpecifiedTests=false
   ```
2. **Zero compile errors** in: the aggregate class, all command classes handled here, all event classes handled here, all child entities reachable via `@EntityMember`, the primary test class.

If aggregate has NO test class, end condition is just (2). Skip T.1–T.5 below.

## Output

- target: <FQ aggregate class>
- decisions:
    - path: <A (Spring Boot) | B (non-Spring)>
    - variant: <simple | multi-entity | polymorphic>
    - creation-policy: <NEVER | ALWAYS-handled | ALWAYS-static-factory>
    - test-fixture: <migrated | none>
    - snapshotting: <none | accept-drop | pause-migration | remove-feature-first>             # B1
    - non-spring-configurer: <none | proceed-class-edits-only | pause-migration>               # B2
    - map-typed-aggregate-member: <none | surface-and-defer | pause-migration>                 # B3
    - saga-test-fixture-flagged: <none | surface-and-skip-test | pause-migration>              # B4
- needs-user-decision: <true | false>
- needs-user-decision-reason: <text> (only when true)
- notes: optional

## Preflight (ALWAYS run first)

Maybe the job is already done — don't waste context.

1. **Read [not-supported.md](not-supported.md) first** — run every Detection grep listed there against the candidate aggregate, its events, child entities, and the configuration referencing it. If any blocker fires, follow that file's `AskUserQuestion` flow and apply its "Effect on Procedure" before doing anything else. Recipe must NOT proceed past Preflight while a blocker is unresolved.
2. Check compilation problems on the aggregate file + its primary test file. Use `mcp__ide__getDiagnostics` if available, else scoped `./mvnw -P migration-aggregate-<AggregateSimpleName> test-compile`.
3. If zero compile problems AND test class exists, run scoped tests (see End condition).
4. If green AND no blocker fired → STOP. `AskUserQuestion`:
   - **Skip** *(Recommended)* — treat as already migrated.
   - **Deep verify** — diff current source against AF4 baseline (`git log` / `git show`) to confirm nothing was silently lost (dropped `snapshotTriggerDefinition`, missing `@EventTag`, lost `@CreationPolicy` semantics, missed multi-entity field).
5. Only proceed to procedure if user picks **Deep verify** OR step 2/3 reported failures.

## In scope

- ONE aggregate class annotated `@Aggregate` (Spring) or `@AggregateRoot` / `@Aggregate` (core).
- ALL command classes whose `@CommandHandler` lives on this aggregate.
- ALL event classes whose `@EventSourcingHandler` lives on this aggregate.
- Child entities reachable via `@AggregateMember` on this aggregate.
- The aggregate's primary test class (typically `<Aggregate>Test`) plus its direct subclasses.

## Out of scope

- DCB-style decomposition or any architectural restructuring.
- Snapshotting configuration — see [not-supported.md](not-supported.md) B1.
- Native (non-Spring) `EventSourcingConfigurer` wiring — see [not-supported.md](not-supported.md) B2.
- Map-typed `@AggregateMember` — see [not-supported.md](not-supported.md) B3.
- `SagaTestFixture` migration — see [not-supported.md](not-supported.md) B4.

## Slim FQN cheatsheet

Full table in [annotation-cheatsheet.md](annotation-cheatsheet.md).

| Removed (AF4) | Replaced by (AF5) |
|---|---|
| `org.axonframework.spring.stereotype.Aggregate` | `org.axonframework.extension.spring.stereotype.EventSourced` |
| `org.axonframework.modelling.command.AggregateIdentifier` | *(removed — use `tagKey` + `@EventTag`)* |
| `org.axonframework.modelling.command.TargetAggregateIdentifier` | `org.axonframework.modelling.annotation.TargetEntityId` |
| `org.axonframework.commandhandling.CommandHandler` | `org.axonframework.messaging.commandhandling.annotation.CommandHandler` |
| `org.axonframework.eventsourcing.EventSourcingHandler` | `org.axonframework.eventsourcing.annotation.EventSourcingHandler` |
| `org.axonframework.modelling.command.AggregateLifecycle.apply(...)` | `org.axonframework.messaging.eventhandling.gateway.EventAppender#append(...)` |
| `org.axonframework.modelling.command.CreationPolicy` | *(removed — encoded by handler shape)* |
| `org.axonframework.modelling.command.AggregateMember` | `org.axonframework.modelling.entity.annotation.EntityMember` |
| `org.axonframework.test.aggregate.AggregateTestFixture` | `org.axonframework.test.fixture.AxonTestFixture` |
| `org.axonframework.serialization.Revision` | `org.axonframework.messaging.eventhandling.annotation.Event#version` |

> ⚠️ `@InjectState` does NOT exist — always use `@InjectEntity`.

## Procedure

### Step 1 — Identify

1.1. Work on class given as target. If none, find first `@Aggregate`/`@AggregateRoot` class with `@EventSourcingHandler` methods.
1.2. Identify all **command classes** — first param of every `@CommandHandler` on aggregate (and constructor).
1.3. Identify all **event classes** — first param of every `@EventSourcingHandler`.
1.4. Identify primary **test class** (`<Aggregate>Test`) and direct subclasses (`grep -rln "extends <Aggregate>Test"`). Migrate base first.

### Step 2 — Detect variant

- **Simple** — no `@AggregateMember`, no concrete `@Aggregate`-annotated subclasses. Continue with steps 3–14, then Path A/B, then test fixture.
- **Multi-entity** — has any `@AggregateMember` field. Run steps 3–14 on root, then apply [multi-entity-migration.md](multi-entity-migration.md). **Map-typed members are a breaking change** — surface via `AskUserQuestion` before rewriting.
- **Polymorphic** — abstract/concrete superclass with subclasses also annotated `@Aggregate` and inheriting handlers. Run steps 3–14 on base + each subtype, then apply [polymorphism-migration.md](polymorphism-migration.md). Concrete subtypes do NOT carry `@EventSourcedEntity` themselves — discovered through base.

Variants are not mutually exclusive; apply both addenda when both detection rules fire.

### Steps 3–14 — Class-level transformation

3. In each command class:
   - Remove `import org.axonframework.modelling.command.TargetAggregateIdentifier`.
   - Add `import org.axonframework.modelling.annotation.TargetEntityId`.
   - Replace `@TargetAggregateIdentifier` → `@TargetEntityId`.
4. Annotate each command class with `@Command` (`org.axonframework.messaging.commandhandling.annotation.Command`). If AF4 had `@RoutingKey` on a property → `@Command(routingKey = "<propertyName>")` and remove `@RoutingKey` annotation + import.
5. In aggregate, identify the `@AggregateIdentifier`-annotated property and the `@EventSourcingHandler` that sets it from an event property. That event property is the one to annotate with `@EventTag`.
6. Annotate aggregate-id field in **every** event with `@EventTag(key = "<EntityName>")`. Use entity's simple class name (e.g. `"GiftCard"`) so it matches entity's `tagKey`. Without DCB, exactly ONE `@EventTag` per event.
    - When `@EventTag` is used WITHOUT `key`, framework derives it from the field name. Recommended: be explicit (`@EventTag(key = "Bike")`).
    - Pick a `tagKey` that conveys the **entity type** (`"Bike"`) rather than the field name (`"bikeId"`) — stays stable across renames.
7. Annotate each event class with `@Event` (`org.axonframework.messaging.eventhandling.annotation.Event`). If event had `@Revision("x")` → `@Event(version = "x")` and remove `@Revision` annotation + import. Otherwise add bare `@Event` (default name = simple class name, default version = `0.0.1`).
8. Remove `@AggregateIdentifier` annotation (and import) from aggregate. Id field stays as regular field.
9. Replace import `org.axonframework.eventsourcing.EventSourcingHandler` → `org.axonframework.eventsourcing.annotation.EventSourcingHandler`.
10. Replace import `org.axonframework.commandhandling.CommandHandler` → `org.axonframework.messaging.commandhandling.annotation.CommandHandler`.
11. Annotate aggregate's no-arg constructor with `@EntityCreator` (mandatory in AF5). If no no-arg ctor exists, add one. Framework instantiates entity via this ctor before applying events.

    `@EntityCreator` patterns (pick one — for architecture-neutral migration prefer #1):
    1. **No-arg constructor** (recommended) — framework creates empty instance; identifier and state set by `@EventSourcingHandler` of first event.
    2. **Identifier-only constructor** — `public Entity(@InjectEntityId String id) { this.id = id; }`.
    3. **Creation from origin event** — `public Entity(CreationEvent event) { ... }`. Pick this only when AF4 used a constructor command-handler with an event payload of the same shape.
12. Replace `AggregateLifecycle.apply(event)` → `eventAppender.append(event)`. Add `EventAppender eventAppender` as method parameter to every `@CommandHandler`. Remove static import of `AggregateLifecycle.apply`.
13. Migrate `@CreationPolicy` / `AggregateCreationPolicy` (remove imports + annotation) by reshaping the command handler. **The wrong shape compiles cleanly and only fails at test time.** Full matrix in [creation-policy-decision.md](creation-policy-decision.md). Short version:
    - `ALWAYS` → **`static`** `@CommandHandler`.
    - `CREATE_IF_MISSING` → **instance** `@CommandHandler` + no-arg `@EntityCreator`. Do NOT use `static + @InjectEntity` here unless AF4 already threw on existing entities — that flips semantics.
    - `NEVER` (or absent) → instance `@CommandHandler` (default).
14. Apply variant addenda from Step 2 if they fired.

### Path A — Spring Boot

Use when project depends on `axoniq-spring-boot-starter` (or AF4 `axon-spring-boot-starter`).

A.1. Replace `@Aggregate` (`org.axonframework.spring.stereotype.Aggregate`) → `@EventSourced` (`org.axonframework.extension.spring.stereotype.EventSourced`).

A.2. Configure `@EventSourced`:
- **`tagKey`** — same value as `@EventTag(key = ...)` on events. Default = entity's simple class name; if you use that as the tag key, you can omit `tagKey`. Recommended: be explicit (`@EventSourced(tagKey = "GiftCard")`).
- **`idType`** — set when AF4 `@AggregateIdentifier` field is **NOT** `String`. Default is `String.class`; mismatched types cause silent identifier-resolution failures. Example: `@EventSourced(tagKey = "Army", idType = ArmyId.class)`.
- **`concreteTypes`** — only when polymorphism addendum applies.

> ⚠️ **`snapshotTriggerDefinition` / caching attributes are NOT portable.** Preflight already ran [not-supported.md](not-supported.md) B1 — apply the recorded decision (`snapshotting`) here: if `accept-drop`, omit the attribute from `@EventSourced`; otherwise the recipe already exited.

### Path B — Native (non-Spring)

Not supported by this recipe yet — see [not-supported.md](not-supported.md) B2. Preflight already routed: if user picked `proceed-class-edits-only`, Steps 3–14 still run, but Path A and Path B steps are skipped.

## Test fixture migration

Migrates AF4's `AggregateTestFixture` → AF5's `AxonTestFixture` built from `ApplicationConfigurer`. Full mapping table + gotchas in [test-fixture-mapping.md](test-fixture-mapping.md).

> If project has no test class for this aggregate, **skip T.1–T.5** and proceed to Verify.

T.1. Find test class (`<Aggregate>Test`) and subclasses (`grep -rln "extends <Aggregate>Test"`). Migrate base first.

T.2. Replace `AggregateTestFixture` with `AxonTestFixture`:
- Import: `org.axonframework.test.aggregate.AggregateTestFixture` → `org.axonframework.test.fixture.AxonTestFixture`.
- Field type: `AggregateTestFixture<?>` → `AxonTestFixture`.
- `@BeforeEach`: `new AggregateTestFixture<>(<Aggregate>.class)` → `AxonTestFixture.with(<configurer>)`.
- Minimal configurer (replace `IdType` / `Aggregate` with concrete types):
  ```java
  EventSourcingConfigurer.create()
                         .registerEntity(EventSourcedEntityModule.autodetected(IdType.class, Aggregate.class))
  ```
  Default `Customization(integrationEnabled=false)` already disables Axon Server / Postgres enhancers.
- Add `@AfterEach tearDown() { fixture.stop(); }`.

T.3. Convert each test method to fluent given/when/then. Mapping in [test-fixture-mapping.md](test-fixture-mapping.md). Common edits:
- `fixture.given(events…)` → `fixture.given().events(events…)`.
- `fixture.givenNoPriorActivity()` → `fixture.given().noPriorActivity()`.
- `.when(cmd)` → `.when().command(cmd)`.
- `.expectEvents(events…)` → `.then().events(events…)`.
- `.expectException(Cls.class)` → `.then().exception(Cls.class)`.

> **`EventMessage` accessors are record-style in AF5.** Inside `eventsSatisfy(events -> { ... })` lambdas (or any other place handling raw `EventMessage`), use `events.get(0).payload()` and `events.get(0).metaData()` — **NOT** AF4's `getPayload()` / `getMetaData()`.

T.4. Adjust behavioral assertions for AF5 semantics. Full list in [test-fixture-mapping.md](test-fixture-mapping.md). Most common:
- `AggregateNotFoundException` is **NOT** thrown for instance handlers in AF5. With no-arg `@EntityCreator`, framework always materializes empty entity, so handler runs and any domain rule against empty state surfaces instead.
- Static (creational) handlers throw `EntityAlreadyExistsForCreationalCommandHandlerException` when entity already exists. If you see this in a test that should succeed on existing entities, the handler shouldn't be `static` — re-check Step 13 against [creation-policy-decision.md](creation-policy-decision.md).

T.5. Run just the migrated tests. Confirm they pass before moving on. If they fail, do NOT declare success — re-check Step 13 (handler shape) and T.4 (exception expectations). Test run is also the smoke test for Step 13: a wrong static-vs-instance `CreationPolicy` choice has no compile-time signal.

## Verify (against End condition)

If surrounding code still uses AF4 APIs, set up a `migration-aggregate-<AggregateSimpleName>` profile via [../maven-profile/maven-profile.md](../maven-profile/maven-profile.md). Then (e.g. `migration-aggregate-Faculty`):

```bash
./mvnw -f <target>/pom.xml test -P migration-aggregate-<AggregateSimpleName> \
  -Dtest='<FQTestClass1>,<FQTestClass2>' \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Drop the `-P migration-aggregate-*` flag only if surrounding code already compiles cleanly.

> ⚠️ **Prefer per-file `<include>` over package wildcards.** A glob like `com/example/write/**/*.java` pulls in every file in that package — including `*Mcp.java`, `*RestApi.java`, and other non-migration files that may still use AF4 APIs or Java preview features. If those files fail compilation, switch from wildcard to explicit per-file `<include>` list.

> ⚠️ **Multi-module reactor (`-pl <a>,<b>`) needs both surefire flags.** When `-pl` includes modules that don't contain a class matching the `-Dtest=…` pattern, surefire fails with `No tests matching pattern "…" were executed!` for empty modules. Always pass **both** `-DfailIfNoTests=false` (for plain `surefire:test`) **and** `-Dsurefire.failIfNoSpecifiedTests=false` (for the explicit `-Dtest=…` filter).

## Reference index (this recipe's local references)

- [annotation-cheatsheet.md](annotation-cheatsheet.md) — full FQN tables.
- [creation-policy-decision.md](creation-policy-decision.md) — `ALWAYS`/`CREATE_IF_MISSING`/`NEVER` → AF5 handler shape.
- [multi-entity-migration.md](multi-entity-migration.md) — `@AggregateMember` hierarchies addendum. Map-not-supported breaking change here.
- [polymorphism-migration.md](polymorphism-migration.md) — inherited handlers, `concreteTypes` registration.
- [test-fixture-mapping.md](test-fixture-mapping.md) — `AggregateTestFixture` → `AxonTestFixture` full mapping + gotchas.
- [examples/](examples/) — curated before/after migrations.
