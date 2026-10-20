# Upgrade notes: Axon Framework / Axoniq Framework 5.3.0-SNAPSHOT to 5.4.0

Application commit hashes here refer to branch `MateuszNaKodach/upgrade-to-axon-5.4.0`, which was
squash-merged into `axon5/main` as `e99267c`; the branch stays on origin so the hashes resolve.

Upgrade date: 2026-10-12. All artifacts (`org.axonframework:*`, `org.axonframework.extensions.spring:*`,
`io.axoniq.framework:*`) come from Maven Central at `5.4.0` (GA, released the same day). The app no longer
depends on locally built tracing-branch snapshots, so `./mvnw -nsu` is not needed anymore.

Sources used: release notes of
[Axon Framework 5.4.0](https://github.com/AxonIQ/AxonFramework/releases/tag/axon-5.4.0) and
[Axoniq Framework 5.4.0](https://github.com/AxonIQ/axoniq-framework/releases/tag/axoniq-5.4.0)
(plus 5.3.x and 5.4.0-RC1), and the 5.4.0 sources jars.

## Result

| Step                                            | 5.3.0-SNAPSHOT (baseline) | 5.4.0, no code changes | 5.4.0 + workaround |
|-------------------------------------------------|---------------------------|------------------------|--------------------|
| `./mvnw test-compile`                           | OK                        | OK                     | OK                 |
| New deprecation warnings                        | -                         | 3 (see below)          | 3                  |
| `./mvnw test` (104 tests)                       | 104 pass                  | 102 pass, 2 errors     | 104 pass (119 with the tests added later) |
| Compatibility run, JPA event store              | -                         | OK                     | OK                 |
| Compatibility run, Axon Server aggregate-based  | -                         | REST 500 on commands   | OK                 |
| Compatibility run, Axon Server DCB              | -                         | REST 500 on commands   | OK                 |

Changes in this repository:

- `pom.xml`: `axon.version` / `axon5.version` set to `5.4.0`.
- `AxonServerVoidCommandResultWorkaroundConfiguration` (new): workaround for regression 1.
- Tests that keep the checks of this upgrade: the workaround unit and canary tests (regression 1), and
  `JpaEventStoreDataFromAxon530CompatibilityTest`, which starts the app on a JPA event store written by the
  pre-upgrade 5.3.0-SNAPSHOT build (fixture and its generator in `src/test/resources/compat/af-5.3.0-SNAPSHOT/`).
- `compat-probes/axon-server-command-interop`: mixed-version command result probe (see regression 1).

Runtime dependency set is unchanged apart from versions: `axonserver-connector-java` 2026.0.0 to 2026.1.0,
`entitlement-manager` 1.1.0 to 1.3.2, `tools.jackson` 3.2.1 to 3.2.3, `protobuf-java` 4.34.0 to 4.36.0.

## Regression 1: void command results fail over Axon Server (worked around)

What worked before: on 5.3.0-SNAPSHOT a command whose handler returns nothing, dispatched through Axon Server,
completed `commandGateway.send(...).resultAs(Void.class)` with `null`.

What happens on 5.4.0: the same call fails, although the command was handled and its events were stored:

```
org.axonframework.conversion.ConversionException: Exception when trying to convert object of type 'byte[]'
to 'java.lang.Void'
  at org.axonframework.conversion.jackson.JacksonConverter.convert(JacksonConverter.java:169)
  ...
  at org.axonframework.messaging.commandhandling.gateway.ConvertingCommandGateway$ConvertingCommandResult.lambda$resultAs$0(ConvertingCommandGateway.java:88)
Caused by: tools.jackson.databind.exc.MismatchedInputException: No content to map due to end-of-input
```

Affected: only the Axon Server profiles (`axonserver-aggregate`, `axonserver-dcb`), so every command sent through
the Axon Server command bus. The default JPA profile uses the local command bus and is not affected. In this app
that is `BuildDwellingRestApi`, `IncreaseAvailableCreaturesRestApi`, `RecruitCreatureRestApi`, the matching `*Mcp`
tools and `PaidCommandInterceptor`. Failing tests without the workaround (they use `axonserver-dcb`):
`RecruitCreatureRequiresResourcesTest.whenRecruitingCreature_thenProcessPaymentAndRecruitSuccessfully` and
`RecruitCreatureRequiresResourcesTest.givenInsufficientResources_whenRecruitingCreature_thenNoCreatureRecruitedEventStored`.

Observed in the compatibility run with the workaround switched off: `PUT .../available-creatures-increases`
returned HTTP 500, but the dwelling's available creatures went from 0 to 5. The command was applied, only the
result could not be read. A client that retries on that error applies the change twice.

Correction (same day, after capturing the wire format of the real released code): this is not new in 5.4.0.
Released versions fail the same way since **5.3.2**. The app's baseline was a local 5.3.0-SNAPSHOT build that
predates the cause, which is why it passed. The analysis below replaces the first version of this section.

| Version                      | Handler sends for a void result              | Dispatcher reads it as                   | `resultAs(Void.class)` |
|------------------------------|----------------------------------------------|------------------------------------------|------------------------|
| 5.3.0, 5.3.1, local 5.3.0-SNAPSHOT | no payload (no result message)         | null payload                             | `null`                 |
| 5.3.2, 5.3.3, 5.4.0          | payload typed `java.lang.Void`, empty data   | `byte[0]` payload typed `java.lang.Void` | fails                  |
| local fix (5.4.1-SNAPSHOT)   | unchanged: `java.lang.Void`, empty data      | null payload, type `java.lang.Void`      | `null`                 |

Cause:

- Axon Framework `faaff07d36` "Ensure CommandResultMessage is never null" (PR AxonIQ/AxonFramework#4994, `main`,
  merged 2026-09-02), backported to 5.3.x as `4b971815a1` (PR AxonIQ/AxonFramework#4999, first released in
  `axon-5.3.2`, not in `axon-5.3.1`): `SimpleCommandBus` turns a void handler result into a result message typed
  `java.lang.Void` with a `null` payload instead of no result message.
- Connector handling side, `CommandConverter#convertResultMessage`: writes that as a payload typed `java.lang.Void`
  with empty data (byte-identical in 5.3.3 and 5.4.0).
- Connector dispatching side, `CommandConverter#convertCommandResponse`: treats the response as "no result" only
  when the payload type is empty, so the empty data becomes a `byte[0]` payload and `resultAs(Void.class)` hands
  it to Jackson.
- Axoniq Framework `f2b25ad6a9` "Drop nullability for CommandResultMessage" (PR #270) only adapted the connector on
  `main` to the never-null result; it is not the origin for released versions.

Workaround: `AxonServerVoidCommandResultWorkaroundConfiguration` registers a command `MessageDispatchInterceptor`
(only when `axon.axonserver.enabled=true`) that maps a result typed `java.lang.Void` with an empty `byte[]`
payload back to a `null` payload. All `resultAs(Void.class)` call sites stay unchanged. The first version matched
on the empty data alone and also turned a real empty `String` result into `null` (Jackson serializes `""` to an
empty `byte[]`); the interop probe found it and commit `3798145` narrowed it to the `java.lang.Void` type. Switch it
off with `application.workarounds.axon-server-void-command-result.enabled=false`, and delete the class once a
framework release reads void results correctly (`AxonServerVoidCommandResultCanaryTest` fails at that point).

Mixed versions (rolling upgrades), proven with `compat-probes/axon-server-command-interop` (real jars of 5.3.0,
5.3.1, 5.3.2, 5.3.3, 5.4.0, the local 5.3.0-SNAPSHOT, the local fix and 5.4.0 with this workaround, over Axon
Server; see its README for the full matrix): any released dispatcher fails on void results from any 5.3.2+ or
fixed handler; the fixed dispatcher and this workaround read void results as `null` from every handler; value,
empty `String` and failure results are identical in every pair.

Upstream fix: https://github.com/AxonIQ/axoniq-framework/pull/637, merged to `main` (`d4268313d0`) and
backported to `axoniq-5.4.x` by https://github.com/AxonIQ/axoniq-framework/pull/641 for 5.4.1. Only the
dispatching side changes: it reads Void-typed empty data as `null`, keeping the wire format; wire samples of the
released 5.3.3 and 5.4.0 connectors are its tests. With it installed as `5.4.1-SNAPSHOT` and the workaround removed (branch
`MateuszNaKodach/connector-fix-5.4.1-check`, commit `93b62fb`), this app's full suite passes (104 tests); with the
released 5.4.0 connector, both `RecruitCreatureRequiresResourcesTest` tests fail again. Once 5.4.1 is released,
remove the workaround; `AxonServerVoidCommandResultCanaryTest` fails on that version to signal it.

Tests added for this regression: `EmptyCommandResultAsNullInterceptorTest` (workaround unit test, incl. the empty
`String` case) and `AxonServerVoidCommandResultCanaryTest` (workaround on: `null`; off: the upstream failure).

## Fixed between 5.4.0-RC1 and 5.4.0 (no action needed)

Two problems seen with 5.4.0-RC1 are gone in GA:

- `axon-server-connector` was an optional dependency of `axoniq-spring-boot-autoconfigure` in RC1, so the
  starter did not bring it in and `AxonServerEventStoreConfiguration` / `GrpcTracingConfiguration` did not compile.
  Fixed by Axoniq Framework PR #591, commit `498abdabe8a3` "Make the Spring Boot starter depend on the Axon Server
  connector".
- Injecting `UnitOfWorkFactory` by type (test helper `AggregateEventPublisher`) failed with "required a single
  bean, but 3 were found" after AF PR #4980 (`023718ba8aec`, timeout support) registered the
  `UnitOfWorkFactory[CommandBus]` / `UnitOfWorkFactory[QueryBus]` components. Fixed by AF PR #5112
  (`15153a9389ee`) "Prefer the type-level component when autowiring Axon components by type".

## Behavior changes without code changes

- Timeouts are on by default in Spring Boot (AF PR #4980, `AxonTimeoutAutoConfiguration`): message handlers
  30 s (warnings from 10 s, every 1 s), units of work 60 s for the command bus, query bus and event processors.
  5.3 had none. Handlers in this app are short, so nothing changes in practice. Tune with `axon.timeout.*` or turn
  off with `axon.timeout.enabled=false`.
- Licensing (`entitlement-manager` 1.3.2): without a license the app now logs that it got NON-PRODUCTION
  permissions for this machine. Before, it logged an ERROR saying the JVM would shut down after a 15-minute grace
  period.
- DCB mode logs once: "Axon Server does not support sourcing with snapshots in a single round trip. Falling back
  to loading the snapshot separately." That comes from Axoniq Framework PR #397 (`AxonServerEventStorageEngine`
  is now a `SnapshotStore`) and is a performance hint for older Axon Server versions. The local
  `axoniq/axonserver:latest` image the tests used is 2025.2.7; Docker Hub `latest` is 2026.1.6.

## Deprecations

`new InMemorySnapshotStore()` is deprecated since 5.4.0 in favor of `new InMemorySnapshotStore(Converter)`
(AF issue #4865, PR #4960). Used in `JpaEventStoreConfiguration`, `AxonServerEventStoreConfiguration` and
`DwellingTest`. Kept as is: it behaves exactly as in 5.3. Follow-up worth doing: the no-arg store keeps the
snapshot by reference, and `Dwelling` is mutable (public fields reassigned in `evolve`), which is the case the
deprecation warns about. Switching needs `Dwelling` to round-trip through Jackson, so it is a separate change.

## Release-note items checked and not affecting this app

- Processor token-store name no longer defaults to `tokenStore` (AF #4809): the app uses the autoconfigured
  `tokenStore` bean.
- JPA gap defaults aligned with core defaults (AF #4799): the app builds `AggregateBasedJpaEventStorageEngine`
  itself with core defaults, so nothing changes.
- Sequencing policy falling back to the event identifier when it resolves nothing (AF #4797): all processors
  use `MetadataSequencingPolicy` on `gameId`, which every event carries.

## Compatibility run (old data, new version)

Each scenario: start the 5.3.0-SNAPSHOT jar, create data through REST (dwellings, available creatures,
recruitments, which make the automation add creatures to an army), stop it, start the 5.4.0 jar on the same
stores and continue. Infrastructure: worktree compose Postgres, compose Axon Server 2026.0.0 for aggregate-based,
a throwaway Axon Server 2025.2.7 dev-mode container with a DCB context for DCB. Each scenario used its own Postgres
database so token types did not mix.

| Check                                                                 | JPA | AS aggregate | AS DCB |
|-----------------------------------------------------------------------|-----|--------------|--------|
| Tokens unchanged at 5.4 startup (same values, same `__config` id)     | yes | yes          | yes    |
| No reprocessing (no duplicate `CreatureAddedToArmy`, read model same) | yes | yes          | yes    |
| Pooled processors re-claim segments and continue from stored token   | yes | yes          | yes    |
| Entity sourced from 5.3 events enforces rules (rebuild, over-recruit) | yes | yes          | yes    |
| New events appended to 5.3 streams (`Dwelling`, `Army`)               | yes | yes          | yes    |
| Snapshot path (`@Snapshotting(afterEvents = 3)`) over old + new events| yes | yes          | yes    |
| DLQ-enabled automation reacts to new events                           | yes | yes          | yes    |
| Subscription query (`/watch`) streams updates                         | yes | yes          | yes    |
| Hibernate `ddl-auto=update` schema diff after 5.4 start               | none| -            | -      |
| Rollback: 5.3 started again on data and tokens written by 5.4         | yes | -            | -      |

Domain-rule rejections return HTTP 500 on both versions (no exception mapping in the REST layer), so that is
not a change.
