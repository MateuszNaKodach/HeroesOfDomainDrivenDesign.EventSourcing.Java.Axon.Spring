# Axon Server command result interop probe

A runnable probe for one question: when the node that dispatches a command and the node that handles it run
different Axon Framework / Axoniq Framework versions, and the command travels through Axon Server, does the
dispatcher still read the command result correctly?

It was written for the regression in which `resultAs(Void.class)` fails on the dispatcher for a command handler
that returns nothing (`ConversionException` from `byte[]` to `java.lang.Void`, caused by
`MismatchedInputException: No content to map due to end-of-input`). The probe checks four kinds of results for
every pair of variants against an `EXPECTED` table encoded in `run-matrix.sh`, and the script exits non-zero on
any mismatch.

This folder is a standalone Maven project. The application's root `pom.xml` has no `<modules>`, so the main build
never builds it.

## Prerequisites

- Docker, with the image `axoniq/axonserver:2025.2.7` (or any image passed in `AXONSERVER_IMAGE`).
- JDK 21 or newer on the `PATH` (or `JAVA=/path/to/java`), and `curl`.
- Two artifacts that only exist in the local Maven repository (always build with `-nsu`, never `-U`):
  - the fixed connector `io.axoniq.framework:axon-server-connector:5.4.1-SNAPSHOT` for `v541fix`. It is not
    published. Build it from the fix commit without touching existing checkouts, and always with `-Drevision`, so
    the released 5.4.0 jars in `~/.m2` stay untouched:

    ```bash
    git -C ~/GitRepos/AxonIQ/AxoniqFramework worktree add --detach /tmp/axoniq-fix 5ed57f713e
    (cd /tmp/axoniq-fix && ./mvnw -Drevision=5.4.1-SNAPSHOT -pl connector/axon-server-connector -am install -DskipTests)
    git -C ~/GitRepos/AxonIQ/AxoniqFramework worktree remove /tmp/axoniq-fix
    ```

  - the `5.3.0-SNAPSHOT` jars of the local tracing build the application ran on before the 5.4.0 upgrade, for
    `v530snapshot` (`org/axonframework/*/5.3.0-SNAPSHOT` and `io/axoniq/framework/*/5.3.0-SNAPSHOT`).

  Without them, leave the variant out (see `HANDLERS`, `DISPATCHERS` below).

## How to run

```bash
compat-probes/axon-server-command-interop/run-matrix.sh
```

It works from bash 3.2 (macOS) or zsh. The script:

1. builds one jar per variant (skip with `SKIP_BUILD=true`),
2. starts a throwaway Axon Server in dev mode on random host ports, initialises it with
   `POST /v2/cluster/init` and waits for the `default` context,
3. for every handler variant H of the full matrix: starts H, waits until Axon Server lists its four command
   handlers (`/v1/public/command-metrics`), runs every dispatcher variant D against it, stops H and waits until its
   handlers are gone; then does the same for each same-version pair (D = H),
4. prints the matrix (rows D, columns H, one table per case) and the same-version table, lists every mismatch with
   expected and actual value, and exits 1 on any mismatch,
5. always removes the container and stops running nodes on exit (`trap`).

Node logs and the raw `PROBE_*` lines of every pair land in `target/matrix/`.

| Variable             | Default                                       | Meaning                                         |
|----------------------|-----------------------------------------------|-------------------------------------------------|
| `HANDLERS`           | `v530snapshot v533 v540 v541fix`              | handler variants of the full matrix (needs v540) |
| `DISPATCHERS`        | `v530snapshot v533 v540 v541fix v540workaround` | dispatcher variants of the full matrix (needs v540) |
| `SAME_VERSION_PAIRS` | `v530 v531 v532`                              | variants only run against themselves            |
| `AXONSERVER_IMAGE`   | `axoniq/axonserver:2025.2.7`                  | Axon Server image                               |
| `SKIP_BUILD`         | `false`                                       | reuse the jars in `target/<variant>/`           |
| `REPORT_ONLY`        | `false`                                       | re-check the last run's results only            |
| `MVN`, `JAVA`        | repository `mvnw`, `java`                     | tools                                           |
| `DISPATCH_TIMEOUT`   | `120`                                         | seconds one dispatcher run may take             |

`v540` is required in both full-matrix lists, because its same-version pair is the baseline of the failure case.

## The probe application

A Spring Boot 3.5 application with `io.axoniq.framework:axoniq-spring-boot-starter` (so commands go through Axon
Server) and the Jackson converter. The role is chosen with `probe.role`:

