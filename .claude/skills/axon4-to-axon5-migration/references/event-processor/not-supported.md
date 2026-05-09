# Recipe `event-processor` — not-supported / blockers

**Read this file BEFORE running `## Procedure`.** Each blocker below has a Detection grep and an `AskUserQuestion` flow. If a blocker fires and the user does not pick a path that maps onto AF5, exit with `needs-user-decision=true` — never silently rewrite around an unresolved blocker.

> 🚨 **DATA MIGRATION IS NOT IN SCOPE.** This skill rewrites **code only** (annotations, imports, dispatcher wiring, processor-bound config). Token-store rows, DLQ entries, and any other persisted state owned by event processors are NOT migrated by this skill. Token data is usually rebuildable by replay (full replay rewrites all tokens), so a `move-to-jpa-token-store` choice is a **code-rewrite + replay-from-event-store** plan — **not** a Mongo→relational data export. The user owns and runs the replay (or any out-of-band copy) themselves.

## How to use

1. Run every Detection grep below from the target root, scoped to the candidate class and the configuration tied to its processing group.
2. For each blocker that fires:
   - Run the `AskUserQuestion` exactly as written.
   - Record the user's pick under Output `decisions.<key>`.
   - Apply "Effect on Procedure".
3. Only when every fired blocker has a recorded outcome → proceed to `## Procedure`.

## Blockers

### B1 — `MongoTokenStore` (no AF5 release of `axon-mongo`)

**Why blocker.** No AF5 release of `axon-mongo`; no AF5 `MongoTokenStore`. Token data is per-processor and can usually be rebuilt by replay from the event store, so a switch is feasible — but it MUST be a deliberate user decision.

**Detection.**

```bash
grep -RnE 'MongoTokenStore|registerTokenStore.*Mongo|org\.axonframework\.extensions\.mongo' \
     --include='*.java' --include='*.kt' --include='*.yml' --include='*.yaml' --include='*.properties' \
     <project root> 2>/dev/null
```

Also detect via the standard processor-group sweep (Procedure step 2 in the main file).

**AskUserQuestion — choose one:**

- `move-to-jpa-token-store` — **code-rewrite only — switch the bean to AF5 `JpaTokenStore`.** This skill will NOT copy token rows from Mongo to the new JPA table. Token data is rebuilt by **replay from the event store**, which the user runs themselves (and verifies against expected projection state). If the project cannot tolerate a replay (large event log, side-effecting projections, idempotency concerns), pick `pause-migration` instead.
- `pause-migration` — stop; user replaces token store (incl. data plan) before resuming.
- `accept-stays-af4` — keep the token-store slice on AF4 deps; recipe exits with `needs-user-decision=true`.

**Output decision key.** `mongo-token-store: <none | move-to-jpa-token-store | pause-migration | accept-stays-af4>`

**Effect on Procedure.**
- `move-to-jpa-token-store` → proceed; surface for the write-configuration recipe to replace the bean. Add a learnings line: *"User accepts replay-from-event-store as the token-data plan; this skill does NOT copy token rows."*
- `pause-migration` / `accept-stays-af4` → emit Output, exit. No code change in this class.

### B2 — Saga handler in candidate (`@SagaEventHandler` / `@StartSaga` / `@EndSaga`)

**Why blocker.** Sagas have no automatic AF5 rewrite. Workflow support is on the Axoniq roadmap. The candidate is a saga, not an event-processor — wrong recipe.

**Detection.**

```bash
grep -RlnE '@SagaEventHandler\b|@StartSaga\b|@EndSaga\b|@Saga\b' \
     --include='*.java' --include='*.kt' <candidate file>
```

**AskUserQuestion — choose one:**

- `wrong-recipe-skip` *(Recommended)* — orchestrator routes this candidate to the (unsupported) saga slot; this recipe exits without touching the file.
- `pause-migration` — stop; user removes saga before resuming.

**Output decision key.** `saga-handler-detected: <none | wrong-recipe-skip | pause-migration>`

**Effect on Procedure.** Either path → emit Output with `needs-user-decision=true`, exit. No edits to the candidate.

### B3 — `axon-kafka` extension (no AF5 release)

**Why blocker.** No AF5 release of `axon-kafka`. AF5's `EventBus` and streaming abstractions changed shape — `KafkaPublisher`, `StreamableKafkaMessageSource`, `KafkaMessageSourceConfigurer`, `KafkaProperties`, `axon-kafka-spring-boot-autoconfigure` all reference AF4-only APIs. Even a manual rewrite is non-trivial; there is no automatic translation path. Affects publication side (`KafkaPublisher`) and the consumption side that wires the processor's `messageSource` to a `StreamableKafkaMessageSource`.

**Detection.**

```bash
grep -RnE 'org\.axonframework\.extensions\.kafka|axon-kafka|KafkaPublisher|StreamableKafkaMessageSource|KafkaMessageSourceConfigurer' \
     --include='*.java' --include='*.kt' --include='*.yml' --include='*.yaml' --include='*.properties' --include='pom.xml' --include='*.gradle*' \
     <project root> 2>/dev/null
```

Also detect via the standard processor-group sweep (Procedure step 2 in the main file) — Kafka is most often wired per-processor via a `messageSource(...)` callback or `KafkaMessageSourceConfigurer`.

**AskUserQuestion — choose one:**

- `accept-stays-af4` — Kafka slice stays on AF4 deps; the modules touching `axon-kafka` will not compile against AF5; user accepts that scope is pinned. Recipe exits without touching this candidate.
- `pause-migration` — stop; user replaces Kafka integration (e.g. native Kafka client + custom `EventBus` adapter, or move publication to Axon Server) before resuming.
- `remove-feature-first` — user agrees to delete the Kafka wiring now and re-introduce a non-Axon Kafka integration later.

**Output decision key.** `axon-kafka: <none | accept-stays-af4 | pause-migration | remove-feature-first>`

**Effect on Procedure.** Any non-`none` choice → emit Output with `needs-user-decision=true`, exit. No edits to this candidate. Add a learnings line: *"User accepts axon-kafka has no AF5 path; Kafka slice is the user's responsibility, out-of-band."*

> 🚨 **No data migration.** Switching off `axon-kafka` does not migrate, replay, or re-publish any messages already on Kafka topics. Topic state is untouched by this skill — replay / catch-up is the user's responsibility.
