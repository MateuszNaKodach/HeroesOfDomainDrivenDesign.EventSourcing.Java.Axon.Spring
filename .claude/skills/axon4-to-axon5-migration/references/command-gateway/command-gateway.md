# Recipe: `CommandGateway` caller (top-of-chain, non-handler)

Atomic migration of ONE class that dispatches commands via `CommandGateway` from outside any message handler — typically Spring `@RestController`, `@Scheduled` runner, `CommandLineRunner`, or input adapter / service that is the **first cause** of a command and therefore has **no active `ProcessingContext`**.

## Goal

The class compiles on AF5:
- `CommandGateway` import switched to AF5 location.
- `sendAndWait(...)` calls converted to AF5 equivalents (still available).
- AF4 `send(cmd, metadata)` (returned `CompletableFuture<R>`) rewritten through AF5's `CommandResult` shape.
- Surrounding method's return type adapted (e.g. Spring controller `CompletableFuture<R>`) so framework consumes the future.

> **Hard rule — gateway vs dispatcher.** From `CommandDispatcher` Javadoc: gateway for top-of-chain entry points (REST controllers, schedulers, CLI), dispatcher inside another handler. This recipe handles only the gateway side. Handler-resident dispatch goes to the event-processor recipe (or future command/query/saga handler recipes).

## Inputs

- target: FQ class name of the top-of-chain dispatcher (controller / scheduler / runner injecting `CommandGateway`) (required)
- target_test: FQ test class name (optional)

## End condition

