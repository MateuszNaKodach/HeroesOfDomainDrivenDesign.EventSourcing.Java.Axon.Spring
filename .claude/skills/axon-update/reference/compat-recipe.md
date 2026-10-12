# Old data, new version: the compatibility run

Tests start from empty stores; a deployed app does not. This run answers: can the new
version continue exactly where the old one stopped, on the same event store, token store
and Axon Server context, without reprocessing, losing events, or changing results?

Contents: 1. Inventory the storage modes. 2. Plan the matrix. 3. Infrastructure.
4. Jars. 5. Run one mode. 6. Checks. 7. Rollback. 8. Mixed versions. 9. Other stores.
10. Clean up.

## 1. Inventory the storage modes (before touching Docker)

Start from `scripts/detect.py` output, then read the configuration classes it points at.
For each way the app can run, write down:

| Aspect | Where to look | Typical values |
|---|---|---|
| Event storage engine | `@Bean EventStorageEngine`, `*EventStorageEngine` classes, `axon.axonserver.enabled` | JPA/JDBC aggregate-based engine, Postgres engine, Axon Server aggregate-based, Axon Server DCB, in-memory |
| How it is switched | `@ConditionalOnProperty`, Spring profiles, custom properties | `axon.axonserver.enabled`, an app property such as `...eventstore.mode` |
| Default | base `application.yml`/`.properties`; AF5 enables Axon Server by default when the connector is on the classpath unless disabled | |
| Axon Server context type | `axon.axonserver.context`, the engine used | DCB context vs classic (aggregate-based) context |
| Token store | autoconfigured (JPA/JDBC when a datasource exists), custom `TokenStore` bean | `token_entry` table in the app database |
| Snapshot store | `SnapshotStore` beans, `@Snapshotting` entities | in-memory (lost on restart), Axon Server, JPA |
| Processors | `axon.eventhandling.processors.*.mode`, namespaces/processing groups | pooled streaming, subscribing, persistent streams |
| Dead letters | `dlq.enabled`, dead-letter queue beans | JPA/JDBC dead-letter table |
| Sequencing | `@SequencingPolicy`, processor config | per aggregate, per metadata key |
| Which tests use which mode | `@ActiveProfiles` in tests | |

Test every mode the app can actually run in, default first. A mode only used by an
experiment profile still counts if the user says so; ask when unsure. Say in the report
which modes were tested and which were not.

## 2. Plan the matrix

One row per mode. For each row decide:

- infrastructure (database, Axon Server node and context),
- an isolated store: a **separate database** per mode (and a separate Axon Server context
  or container). Tokens of one engine are not valid for another (for example a
  `GapAwareTrackingToken` from a JPA store vs a `GlobalSequenceTrackingToken` from Axon
  Server); mixing them makes processors loop on "incompatible token" errors and
  invalidates the run,
- the profile and properties to start the app with,
- the data scenario (section 5).

## 3. Infrastructure

Prefer the project's own `docker compose` file (it reflects what the app expects), but
make sure you are not sharing containers or ports with another checkout: use a distinct
compose project name (`COMPOSE_PROJECT_NAME` or `-p`) and free host ports. If the project
has no compose file, start what you need:

```bash
docker run -d --name axu-pg -p 0:5432 -e POSTGRES_USER=app -e POSTGRES_PASSWORD=app -e POSTGRES_DB=app postgres:16
docker port axu-pg 5432        # host port to use in the JDBC URL
docker exec axu-pg psql -U app -d app -c 'create database mode_b'   # one database per mode
```

Axon Server:

- A fresh node has no contexts until it is initialized. A dev-mode container plus
  `POST /v2/cluster/init?dcb=true|false` gives a `default` context of the wanted type
  (`axc_axonserver_dev` and `axc_axonserver_init` in `compat-lib.sh` do this).
