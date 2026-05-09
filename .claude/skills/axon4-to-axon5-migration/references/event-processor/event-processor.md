# Recipe: Event-handling component (EventProcessor / Projector / Saga-like reactor)

Atomic migration of ONE class with `@EventHandler` methods, typically `@ProcessingGroup`-annotated, optionally dispatching commands in response.

## Goal

The event-handling class compiles and behaves on AF5 APIs:
- `@ProcessingGroup` → `@Namespace`.
- `@EventHandler` import moves to AF5 location.
- Class-level `CommandGateway` field replaced with method-parameter `CommandDispatcher` bound to current `ProcessingContext`.
- Blocking `commandGateway.sendAndWait(...)` → async `commandDispatcher.send(...)` returning `CompletableFuture<?>`.
- `@DisallowReplay`, `@MetaDataValue` (capitalized to `@MetadataValue` in AF5) imports updated.

## Inputs

- target: FQ class name of the event-handling component (required)
- target_test: FQ test class name (optional — auto-discovered as `<target>Test` if absent)

## End condition

1. Zero compile errors in the event-handling class and its primary test class.
2. If the projector has a test class, scoped tests pass.

## Output

- target: <FQ class>
- decisions:
    - path: <A (Spring Boot) | B (non-Spring)>
    - processing-group: <name | "n/a">
    - event-handler-mode: <subscribing | tracking | "n/a">
    - mongo-token-store: <none | move-to-jpa-token-store | pause-migration | accept-stays-af4>   # B1
    - saga-handler-detected: <none | wrong-recipe-skip | pause-migration>                         # B2
    - axon-kafka: <none | accept-stays-af4 | pause-migration | remove-feature-first>             # B3
- needs-user-decision: <true | false>
- needs-user-decision-reason: <text> (only when true)
- notes: optional

## Preflight

1. **Read [not-supported.md](not-supported.md) first** — run every Detection grep listed there against the candidate class and its processor-group config. If any blocker fires, follow that file's `AskUserQuestion` flow and apply its "Effect on Procedure" before doing anything else. Recipe must NOT proceed past Preflight while a blocker is unresolved.
2. Check compilation problems on the file. If zero AND the test class compiles too:
3. Run scoped tests if they exist.
4. If green AND no blocker fired → STOP. `AskUserQuestion`: Skip / Deep verify.
5. Only proceed if user picks **Deep verify** or step 2/3 reported failures.

## In scope

ONE class — typically Spring `@Component` — that:
- Is annotated with `@ProcessingGroup("...")` (`org.axonframework.config.ProcessingGroup`), AND/OR
- Has at least one method annotated `@EventHandler` (`org.axonframework.eventhandling.EventHandler`).

Optionally:
- Injects `CommandGateway` (`org.axonframework.commandhandling.gateway.CommandGateway`) as constructor/field dependency.
- Calls `commandGateway.sendAndWait(...)` / `send(...)` inside `@EventHandler` methods.
- Uses `@MetaDataValue` (`org.axonframework.messaging.annotation.MetaDataValue`) on handler params.
- Annotated `@DisallowReplay` (`org.axonframework.eventhandling.DisallowReplay`).

## Out of scope

- Sagas (`@SagaEventHandler`, `@StartSaga`, `@EndSaga`) — currently not supported by this skill; the orchestrator surfaces it at INIT.
- Top-of-chain `CommandGateway` callers (REST controllers etc. with NO `@EventHandler`) — those belong to the command-gateway recipe.

## FQN cheat sheet

