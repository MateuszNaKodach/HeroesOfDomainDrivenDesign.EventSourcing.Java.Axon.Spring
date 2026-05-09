# `@CreationPolicy` decision matrix

AF5 removes `@CreationPolicy` / `AggregateCreationPolicy`. The AF4 enum value is
expressed in AF5 by the **shape of the command handler** (static vs instance) and
the optional `@InjectEntity` parameter.

> **No compile-time signal.** Picking the wrong shape compiles cleanly and only
> surfaces at test time, typically as
> `EntityAlreadyExistsForCreationalCommandHandlerException` or as a domain rule
> being applied to an empty entity. **Always run the corresponding tests after
> migrating a handler that had `@CreationPolicy`.**

## Decision table

| AF4 | AF5 handler shape | `@EntityCreator` needed? | What the framework does | Failure when wrong |
|---|---|---|---|---|
| `@CreationPolicy(ALWAYS)` (creation-only command) | **`static`** `@CommandHandler` method | No (creational handler does not need the entity) | Calls handler only when no entity exists; throws `EntityAlreadyExistsForCreationalCommandHandlerException` if it does | If you used instance + no-arg `@EntityCreator`, an existing entity is overwritten silently |
| `@CreationPolicy(CREATE_IF_MISSING)` | **instance** `@CommandHandler` (NOT static) | Yes, no-arg `@EntityCreator` | Materializes an empty entity on first invocation; same handler runs whether the entity is new or pre-existing — matches AF4's create-or-update | If you make it `static + @InjectEntity`, framework treats it as creational-only and throws on existing entities |
| `@CreationPolicy(NEVER)` (or no `@CreationPolicy`) | **instance** `@CommandHandler` | No (default) | Default behavior — handler runs against existing entity | None (this is the default) |

## Recommended migration steps

1. Identify the AF4 `@CreationPolicy` annotation on each `@CommandHandler` and remove it (along with the `@CreationPolicy` and `AggregateCreationPolicy` imports).
2. Apply the row from the table above:
   - `ALWAYS` → make the method `static`. Add `EventAppender eventAppender` parameter. The first event the handler appends must carry an `@EventTag` matching the entity's `tagKey`.
   - `CREATE_IF_MISSING` → keep instance, but ensure the entity has a no-arg `@EntityCreator`. The same handler covers both the creation and update flows.
   - `NEVER` → leave as instance method (just remove the annotation).
3. Re-run the test class. Watch specifically for:
   - `EntityAlreadyExistsForCreationalCommandHandlerException` (`org.axonframework.modelling.entity`) — wrong static-vs-instance choice.
   - `NullPointerException` from a `@CommandHandler` that touches a field set only by an `@EventSourcingHandler` — see "NPE on null state" below.

## Doc-aligned alternative for `CREATE_IF_MISSING`

The official docs describe an alternative implementation using a static handler with
`@InjectEntity @Nullable`:

```java
@CommandHandler
public static void handle(IssueGiftCard cmd,
                          EventAppender eventAppender,
                          @InjectEntity @Nullable GiftCard giftCard) {
    if (giftCard != null) {
        throw new IllegalStateException("GiftCard already exists");
    }
    eventAppender.append(new GiftCardIssued(cmd.cardId(), cmd.amount()));
}
```

This makes "create only if missing" explicit. Use this **only when AF4 already
threw on existing entities** — it changes semantics for create-or-update flows.
For an architecture-neutral migration, prefer the instance-handler + no-arg
`@EntityCreator` row from the table above.

## NPE on null state (gotcha)

When migrating `CREATE_IF_MISSING`, AF5 always materializes an empty entity, so
the instance handler now runs on `this` with all fields default-initialised
(`null`, `0`, `false`). If the AF4 handler had implicit guarding via
`AggregateNotFoundException`, the AF5 handler will instead enter the body and
NPE on the first method call on a null field.

Two acceptable fixes:

1. **Domain-level guard** — add an explicit `if (this.id == null) { ... }` check
   at the top of the handler and throw a meaningful domain exception.
2. **Test expectation** — if the original AF4 test asserted `AggregateNotFoundException`,
   update the assertion (see `test-fixture-mapping.md`) to expect the actual
   domain exception. If only NPE surfaces, expect `Exception.class` and add a
   `// TODO` to harden the domain model.
