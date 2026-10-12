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

## 2-3. Owning repo and origin

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
- Pinned only `io.axoniq.framework:axon-server-connector:5.4.1-SNAPSHOT` in `dependencyManagement`,
  deleted the workaround class, built with `./mvnw -nsu`.
- Expected: full suite equal to the baseline with the workaround (104 tests, all green), negative
  control with the connector pinned back to 5.4.0 failing `RecruitCreatureRequiresResourcesTest`
  with the original `ConversionException`, then green again with the snapshot pin.

## 9. PR (prepared, not pushed in this run)

Title: "Send no payload for empty command results over Axon Server". Body built from the fix
report: symptom and impact, origin commit/PRs, both-sides fix and the version-mix matrix, tests,
app validation with negative control, the remaining broken mix and the "empty byte[] result now
reads as null" edge case. Push and PR waited for the user's explicit confirmation.