| Element | AF4 | AF5 |
|---|---|---|
| `@ProcessingGroup` | `org.axonframework.config.ProcessingGroup` | *(removed)* — replaced with `@Namespace` |
| `@Namespace` | n/a | `org.axonframework.messaging.core.annotation.Namespace` |
| `@EventHandler` | `org.axonframework.eventhandling.EventHandler` | `org.axonframework.messaging.eventhandling.annotation.EventHandler` |
| `@DisallowReplay` | `org.axonframework.eventhandling.DisallowReplay` | `org.axonframework.messaging.eventhandling.replay.annotation.DisallowReplay` |
| `@ResetHandler` | `org.axonframework.eventhandling.ResetHandler` | `org.axonframework.messaging.eventhandling.replay.annotation.ResetHandler` |
| `@MetaDataValue` (AF4) / `@MetadataValue` (AF5 — capital D) | `org.axonframework.messaging.annotation.MetaDataValue` | `org.axonframework.messaging.core.annotation.MetadataValue` |
| `CommandGateway` (interface) | `org.axonframework.commandhandling.gateway.CommandGateway` | `org.axonframework.messaging.commandhandling.gateway.CommandGateway` (kept; package moved) |
| `CommandDispatcher` (new in AF5) | n/a | `org.axonframework.messaging.commandhandling.gateway.CommandDispatcher` |
| `@SequencingPolicy` (annotation) | n/a | `org.axonframework.messaging.core.annotation.SequencingPolicy` |
| `SequentialPerAggregatePolicy` | `org.axonframework.eventhandling.async.SequentialPerAggregatePolicy` | `org.axonframework.messaging.core.sequencing.SequentialPerAggregatePolicy` |
| `SequentialPolicy` | `org.axonframework.eventhandling.async.SequentialPolicy` | `org.axonframework.messaging.core.sequencing.SequentialPolicy` |
| `MetadataSequencingPolicy` | n/a | `org.axonframework.messaging.core.sequencing.MetadataSequencingPolicy` |
| `PropertySequencingPolicy` | n/a | `org.axonframework.messaging.core.sequencing.PropertySequencingPolicy` |
| `RoutingKeySequencingPolicy` | n/a | `org.axonframework.messaging.core.sequencing.RoutingKeySequencingPolicy` |
| `HierarchicalSequencingPolicy` | n/a | `org.axonframework.messaging.core.sequencing.HierarchicalSequencingPolicy` |
| `NoOpSequencingPolicy` (replaces `FullConcurrencyPolicy`) | `org.axonframework.eventhandling.async.FullConcurrencyPolicy` | `org.axonframework.messaging.core.sequencing.NoOpSequencingPolicy` |

## Procedure

### 1. Locate the candidate

If user named target, use it. Otherwise:

```bash
grep -RlnE 'org\.axonframework\.config\.ProcessingGroup|org\.axonframework\.eventhandling\.EventHandler' \
     --include='*.java' --include='*.kt' <target>/src
```

Pick first lexical file that has BOTH `@EventHandler` method AND at least one of: `@ProcessingGroup`, `CommandGateway` field, AF4 `@DisallowReplay`, AF4 `@MetaDataValue` param.

### 2. Sweep for external configuration tied to this processor

Before transforming, grep for the AF4 processing-group name (string in `@ProcessingGroup("...")`).

```bash
# YAML / properties — typical keys: axon.eventhandling.processors.<group>.*,
# including …<group>.dlq.enabled / …<group>.dlq.*
grep -rln --include='*.yml' --include='*.yaml' --include='*.properties' \
     '<group-name>' <project root>

# Java config — @Bean / EventProcessingConfigurer calls bound to this group
grep -rln --include='*.java' --include='*.kt' \
     -e '<group-name>' \
     -e 'registerSequencingPolicy' \
     -e 'registerListenerInvocationErrorHandler' \
     -e 'registerErrorHandler' \
     -e 'registerDeadLetterQueue' \
     -e 'registerDeadLetterQueueProvider' \
     -e 'registerEnqueuePolicy' \
     -e 'SequencedDeadLetterQueue' \
     -e 'EnqueuePolicy' \
     -e 'registerTokenStore' \
     -e 'MongoTokenStore' \
     -e 'org\.axonframework\.extensions\.mongo' \
     -e 'org\.axonframework\.extensions\.kafka' \
     -e 'KafkaPublisher' \
     -e 'StreamableKafkaMessageSource' \
     -e 'KafkaMessageSourceConfigurer' \
     <source roots>
```

What to do with the findings:

