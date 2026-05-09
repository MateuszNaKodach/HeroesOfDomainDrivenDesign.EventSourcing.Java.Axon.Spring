# Recipe: Configuration reader

Atomic migration of ONE class that **reads** Axon Framework configuration at runtime — typically Spring `@Component` / service that injects a configuration bean to look up a registered framework component (event processor, dead-letter processor, token store, command bus, query bus, event bus, event store, custom component) and call methods on it.

## Goal

The class compiles on AF5:
- Injected bean type changed to `AxonConfiguration` (`org.axonframework.common.configuration.AxonConfiguration`) — or `Configuration` (`org.axonframework.common.configuration.Configuration`) when the class only *reads* and never starts/shuts down the root.
- AF4 dedicated lookup methods (`eventProcessor`, `eventProcessorByProcessingGroup`, `sequencedDeadLetterProcessor`, `tokenStore`, `commandBus`, `queryBus`, `eventBus`, `eventStore`, `sagaConfiguration`, …) rewritten to AF5 generic two-step lookup:
  ```java
  axonConfiguration
      .getModuleConfiguration("<module-name>")
      .flatMap(m -> m.getOptionalComponent(<TargetType>.class[, "<componentName>"]))
  ```
  or — for components registered on the root — direct `axonConfiguration.getOptionalComponent(<TargetType>.class[, "<name>"])`.
- Looked-up component type updated where AF5 renamed/moved it (`TrackingEventProcessor` → `StreamingEventProcessor`).
- Call sites adapted where lifecycle / DLQ methods became async (`CompletableFuture<Void>` for `start()`, `shutdown()`, `resetTokens()`, `processAny()`, `process(...)`).
- `shutDown()` (AF4, capital `D`) renamed to `shutdown()` (AF5).

## Inputs

- target: FQ class name injecting `Configuration` / `EventProcessingConfiguration` (required)
- target_test: FQ test class name (optional)

## End condition

1. Zero compile errors in the class itself and its primary test class.
2. If test class exists, scoped tests pass.

## Output

- target: <FQ class>
- decisions:
    - path: <A (Spring Boot) | B (non-Spring)>
    - injected-type: <Configuration | EventProcessingConfiguration>
- needs-user-decision: <true | false>
- needs-user-decision-reason: <text> (only when true)
- notes: optional

## Preflight

1. Class injects `AxonConfiguration` (not `Configuration` from AF4 location)?
2. No AF4 dedicated lookup methods (`eventProcessor*`, `sequencedDeadLetterProcessor`, `tokenStore`, `commandBus`, `queryBus`, `eventBus`, `eventStore`)?
3. Compile clean?
4. If all yes → STOP. `AskUserQuestion`: Skip / Deep verify.

## In scope

ONE class with field / constructor parameter / method parameter typed as one of the AF4 read-side configuration interfaces:
- `org.axonframework.config.Configuration` (root — `commandBus()`, `queryBus()`, `eventBus()`, `eventStore()`, `findComponent(...)`, …)
- `org.axonframework.config.EventProcessingConfiguration` (event processing — `eventProcessor(name)`, `eventProcessorByProcessingGroup(group)`, `sequencedDeadLetterProcessor(group)`, `tokenStore(group)`, `sagaConfiguration(type)`, …)
- Any other AF4 sub-configuration whose dedicated lookup is replaced by `getOptionalComponent` in AF5.

## Out of scope

- Configuration **writers** (classes that configure rather than read — `Configurer`, `ConfigurerModule`, `EventProcessingConfigurer` lambdas, `@Bean` returning a configurer, `registerSequencingPolicy`, `registerErrorHandler`). Out of scope here.
- Saga lookup (`sagaConfiguration(type)`) — sagas are out-of-scope. Currently not supported by this skill; the orchestrator surfaces it at INIT.

## FQN cheat sheet

### AF4 (remove)

| Element | FQN |
|---|---|
| `Configuration` | `org.axonframework.config.Configuration` |
| `EventProcessingConfiguration` | `org.axonframework.config.EventProcessingConfiguration` |
| `TrackingEventProcessor` | `org.axonframework.eventhandling.TrackingEventProcessor` |
| `EventProcessor` | `org.axonframework.eventhandling.EventProcessor` |
| `StreamingEventProcessor` (AF4 loc.) | `org.axonframework.eventhandling.StreamingEventProcessor` |
| `SequencedDeadLetterProcessor` | `org.axonframework.eventhandling.deadletter.SequencedDeadLetterProcessor` |
| `TokenStore` | `org.axonframework.eventhandling.tokenstore.TokenStore` |

