# Multi-entity migration (addendum)

Apply this addendum **after** running the base aggregate-migration steps,
when the aggregate has child entities.

Authoritative reference: <https://docs.axoniq.io/axon-framework-reference/5.1/migration/paths/aggregates/multi-entity-migration.html>

## Detection rule

The aggregate is multi-entity if **any** field on the aggregate class is annotated
with `@AggregateMember` (FQN: `org.axonframework.modelling.command.AggregateMember`).
Typical shapes:

- `@AggregateMember List<Child> children`
- `@AggregateMember Map<Key, Child> childrenByKey` ← **breaking change in AF5**, see below

Search:

```bash
grep -rln '@AggregateMember' src/main/java
```

## What changes

| AF4 | AF5 |
|---|---|
| `@AggregateMember` on field | `@EntityMember(routingKey = "<childIdProperty>")` on field |
| `@EntityId` on the child entity's id field | not required; child id is typically captured via the child constructor or the parent's `@EventSourcingHandler` |
| `Map<K, Child>` | **not supported** — must be modelled as `List<Child>` and key resolution moved into the entity |
| Child class is a plain POJO (no `@AggregateRoot`/`@EventSourcedEntity`) | unchanged — still a plain class |

## Steps

1. **Search for `@AggregateMember`** and confirm scope. If multiple aggregate
   classes have child entities, run this addendum once per aggregate.
2. **For each `@AggregateMember` field on the aggregate:**
   1. Replace the annotation `@AggregateMember` with
      `@EntityMember(routingKey = "<childIdProperty>")`.
   2. Replace the import
      `org.axonframework.modelling.command.AggregateMember` with
      `org.axonframework.modelling.entity.annotation.EntityMember`.
   3. Determine `routingKey`: the child-entity field whose value matches the
      command's target-id field. Example: a `Transaction` entity with field
      `transactionId` and a `RedeemCardCommand` carrying a `transactionId`
      property → `routingKey = "transactionId"`.
3. **For each child entity class:**
   1. Remove the `@EntityId` annotation and its import
      (`org.axonframework.modelling.command.EntityId`). The id field stays as a
      plain field; AF5 does not require an annotation here when the child is
      constructed via constructor or set in the parent's `@EventSourcingHandler`.
   2. Leave any `@CommandHandler` / `@EventSourcingHandler` methods on the child
      in place. Update their imports to the AF5 packages.
4. **Maps must be migrated to `List`.** If a field is `Map<K, Child>`:
   1. **Stop and surface this to the user via `AskUserQuestion`** — this is a
      breaking change in shape; the framework no longer routes via map keys.
   2. Recommended replacement: `List<Child>` plus a child-side `key` field (or
      `equals`/`hashCode` on the existing id) to preserve uniqueness; the parent's
      sourcing handlers add/remove from the list.
   3. Update every reader of the map (other handlers in the parent, projections,
      tests). This is **not** a mechanical migration.
5. **Routing-key sanity check.** Ensure the command class field used for routing
   to a specific child has an `@EntityMember`-routable name. The `routingKey`
   value on `@EntityMember` must match the child's id-field name (or the field
   is exposed by a getter of the same base name).

## Tagging implications

The `@EventTag` on events still tags the **aggregate-root identifier**, not the
child entity id. Without DCB, exactly one `@EventTag` per event, matching the
aggregate's `tagKey`.

## Verify

After applying this addendum:

1. Compile the aggregate, the child entity class, and the test class.
2. Run the test class — it surfaces routing-key mismatches that the compiler
   misses.

## Out of scope

- DCB-style multi-entity decomposition. That requires re-modelling the
  domain and is intentionally not part of an architecture-neutral migration.
