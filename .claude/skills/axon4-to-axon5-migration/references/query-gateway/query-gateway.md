# Recipe: `QueryGateway` caller (top-of-chain, non-handler)

Atomic migration of ONE class that dispatches queries via `QueryGateway` from outside any message handler — typically Spring `@RestController`, service entry point, scheduler.

## Goal

The class compiles on AF5:
- `QueryGateway` import switched to AF5 location.
- `ResponseType` wrapper removed (eliminated in AF5).
- `query(...)` / `subscriptionQuery(...)` rewritten to use plain `Class<R>` overloads.
- Multi-response queries split into `queryMany(...)`.
- Surrounding method's return type adapted (e.g. Spring controller `CompletableFuture<R>`).
- Named queries (`queryGateway.query("name", payload, ...)`) move from call-site name to payload-class via `@Query` annotation.

> **Hard rule — never wrap a dispatch in `GenericQueryMessage` to preserve a named query.** Constructing `new GenericQueryMessage(new MessageType("name"), payload)` at the call site is mechanically valid but architecturally wrong: scatters routing names across every dispatch site instead of keeping them on the payload type. The only correct migration for an AF4 named query is to put the name on the payload class via `@Query` (`org.axonframework.messaging.queryhandling.annotation.Query`). If AF4 payload was bare scalar (`String`, enum, `Long`, …), introduce a dedicated record and annotate it.

## Inputs

- target: FQ class name of the top-of-chain dispatcher injecting `QueryGateway` (required)
- target_test: FQ test class name (optional)

## End condition

1. Zero compile errors in the class.
2. No `ResponseType` / `ResponseTypes.*` wrappers remain.
3. Surrounding method's return type flows the result correctly.
4. Verify decided by user — often integration test.

## Output

- target: <FQ class>
- decisions:
    - path: <A (Spring Boot) | B (non-Spring)>
    - query-shape: <single | stream | subscription>
- needs-user-decision: <true | false>
- needs-user-decision-reason: <text> (only when true)
- notes: optional

## Preflight

1. File imports `QueryGateway` from `org.axonframework.messaging.queryhandling.gateway` (AF5 FQN), not `org.axonframework.queryhandling` (AF4 FQN).
2. No imports of `org.axonframework.messaging.responsetypes.*` (`ResponseType` / `ResponseTypes` SPI removed in AF5).
3. No call sites wrapping in `instanceOf(...)` / `multipleInstancesOf(...)` / `optionalInstanceOf(...)` or referencing `ResponseTypes` statically.
4. No `queryGateway.scatterGather(...)` (removed in AF5; if present → **not** a no-op, flag for user).
5. Subscription-query call sites pass plain `Class<I>` / `Class<U>`; callers expecting AF4 `Mono<I>` initial result already adapted to AF5 `Flux<I>` shape.
6. **No bare blocking calls without timeout.** `.get()` / `.join()` without `.orTimeout(...)` at sync boundaries is a real fix the recipe should apply — **not** a no-op. Most common diff post-recipe.
7. **No AF4 named-query call sites.** Any `queryGateway.query("<name>", payload, ...)` / `queryGateway.queryMany("<name>", payload, ...)` (3-arg, first arg `String`) is the AF4 named overload, **removed in AF5** — apply rewrite from section 3 even when 1–6 hold.

If items 1–5 hold AND item 6 holds: file is recipe-pre-migrated → STOP. `AskUserQuestion`: Skip / Deep verify.

## In scope

ONE class that:
- Imports `org.axonframework.queryhandling.QueryGateway` (AF4), AND
- Holds it as class-level dependency, AND
- Calls `queryGateway.query(...)` / `subscriptionQuery(...)` / `streamingQuery(...)` / `scatterGather(...)`, AND
- Is **NOT** a message-handling component.

## Out of scope

- Handler-resident dispatch — see the event-processor recipe.
- `scatterGather(...)` removal — flag for user; **removed in AF5 with no drop-in replacement**.
- `@QueryHandler` classes (the receiving side) — see the query-handler recipe.

## FQN cheat sheet

