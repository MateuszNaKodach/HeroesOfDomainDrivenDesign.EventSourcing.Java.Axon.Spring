# Worked example: one app, 5.3.0-SNAPSHOT to 5.4.0

**This is an example, not part of the workflow.** It shows what each step produced for
one real application (an event-sourced Spring Boot sample with a JPA/Postgres event store
by default and two Axon Server profiles). Names, versions, ports and findings are
specific to that app and that release; do not assume them for another app. The full
upgrade notes of this example live in the app's repository as `docs/UPGRADE-AXON-5.4.0.md`.

## Step 1: detection

- Maven, one `pom.xml`, version properties `axon.version` and `axon5.version`, artifacts
  `axoniq-spring-boot-starter`, `axoniq-tracing-micrometer`, test artifacts
  `axon-spring-boot-starter-test`, `axon-test`, `axoniq-testcontainer`.
- Current `5.3.0-SNAPSHOT` (locally built feature-branch snapshots, so `-nsu` was needed
  for the baseline), target `5.4.0` (Maven Central `release`).
- Storage modes: default profile `axon.axonserver.enabled=false` with a custom
  `AggregateBasedJpaEventStorageEngine` bean; profile `axonserver-aggregate`
  (`AggregateBasedAxonServerEventStorageEngine`, classic context); profile
  `axonserver-dcb` (Axon Server's default DCB engine). Token store: autoconfigured JPA.
  Snapshot store: `InMemorySnapshotStore`. Pooled processors with DLQ for automations,
  one subscribing processor.

## Step 2: release notes

Read `axon-5.3.0-RC1` .. `axon-5.4.0` and `axoniq-5.3.0-RC1` .. `axoniq-5.4.0`. Items
that turned out to matter:

- AF #4980 timeouts reintroduced, on by default in Spring Boot (behavior change, kept).
- AF #4994 / axoniq commit `f2b25ad6a9`: `CommandResultMessage` never null (caused the
  regression below).
- AF #5112 prefer the type-level component when autowiring (fixed an RC1-only ambiguity).
- axoniq #591 starter depends on the Axon Server connector again (fixed an RC1-only
  compile failure).
- AF #4809 token-store name, #4799 JPA gap defaults, #4797 sequencing fallback: checked,
  not affecting (app wires its own engine, uses the default `tokenStore` bean, every event
  carries the sequencing key).

## Steps 3 and 4: baseline and bump

- Baseline: 104 tests, all green.
- After the bump: compile OK; 3 new deprecation warnings (`new InMemorySnapshotStore()`,
  replaced by a `Converter`-based constructor; kept, follow-up noted because the
  snapshotted entity is mutable); dependency diff showed the same artifact set with
  version moves only; 102 of 104 tests green.
- Log diff: new licensing messages (non-production permissions instead of a shutdown
  warning) and a DCB snapshot-sourcing performance hint.

## Step 5: the regression

- Symptom: `resultAs(Void.class)` on commands routed through Axon Server failed with
  `ConversionException ... 'byte[]' to 'java.lang.Void'` / `MismatchedInputException: No
  content to map due to end-of-input`.
- Cause: the connector's `CommandConverter` now sends a typed, empty payload for a `null`
  result, and the receiving side only treats an untyped payload as "no result".
- Origin: axoniq-framework commit `f2b25ad6a9` (PR #270), not in `axoniq-5.3.3`, first in
  `axoniq-5.4.0-RC1`, still on `main`.
- Runtime effect found in the compatibility run: HTTP 500 for the client, but the command
  was applied (retry would double-apply).
- Workaround: one `MessageDispatchInterceptor<CommandMessage>` bean, active only with
  Axon Server, mapping an empty `byte[]` result back to a `null` payload, switchable by a
  property. All call sites unchanged; 104 of 104 green.
- Not tested: mixed 5.3/5.4 nodes; by code reading, a 5.3 dispatcher would fail the same
  way on results from a 5.4 handler.

## Step 7: compatibility run

- JPA: compose Postgres, one database. Old jar created dwellings, recruitments, and the
  automation added creatures to an army; new jar on the same DB: tokens identical, no
  reprocessing, schema identical, rejections and new events correct, subscription query
  streamed. Rollback (old jar on new data) also fine.
- Axon Server aggregate-based: compose Axon Server (classic `default` context after
  `POST /v2/cluster/init`), separate database for tokens. Same checks green with the
  workaround; with it switched off the regression reproduced through the REST API.
- Axon Server DCB: the compose server (2026.0.0) ignored `dcb=true` on init and an
  unlicensed node could not create another context, so a throwaway dev-mode container
  (2025.2.7) with a DCB `default` context was used, separate database. Same checks green.

## Outcome

Two commits: the version bump together with the workaround (keeps the build green), and
the upgrade notes.