| Finding | Where it migrates |
|---|---|
| `axon.eventhandling.processors.<group>.sequencing-policy` / `registerSequencingPolicy` / custom `SequencingPolicy` `@Bean` | Step 7 here (annotate `@SequencingPolicy` on the class) AND delete AF4 source via the write-configuration recipe |
| `registerListenerInvocationErrorHandler` / `registerErrorHandler` / `@Bean ListenerInvocationErrorHandler` | write-configuration recipe — `.customized(builder -> builder.errorHandler(...))` |
| `registerPooledStreamingEventProcessor` / `registerSubscribingEventProcessor` / `assignHandlerTypesMatching` | write-configuration recipe — `EventProcessorDefinition.pooledStreaming(...)` etc. |
| `axon.eventhandling.processors.<group>.dlq.*` / `registerDeadLetterQueue*` / `registerEnqueuePolicy` / `@Bean SequencedDeadLetterQueue` / `@Bean EnqueuePolicy` / `JpaSequencedDeadLetterQueue` / `MongoSequencedDeadLetterQueue` | Defer to the write-configuration recipe — DLQ migrates with the Axoniq commercial dependency (`io.axoniq.framework:axoniq-dead-letter`). NOT a blocker; just leave the AF4 DLQ wiring untouched here so write-configuration owns the bean swap (FQN / artifact change) when it runs. |
| `MongoTokenStore` / `registerTokenStore(MongoTokenStore...)` / `org.axonframework.extensions.mongo.*` token-store imports | **Blocker B1 — see [not-supported.md](not-supported.md).** User picks JPA token store / pause / accept-stays-af4. |
| `KafkaPublisher` / `StreamableKafkaMessageSource` / `KafkaMessageSourceConfigurer` / `org.axonframework.extensions.kafka.*` | **Blocker B3 — see [not-supported.md](not-supported.md).** No AF5 release of `axon-kafka`. User picks accept-stays-af4 / pause / remove-feature-first. |

If sweep finds nothing → AF5 defaults apply, skip step 7.

### 3. Replace `@ProcessingGroup` → `@Namespace`

1:1 string argument. Update import.

**Binding rule.** The string in `@Namespace("<name>")` MUST equal every external reference to this processor — Spring keys (`axon.eventhandling.processors.<name>.*`), `pooledStreamingMatching("<name>")` / `subscribingMatching("<name>")` calls, programmatic registration. Mismatch compiles fine but produces a silent no-op (no events delivered). Match whatever the config side uses.

**AF4 had no `@ProcessingGroup` — still add `@Namespace` if external config references this handler.** AF4 implicitly defaulted the group name to the handler's **package name**. AF5 does NOT auto-derive — missing `@Namespace` means no namespace at all. If sweep (step 2) found `axon.eventhandling.processors.<name>.*` or `pooledStreamingMatching("<name>")` referencing this handler, add `@Namespace("<name>")` using the same string the config side uses (conventionally the FQ package name when AF4 had no `@ProcessingGroup`).

### 4. Migrate `@EventHandler` import + sibling annotations

- `@EventHandler`: AF4 → AF5 location.
- `@DisallowReplay`: AF4 → AF5 location.
- `@ResetHandler`: AF4 → AF5 location (same package move as `@DisallowReplay`).
- `@MetaDataValue` → `@MetadataValue` (note capital D). Update both name and import.
- Keep framework-agnostic stereotypes (`@Component`, `@Service`, …) untouched.

**Do NOT change handler-method visibility, name, or parameter order.** AF5 resolves parameters by type/annotation, not position.

### 5. Replace class-level `CommandGateway` with method-parameter `CommandDispatcher`

In AF5, dispatching commands FROM a handler must use `CommandDispatcher` (gets the current `ProcessingContext` automatically). The class-level `CommandGateway` field is removed.

**Decision rule** (from `CommandDispatcher` Javadoc): gateway = top-of-chain entry points; dispatcher = inside another handler. If the gateway is genuinely used outside any handler (exposed publicly, called from a non-handler helper, used in a method without `ProcessingContext`), keep it as a class-level dependency and only update its import to the AF5 FQN.

Steps (in-context case):
1. Remove ONLY the `CommandGateway` field (e.g. `private final CommandGateway commandGateway;`). Other private fields on the class (calculators, repositories, helpers) stay untouched.
2. Remove ONLY the gateway parameter from the constructor — leave any other constructor parameters in place and keep their corresponding field assignments. Delete the entire constructor only when the gateway was its **sole** parameter; in that case Spring uses the default no-arg one.
3. For each `@EventHandler` (and any other in-context handler) that needs to send commands, declare `CommandDispatcher commandDispatcher` as a method parameter — auto-injected by `CommandDispatcherParameterResolverFactory`:
   ```java
   @EventHandler
   public CompletableFuture<?> on(SomeEvent e, CommandDispatcher commandDispatcher) {
       return commandDispatcher.send(new MyCommand(e.id()));
   }
   ```
