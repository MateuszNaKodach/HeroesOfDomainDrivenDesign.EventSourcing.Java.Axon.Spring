# Worked example: void command results over Axon Server (Axoniq Framework 5.4.0)

EXAMPLE ONLY. This records one real run of the workflow so the steps have a concrete shape. Do
not copy its class names, versions or fix into another bug.

## 1. Symptom (from the app)

- App: Spring Boot 3.5 sample app, upgraded from 5.3.0-SNAPSHOT to 5.4.0. Found by the
  `axon-update` skill.
- Failing tests: `RecruitCreatureRequiresResourcesTest` (2 tests, profile `axonserver-dcb`).
  Only profiles that route commands through Axon Server; the local command bus is fine.
- Exception, from `commandGateway.send(cmd).resultAs(Void.class)`:

```
org.axonframework.conversion.ConversionException: Exception when trying to convert object of type 'byte[]'
to 'java.lang.Void'
  at org.axonframework.conversion.jackson.JacksonConverter.convert(JacksonConverter.java:169)
  ...
  at org.axonframework.messaging.commandhandling.gateway.ConvertingCommandGateway$ConvertingCommandResult.lambda$resultAs$0(ConvertingCommandGateway.java:88)
Caused by: tools.jackson.databind.exc.MismatchedInputException: No content to map due to end-of-input
```

- Impact: the command was applied (events stored) but the caller got an error; a REST client
  saw HTTP 500 and could retry, applying the change twice.
- Workaround in the app: a command `MessageDispatchInterceptor` mapping an empty `byte[]` result
  payload to `null`, toggle `application.workarounds.axon-server-void-command-result.enabled`.

> **Correction recorded on purpose.** The first pass of this example (sections 2-5 below as first
> written) was built on a wrong premise: that 5.3.x handlers sent no payload for void results and
> that the bug came with `f2b25ad6a9` in 5.4.0-RC1. Golden samples captured from the real released
> 5.3.3 and 5.4.0 handling stacks, and a mixed-version probe with real jars, showed instead:
> released 5.3.2, 5.3.3 and 5.4.0 handlers all send a payload typed `java.lang.Void` with empty
> data, and every released dispatcher fails on it. The cause is AxonFramework `faaff07d36`
> (PR #4994, backported as `4b971815a1`, PR #4999, first released in `axon-5.3.2`); 5.3.0 and 5.3.1
> work. The app's baseline was a local 5.3.0-SNAPSHOT that predates it. The first fix (handler sends
> no payload) was therefore replaced by a dispatcher-only fix that keeps the released wire format;
> see "Final state" at the end. Keep reading the first pass as an example of how a plausible
> code-reading conclusion goes wrong.

## 2-3. Owning repo and origin (first pass, superseded)

- Class: `io.axoniq.framework.axonserver.connector.command.CommandConverter`, module
  `connector/axon-server-connector` in `AxonIQ/axoniq-framework`.
- Handling side `convertResultMessage` wrote a typed payload with `new byte[0]` for a `null`
  result; dispatching side `convertCommandResponse` only treated an empty payload *type* as "no
  result", so the empty bytes reached Jackson.