### AF5 (add)

| Element | FQN |
|---|---|
| `AxonConfiguration` | `org.axonframework.common.configuration.AxonConfiguration` |
| `Configuration` (read-only parent) | `org.axonframework.common.configuration.Configuration` |
| `EventProcessor` | `org.axonframework.messaging.eventhandling.processing.EventProcessor` |
| `StreamingEventProcessor` | `org.axonframework.messaging.eventhandling.processing.streaming.StreamingEventProcessor` |
| `PooledStreamingEventProcessor` | `org.axonframework.messaging.eventhandling.processing.streaming.pooled.PooledStreamingEventProcessor` |
| `SubscribingEventProcessor` | `org.axonframework.messaging.eventhandling.processing.subscribing.SubscribingEventProcessor` |
| `SequencedDeadLetterProcessor` | `org.axonframework.messaging.eventhandling.deadletter.SequencedDeadLetterProcessor` |
| `TokenStore` | confirm in source — moved under `org.axonframework.eventstreaming.tokenstore.*` |

## Procedure

### 1. Locate

```bash
grep -RlnE 'org\.axonframework\.config\.(Configuration|EventProcessingConfiguration)' \
     --include='*.java' --include='*.kt' <target>/src
```

Pick a file that **uses** the AF4 type for a read operation (method call on the field). Skip files where the type only appears inside `@Bean` / `Configurer` / `ConfigurerModule` blocks — those are write-side, out of scope.

### 2. Switch the injected bean type

- If class only reads (never starts/shuts down root) → `Configuration` (`org.axonframework.common.configuration.Configuration`).
- If class also touches root lifecycle (rare for readers) → `AxonConfiguration` (`org.axonframework.common.configuration.AxonConfiguration`).
- If class injected `EventProcessingConfiguration` directly → switch to `AxonConfiguration` (entry point for module lookups).
- Rename field to track new type (`eventProcessingConfiguration` → `axonConfiguration`). Update constructor parameter.
- Spring stereotypes (`@Component`, `@Service`) and `@Autowired` / `@Transactional` preserved as-is.

### 3. Rewrite AF4 dedicated lookups → AF5

Two AF5 shapes:

- **Module-scoped** (event processing components live inside per-processor modules): `axonConfig.getModuleConfiguration("<module-name>").flatMap(m -> m.getOptionalComponent(<Type>.class[, "<componentName>"]))`.
- **Root-scoped** (buses, stores registered on the root): `axonConfig.getOptionalComponent(<Type>.class[, "<name>"])` directly — no module hop.

Module name strings are case-sensitive **conventions** emitted by AF5 `*Module` classes. The event-processor module name is `"EventProcessor[" + processorName + "]"` (one module per processor, **not** a single `"EventProcessing"` module).

| AF4 call | AF5 replacement |
|---|---|
| `config.commandBus()` | `axonConfig.getOptionalComponent(CommandBus.class).orElseThrow()` |
| `config.queryBus()` | `axonConfig.getOptionalComponent(QueryBus.class).orElseThrow()` |
| `config.eventBus()` | `axonConfig.getOptionalComponent(EventBus.class).orElseThrow()` |
| `config.eventStore()` | `axonConfig.getOptionalComponent(EventStore.class).orElseThrow()` |
| `epc.eventProcessor(name)` | `axonConfig.getModuleConfiguration("EventProcessor[" + name + "]").flatMap(m -> m.getOptionalComponent(EventProcessor.class))` |
| `epc.eventProcessor(name, EventProcessor.class)` | `axonConfig.getModuleConfiguration("EventProcessor[" + name + "]").flatMap(m -> m.getOptionalComponent(EventProcessor.class))` |
| `epc.eventProcessorByProcessingGroup(group)` | `axonConfig.getModuleConfiguration("EventProcessor[" + group + "]").flatMap(m -> m.getOptionalComponent(EventProcessor.class))` |
| `epc.eventProcessorByProcessingGroup(group, StreamingEventProcessor.class)` | `axonConfig.getModuleConfiguration("EventProcessor[" + group + "]").flatMap(m -> m.getOptionalComponent(StreamingEventProcessor.class))` |
| `epc.tokenStore(processor)` | `axonConfig.getModuleConfiguration("EventProcessor[" + processor + "]").flatMap(m -> m.getOptionalComponent(TokenStore.class))` |
| `epc.sequencedDeadLetterProcessor(group)` | `axonConfig.getModuleConfiguration("EventProcessor[" + group + "]").flatMap(m -> m.getOptionalComponent(SequencedDeadLetterProcessor.class, "EventHandlingComponent[" + group + "][" + componentName + "]"))` |