- `handler` registers command handlers for `VoidProbe` (returns nothing), `ValueProbe` (returns `"pong"`),
  `EmptyStringProbe` (returns `""`) and `FailProbe` (throws `IllegalStateException("probe failure")`).
- `dispatcher` registers no command handlers, so Axon Server must route to the handler node. It sends each command
  once, reads the result with `resultAs(Void.class)` or `resultAs(String.class)`, prints one line per case and
  exits:

  ```
  PROBE_RESULT|void|OK|null
  PROBE_RESULT|value|OK|"pong"
  PROBE_RESULT|failure|ERROR|org.axonframework.messaging.commandhandling.CommandExecutionException: java.lang.IllegalStateException: probe failure
  ```

  A "no handler" failure is retried for a few seconds (`PROBE_RETRY` lines), for the moment in which Axon Server
  has not seen the handler subscriptions yet. No retry was needed in the recorded runs.

Each node prints a `PROBE_VARIANT` banner with the `axon-messaging` and `axon-server-connector` jars it actually
loaded, and gets its own `spring.application.name`, component name and client id.

## Variants

All variants are built from the same sources, one Maven profile each. The profile sets the version properties;
`dependencyManagement` pins the framework BOM, `axon-server-connector` and `axoniq-distributed-messaging`
explicitly, and every variant gets its own build directory (`target/<variant>/`):

```bash
./mvnw -f compat-probes/axon-server-command-interop/pom.xml -nsu -P<variant> package -DskipTests
```

| Variant          | org.axonframework | io.axoniq.framework | axon-server-connector | Role in the run                |
|------------------|-------------------|---------------------|-----------------------|--------------------------------|
| `v530snapshot`   | 5.3.0-SNAPSHOT    | 5.3.0-SNAPSHOT      | 5.3.0-SNAPSHOT        | full matrix                    |
| `v530`           | 5.3.0             | 5.3.0               | 5.3.0                 | same-version pair              |
| `v531`           | 5.3.1             | 5.3.1               | 5.3.1                 | same-version pair              |
| `v532`           | 5.3.2             | 5.3.2               | 5.3.2                 | same-version pair              |
| `v533`           | 5.3.3             | 5.3.3               | 5.3.3                 | full matrix                    |
| `v540`           | 5.4.0             | 5.4.0               | 5.4.0                 | full matrix                    |
| `v541fix`        | 5.4.0             | 5.4.0               | 5.4.1-SNAPSHOT (fix)  | full matrix                    |
| `v540workaround` | 5.4.0             | 5.4.0               | 5.4.0                 | full matrix, dispatcher only   |

- `v530snapshot` resolves, with `-nsu`, to the locally installed `axon-messaging-5.3.0-SNAPSHOT.jar` (the app's
  tracing build) and `axon-server-connector-5.3.0-20260805.123232-36`.
- `v541fix` keeps `axoniq-distributed-messaging` at 5.4.0, so the connector is the only difference to `v540`.
- `v540workaround` bakes `probe.workaround.enabled=true` into the jar. That registers a copy of the application's
  `AxonServerVoidCommandResultWorkaroundConfiguration` dispatch interceptor as of application commit `3798145`: a
  result typed `java.lang.Void` with an empty `byte[]` payload becomes a `null` payload. It only acts on the
  dispatching side, so it is a dispatcher-only variant.

## How the expectations are derived

`value` must be `"pong"` and `emptyString` must be `""` for every pair. `failure` must equal the 5.4.0 same-version
pair (`CommandExecutionException: java.lang.IllegalStateException: probe failure`) everywhere.

`void` follows from two properties of each variant, both read from the framework sources:

1. **What the handling side sends for a void command handler.**
   - No payload: 5.3.0, 5.3.1 and the 5.3.0-SNAPSHOT build. Their `SimpleCommandBus` completes a handler without
     result with a `null` `CommandResultMessage`, and `CommandConverter#convertResultMessage` sends a
     `CommandResponse` without payload for a `null` message.
   - A payload typed `java.lang.Void` with empty data: 5.3.2 and later, including the fix. Axon Framework commit
     `4b971815a1` ("Ensure CommandResultMessage is never null", 5.3.2, the 5.3.x backport of `faaff07d36`) makes
     `SimpleCommandBus` return `GenericCommandResultMessage(new MessageType(Void.class), null)`. The connector
     writes that with the type name and `Objects.requireNonNullElse(payload, new byte[0])`. The connector's
     `CommandConverter` is byte-identical between 5.3.1 and 5.3.3; the change in behavior comes from the framework.
