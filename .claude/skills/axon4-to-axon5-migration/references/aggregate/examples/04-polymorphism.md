# 04 — Polymorphic aggregate (AutoDetected)

**Why this case is interesting:** TBD — abstract base + two concrete subtypes
inheriting handlers. Migration target uses
`@EventSourcedEntity(concreteTypes = {...})` on the base only.

**Variant:** polymorphic

## Before (AF4)

```java
// TODO — abstract base @Aggregate + subclasses @Aggregate + manual
//        AggregateConfigurer.withSubtypes(...) call (or Spring auto-detection)
```

## After (AF5)

```java
// TODO — @EventSourcedEntity(concreteTypes = { Sub1.class, Sub2.class }) on
//        the base; subtypes carry no class-level annotation
```

## What changed

- TODO (cross-reference `polymorphism-migration.md`)

## Caveats

- TODO