4. Update the import to AF5 `CommandDispatcher` location.

If gateway is used outside handler methods too → **mixed class** — surface in Output notes so the orchestrator schedules the command-gateway recipe as a follow-up pass.

### 6. Rewrite blocking `sendAndWait(...)` → async `send(...)`

| AF4 | AF5 |
|---|---|
| `commandGateway.sendAndWait(cmd)` | `commandDispatcher.send(cmd)` |
| `commandGateway.sendAndWait(cmd, metadata)` | `commandDispatcher.send(cmd, metadata)` |
| `commandGateway.send(cmd)` (fire-and-forget) | `commandDispatcher.send(cmd)` |
| `commandGateway.sendAndWait(cmd, ResultType.class)` | `commandDispatcher.send(cmd, ResultType.class)` (returns `CompletableFuture<ResultType>`) |

Change `@EventHandler` method return type `void` → `CompletableFuture<?>` whenever it now returns the dispatcher's result. Framework consumes the future — NO manual `.join()` / `.get()` / blocking.

**Dispatch shape — preference order:**

1. **Single dispatch → return the future directly.** `return commandDispatcher.send(cmd, metadata);` — framework consumes the result. Add `import java.util.concurrent.CompletableFuture;`.
2. **Loop / multiple dispatches → `CompletableFuture.allOf(...)`.** Collect each dispatch's `CompletableFuture<? extends Message>` (via `.getResultMessage()` on the `CommandResult`) into an array, return `CompletableFuture.allOf(futures)`.
   ```java
   var futures = items.stream()
       .map(it -> commandDispatcher.send(commandFor(it), metadata).getResultMessage())
       .toArray(CompletableFuture[]::new);
   return CompletableFuture.allOf(futures);
   ```
3. **Last resort — block with explicit timeout.** ONLY when the surrounding code cannot become async. Always `.getResultMessage().orTimeout(d, unit).join()` — never plain `.join()` / `.get()`:
   ```java
   commandDispatcher.send(cmd, metadata)
       .getResultMessage()
       .orTimeout(2, TimeUnit.SECONDS)
       .join();
   ```
   `CommandResult` is NOT a `CompletableFuture` — `.orTimeout(...)` lives on `CompletableFuture`, so `.getResultMessage()` is required first.

**Branching rules (when option 1 applies):**

- Every branch must return a future. Branches with no dispatch return `CompletableFuture.completedFuture(null)`.
- **Conditional dispatch — invert and early-return.** When AF4 was `if (cond) { commandGateway.sendAndWait(...); }` (no else, false branch is empty), invert and early-return for the no-op branch:
  ```java
  // PREFERRED
  if (!cond) {
      return CompletableFuture.completedFuture(null);
  }
  return commandDispatcher.send(cmd, metadata);
  ```
- Post-dispatch work (logging, bookkeeping) → chain with `.thenRun(...)` / `.thenApply(...)` on `commandResult.getResultMessage()`, NOT block.

**`CommandResult` vs `CompletableFuture`:** `commandDispatcher.send(...)` returns `CommandResult`, not `CompletableFuture`. Returning `CommandResult` directly from a `CompletableFuture<?>`-typed handler is fine — AF5's adapter accepts it. But to **compose** (`thenRun`, `allOf`, `orTimeout`, …), call `.getResultMessage()` first to get `CompletableFuture<? extends Message>`.

### 7. Sequencing policy — move from external config to `@SequencingPolicy` on the class

AF4: sequencing policy configured **outside** the class (Spring YAML `axon.eventhandling.processors.<group>.sequencing-policy`, or `@Bean`/`registerSequencingPolicy(group, factory)` in a config class). AF5: same policy *types* but attached directly to the handling component via `@SequencingPolicy`.

**Apply only when AF4 had an explicit override.** AF5 default is `HierarchicalSequencingPolicy(SequentialPerAggregatePolicy → SequentialPolicy)` — identical behaviour to AF4's default `SequentialPerAggregatePolicy` for aggregate-based event stores. If AF4 relied on the default, skip this step.

