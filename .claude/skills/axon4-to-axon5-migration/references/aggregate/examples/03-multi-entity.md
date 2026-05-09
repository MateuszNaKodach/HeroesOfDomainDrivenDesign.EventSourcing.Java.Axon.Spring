# 03 — Multi-entity aggregate

**Why this case is interesting:** TBD — `@AggregateMember` → `@EntityMember`
with `routingKey`. If the AF4 source uses `Map<K, Child>`, document the
mandatory rewrite to `List<Child>` separately (e.g. `03b-multi-entity-map.md`).

**Variant:** multi-entity, Spring

## Before (AF4)

```java
// TODO — root aggregate + child entity (List form)
```

## After (AF5)

```java
// TODO — @EntityMember(routingKey = "...") version
```

## What changed

- TODO (cross-reference `multi-entity-migration.md`)

## Caveats

- TODO