| Element | AF4 | AF5 |
|---|---|---|
| `QueryGateway` | `org.axonframework.queryhandling.QueryGateway` | `org.axonframework.messaging.queryhandling.gateway.QueryGateway` |
| `ResponseType` (and `ResponseTypes`) | `org.axonframework.messaging.responsetypes.ResponseType` | *(removed — use plain `Class<R>`)* |
| `@Query` (new in AF5) | n/a | `org.axonframework.messaging.queryhandling.annotation.Query` |
| `SubscriptionQueryResult` | `org.axonframework.queryhandling.SubscriptionQueryResult` | **split** — `SubscriptionQueryResponse<I, U>` (gateway, payloads) and `SubscriptionQueryResponseMessages` (bus, messages); FQN under `org.axonframework.messaging.queryhandling.*` |
| `MetaData` | `org.axonframework.messaging.MetaData` | `org.axonframework.messaging.core.Metadata` |
| `GenericQueryMessage` | `org.axonframework.queryhandling.GenericQueryMessage` | `org.axonframework.messaging.queryhandling.GenericQueryMessage` — **never construct at call sites** to preserve a named query (see hard rule); use `@Query`-annotated payload classes instead |

## Procedure

### 1. Locate

```bash
grep -rln --include='*.java' --include='*.kt' \
  'org.axonframework.queryhandling.QueryGateway' \
  <target>/src
```

Filter out files with handler annotations (same as command-gateway recipe).

### 2. Update import

`org.axonframework.queryhandling.QueryGateway` → `org.axonframework.messaging.queryhandling.gateway.QueryGateway`.

### 3. Handle named queries — `@Query` annotation

**If the AF4 call is** `queryGateway.query("queryName", payload, ResponseType...)`:

a. Change call site to use the typed overload (no name string, no `ResponseType` wrapper):
   ```java
   queryGateway.query(payload, ResponseClass.class);
   ```
b. **Move the name to the payload class** via `@Query`. `@Query.name()` becomes the `QualifiedName.localName()` AF5 uses to route. Match the AF4 string **exactly** so the handler-side `@QueryHandler(queryName = "<the AF4 name>")` registration keeps routing to the same method:
   ```java
   import org.axonframework.messaging.queryhandling.annotation.Query;

   @Query(name = "queryName")
   public record GetXById(String id) { }
   ```
   `@Query.namespace()` defaults to package, `@Query.name()` defaults to simple class name. Set `name` **explicitly** to AF4 string — never rely on default; AF4 name and class name almost never agree.
c. If AF4 payload was a bare scalar (`String`, enum, `Long`, …), introduce a record wrapper and annotate it.
d. **Coupled handler-side edit (in scope here, only when payload class changes).** When the AF4 payload was a bare scalar and a new record is introduced, the matching `@QueryHandler(queryName = "<name>")` method's parameter type must be updated to accept the new record — otherwise build breaks. Apply in same run even though `@QueryHandler` migration is normally another recipe; the change is inseparable.
   ```java
   // before — handler took the bare scalar
   @QueryHandler(queryName = "getStatus")
   public PaymentStatus getStatus(String paymentId) { ... }

   // after — handler takes the new record
   @QueryHandler(queryName = "getStatus")
   public PaymentStatus getStatus(GetPaymentStatusQuery query) {
       return ... query.paymentId() ...;
   }
   ```
   When AF4 payload was already a dedicated class, handler signature unchanged — only the payload class gains `@Query`.

### 4. Remove `ResponseType` wrappers — table

| AF4 | AF5 | Returns |
|---|---|---|
| `queryGateway.query(payload, R.class)` (already `Class` overload) | same — **import-only change** | `CompletableFuture<R>` |
| `queryGateway.query(payload, ResponseTypes.instanceOf(R.class))` | `queryGateway.query(payload, R.class)` | `CompletableFuture<R>` |
| `queryGateway.query(payload, ResponseTypes.optionalInstanceOf(R.class))` | `queryGateway.query(payload, R.class)` — future resolves to `null` if absent | `CompletableFuture<R>` (nullable) |
| `queryGateway.query(payload, ResponseTypes.multipleInstancesOf(R.class))` | `queryGateway.queryMany(payload, R.class)` | `CompletableFuture<List<R>>` |
| `queryGateway.query("name", payload, ResponseTypes.instanceOf(R.class))` | `queryGateway.query(payload, R.class)` + `@Query(name = "name")` on payload | `CompletableFuture<R>` |
| `queryGateway.query("name", payload, ResponseTypes.multipleInstancesOf(R.class))` | `queryGateway.queryMany(payload, R.class)` + `@Query(name = "name")` on payload | `CompletableFuture<List<R>>` |
| `queryGateway.query("name", payload, ResponseTypes.optionalInstanceOf(R.class))` | `queryGateway.query(payload, R.class)` + `@Query(name = "name")` on payload (future resolves `null` if absent) | `CompletableFuture<R>` (nullable) |
| `queryGateway.streamingQuery(payload, R.class)` | same — **import-only change** | `Publisher<R>` |
| `queryGateway.scatterGather(...)` | **REMOVED** — flag for user; no drop-in | — |

