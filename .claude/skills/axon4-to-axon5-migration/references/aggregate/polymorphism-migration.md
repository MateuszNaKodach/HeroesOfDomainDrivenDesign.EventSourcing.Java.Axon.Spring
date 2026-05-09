# Polymorphic aggregate migration (addendum)

Apply this addendum **after** running the base aggregate-migration steps on
the abstract/base aggregate **and on each concrete subtype**, when the
aggregate hierarchy uses inheritance to share command and event-sourcing
handlers.

Authoritative reference: <https://docs.axoniq.io/axon-framework-reference/5.1/migration/paths/aggregates/polymorphism-migration.html>

## Detection rule

The aggregate is polymorphic if any of the following hold:

- A class annotated with `@Aggregate` (or `@AggregateRoot`) is **abstract** and
  has subclasses also annotated with `@Aggregate`.
- The AF4 configuration calls `AggregateConfigurer.defaultConfiguration(...).withSubtypes(...)`.
- Subtypes inherit `@CommandHandler` / `@EventSourcingHandler` methods from the
  base class.

Search:

```bash
grep -rln 'withSubtypes\|extends .*Aggregate\b' src/main/java
```

## Path choice: AutoDetected (recommended) vs Declarative

**AutoDetected** is the default migration target — the base type's annotation
declares its concrete subtypes and the framework wires up the hierarchy.
**Declarative** uses `EventSourcedEntityModule.declarative(...)` plus
`PolymorphicEntityMetamodel`; pick this only when you need to override the
metamodel (rare; out of scope here — link to the docs URL above and stop).

## Steps (AutoDetected path)

1. **Run base aggregate-migration steps on the abstract base type.** Replace `@Aggregate`/
   `@AggregateRoot` with `@EventSourcedEntity` (or `@EventSourced` in Spring),
   migrate `@CommandHandler` / `@EventSourcingHandler` imports, remove
   `@AggregateIdentifier`, etc. The base class keeps its `@EntityCreator`
   constructor (no-arg, by default). Annotate the field that used to be
   `@AggregateIdentifier` with the same `tagKey` semantics as a non-polymorphic
   aggregate.
2. **Declare the concrete subtypes on the base.**
   - Core (non-Spring):
     ```java
     @EventSourcedEntity(
         tagKey = "GiftCard",
         concreteTypes = { OpenLoopGiftCard.class, RechargeableGiftCard.class }
     )
     public abstract class GiftCard { /* ... */ }
     ```
   - Spring:
     ```java
     @EventSourced(
         tagKey = "GiftCard",
         concreteTypes = { OpenLoopGiftCard.class, RechargeableGiftCard.class }
     )
     public abstract class GiftCard { /* ... */ }
     ```
3. **Run base aggregate-migration steps on each concrete subtype.** Migrate annotations and
   handlers as if it were a standalone aggregate. **Do NOT** annotate the
   concrete subtypes with `@EventSourcedEntity` / `@EventSourced` — they are
   discovered through the base type's `concreteTypes`. Remove any `@Aggregate`
   annotations the AF4 subtypes carried.
4. **Update configuration.**
   - Spring (Path A): no extra registration needed. Auto-detection picks up the
     hierarchy through the base's `@EventSourced(concreteTypes = …)`.
   - Native (Path B, deferred): once supported, register the base type only:
     ```java
     configurer.registerEntity(
         EventSourcedEntityModule.autodetected(String.class, GiftCard.class)
     );
     ```
     Remove the AF4 `AggregateConfigurer.defaultConfiguration(…).withSubtypes(…)`
     call.
5. **Verify command routing.** Run the test classes for **all** concrete
   subtypes. Polymorphic dispatch is the most common failure mode here:
   - A command meant for `OpenLoopGiftCard` arriving at `GiftCard` indicates a
     missing entry in `concreteTypes`.
   - Inherited handlers that no longer fire usually mean a sub-type forgot to
     extend the migrated base or the package layout broke discovery.

## Imports

| Element | FQN |
|---|---|
| `@EventSourcedEntity(concreteTypes = ...)` | `org.axonframework.eventsourcing.annotation.EventSourcedEntity` (existing FQN; the `concreteTypes` attribute is the addition) |
| `@EventSourced(concreteTypes = ...)` (Spring) | `org.axonframework.extension.spring.stereotype.EventSourced` (existing FQN; same attribute) |

## Out of scope

- Declarative `PolymorphicEntityMetamodel` configuration — link out to the docs
  URL above. Pick AutoDetected for an architecture-neutral migration.