Detection (during step 2 sweep):
- YAML/properties: `axon.eventhandling.processors.<group>.sequencing-policy`.
- Java config: `EventProcessingConfigurer#registerSequencingPolicy(group, factory)` or a `@Bean SequencingPolicy<?>` wired by group name.
- Custom impl: a class implementing AF4 `org.axonframework.eventhandling.async.SequencingPolicy` — out of scope for this skill (separate signature change + package move).

Mapping AF4 → AF5 policy class:

| AF4 setting / bean | AF5 policy class |
|---|---|
| `SequentialPerAggregatePolicy` (default, explicit) | `SequentialPerAggregatePolicy` |
| `SequentialPolicy` (full serial) | `SequentialPolicy` |
| `FullConcurrencyPolicy` (no ordering) | `NoOpSequencingPolicy` |
| keyed on a metadata field | `MetadataSequencingPolicy` (`parameters = "<metadataKey>"`) |
| keyed on a payload property | `PropertySequencingPolicy` (`parameters = "<propertyName>"`) |
| `@RoutingKey` on the message | `RoutingKeySequencingPolicy` |
| hierarchical / fallback chain | `HierarchicalSequencingPolicy` |
| custom impl | flag for user — out of scope |

Apply:

1. Annotate the event-processor class (or a single `@EventHandler` method when policy applies to one handler only — method-level wins over class-level):
   ```java
   @Namespace("...")
   @SequencingPolicy(type = MetadataSequencingPolicy.class, parameters = "<metadataKey>")
   class MyProcessor { ... }
   ```
   - `parameters` is `String[]`. Single literal is fine; multiple → `parameters = {"a", "b"}`.
   - Compile-time `String` constants (e.g. `GameMetaData.GAME_ID_KEY`) work — annotation-legal.
   - `type` must be `Class<? extends SequencingPolicy>` from `org.axonframework.messaging.core.sequencing.*`.

2. **Delete the AF4 source** so it can't drift — surface in Output notes for the write-configuration recipe to handle:
   - Remove the YAML/properties key.
   - Remove the `@Bean` / `registerSequencingPolicy(...)` from the Java config.

3. **If AF4 had a custom `SequencingPolicy<EventMessage<?>>` impl, migrate the impl class.** The annotation on the processor is the same mechanical move; the impl class itself is a deterministic three-edit rewrite:

   | AF4 | AF5 |
   |---|---|
   | `import org.axonframework.eventhandling.async.SequencingPolicy;` | `import org.axonframework.messaging.core.sequencing.SequencingPolicy;` |
   | `class MyPolicy implements SequencingPolicy<EventMessage<?>>` | unchanged — same generic param works (`SequencingPolicy<M extends Message<?>>`). |
   | `Object getSequenceIdentifierFor(EventMessage<?> event)` | `Optional<Object> sequenceIdentifierFor(EventMessage<?> event, ProcessingContext context)` — rename method, add `ProcessingContext` param, wrap the AF4 return in `Optional.ofNullable(...)`. |
   | `event.getPayload()` / `event.getMetaData()` inside the impl | `event.payload()` / `event.metaData()` (record-style accessors, AF5-wide). |

   Imports to add: `import java.util.Optional;` and `import org.axonframework.messaging.unitofwork.ProcessingContext;`. Imports to drop: AF4 `org.axonframework.eventhandling.async.SequencingPolicy`.

   Annotate the processor class (Step 7 main flow above) with `@SequencingPolicy(type = MyPolicy.class)` pointing at the migrated impl FQN.

   If the AF4 impl returned `null` to mean "no sequencing key" (rare, but legal in AF4), translate via `Optional.ofNullable(...)`. If the AF4 impl returned a sentinel (`""` or `0L`), keep the sentinel — only `null` should become `Optional.empty()`.

### 8. Cleanup after gateway removal

After the gateway is gone, scan the class for:
- Now-empty constructors → delete (Spring uses default no-arg) or leave as default.
- Now-unused private fields → delete.
- Stale imports (`CommandGateway`, AF4 `org.axonframework.*` packages) → delete.

### 9. Out-of-scope: helper-class metadata imports

A common project pattern: a helper like `XxxMetaData.with(...)` that returns the framework's metadata type. In AF5 that should be `org.axonframework.messaging.core.Metadata`. **The per-processor recipe does NOT migrate that helper** — it lives in another package and may be shared across many handlers. If `commandDispatcher.send(cmd, helperOut)` fails to compile because the helper still returns AF4 `org.axonframework.messaging.MetaData`, **flag it for the user** as a follow-up — do not edit the helper here.

