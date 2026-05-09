# Recipe `aggregate` — not-supported / blockers

**Read this file BEFORE running `## Procedure`.** Each blocker below has a Detection grep and an `AskUserQuestion` flow. The aggregate recipe must NOT silently rewrite around an unresolved blocker — wrong shape compiles cleanly and only fails at test time, so user input is required where AF5 has no portable target.

> 🚨 **DATA MIGRATION IS NOT IN SCOPE.** This skill rewrites **code only** (annotations, imports, handler shapes, test fixture). It does NOT migrate snapshots, event-store rows, or any other persisted aggregate state. Dropping `snapshotTriggerDefinition` only removes the **code config** — existing snapshot rows in storage are not deleted, rewritten, or re-read by this skill. Cleanup of stale snapshot data is the user's responsibility, out-of-band.

## How to use

1. Run every Detection grep below against the candidate aggregate class, its events, child entities, and the configuration referencing it.
2. For each blocker that fires:
   - Run the `AskUserQuestion` exactly as written.
   - Record the user's pick under Output `decisions.<key>`.
   - Apply "Effect on Procedure".
3. Only when every fired blocker has a recorded outcome → proceed to `## Procedure`.

## Blockers

### B1 — `snapshotTriggerDefinition` / `Snapshotter` / `SnapshotTriggerDefinition`

**Why blocker.** AF5's `@EventSourcedEntity` does not yet expose a finalized snapshot API. The `snapshotTriggerDefinition` attribute does NOT exist on `@EventSourced` — silently dropped during a naive rewrite. AF4 trigger types (`EventCountSnapshotTriggerDefinition`, `SpringAggregateSnapshotterFactoryBean`) have no AF5 rename target.

**Detection.**

```bash
grep -RnE 'snapshotTriggerDefinition|Snapshotter|SnapshotTriggerDefinition' \
     --include='*.java' --include='*.kt' <aggregate file> <aggregate package>
```

Also inspect the `@Aggregate` annotation directly — if it carries `snapshotTriggerDefinition = "..."`, this blocker fires.

**AskUserQuestion — choose one:**

- `accept-drop` — drop the attribute; user accepts no snapshotting until AF5 ships the API.
- `pause-migration` — stop; user removes/relocates snapshot config first.
- `remove-feature-first` — user deletes snapshot config now and will re-introduce later when AF5 has the API.

**Output decision key.** `snapshotting: <none | accept-drop | pause-migration | remove-feature-first>`

**Effect on Procedure.**
- `accept-drop` → proceed; do NOT carry `snapshotTriggerDefinition` over to `@EventSourced`.
- `pause-migration` → emit Output with `needs-user-decision=true`, exit.
- `remove-feature-first` → emit Output with `needs-user-decision=true`, exit; user will return after the cleanup commit.

Same surfacing applies to any `Snapshotter` / `SnapshotTriggerDefinition` reference reachable from this aggregate. Caching attributes on `@Aggregate` are similarly not portable — fold into this decision.

### B2 — Native (non-Spring) `EventSourcingConfigurer` wiring (Path B deferred)

**Why blocker.** Path B (programmatic Configuration API on a non-Spring project) is not implemented in this recipe yet. Annotation rewrites on the aggregate class are still safe, but configuration migration is out of scope.

**Detection.**

```bash
# Spring Boot present?
grep -RnE 'spring-boot-starter|@SpringBootApplication' \
     --include='pom.xml' --include='*.gradle*' --include='*.java' --include='*.kt' . 2>/dev/null

# Programmatic configurer in the project?
grep -RnE 'EventSourcingConfigurer|DefaultConfigurer\.defaultConfiguration\(\)|configurer\.componentRegistry' \
     --include='*.java' --include='*.kt' src 2>/dev/null
```

If first grep returns nothing AND second returns hits → blocker fires.

**AskUserQuestion — choose one:**

- `proceed-class-edits-only` *(Recommended)* — apply Steps 3–14 (annotation rewrites) only; do NOT touch configurer code; surface the configurer migration as a follow-up.
- `pause-migration` — stop; user migrates configuration manually first.

**Output decision key.** `non-spring-configurer: <none | proceed-class-edits-only | pause-migration>`

**Effect on Procedure.**
- `proceed-class-edits-only` → run Steps 3–14 + Step 2 variant addenda; skip Path A and Path B; emit Output noting the configurer slice was not touched.
- `pause-migration` → emit Output with `needs-user-decision=true`, exit.

Defer to the docs at <https://docs.axoniq.io/axon-framework-reference/5.1/migration/paths/aggregates/configuration-migration.html>.

### B3 — Map-typed `@AggregateMember` (multi-entity breaking change)

**Why blocker.** `Map<K, V>`-typed `@AggregateMember` collections are a breaking change in AF5 — `@EntityMember` does not support the same Map shape. Auto-rewriting silently re-keys the collection; safer to surface.

**Detection.**

```bash
grep -RnE '@AggregateMember[\s\S]{0,200}Map<' \
     --include='*.java' --include='*.kt' <aggregate file>
```

**AskUserQuestion — choose one:**

- `surface-and-defer` *(Recommended)* — emit Output noting Map-typed member; user redesigns to `List` / `Set` first, then re-runs recipe.
- `pause-migration` — stop; user redesigns now.

**Output decision key.** `map-typed-aggregate-member: <none | surface-and-defer | pause-migration>`

**Effect on Procedure.** Either path → emit Output with `needs-user-decision=true`, exit. No edits.

### B4 — `SagaTestFixture` on the aggregate's test class

**Why blocker.** `SagaTestFixture` has no AF5 replacement. Surfacing prevents silent rewrites of saga tests using the aggregate's primary test path.

**Detection.**

```bash
grep -RnE 'SagaTestFixture' \
     --include='*.java' --include='*.kt' <test class>
```

**AskUserQuestion — choose one:**

- `surface-and-skip-test` *(Recommended)* — leave the saga test on AF4 deps; record skip; recipe migrates the aggregate but not this test.
- `pause-migration` — stop.

**Output decision key.** `saga-test-fixture-flagged: <none | surface-and-skip-test | pause-migration>`

**Effect on Procedure.**
- `surface-and-skip-test` → run aggregate steps; skip T.1–T.5 for this test class.
- `pause-migration` → emit Output with `needs-user-decision=true`, exit.