1. Zero compile errors in the class itself.
2. The surrounding method's return type flows the dispatch result correctly (no leftover `void` where the framework expected a future, no leftover blocking where async was intended).
3. Verify decided by user — often integration test or manual smoke-check (the request thread shouldn't block; controller should return `CompletableFuture<R>` async to Spring).

## Output

- target: <FQ class>
- decisions:
    - path: <A (Spring Boot) | B (non-Spring)>
    - return-shape: <mvc | scheduler | reactive>
- needs-user-decision: <true | false>
- needs-user-decision-reason: <text> (only when true)
- notes: optional

## Preflight

1. Read the file. Already imports `org.axonframework.messaging.commandhandling.gateway.CommandGateway`?
2. Check compilation problems on the file. Zero AND no `commandGateway.send(cmd, metadata)` line that returns `CommandResult` instead of `CompletableFuture`?
3. If both clean → STOP. `AskUserQuestion`: Skip / Deep verify.
4. Only proceed if user picks **Deep verify** or step 1/2 reported failures.

## In scope

ONE class that:
- Imports `org.axonframework.commandhandling.gateway.CommandGateway` (AF4 location), AND
- Holds it as class-level dependency (typically constructor-injected, sometimes field-injected), AND
- Calls `commandGateway.send(...)` and/or `commandGateway.sendAndWait(...)`, AND
- Is **NOT** a message-handling component — no methods annotated `@EventHandler`, `@CommandHandler`, `@QueryHandler`, `@MessageHandlerInterceptor`, `@SagaEventHandler`, or any `@MessageHandler` meta-annotation.

## Out of scope

- Handler-resident dispatch (in-handler calls to gateway/dispatcher) — see event-processor recipe.
- Mixed class (some methods are handlers, some top-of-chain dispatchers): run handler recipe FIRST; this recipe touches non-handler methods on follow-up.
- Custom callback class implementing AF4's `CommandCallback` SPI directly — that SPI was removed; flag for user.
- Helper class (e.g. `XxxMetaData.with(...)`) returning AF4 `MetaData` — shared across many call sites; lives in another package; flag for user as a follow-up.

## FQN cheat sheet

| Element | AF4 | AF5 |
|---|---|---|
| `CommandGateway` (interface) | `org.axonframework.commandhandling.gateway.CommandGateway` | `org.axonframework.messaging.commandhandling.gateway.CommandGateway` |
| `CommandResult` (new in AF5) | n/a | `org.axonframework.messaging.commandhandling.gateway.CommandResult` |
| `Metadata` | `org.axonframework.messaging.MetaData` | `org.axonframework.messaging.core.Metadata` |
| `CommandExecutionException` | `org.axonframework.commandhandling.CommandExecutionException` | `org.axonframework.messaging.commandhandling.CommandExecutionException` |

## Procedure

### 1. Locate candidate

If user named target, use it. Otherwise:

```bash
grep -rln --include='*.java' --include='*.kt' \
  'org.axonframework.commandhandling.gateway.CommandGateway' \
  <target>/src
```

From that list, exclude files with handler annotations:

```bash
grep -L \
  -e '@EventHandler' \
  -e '@CommandHandler' \
  -e '@QueryHandler' \
  -e '@MessageHandlerInterceptor' \
  -e '@SagaEventHandler' \
  <files-from-previous-step>
```

Pick first remaining (lexical order).

### 2. Update `CommandGateway` import

Single-line change. Switch import to AF5 FQN. Constructor / field type / variable name stay the same — `CommandGateway` is still the right interface for top-of-chain callers in AF5. Do **NOT** replace with `CommandDispatcher` (that's reserved for in-handler use).

### 3. Rewrite call sites — AF4 → AF5 shape table

> **Critical**: AF5's `CommandGateway.send(cmd, metadata)` returns `CommandResult`, **NOT** `CompletableFuture`. Any AF4 line `CompletableFuture<R> f = commandGateway.send(cmd, metadata)` compiles in AF4 but **NOT** in AF5.

| AF4 call | AF5 replacement | Returns |
|---|---|---|
| `commandGateway.send(cmd)` | `commandGateway.send(cmd, Object.class)` | `CompletableFuture<Object>` |
| `commandGateway.send(cmd, metadata)` (returned `CompletableFuture<Void>` in AF4) | `commandGateway.send(cmd, metadata).resultAs(Void.class)` | `CompletableFuture<Void>` |
| `commandGateway.send(cmd)` returning typed `CompletableFuture<R>` (AF4) | `commandGateway.send(cmd, R.class)` | `CompletableFuture<R>` |
| `commandGateway.send(cmd, callback)` (AF4 callback overload) | `commandGateway.send(cmd).onSuccess(...).onError(...)` — keep behavior identical, do NOT silently change error semantics | `CommandResult` (chained) |
| `commandGateway.sendAndWait(cmd)` | `commandGateway.sendAndWait(cmd)` (still exists) — **prefer** rewriting to async path when surrounding method can return `CompletableFuture` | `Object` (blocking) |
| `commandGateway.sendAndWait(cmd, R.class)` | `commandGateway.sendAndWait(cmd, R.class)` (still exists) — same preference | `R` (blocking) |
| `commandGateway.sendAndWait(cmd, timeout, unit)` | `commandGateway.sendAndWait(cmd)` (no built-in timeout overload in AF5; if needed: `commandGateway.send(cmd, R.class).orTimeout(timeout, unit).join()`) | `Object` (blocking) |

Notes:
- `CommandResult` is **NOT** a `CompletableFuture`. Anywhere AF4 code assigned `commandGateway.send(...)` to `CompletableFuture` variable or returned it from `CompletableFuture<R>` method, you need `.resultAs(R.class)` or `.getResultMessage().thenApply(m -> ...)` to obtain a real future.
- `commandGateway.send(cmd, R.class)` (no metadata) is a convenience overload returning `CompletableFuture<R>` directly — use when no metadata involved.
- Prefer `.resultAs(R.class)` over `.getResultMessage().thenApply(...)` for plain type extraction; reach for the latter only when you also need the `Message` (metadata, identifier).

### 4. Adapt the surrounding method's return type

Three common shapes:

**Spring MVC controller** — Spring serves `CompletableFuture<R>` async out of the box. **Prefer**: return `CompletableFuture<R>` from handler method and pipe dispatch result through `.resultAs(R.class)`:

```java
@PutMapping("/things/{id}")
CompletableFuture<Void> putThings(...) {
    var command = ...;
    return commandGateway.send(command, MyMetadata.with(...))
                         .resultAs(Void.class);
}
```

**Scheduler / runner returning `void`** — AF5's `sendAndWait(...)` still exists. Keep it if surrounding method must run to completion before returning (cron jobs, startup runners, integration tests). Just update the import.

**Reactive return type (Mono / Flux)** — Bridge AF5 `CompletableFuture` from `.resultAs(R.class)` into reactive type method already returns (`Mono.fromFuture(...)`). Project-specific.

### 5. Verify nothing else

- Stale imports — remove remaining `org.axonframework.commandhandling.*` AF4 imports no longer referenced.
- Try/catch on `CommandExecutionException` — FQN moved. Update if present.
- AF4 `CommandCallback` implementations — flag for user, don't silently change.

> Don't introduce abstractions or refactors not required by the AF5 API change.

## Verify (against End condition)

Compile-only is sufficient since this recipe touches a single class:

```bash
./mvnw -f <target>/pom.xml -P migration-command-gateway-<ClassSimpleName> test-compile -DskipTests \
  -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
```

If integration test exists for this controller/runner, prefer running it.

## Examples

See [examples/](examples/) for before/after.