### 10. Verify nothing else needs migrating in this class

- Try/catch on `CommandExecutionException`: FQN moved (`org.axonframework.commandhandling` → `org.axonframework.messaging.commandhandling`). Update if present.
- `CommandCallback` SPI removed — **rewrite to `CommandResult.onSuccess(...).onError(...)`** (mechanical):

  | AF4 shape | AF5 rewrite |
  |---|---|
  | `commandGateway.send(cmd, new CommandCallback<C, R>() { onResult(cmdMsg, resultMsg) { if (resultMsg.isExceptional()) {ERR} else {OK} } })` | `commandDispatcher.send(cmd).onSuccess(resultMsg -> { OK }).onError((resultMsg, throwable) -> { ERR });` |
  | older split: `new CommandCallback<C, R>() { onSuccess(cmdMsg, result, metadata) {OK}; onFailure(cmdMsg, cause) {ERR} }` | same — `.onSuccess(resultMsg -> { OK using resultMsg.payload() / resultMsg.metaData() }).onError((resultMsg, throwable) -> { ERR using throwable })` |
  | `LoggingCallback.INSTANCE` / `NoOpCallback.INSTANCE` (fire-and-forget logging) | drop the second arg entirely — `commandDispatcher.send(cmd);` (framework logs failures by default) |
  | Inside an `@EventHandler` body | use `commandDispatcher` per Step 5; chain `.onSuccess` / `.onError` on the returned `CommandResult` BEFORE returning, not after `.getResultMessage()` |

  Imports: drop `org.axonframework.commandhandling.callbacks.CommandCallback` (and `LoggingCallback` / `NoOpCallback` / `FutureCallback`). No AF5 import to add — `onSuccess` / `onError` live on `CommandResult` returned by `commandDispatcher.send(...)`.

  **`CommandResult` lambda parameter types.**
  - `onSuccess(Consumer<? super CommandResultMessage<?>>)` → lambda param is `CommandResultMessage<?>`. Read payload via `.payload()`, metadata via `.metaData()`.
  - `onError(BiConsumer<CommandResultMessage<?>, Throwable>)` → lambda gets both the (possibly null) result message and the throwable.

  **Caveats.**
  - If AF4 callback was used to **block until completion** (e.g. `FutureCallback` then `.getResult()`), this is no longer a callback — it's a sync wait. Use the "Last resort — block with explicit timeout" pattern from Step 6 (`.getResultMessage().orTimeout(d, unit).join()`) instead of trying to preserve the callback.
  - If the callback referenced a class field outside the handler scope (rare), keep the closure intact — lambdas capture by reference like inner classes.

## Verify (against End condition)

If surrounding code still uses AF4 APIs, set up a `migration-event-processor-<ProcessorSimpleName>` profile via [../maven-profile/maven-profile.md](../maven-profile/maven-profile.md) (e.g. `migration-event-processor-WhenCreatureRecruitedThenAddToArmy`). Then:

```bash
./mvnw -f <target>/pom.xml test -P migration-event-processor-<ProcessorSimpleName> \
  -Dtest='<FQTestClass>' \
  -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
```

Drop the `-P migration-event-processor-*` flag only if surrounding code already compiles cleanly.

If the projector has no test class, scoped `test-compile` is sufficient — but still better to flag to user as a follow-up to add tests.

## Variants

- **Pure projector (no command dispatch).** Handler reads event + updates read model — no `CommandGateway`. Apply steps 1–4 + 8–10 only. Skip 5–7. Return type stays `void`.
- **Saga-like reactor with non-handler dispatch.** Class also exposes a public method that dispatches commands outside any `ProcessingContext` (REST endpoint, scheduler entry). Keep `CommandGateway` as a class-level dependency for that path AND add `CommandDispatcher` as a method parameter on in-context handlers. Both coexist.
- **Mixed `@EventHandler` + `@QueryHandler`.** `CommandDispatcher` parameter resolution applies to query handlers too — declare it as a method parameter wherever the handler runs inside a `ProcessingContext`. Out of scope here; surface in Output notes so the orchestrator schedules the query-handler recipe afterwards.

## Examples

See [examples/](examples/) for real-world before/after migrations.