#### Return-type shift: `Optional<T>` → direct `T` (root-scoped)

AF4 root lookups (`commandBus()`, `queryBus()`, `eventBus()`, `eventStore()`) returned the bean directly. AF5's `getOptionalComponent(...)` returns `Optional<T>`. Use `.orElseThrow(...)` if AF4 code assumed presence; otherwise propagate the optional.

Module-scoped lookups in AF4 returned `Optional<T>` already — same shape after rewrite.

#### DLQ named-component lookup

`SequencedDeadLetterProcessor` lives **inside** the processor module, but the module typically holds many — one per event-handling component. Disambiguate with the AF5 component name `"EventHandlingComponent[" + processorName + "][" + componentName + "]"`:

```java
axonConfig.getModuleConfiguration("EventProcessor[" + processorName + "]")
          .flatMap(m -> m.getOptionalComponent(
              SequencedDeadLetterProcessor.class,
              "EventHandlingComponent[" + processorName + "][" + componentName + "]"))
          .ifPresent(...);
```

> **DLQ flag.** `SequencedDeadLetterProcessor` itself is in the AF5 free build (`org.axonframework.messaging.eventhandling.deadletter`). Read-side lookup compiles against free AF5. **However**, the underlying DLQ store / wiring (e.g. JPA dead-letter sequence, persistent dead-letter queue beans) is Axoniq commercial. If the candidate class also instantiates / configures a DLQ implementation, flag it as commercial — that part belongs to a `axon4-to-axoniq5-*` recipe, not here.

### 4. Update component types

- `TrackingEventProcessor` → `StreamingEventProcessor` (and import). `TrackingEventProcessor` is **removed** in AF5.
- `EventProcessor`, `StreamingEventProcessor`, `PooledStreamingEventProcessor`, `SubscribingEventProcessor`: package moved under `org.axonframework.messaging.eventhandling.processing.*`.
- If AF4 was specifically typed (`TrackingEventProcessor.class`), broaden to `StreamingEventProcessor` — gives `supportsReset()`, `resetTokens()`, `start()`, `shutdown()` without committing to `PooledStreamingEventProcessor`.

### 5. Adapt async lifecycle / DLQ calls

AF4 sync → AF5 async. Return type changes from `void` to `CompletableFuture<Void>`:

- `processor.start()` → `CompletableFuture<Void>`
- `processor.shutDown()` (AF4) → `processor.shutdown()` (AF5) → `CompletableFuture<Void>` — **rename method** (capital `D` → lowercase `d`) and add the future handling.
- `processor.resetTokens()` → `CompletableFuture<Void>`
- DLQ `processor.processAny()` → `CompletableFuture<Void>`
- DLQ `processor.process(...)` → `CompletableFuture<Void>`

Bridge with `.orTimeout(<duration>, <unit>).join()` (project rule on `CompletableFuture` blocking — never naked `.join()` / `.get()`):

```java
eventProcessor.shutdown().orTimeout(30, TimeUnit.SECONDS).join();
eventProcessor.resetTokens().orTimeout(30, TimeUnit.SECONDS).join();
eventProcessor.start().orTimeout(30, TimeUnit.SECONDS).join();
```

Add `import java.util.concurrent.TimeUnit;` if absent. If existing code uses naked `.join()`, **upgrade** to `.orTimeout(...).join()` (default 30s) as part of this migration.

When the caller is itself async-capable (returns `CompletableFuture<?>` or composes via `thenCompose`), prefer chaining over blocking — keep the timeout at the outer caller.

### 6. Sweep imports / redundant fields

- Remove stale AF4 imports: `org.axonframework.config.*`, `org.axonframework.eventhandling.TrackingEventProcessor`, AF4-located `TokenStore` / `EventProcessor` / `StreamingEventProcessor`.
- Common AF4 shape injects **both** `EventProcessingConfiguration` and `TokenStore` (the latter for `fetchSegments` / `fetchToken`). AF5 routes both through `axonConfiguration.getOptionalComponent(TokenStore.class[, name])` — the separately injected `TokenStore` field is usually now redundant. Delete it (and constructor param) unless flagged out of scope.

## Verify (against End condition)

```bash
./mvnw -f <target>/pom.xml -P migration-read-config-<ClassSimpleName> test-compile -DskipTests \
  -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
```

If test class exists:

```bash
./mvnw -f <target>/pom.xml -P migration-read-config-<ClassSimpleName> test \
  -Dtest='<FQTestClass>' \
  -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
```

## Examples

See [examples/](examples/).