- Some server versions ignore the `dcb` flag on init. Verify with
  `GET /v1/public/context` (`dcbContext: true`) or the server log ("Creating DCB
  context"). If it is wrong, use another image version or another container.
- An unlicensed node cannot create additional contexts (replication group limit). Use a
  separate container per context type instead.
- `axoniq/axonserver:latest` is whatever was pulled last on this machine. Inspect the
  image and record the server version you actually tested.
- Point the app at the container with its own properties (for example
  `AXON_AXONSERVER_SERVERS=localhost:<grpc-port>`, `AXON_AXONSERVER_CONTEXT=...`).

If Docker is not running, start it or ask the user; do not skip the run silently.

## 4. Jars

Build both jars without tests and copy them to the scratch dir, so later rebuilds do not
overwrite them:

```bash
(cd $SCRATCH/baseline && ./mvnw -B -q -DskipTests package) && cp $SCRATCH/baseline/target/*.jar $SCRATCH/old.jar
./mvnw -B -q -DskipTests package && cp target/*.jar $SCRATCH/new.jar
```

Pick the executable jar (Spring Boot repackaged), not `-plain`/`-sources` jars. Rebuild
`new.jar` after every workaround change.

## 5. Run one mode

```bash
export AXC_DIR=$SCRATCH/compat AXC_PORT=<free-port> AXC_PROFILES=<profile or empty>
export AXC_DB_URL=jdbc:postgresql://localhost:<pg-port>/<mode-db> AXC_DB_USER=... AXC_DB_PASSWORD=...
export AXC_PG_CONTAINER=<pg-container> AXC_PG_DB=<mode-db> AXC_PG_USER=...
. $AXU/scripts/compat-lib.sh

axc_start $SCRATCH/old.jar <mode>-1-old
#   ... generate data through the app's API (axc_req) ...
sleep 5                                    # let processors catch up
axc_tokens                                 # sanity: processors advanced, segments claimed
axc_stop
axc_snapshot_tokens <mode>-tokens-old
axc_snapshot <mode>-events-old "<select over the event table, ordered>"
axc_snapshot <mode>-readmodel-old "<select over projection tables>"
axc_schema <mode>-old

axc_start $SCRATCH/new.jar <mode>-2-new
sleep 5
axc_snapshot_tokens <mode>-tokens-new-start; axc_diff <mode>-tokens-old <mode>-tokens-new-start
axc_snapshot <mode>-events-new-start "<same select>"; axc_diff <mode>-events-old <mode>-events-new-start
axc_schema <mode>-new; axc_diff schema-<mode>-old.sql schema-<mode>-new.sql
#   ... continue the scenario on the same entities, see section 6 ...
axc_logscan $AXC_DIR/<mode>-2-new.log
axc_stop
```

Generating data: find the app's entry points (REST controllers, `.http` files,
OpenAPI at `/v3/api-docs` or similar, messaging endpoints, MCP tools). Write the requests
for this app in the scratch dir; they are app-specific and do not belong in the skill.
Cover:

- several entities of each type that matters, some with more events than the snapshot
  threshold (`@Snapshotting(afterEvents = N)`), some with only one or two;
- flows that trigger automations / process managers / sagas, so streaming processors and
  their downstream commands run (and dead-letter queues are exercised if possible);
- projections and queries (single, multiple, streaming, subscription);
- one request that the domain rejects, to record the old status code and message.

Write down the expected state (counts, values) after the old run, so the new run is
checked against facts, not memory.

When the app's API cannot reach some flow (no endpoint), say so in the report rather than
appending events behind the app's back; hand-written events test the script, not the app.

## 6. Checks on the new version

| Check | How | Pass when |
|---|---|---|
| Tokens unchanged at startup | `axc_diff` tokens old vs new-start | identical (claims are excluded) |
| Segments re-claimed | `axc_tokens` | `claimed` for running processors |
| No reprocessing | events and read models old vs new-start; automations did not emit duplicates | identical |
| Schema unchanged | `axc_schema` diff | identical, or every difference explained (`ddl-auto=update`, migrations) |
| Entities from old events enforce rules | send a command the domain must reject because of old state (already exists, insufficient quantity) | rejected with the same error as on the old version |
| Entities from old events accept new events | commands on old entities succeed; per-entity sequence continues without gaps or conflicts | events appended, state correct |
| Snapshots | push an entity over its snapshot threshold, then load it again | correct state; note if the snapshot store is in-memory (empty after restart) |
| Processors continue | tokens advance past the old position; projections reflect new events | advanced, correct |
| Automations / DLQ | trigger an automation; inspect the dead-letter table | downstream commands executed once; no unexpected dead letters |
| Subscription queries | open a stream (`curl -N --max-time 10 ...`) while sending updates | initial result plus one update per change |
| Logs | `axc_logscan` / `axc_logdiff` vs the old run | no new ERROR; every new WARN explained |

Record command results as the client sees them (HTTP status) and as the store sees them
(events written). A command that fails for the client but was applied is a regression on
its own.

## 7. Rollback (optional, cheap)

Stop the new jar, start the old jar on the data and tokens written by the new one, repeat
a few checks. This tells the user whether a deployment can be rolled back.

## 8. Mixed versions during a rolling update

With a distributed command/query bus (Axon Server or other connectors), old and new nodes
talk to each other during a rolling deployment. For each regression on the wire (results,
errors, metadata, routing), reason about both directions: old dispatcher with new handler,
new dispatcher with old handler. Testing it needs two app instances on the same Axon
Server with control over routing; if you did not test it, write the analysis and mark it
"not tested" in the report.

## 9. Other stores

- Non-Postgres databases: replace `axc_sql` / `axc_schema` with the database's client
  (same function names keep the recipe intact). Token payload may be a BLOB; convert it
  to text for snapshots.
- Events in Axon Server: read them through the app's own API, or the Axon Server HTTP API
  (`/v1/events?aggregateId=...&context=...` for aggregate-based contexts). Note that the
  numbers it shows may be global positions rather than per-aggregate sequence numbers.
- In-memory event store: there is no data to carry over; the compatibility run does not
  apply. Say so in the report.

## 10. Clean up

Stop the app (`axc_stop`), remove containers and databases you created, remove the
baseline worktree, and tell the user about anything left running (for example the
project's compose stack) and how to stop it.