2. **Whether the dispatching side reads that typed empty Void payload as "no result".**
   - No: every released connector and the snapshot only treat a payload without type as "no result", hand
     `byte[0]` to `resultAs(Void.class)`, and Jackson fails.
   - Yes: the fix (an empty type, or empty data typed `java.lang.Void`, is a `null` payload) and the workaround.

`void` fails exactly when the handler sends the typed empty Void payload and the dispatcher does not tolerate it.

## How the expectations changed

The expectations were written down before the first run (commit `07e1d22`), and only changed after the root cause
of a difference was found:

1. **void, H=v533 with released dispatchers: expected OK, got ERROR.** The prior assumed every 5.3.x handler sends
   no payload for a void result. That only holds up to 5.3.1 (see above). The same-version pairs confirm it:
   5.3.0 and 5.3.1 pass, **5.3.2 is the first release that fails**. The app never saw the failure before the
   upgrade because it ran on the local 5.3.0-SNAPSHOT build, which predates `4b971815a1`: its handler sends no
   payload, so every dispatcher reads it as `null`.
2. **emptyString through the workaround: expected `""`, got `null`.** The Jackson converter converts a `String`
   result to `byte[]` through `StringToByteArrayConverter`, so `""` travels as a payload typed
   `java.lang.String` with empty data. The first version of the workaround mapped every empty `byte[]` to `null`
   without looking at the type. Result of that run (rows D, columns H):

   ```
   case: emptyString
     D \ H           | v533                              | v540                              | v541fix
     v540workaround  | OK null                           | OK null                           | OK null
   ```

   The application narrowed the workaround to results typed `java.lang.Void` (application commit `3798145`), the
   probe copy followed (commit `3dec1d3`), and `""` now stays `""` through the workaround.
3. **The fix changed from handler-and-dispatcher to dispatcher-only.** The first fix (`e286c7a9e6`) also stopped
   the handler from sending a payload for a void result, so a fixed handler worked with every released
   dispatcher. The final fix (`5ed57f713e`, production change in `4059420359`) keeps the released wire format on
   the handling side byte for byte and only makes the dispatcher tolerant. A fixed handler is therefore expected
   to fail with released dispatchers, exactly like a released handler, and a fixed dispatcher works with every
   handler.

## What it means for a rolling upgrade

- With released versions only, void command results over Axon Server fail for every pair whose handler runs 5.3.2
  or later, whatever the dispatcher version.
- With the fix, the dispatcher decides: a fixed dispatcher (or one with the narrowed workaround) reads void results
  from every handler version, including released and snapshot ones. Released dispatchers stay broken against 5.3.2+
  handlers until they are upgraded too.
- Non-void results and failures are unaffected in every pair.

## Last run

Date: 2026-10-12, 17:52 CEST. Fix: AxoniqFramework branch `MateuszNaKodach/fix-void-command-result-wire-goldens`
at `5ed57f713e9aae324936ae9c47f32acc7001297b`. Workaround copy: application commit `3798145`. Axon Server
`axoniq/axonserver:2025.2.7`. Script exit code 0.