- Introducing commit `f2b25ad6a9` "Drop nullability for CommandResultMessage" (PR #270 "Local
  dispatch shortcut for command and query buses"), following AxonFramework PR #4994 "Ensure
  returned `CommandResultMessage` is never `null`" (`4adec434c3d6`).
- Last good: `axoniq-5.3.3` (not contained, "diverged"). First affected: `axoniq-5.4.0-RC1`. Still on
  `main` at the time (the commit was the last change to the file). No existing issue or PR found.
- Compatibility insight from reading 5.3.3: a 5.3 dispatcher has the same type-only check, so a
  mixed 5.3/5.4.0 cluster fails too.

## 4-5. Worktree and TDD

- Worktree from tag `axoniq-5.4.0`, branch `<user>/fix-void-command-result-5.4.1`.
- Tests added/changed in `CommandConverterTest`:
  - null-payload result produces a response without payload (replaced a test that asserted the
    buggy empty-bytes format);
  - response with type set and empty data (the 5.4.0 wire format) reads as a null payload;
  - response without payload keeps identifier and metadata;
  - round trip null payload through both methods with the Jackson converter, `payloadAs(Void.class)`
    returns null (RED reproduced the app's exact exception chain);
  - round trip of a real `byte[]` result keeps type, identifier and metadata.
- RED: 21 tests, 3 failures, 1 error. GREEN: `CommandConverterTest` 21/21, module 288/288.
- Fix on both sides: handling side omits the payload for a `null` result (5.3 wire format, keeps
  5.3 dispatchers working); dispatching side treats empty type OR empty data as no payload (keeps
  new dispatchers working with already deployed 5.4.0 handlers). Remaining broken mix: 5.3.x or
  5.4.0 dispatcher calling a 5.4.0 handler (fixed only by upgrading that handler).
- No Axon Server integration test for commands existed in the module, so none was added.

## 6. Local install

```bash
./mvnw -Drevision=5.4.1-SNAPSHOT -pl connector/axon-server-connector -am install -DskipTests
```

Installed `axon-server-connector` and its in-reactor dependency `axoniq-distributed-messaging` as
`5.4.1-SNAPSHOT`; the open source framework stayed on 5.4.0 from Central. The `5.4.0` directories
in `~/.m2` were verified unchanged (checksums and timestamps).

## 7. App validation

- Child worktree of the upgrade branch. Gotcha hit: the worktree tool based it on the default
  branch (pre-upgrade commit); it had to be reset to the upgrade branch tip before editing.
- Pinned `io.axoniq.framework:axon-server-connector:5.4.1-SNAPSHOT` in `dependencyManagement`,
  deleted the workaround class, built with `./mvnw -nsu`.
- Second gotcha: the snapshot connector pulled its sibling `axoniq-distributed-messaging`
  5.4.1-SNAPSHOT transitively. That sibling was also pinned back to 5.4.0, so the connector jar
  was the only artifact that differed from the release (`dependency:list` showed exactly one
  SNAPSHOT).
- Result: full suite 104 tests, 0 failures, 0 errors (same as the baseline with the workaround).
- Negative control: with the connector pinned back to 5.4.0 (workaround still deleted), both
  `RecruitCreatureRequiresResourcesTest` tests errored with the original
  `ConversionException` (`byte[]` to `java.lang.Void`, "No content to map due to end-of-input").
  Restoring the snapshot pin made the two affected classes green again (8 tests).

## 9. PR

- Squashed the four exploratory commits into two clean ones on a fresh branch: "Read Void-typed
  empty command results from Axon Server as null" and "Pin the command wire format to samples of
  released connectors". Module suite: 315/315.
- The description has:
  - the symptom;
  - a per-version table (5.3.0/5.3.1, 5.3.2 to 5.4.0, the fix);
  - the origin with hashes;
  - the fix, and why the handler side stays unchanged;
  - the mixed-version matrix;
  - the TDD evidence;
  - a table of the app's commits with links (after pushing the app branch).
- Push and PR waited for the user's explicit confirmation.
- First opened against `axoniq-5.4.x`, because the skill then preferred maintenance branches. The
  user had it rebased onto `main`: in axoniq-framework, fixes merge to `main`, and a
  `/backport axoniq-5.4.x` comment makes the bot open the backport.
- Result:
  - AxonIQ/axoniq-framework#637 merged to `main` (`d4268313d0`);
  - backport #641 merged to `axoniq-5.4.x` for 5.4.1.
- Validation run after the rebase:
  - Docker had stopped, so Testcontainers errors came first;
  - `-am` stopped in a sibling module, leaving stale connector reports;
  - see "Read test results you can trust" in `local-build.md`.
- The description first said a 5.3.0/5.3.1 dispatcher reads "a `null` payload". Reading the
  5.3.1 sources showed there was no result message at all (`getResultMessage()` completes with
  `null`). So `resultAs(Void.class)` worked there, but `onSuccess` was silently skipped. Fixed in
  the PR text.

## Final state

- Fix: dispatching side only. `convertCommandResponse` reads an empty type, or a `java.lang.Void`
  type without data, as a `null` payload that keeps the Void type, identifier and metadata. The
  handling side and therefore the wire format are byte-identical to every released version. The
  type, not the data alone, decides, so empty `String` / `byte[]` results stay intact.
- Rejected: the handler sending no payload. It is a new wire shape in a patch release, and a
  5.3.3 dispatcher reads it as `getResultMessage() == null`, so `onSuccess` handlers are silently
  skipped.
- Evidence: `CommandConverterWireCompatibilityTest` (24 tests on golden samples of 5.3.3 and 5.4.0,
  RED on `axoniq-5.4.0`, GREEN on the fix); a mixed-version probe with real jars (5.3.0, 5.3.1,
  5.3.2, 5.3.3, 5.4.0, local 5.3.0-SNAPSHOT, the fix, 5.4.0 with the app workaround): the fixed
  dispatcher reads void as `null` from every handler, released dispatchers behave as released.
- App: full suite 104/104 without the workaround on the fix; the negative control fails again on
  the released connector. The same probe also showed that the app's first workaround (matching on
  empty bytes only) turned a real empty String into `null`; it was narrowed to the Void type.
