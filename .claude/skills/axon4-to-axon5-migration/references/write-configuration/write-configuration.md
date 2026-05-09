# Recipe: Configuration writer

Atomic migration of ONE class that **configures** Axon Framework — Spring `@Configuration` with `@Bean ConfigurerModule` / `@Bean Configurer` / `@Bean EventProcessingConfigurer`-lambda methods, OR a non-Spring entry point that builds a `Configurer` directly via `DefaultConfigurer.defaultConfiguration()`.

## Goal

Switch the configuration shape:
- `Configurer` / `DefaultConfigurer` → focused `ApplicationConfigurer` (`MessagingConfigurer` / `ModellingConfigurer` / `EventSourcingConfigurer`).
- `ConfigurerModule` → `ConfigurationEnhancer` acting on a `ComponentRegistry`.
- `EventProcessingConfigurer` calls → one `@Bean EventProcessorDefinition` per processor (`pooledStreaming` / `subscribing` / `pooledStreamingMatching` / `subscribingMatching`) with `.assigningHandlers(...)` + `.customized(...)` / `.notCustomized()`.
- `Configurer.onStart` / `onShutdown` → `lifecycleRegistry(...)` (free-standing) or `ComponentDefinition.onStart` / `onShutdown` (component-tied).
- `Lifecycle` interface → folded into the `ComponentDefinition` registration.
- Component registration → `componentRegistry(cr -> cr.registerComponent(...))` (factory's `config` arg is now AF5 read-only `Configuration`).

## Inputs

- target: FQ `@Configuration` class declaring `Configurer` / `ConfigurerModule` / `EventProcessingConfigurer` beans (required)
- target_test: FQ test class name (optional)

## End condition

1. Zero compile errors in the configuration class itself and any test that exercises it.
2. Scoped `test-compile` succeeds.
3. If config has integration test, scoped tests pass.

## Output

- target: <FQ class>
- decisions:
    - path: <A (Spring Boot) | B (non-Spring)>
    - bean-kind: <Configurer | ConfigurerModule | EventProcessingConfigurer>
- needs-user-decision: <true | false>
- needs-user-decision-reason: <text> (only when true)
- notes: optional

## Preflight

1. Class already imports AF5 configurers (`MessagingConfigurer` / `ModellingConfigurer` / `EventSourcingConfigurer` / `ConfigurationEnhancer`)?
2. No AF4 `Configurer` / `ConfigurerModule` / `DefaultConfigurer` references?
3. Compile clean?
4. If all yes → STOP. Skip / Deep verify.

## Selection rule

If user names a target → use it. Else: pick **first lexical-order** file matching the "In scope" shape. One class per run.

Strongest signals = AF4 imports:
`org.axonframework.config.Configurer`, `org.axonframework.config.ConfigurerModule`, `org.axonframework.config.EventProcessingConfigurer`, `org.axonframework.config.DefaultConfigurer`, `org.axonframework.lifecycle.Lifecycle`.

Skip files whose only AF4 reference is read-side — those belong to the read-configuration recipe.

## In scope

ONE class that uses any AF4 *write-side* configuration API. At least one of:
- `@Bean ConfigurerModule` (Spring).
- `@Bean Configurer` / `DefaultConfigurer.defaultConfiguration()` (non-Spring or manual entry point).
- `EventProcessingConfigurer` lambda — `registerPooledStreamingEventProcessor`, `registerTrackingEventProcessor`, `registerSubscribingEventProcessor`, `assignHandlerTypesMatching`, `byDefaultAssignTo`, `registerSequencingPolicy`, `registerListenerInvocationErrorHandler`, `registerErrorHandler`, `registerDefaultErrorHandler`.
- AF4 lifecycle hooks — `configurer.onStart(Phase, () -> ...)` / `configurer.onShutdown(Phase, () -> ...)`, OR class implements `org.axonframework.lifecycle.Lifecycle`.
- AF4 component registration — `configurer.registerComponent(MyService.class, config -> ...)`.

## Out of scope

- Configuration **readers** (classes that read rather than configure) — see the read-configuration recipe.
- **DLQ wiring** — `registerDeadLetterQueue` / `registerDeadLetterQueueProvider` / `registerEnqueuePolicy` / `JpaSequencedDeadLetterQueue` / `MongoSequencedDeadLetterQueue` / `SequencedDeadLetterQueueProviderConfigurerModule`. DLQ is Axoniq commercial (`io.axoniq.framework:axoniq-dead-letter`). Belongs to a future `axon4-to-axoniq5-deadletter` recipe. **Do not migrate here.** Leave the AF4 DLQ code intact and flag it for the user.
- Per-handler-class edits — `@ProcessingGroup` → `@Namespace`, `@SequencingPolicy` placement, `CommandGateway` → `CommandDispatcher`, `@EventHandler` / `@QueryHandler` import moves. Handled by the event-processor recipe and per-handler recipes.

## FQN cheat sheet

### AF4 (remove)

| Element | FQN |
|---|---|
| `Configurer` | `org.axonframework.config.Configurer` |
| `DefaultConfigurer` | `org.axonframework.config.DefaultConfigurer` |
| `ConfigurerModule` | `org.axonframework.config.ConfigurerModule` |
| `EventProcessingConfigurer` | `org.axonframework.config.EventProcessingConfigurer` |
| `Configuration` (AF4 root) | `org.axonframework.config.Configuration` |
| `Lifecycle` | `org.axonframework.lifecycle.Lifecycle` |
| `Phase` | `org.axonframework.common.lifecycle.Phase` *(stays — same FQN in AF5)* |
| `TrackingEventProcessorConfiguration` | `org.axonframework.eventhandling.TrackingEventProcessorConfiguration` |

### AF5 (add)

| Element | FQN |
|---|---|
| `ApplicationConfigurer` | `org.axonframework.configuration.ApplicationConfigurer` |
| `AxonConfiguration` | `org.axonframework.configuration.AxonConfiguration` |
| `Configuration` (AF5 read-only) | `org.axonframework.configuration.Configuration` |
| `ConfigurationEnhancer` | `org.axonframework.configuration.ConfigurationEnhancer` |
| `ComponentRegistry` | `org.axonframework.configuration.ComponentRegistry` |
| `ComponentDefinition` | `org.axonframework.configuration.ComponentDefinition` |
| `LifecycleRegistry` | `org.axonframework.configuration.LifecycleRegistry` |
| `Phase` | `org.axonframework.common.lifecycle.Phase` |
| `MessagingConfigurer` | `org.axonframework.configuration.MessagingConfigurer` |
| `ModellingConfigurer` | `org.axonframework.modelling.configuration.ModellingConfigurer` |
| `EventSourcingConfigurer` | `org.axonframework.eventsourcing.configuration.EventSourcingConfigurer` |
| `EventProcessorDefinition` (Spring) | `org.axonframework.extension.spring.config.EventProcessorDefinition` |
| `EventHandlerSelector` | `org.axonframework.extension.spring.config.EventHandlerSelector` |
| `EventProcessorSettings` | `org.axonframework.extension.spring.config.EventProcessorSettings` |
| `ErrorHandler` (per-processor) | `org.axonframework.messaging.eventhandling.processing.errorhandling.ErrorHandler` |
| `PropagatingErrorHandler` | `org.axonframework.messaging.eventhandling.processing.errorhandling.PropagatingErrorHandler` |

## Procedure

### 1. Locate

```bash
grep -rln --include='*.java' --include='*.kt' \
  -e 'org.axonframework.config.Configurer\b' \
  -e 'org.axonframework.config.ConfigurerModule' \
  -e 'org.axonframework.config.EventProcessingConfigurer' \
  -e 'org.axonframework.config.DefaultConfigurer' \
  -e 'org.axonframework.lifecycle.Lifecycle\b' \
  <target>/src
```

Pick first lexical match that **defines** AF4 write-side config.

### 2. Pick the right `ApplicationConfigurer`

Configurers form a delegation chain: `MessagingConfigurer` ⊂ `ModellingConfigurer` ⊂ `EventSourcingConfigurer`. Pick highest layer the class touches:

| Class touches | Pick |
|---|---|
| Only messaging (command/event/query bus, message handlers) | `MessagingConfigurer.create()` |
| Adds entities / repositories | `ModellingConfigurer.create()` |
| Adds event sourcing (event store, snapshots, event-sourced entities) | `EventSourcingConfigurer.create()` |

Escape hatches: `configurer.modelling(...)`, `configurer.messaging(...)`, `configurer.componentRegistry(...)`, `configurer.lifecycleRegistry(...)`. Use them when the AF4 call applied to a different layer than the one picked.

### 3. `@Bean ConfigurerModule` → `@Bean ConfigurationEnhancer`

Lambda parameter type changes from `Configurer` (read+write) → `ComponentRegistry` (write only).

```java
// AF4
@Bean
public ConfigurerModule myModule() {
    return configurer -> configurer.registerComponent(
            MyService.class,
            config -> new MyServiceImpl());
}

// AF5
@Bean
public ConfigurationEnhancer myEnhancer() {
    return registry -> registry.registerComponent(
            MyService.class,
            config -> new MyServiceImpl());
}
```

Notes:
- Rename method `*Module` → `*Enhancer` if no other bean references it by name.
- Factory's `config` arg is now AF5 read-only `Configuration`. AF4 calls (`config.eventStore()`, `config.commandBus()`, …) → `config.getComponent(EventStore.class)` / `config.getComponent(CommandBus.class)`. Cross-ref: the read-configuration recipe.
- AF4 `Configurer#configureCommandBus` / `configureEventStore` / `configureSerializer` are **not** on `ComponentRegistry`. They live on the focused `ApplicationConfigurer` — switch to a bean that customises the configurer (step 4) or use the dedicated registration methods in non-Spring code.

### 4. Manual `Configurer` → focused `ApplicationConfigurer` (non-Spring)

```java
// AF4
Configurer configurer = DefaultConfigurer.defaultConfiguration();
configurer.registerComponent(MyService.class, c -> new MyServiceImpl());
Configuration configuration = configurer.buildConfiguration();
configuration.start();

// AF5
EventSourcingConfigurer configurer = EventSourcingConfigurer.create();
configurer.componentRegistry(registry -> registry.registerComponent(
        MyService.class, c -> new MyServiceImpl()));
AxonConfiguration configuration = configurer.build();
configuration.start();
```

- `buildConfiguration()` → `build()`. Return type `AxonConfiguration` (extends `Configuration`).
- Bus / store registration stays on the focused configurer: `registerCommandBus` / `registerQueryBus` / `registerEventSink` on `MessagingConfigurer`; `registerEventStore` on `EventSourcingConfigurer`. Generic components flow through `componentRegistry(cr -> cr.registerComponent(...))`.

### 5. `EventProcessingConfigurer` (Spring) → `@Bean EventProcessorDefinition`

AF4 typically had a `@Bean ConfigurerModule` that called `configurer.eventProcessing()` and chained processor registrations. AF5 expresses each processor as its **own** `@Bean EventProcessorDefinition`. **One bean per processor**, no shared lambda.

```java
// AF4
@Bean
public ConfigurerModule configure() {
    return configurer -> {
        EventProcessingConfigurer p = configurer.eventProcessing();
        p.registerPooledStreamingEventProcessor(
                "my-processor",
                org.axonframework.config.Configuration::eventStore,
                (config, builder) -> builder.initialSegmentCount(8).batchSize(100))
         .assignHandlerTypesMatching(
                "my-processor",
                type -> type.getPackageName().startsWith("com.my.projectors"));
    };
}

// AF5 — one bean per processor
@Bean
public EventProcessorDefinition myProcessorDefinition() {
    return EventProcessorDefinition
            .pooledStreaming("my-processor")
            .assigningHandlers(descriptor -> descriptor.beanType()
                    .getPackageName().startsWith("com.my.projectors"))
            .customized(config -> config.initialSegmentCount(8).batchSize(100));
}
```

Method-mapping cheat sheet:

| AF4 (on `EventProcessingConfigurer`) | AF5 (`EventProcessorDefinition` builder) |
|---|---|
| `registerPooledStreamingEventProcessor(name)` | `pooledStreaming(name).assigningHandlers(...).notCustomized()` |
| `registerPooledStreamingEventProcessor(name, source, customisation)` | `pooledStreaming(name).assigningHandlers(...).customized(config -> /* translate builder */)` |
| `registerSubscribingEventProcessor(name)` | `subscribing(name).assigningHandlers(...).notCustomized()` |
| `registerTrackingEventProcessor(name, ...)` | **Removed.** Switch to `pooledStreaming(name)`. |
| `assignHandlerTypesMatching(group, predicate)` | merged into `assigningHandlers(EventHandlerSelector)` on the processor |
| `byDefaultAssignTo(group)` | the receiving `EventProcessorDefinition` becomes the default sink — give it an `EventHandlerSelector` matching everything not claimed by others, OR use `pooledStreamingMatching(name)` (auto-selects by `@Namespace(name)`) |
| `registerSequencingPolicy(group, factory)` | **Delete.** See step 5a. |
| `registerErrorHandler(group, factory)` | fold into same processor's `.customized(config -> config.errorHandler(...))` — see step 5b |
| `registerDefaultErrorHandler(factory)` | apply to **every** `EventProcessorDefinition` in this class — see step 5b |
| `registerListenerInvocationErrorHandler(group, factory)` | **Removed.** Listener-invocation seam is gone; the per-processor `ErrorHandler` is the only seam now — see step 5b |
| `registerDeadLetterQueue` / `registerDeadLetterQueueProvider` / `registerEnqueuePolicy` | **OUT OF SCOPE.** Flag for `axon4-to-axoniq5-deadletter`. Do not migrate. |

Notes:
- `assigningHandlers` takes an `EventHandlerSelector` lambda; param is `BeanDescriptor` (`descriptor.beanType()`, `descriptor.beanName()`). Replaces *both* `assignHandlerTypesMatching` and `byDefaultAssignTo`.
- `pooledStreamingMatching(name)` / `subscribingMatching(name)` shortcut factories auto-select handlers by `@Namespace(name)`. If the per-handler recipe already ran (handlers carry `@Namespace`) and processor-name matches, prefer `*Matching` — eliminates `assigningHandlers(...)` entirely.
- `.customized(...)` vs `.notCustomized()` — `notCustomized` when AF4 used defaults; `customized(config -> ...)` when AF4 customised the builder. AF5 config object exposes the same surface: `initialSegmentCount`, `batchSize`, `maxClaimedSegments`, etc.
- Properties-based config still works. AF5 binds `axon.eventhandling.processors.<name>.*` to `EventProcessorSettings`. `EventProcessorDefinition` beans coexist with properties; explicit `.customized(...)` overrides the properties.

#### 5a. `registerSequencingPolicy(...)` — **delete from this class**

Sequencing policy is a **handler-side** concern in AF5: `@SequencingPolicy(type = ..., parameters = ...)` on the handler class (or method). The event-processor recipe is responsible for adding the annotation; **this recipe is responsible for deleting the now-dead external registration** so the two cannot drift.

```java
// AF4 — delete entire registration
processingConfigurer.registerSequencingPolicy(
        "my-processor",
        config -> new MetadataSequencingPolicy("aggregateId"));
```

Apply the deletion **even when** the per-handler recipe has not yet run on the matching handler class. Note in the diff which group(s) had a `registerSequencingPolicy(...)` so the user knows where the annotation must land. **Never** inline the policy into `EventProcessorDefinition.customized(...)` — that path does not exist in AF5.

#### 5b. Error handlers — fold into `EventProcessorDefinition.customized(...)`

AF4 had two seams per processing group: `registerErrorHandler` (processor-level — outer loop fails) and `registerListenerInvocationErrorHandler` (per `@EventHandler` invocation). AF5 collapses both into **one** seam: a single `ErrorHandler` per processor via `EventProcessorConfiguration.errorHandler(ErrorHandler)`. `ListenerInvocationErrorHandler` does **not exist** in AF5.

```java
// AF4
return configurer -> {
    EventProcessingConfigurer p = configurer.eventProcessing();
    p.registerErrorHandler("my-processor",
            config -> PropagatingErrorHandler.instance());
    p.registerListenerInvocationErrorHandler("my-processor",
            config -> new LoggingListenerInvocationErrorHandler());
};

// AF5 — both fold into the same .customized(...)
@Bean
public EventProcessorDefinition myProcessorDefinition() {
    return EventProcessorDefinition.pooledStreaming("my-processor")
            .assigningHandlers(/* ... */)
            .customized(config -> config.errorHandler(myErrorHandler()));
}
```

Steps:
1. **Pick the AF5 `ErrorHandler`** for the AF4 setting:
   - `PropagatingErrorHandler.instance()` keeps name and behaviour — re-import from `org.axonframework.messaging.eventhandling.processing.errorhandling.PropagatingErrorHandler`.
   - Custom AF4 `ErrorHandler` impls need their own migration (signature changed); the **registration** rewrite here is mechanical.
   - **`ListenerInvocationErrorHandler` impls are orphaned.** No AF5 equivalent. Two options: (a) fold listener-invocation logic into the processor's single `ErrorHandler`; (b) delete if it was a thin logging wrapper now covered by AF5 default behaviour. **Flag** for the user — never silently drop a custom impl.
2. **Merge into the matching `EventProcessorDefinition`** — add `.errorHandler(...)` inside `.customized(config -> ...)`. If the processor was `.notCustomized()`, switch to `.customized(config -> config.errorHandler(...))`. Other customisations (segments, batch size, …) chain on the same `config`.
3. **`registerDefaultErrorHandler(factory)`** — AF5 has no default-only knob today. Apply the resolved `ErrorHandler` to **every** `EventProcessorDefinition` in this class. Flag any other configuration class in the project that defines `EventProcessorDefinition` beans (separate run).
4. **Delete** AF4 `registerErrorHandler` / `registerListenerInvocationErrorHandler` / `registerDefaultErrorHandler` once the `.errorHandler(...)` has landed.

### 6. Non-Spring `EventProcessingConfigurer` → `MessagingConfigurer#eventProcessing(...)`

When candidate configures event processing programmatically (no Spring):

```java
// AF4
configurer.eventProcessing()
          .registerPooledStreamingEventProcessor("my-processor");

// AF5
messagingConfigurer.eventProcessing(
    eventProcessing -> eventProcessing.pooledStreaming(
        pooledStreaming -> pooledStreaming.processor(
            "my-processor",
            module -> module.eventHandlingComponents(components -> components)
                            .notCustomized())));
```

Rare outside framework tests — most real projects use Spring.

### 7. Lifecycle handlers

AF4 had three places to hook lifecycle:
1. `Configurer.onStart(Phase, Runnable)` / `Configurer.onShutdown(Phase, Runnable)`.
2. Component implementing `Lifecycle` and overriding `registerLifecycleHandlers(LifecycleRegistry)`.
3. `@StartHandler` / `@ShutdownHandler` annotations on framework components.

AF5: lifecycle hooks attach to **two** places — the `LifecycleRegistry` for free-standing hooks, and a `ComponentDefinition` for hooks tied to a specific component.

#### Free-standing `onStart` / `onShutdown`

```java
// AF4
configurer.onStart(Phase.LOCAL_MESSAGE_HANDLER_REGISTRATIONS, () -> {
    // startup
    return CompletableFuture.completedFuture(null);
});

// AF5
configurer.lifecycleRegistry(lr -> lr.onStart(
        Phase.LOCAL_MESSAGE_HANDLER_REGISTRATIONS,
        config -> {
            // startup — config is AF5 Configuration
            return CompletableFuture.completedFuture(null);
        }));
```

Lambda now takes AF5 `Configuration` so the hook can read components without capturing them at registration time. Update the lambda signature and any references inside.

`Phase` constants keep their AF4 FQN (`org.axonframework.common.lifecycle.Phase`) — no import change.

#### Component-tied lifecycle (replacing `Lifecycle` interface)

AF4 `Lifecycle` interface is **removed**. Move both start and shutdown hooks into the `ComponentDefinition` registration:

```java
// AF4
class MyComponent implements Lifecycle {
    @Override
    public void registerLifecycleHandlers(@NotNull Lifecycle.LifecycleRegistry lifecycle) {
        lifecycle.onStart(Phase.LOCAL_MESSAGE_HANDLER_REGISTRATIONS, () -> {});
        lifecycle.onShutdown(Phase.LOCAL_MESSAGE_HANDLER_REGISTRATIONS, () -> {});
    }
}

// AF5 — registration owns the lifecycle
configurer.componentRegistry(cr -> cr.registerComponent(
        ComponentDefinition.ofType(MyComponent.class)
                           .withBuilder(config -> new MyComponent())
                           .onStart(Phase.LOCAL_MESSAGE_HANDLER_REGISTRATIONS, config -> {})
                           .onShutdown(Phase.LOCAL_MESSAGE_HANDLER_REGISTRATIONS, config -> {})));
```

Steps:
1. Remove `implements Lifecycle` and the `registerLifecycleHandlers` override from the component class.
2. Remove the AF4 import `org.axonframework.lifecycle.Lifecycle`.
3. Find the registration site — convert plain `(Type, factory)` → `ComponentDefinition` with hooks attached.
4. If registration lives in a *different* class, **flag** for the user — atomic scope is one class per run.

### 8. Component registration

```java
// AF4
configurer.registerComponent(MyService.class, config -> new MyService());

// AF5 — generic
configurer.componentRegistry(cr -> cr.registerComponent(
        MyService.class,
        config -> new MyService()));

// AF5 — conditional
configurer.componentRegistry(cr -> cr.registerIfNotPresent(
        MyService.class,
        config -> new MyService()));

// AF5 — richer (lifecycle, decorators) — see ComponentDefinition above
```

When AF4 registered the same type *multiple* times under different *names*, use `cr.registerComponent(Type, name, factory)` (3-arg overload). AF5 `getComponents(Type)` returns `Map<String, T>` keyed by name.

### 9. Reading inside the registration factory

Factories given to `registerComponent(Type, factory)` receive AF5 read-only `Configuration` as their argument. Rewrite AF4 read calls inside:

| AF4 factory call | AF5 factory call |
|---|---|
| `config.eventStore()` | `config.getComponent(EventStore.class)` |
| `config.commandBus()` | `config.getComponent(CommandBus.class)` |
| `config.queryBus()` | `config.getComponent(QueryBus.class)` |
| `config.eventBus()` | `config.getComponent(EventSink.class)` *(name change — `EventSink` is AF5 publish-side)* |
| `config.parameterResolverFactory()` | `config.getComponent(ParameterResolverFactory.class)` |
| custom `config.findComponent(Type)` | `config.getOptionalComponent(Type.class)` |

Apply **only** for factories *defined inside* the candidate write-config class. Read-only consumers in other classes → see the read-configuration recipe.

### 10. YAML / properties sweep

Before transforming the class, grep for processor properties:

```bash
grep -rln --include='*.yml' --include='*.yaml' --include='*.properties' \
  'axon\.eventhandling\.processors\.' <project root>
```

AF4 keys are scoped by **processing-group name**; AF5 keeps the same root key but scopes by **processor name** (after AF4's group/processor identity collapses, typically same string). Several leaves were renamed/removed:
- `mode: tracking` is gone — use `mode: pooled` (default streaming) or `mode: subscribing`.
- `sequencing-policy` config moved to class-level `@SequencingPolicy` annotation (handled by per-handler recipe, not here). When the only thing under a group key is `sequencing-policy`, the YAML entry can usually be **deleted** entirely after the per-handler recipe has run.

Note YAML findings in the diff summary; flag for the user as a follow-up edit.

### 11. DLQ sites — flag and leave alone

If the candidate calls `registerDeadLetterQueue(...)`, `registerDeadLetterQueueProvider(...)`, `registerEnqueuePolicy(...)`, or imports `JpaSequencedDeadLetterQueue` / `MongoSequencedDeadLetterQueue` / `SequencedDeadLetterQueueProviderConfigurerModule`:

- **Do not migrate.** DLQ is Axoniq commercial (`io.axoniq.framework:axoniq-dead-letter`) and belongs to a future `axon4-to-axoniq5-deadletter` recipe.
- Leave the AF4 DLQ code **as-is** in this class. It will not compile against AF5 free until the dedicated recipe runs — that is acceptable mid-migration.
- List every DLQ site in the diff summary so the user can route them to the deadletter recipe later.

### 12. Cleanup

After rewrite, remove from this class:
- Stale AF4 imports (`org.axonframework.config.*`, `org.axonframework.lifecycle.Lifecycle`, AF4 `TrackingEventProcessorConfiguration`).
- Now-unused private helper methods (factory lambdas inlined into `registerComponent`, helper builders).
- `implements Lifecycle` clauses on classes whose lifecycle hooks moved to `ComponentDefinition`.
- `@Bean ConfigurerModule` methods that became empty after per-processor extraction — delete the bean entirely.

## Verify (against End condition)

```bash
./mvnw -f <target>/pom.xml -P migration-write-config-<ClassSimpleName> test-compile -DskipTests \
  -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
```

If integration test exists for this configuration:

```bash
./mvnw -f <target>/pom.xml -P migration-write-config-<ClassSimpleName> test \
  -Dtest='<FQTestClass>' \
  -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
```

> Configuration writers usually need an integration test to verify behaviour, not just compilation. If none exists, flag for the user as a follow-up.

## Variants

- **Pure event-processor configuration class** — single `ConfigurerModule` calling `configurer.eventProcessing()`. Apply step 5 (split into one `EventProcessorDefinition` bean per processor). Skip steps 3 (no general module rewrite needed once split), 4, 7, 8.
- **Component-only configuration class** — single `ConfigurerModule` that *only* calls `configurer.registerComponent(...)`. Apply step 3 (→ `ConfigurationEnhancer`), then step 8 implicitly inside the lambda. Skip event-processor steps.
- **Manual `Configurer` builder (non-Spring)** — class with `main` building `DefaultConfigurer` and calling `start()`. Apply step 4 (focused configurer + `build()` rename) plus 6/7/8 as the body uses.
- **`@Configuration` with mixed read+write** — has BOTH a `@Bean ConfigurerModule` AND a method that injects `Configuration` to read state. Migrate **only** the write side here; flag the read side for the read-configuration recipe.