```
Command result interop matrix (rows: dispatcher D, columns: handler H)
Axon Server image: axoniq/axonserver:2025.2.7

case: void
  D \ H           | v530snapshot                      | v533                              | v540                              | v541fix
  v530snapshot    | OK null                           | ERROR ConversionException         | ERROR ConversionException         | ERROR ConversionException
  v533            | OK null                           | ERROR ConversionException         | ERROR ConversionException         | ERROR ConversionException
  v540            | OK null                           | ERROR ConversionException         | ERROR ConversionException         | ERROR ConversionException
  v541fix         | OK null                           | OK null                           | OK null                           | OK null
  v540workaround  | OK null                           | OK null                           | OK null                           | OK null

case: value
  D \ H           | v530snapshot                      | v533                              | v540                              | v541fix
  v530snapshot    | OK "pong"                         | OK "pong"                         | OK "pong"                         | OK "pong"
  v533            | OK "pong"                         | OK "pong"                         | OK "pong"                         | OK "pong"
  v540            | OK "pong"                         | OK "pong"                         | OK "pong"                         | OK "pong"
  v541fix         | OK "pong"                         | OK "pong"                         | OK "pong"                         | OK "pong"
  v540workaround  | OK "pong"                         | OK "pong"                         | OK "pong"                         | OK "pong"

case: emptyString
  D \ H           | v530snapshot                      | v533                              | v540                              | v541fix
  v530snapshot    | OK ""                             | OK ""                             | OK ""                             | OK ""
  v533            | OK ""                             | OK ""                             | OK ""                             | OK ""
  v540            | OK ""                             | OK ""                             | OK ""                             | OK ""
  v541fix         | OK ""                             | OK ""                             | OK ""                             | OK ""
  v540workaround  | OK ""                             | OK ""                             | OK ""                             | OK ""

case: failure
  D \ H           | v530snapshot                      | v533                              | v540                              | v541fix
  v530snapshot    | ERROR CommandExecutionException   | ERROR CommandExecutionException   | ERROR CommandExecutionException   | ERROR CommandExecutionException
  v533            | ERROR CommandExecutionException   | ERROR CommandExecutionException   | ERROR CommandExecutionException   | ERROR CommandExecutionException
  v540            | ERROR CommandExecutionException   | ERROR CommandExecutionException   | ERROR CommandExecutionException   | ERROR CommandExecutionException
  v541fix         | ERROR CommandExecutionException   | ERROR CommandExecutionException   | ERROR CommandExecutionException   | ERROR CommandExecutionException
  v540workaround  | ERROR CommandExecutionException   | ERROR CommandExecutionException   | ERROR CommandExecutionException   | ERROR CommandExecutionException

Same-version pairs (D = H)
  variant         | void                              | value                             | emptyString                       | failure
  v530            | OK null                           | OK "pong"                         | OK ""                             | ERROR CommandExecutionException
  v531            | OK null                           | OK "pong"                         | OK ""                             | ERROR CommandExecutionException
  v532            | ERROR ConversionException         | OK "pong"                         | OK ""                             | ERROR CommandExecutionException

Variants (from the PROBE_VARIANT banner of each node):
  PROBE_VARIANT|v530snapshot|role=dispatcher|workaround=false|messaging=axon-messaging-5.3.0-SNAPSHOT.jar|connector=axon-server-connector-5.3.0-SNAPSHOT.jar
  PROBE_VARIANT|v530snapshot|role=handler|workaround=false|messaging=axon-messaging-5.3.0-SNAPSHOT.jar|connector=axon-server-connector-5.3.0-SNAPSHOT.jar
  PROBE_VARIANT|v533|role=dispatcher|workaround=false|messaging=axon-messaging-5.3.3.jar|connector=axon-server-connector-5.3.3.jar
  PROBE_VARIANT|v533|role=handler|workaround=false|messaging=axon-messaging-5.3.3.jar|connector=axon-server-connector-5.3.3.jar
  PROBE_VARIANT|v540|role=dispatcher|workaround=false|messaging=axon-messaging-5.4.0.jar|connector=axon-server-connector-5.4.0.jar
  PROBE_VARIANT|v540|role=handler|workaround=false|messaging=axon-messaging-5.4.0.jar|connector=axon-server-connector-5.4.0.jar
  PROBE_VARIANT|v541fix|role=dispatcher|workaround=false|messaging=axon-messaging-5.4.0.jar|connector=axon-server-connector-5.4.1-SNAPSHOT.jar
  PROBE_VARIANT|v541fix|role=handler|workaround=false|messaging=axon-messaging-5.4.0.jar|connector=axon-server-connector-5.4.1-SNAPSHOT.jar
  PROBE_VARIANT|v540workaround|role=dispatcher|workaround=true|messaging=axon-messaging-5.4.0.jar|connector=axon-server-connector-5.4.0.jar
  PROBE_VARIANT|v530|role=dispatcher|workaround=false|messaging=axon-messaging-5.3.0.jar|connector=axon-server-connector-5.3.0.jar
  PROBE_VARIANT|v530|role=handler|workaround=false|messaging=axon-messaging-5.3.0.jar|connector=axon-server-connector-5.3.0.jar
  PROBE_VARIANT|v531|role=dispatcher|workaround=false|messaging=axon-messaging-5.3.1.jar|connector=axon-server-connector-5.3.1.jar
  PROBE_VARIANT|v531|role=handler|workaround=false|messaging=axon-messaging-5.3.1.jar|connector=axon-server-connector-5.3.1.jar
  PROBE_VARIANT|v532|role=dispatcher|workaround=false|messaging=axon-messaging-5.3.2.jar|connector=axon-server-connector-5.3.2.jar
  PROBE_VARIANT|v532|role=handler|workaround=false|messaging=axon-messaging-5.3.2.jar|connector=axon-server-connector-5.3.2.jar

RESULT: PASSED (all cells match the expectations)
```
