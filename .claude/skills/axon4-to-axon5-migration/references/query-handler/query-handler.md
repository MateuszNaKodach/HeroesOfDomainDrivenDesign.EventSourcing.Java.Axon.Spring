# Recipe: `@QueryHandler` class

Atomic migration of ONE class that handles queries via methods annotated `@QueryHandler` — typically Spring `@Component` / `@Service` projection or read-model query handler.

## Goal

In the simple case, **import-only** change. Method bodies, parameter lists, return types, the `queryName` attribute, and Spring stereotypes are preserved as-is.

- `@QueryHandler` import: `org.axonframework.queryhandling.QueryHandler` → `org.axonframework.messaging.queryhandling.annotation.QueryHandler`.
- Any sibling AF4 query-handling import (`org.axonframework.queryhandling.*`) on the same class moves under `org.axonframework.messaging.queryhandling.*`.

## Inputs

- target: FQ class name of the `@QueryHandler` host (required)
- target_test: FQ test class name (optional)

## End condition

1. Zero compile errors in the class itself.
2. If the class has a test, scoped tests pass.

## Output

- target: <FQ class>
- decisions:
    - path: <A (Spring Boot) | B (non-Spring)>
- needs-user-decision: <true | false>
- needs-user-decision-reason: <text> (only when true)
- notes: optional

## Preflight

1. `@QueryHandler` already imported from AF5 location?
2. Compile clean?
3. If yes → STOP. `AskUserQuestion`: Skip / Deep verify.

> A class **without an explicit target** is NOT picked autonomously if its only query-handling import is already AF5 — there's no work, autonomous run produces no-op. With explicit target, no-op is still legitimate (recipe just verifies nothing was missed).

## In scope

ONE class with at least one method annotated `@QueryHandler` from AF4 location `org.axonframework.queryhandling.QueryHandler`, AND **NOT** also handling other message types via AF4 imports.

## Out of scope

- Class also carries AF4 `@CommandHandler` / `@EventHandler` — run their dedicated recipes first/instead.
  - Exception: if every other annotation is *already* on AF5 import (recipe-pre-migrated), there's nothing for sibling recipes to do; this recipe finishes the unit.

## FQN cheat sheet

| Element | AF4 | AF5 |
|---|---|---|
| `@QueryHandler` | `org.axonframework.queryhandling.QueryHandler` | `org.axonframework.messaging.queryhandling.annotation.QueryHandler` |
| Query-handling core pkg | `org.axonframework.queryhandling` | `org.axonframework.messaging.queryhandling` |
| `QueryExecutionException` | `org.axonframework.queryhandling.QueryExecutionException` | `org.axonframework.messaging.queryhandling.QueryExecutionException` |
| `MetaData` param type | `org.axonframework.messaging.MetaData` | `org.axonframework.messaging.core.Metadata` |
| `@MetaDataValue` param annot | `org.axonframework.messaging.annotation.MetaDataValue` | `org.axonframework.messaging.core.annotation.MetadataValue` |

Note rename: `MetaData` → `Metadata`, `@MetaDataValue` → `@MetadataValue` (capital `D` dropped).

## Procedure

### 1. Locate

If user named target, use it (even if already on AF5 — recipe will verify and close as no-op).

Otherwise:

```bash
grep -RlnE 'org\.axonframework\.queryhandling\.QueryHandler' \
     --include='*.java' --include='*.kt' <target>/src
```

### 2. Update import

`org.axonframework.queryhandling.QueryHandler` → `org.axonframework.messaging.queryhandling.annotation.QueryHandler`.

### 3. Update sibling AF4 query-handling imports

Move any other `org.axonframework.queryhandling.*` import on this class under `org.axonframework.messaging.queryhandling.*` matching the AF5 module reorganisation.

### 4. Preserve everything else

- Method bodies, parameter lists, return types — keep as-is.
- `queryName` attribute on `@QueryHandler` — keep as-is (same `queryName()` member in AF5).
- Return type / `ResponseType` — preserved; AF5 keeps the same first-parameter-is-payload contract.
- Spring stereotypes (`@Component`, `@Service`, `@RestController`) — keep as-is.
- Query payload type and its annotations — keep as-is.

### 5. Parameter resolution sweep

Parameter resolvers in AF5 still cover: `Metadata`, `@MetadataValue` params, the full `Message`, and the active `ProcessingContext` (AF5 replacement for AF4 `UnitOfWork` param).

- `MetaData` parameter type → rewrite import to `org.axonframework.messaging.core.Metadata` (and rename type ref).
- `@MetaDataValue("k") X x` parameter → rewrite annotation import to `org.axonframework.messaging.core.annotation.MetadataValue` and rename to `@MetadataValue`.
- `UnitOfWork` parameter → out of scope (flag for user — `ProcessingContext` migration is separate).

### 6. Verify nothing else

- `QueryExecutionException` try/catch — update FQN if present.
- Stale AF4 `org.axonframework.queryhandling.*` imports — remove.
- `QueryUpdateEmitter` (subscription-query) usage on the class — out of scope, flag for user.

## Verify (against End condition)

```bash
./mvnw -f <target>/pom.xml -P migration-query-handler-<ClassSimpleName> test-compile -DskipTests \
  -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
```

If class has test:

```bash
./mvnw -f <target>/pom.xml -P migration-query-handler-<ClassSimpleName> test \
  -Dtest='<FQTestClass>' \
  -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
```

## Examples

See [examples/](examples/).