Notes:
- `query(...)` is **always single-response** in AF5. Multi-response → `queryMany(...)`.
- `ResponseType` / `ResponseTypes` SPI **gone**. Drop wrappers and static imports of `ResponseTypes.*`.
- **Custom `ResponseType` subclass** in the project → flag for user; per-callsite plan needed.

### 5. Subscription queries

AF4 `SubscriptionQueryResult` is **split** in AF5: `SubscriptionQueryResponseMessages` (bus, `Message`s) vs `SubscriptionQueryResponse<I, U>` (gateway, payloads). Top-of-chain callers see the gateway flavour.

Drop `ResponseType` wrappers — pass plain `Class<I>` / `Class<U>`. The gateway's `initialResult()` is a `Flux` (not `Mono`) because AF5 supports 0/1/N initial results uniformly.

```java
SubscriptionQueryResponse<R, U> resp = queryGateway.subscriptionQuery(payload, R.class, U.class);
Flux<R> initial = resp.initialResult();   // NOT Mono in AF5
Flux<U> updates = resp.updates();
```

If the AF4 caller assumed a single initial result, collapse with `.next()` / `.singleOrEmpty()` so behaviour stays compatible — flag for user when in doubt. Call `resp.cancel()` when done (project-specific lifecycle).

### 6. Scatter-gather — flag for user

```java
queryGateway.scatterGather(payload, …)
```

**Removed in AF5.** No drop-in replacement. Flag to user; user must redesign (typical replacement: a single broadcast query handler that aggregates internally, or a domain-specific approach).

### 7. Adapt surrounding method's return type

Four shapes:

- **Spring MVC controller** — return `CompletableFuture<R>` directly; Spring serves async out of the box. Most simple migrations end up unchanged here.
- **Reactive return** (`Mono` / `Flux`) — bridge with `Mono.fromFuture(future)` for `query(...)`, `Flux.from(publisher)` for `streamingQuery(...)`. Reactor extension survives in AF5 at `extension.reactor` (active migration, not removal).
- **Blocking caller** (CLI runner, integration test) — `query(...)` returns `CompletableFuture<R>`. Block with `future.orTimeout(<duration>, <unit>).join()`. **Never** bare `.join()` / `.get()`.
- **Synchronous framework callback** (MCP resource handler, `@KafkaListener`, `@JmsListener`, Camel route step) — callback signature requires synchronous return. Same rewrite as blocking caller: `.orTimeout(<d>, <u>).join()`. Pick a timeout consciously (often 30s default; shorter when framework has its own request budget). `CompletionException` from `.join()` is unchecked — existing `catch (Exception)` still matches.

### 8. Verify nothing else

- Stale imports — remove leftover `org.axonframework.queryhandling.*` AF4 imports.
- `ResponseTypes` static imports (`import static org.axonframework.messaging.responsetypes.ResponseTypes.*;`) — drop when no longer referenced.
- `QueryExecutionException` — FQN moved. Update if present.
- Try/catch on AF4 query-handling exceptions whose FQN moved.

Do not introduce abstractions or refactors that aren't required by the AF5 API change.

## Verify (against End condition)

```bash
./mvnw -f <target>/pom.xml -P migration-query-gateway-<ClassSimpleName> test-compile -DskipTests \
  -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
```

If integration test exists, prefer running it.

## Examples

See [examples/](examples/) — includes the named-query-via-`@Query` pattern.
