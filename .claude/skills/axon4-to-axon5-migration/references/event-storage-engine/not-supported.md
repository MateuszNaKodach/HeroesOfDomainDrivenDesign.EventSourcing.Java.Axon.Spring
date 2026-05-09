# Recipe `event-storage-engine` — not-supported / blockers

**Read this file BEFORE running `## Procedure`.** Each blocker below has a Detection grep and an `AskUserQuestion` flow. If any blocker fires and the user does not pick a path that maps to A/B/C, exit the recipe with `needs-user-decision=true` — never auto-rewrite a bean swap when a blocker is unresolved.

> 🚨 **DATA MIGRATION IS NOT IN SCOPE.** This skill rewrites **code only** (bean wiring, imports, annotations, Path-A SQL **schema** for the AF5 table shape). Moving the actual event log / token data between stores (Mongo → AS, Mongo → relational, JDBC → JPA, …) is a **separate, out-of-band project the user owns**. The recipe will NOT export, copy, transform, or re-replay event data. Every `move-to-*` option below is **a code-rewrite choice, not a data-migration offer**. If the user has not planned and rehearsed the data move on a non-prod environment, do NOT pick a `move-to-*` option — pause-migration instead.

## How to use

1. Run every Detection grep below from the target root.
2. For each blocker that fires:
   - Run the listed `AskUserQuestion` exactly as written.
   - Record the user's pick under Output `decisions.<key>`.
   - Apply the "Effect" — proceed, redirect to a different Path, exit, or accept-and-skip-bean-swap.
3. Only when every fired blocker has a recorded outcome → proceed to `## Procedure`.

## Blockers

### B1 — `MongoEventStorageEngine` (no AF5 release of `axon-mongo`)

**Why blocker.** No AF5 release of `axon-mongo`. No AF5 equivalent of `MongoEventStorageEngine`. OpenRewrite recipes do not cover Mongo package renames. Spring Boot autoconfigure (`axon-mongo-spring-boot-autoconfigure`) pulls AF4 transitives. Mongo-backed event store conflicts directly with AF5 paths A/B/C — there is no Mongo Path D.

**Detection.**

```bash
grep -RnE 'MongoEventStorageEngine|org\.axonframework\.extensions\.mongo|axon-mongo' \
     --include='*.java' --include='*.kt' --include='pom.xml' --include='*.gradle*' . 2>/dev/null
```

**AskUserQuestion — choose one:**

- `move-to-axon-server` — **code-rewrite only — Path B.** This skill will NOT migrate event data from Mongo to Axon Server. User MUST plan, run, and verify Mongo→AS data migration **out-of-band, before deploying the AF5 build**. Without that, the AF5 app starts against an empty Axon Server.
- `move-to-jpa` — **code-rewrite only — Path A.** This skill will NOT migrate event data from Mongo to a relational DB. User MUST plan, run, and verify Mongo→relational data migration **out-of-band**. The Path A.4 SQL handles only the AF5 schema/columns shape on the relational side; it does NOT read Mongo.
- `pause-migration` — stop; user replaces Mongo with a supported store (incl. data migration) before resuming.
- `accept-stays-af4` — keep the event-store slice on AF4 deps; recipe exits with `needs-user-decision=true`.

**Output decision key.** `mongo-event-store: <none | move-to-axon-server | move-to-jpa | pause-migration | accept-stays-af4>`

**Effect on Procedure.**
- `move-to-axon-server` → Path B (code rewrite). Add a learnings line: *"User accepts Mongo→Axon Server data migration is their responsibility, out-of-band."*
- `move-to-jpa` → Path A (code rewrite + AF5 schema SQL). Add a learnings line: *"User accepts Mongo→relational data migration is their responsibility, out-of-band; Path-A SQL only shapes the AF5 table, does not read Mongo."*
- `pause-migration` / `accept-stays-af4` → emit Output, exit. No bean swap. No SQL.

### B2 — `JdbcEventStorageEngine` (no AF5 drop-in equivalent)

**Why blocker.** AF5 has no `JdbcEventStorageEngine` yet. Don't write a custom AF5 JDBC engine inside a migration run.

**Detection.**

```bash
grep -RnE 'JdbcEventStorageEngine' \
     --include='*.java' --include='*.kt' src 2>/dev/null
```

**AskUserQuestion — choose one:**

- `move-to-jpa` — **code-rewrite only — Path A.** This skill will NOT migrate event data from the JDBC table layout to the AF5 JPA schema for you beyond the Path A.4 column-rename SQL — and even that SQL **operates on the rows already in the AF4 table**, it does not extract or reshape data from a different store. Verify schema and row counts before/after on a non-prod copy of the DB.
- `move-to-axon-server` — **code-rewrite only — Path B.** This skill will NOT migrate event data from the JDBC store to Axon Server. User MUST plan, run, and verify JDBC→AS data migration **out-of-band, before deploying the AF5 build**.
- `defer-until-af5-jdbc` — stop; wait until AF5 ships a JDBC equivalent.

**Output decision key.** `jdbc-event-store: <none | move-to-jpa | move-to-axon-server | defer-until-af5-jdbc>`

**Effect on Procedure.**
- `move-to-jpa` → Path A (code rewrite + AF5 schema SQL on the existing rows).
- `move-to-axon-server` → Path B (code rewrite). Add a learnings line: *"User accepts JDBC→Axon Server data migration is their responsibility, out-of-band."*
- `defer-until-af5-jdbc` → emit Output with `needs-user-decision=true`, exit.

### B3 — Custom `EventStorageEngine` subclass

**Why blocker.** Project subclasses `EventStorageEngine` (or one of its AF4 implementations) for custom storage / encryption / multitenancy. Reimplementation on top of `AggregateBased*EventStorageEngine` is out of scope for one atomic invocation.

**Detection.**

```bash
grep -RnE 'extends\s+(JpaEventStorageEngine|JdbcEventStorageEngine|MongoEventStorageEngine|AbstractEventStorageEngine|BatchingEventStorageEngine|AxonServerEventStore)\b' \
     --include='*.java' --include='*.kt' src 2>/dev/null
```

**AskUserQuestion — choose one:**

- `surface-and-defer` *(Recommended)* — open a follow-up issue; recipe exits without bean swap.
- `pause-migration` — stop; user removes custom subclass first.

**Output decision key.** `custom-storage-engine-subclass: <none | surface-and-defer | pause-migration>`

**Effect on Procedure.** Either path → emit Output with `needs-user-decision=true`, exit. No bean swap.

### B4 — Custom `Serializer` (NOT a hard blocker, but surface)

**Why surface (soft blocker).** AF5 replaces `Serializer` with `Converter` / `EventConverter`. Jackson/XStream defaults port automatically. **Subclassed serializers, custom `RevisionResolver`, custom `ContentTypeConverter`** do not — flag for follow-up.

**Detection.** During Path A/B/C inspection: any `@Bean Serializer` / `XStreamSerializer` / `JacksonSerializer` subclass / custom `RevisionResolver`.

**Action.** Don't auto-port. Add a line to Output `decisions.serializer-ports-flagged: [<list>]` so the orchestrator records it in `learnings.md` for stabilization.

**Output decision key.** `serializer-ports-flagged: [<class FQNs>] | "none"`
