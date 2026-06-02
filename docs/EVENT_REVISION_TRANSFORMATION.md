# Event Revision Transformation (fix events stored without a revision)

Legacy events stored with **Axon Framework 4** may not carry a payload revision — Axon Server stores it as an
empty `String`. When such events are read under **Axon Framework 5**, message reconstruction fails with:

```
java.lang.IllegalArgumentException: The given version is unsupported because it is empty.
    at org.axonframework.messaging.core.MessageType.<init>(...)
```

(see [AxonIQ/AxonFramework#4625](https://github.com/AxonIQ/AxonFramework/issues/4625)).

This maintenance feature rewrites those events **in place** in Axon Server, giving each one an explicit revision
(default `0.0.1`), using the Axon Server
[Event Transformation](https://docs.axoniq.io/axon-server-reference/v2026.0/axon-server/administration/event-transformation/)
API. Run it **before** migrating the data to AF5.

- Operation: `maintenance/write/transformeventrevisions/EventRevisionTransformation`
- Endpoint: `maintenance/write/transformeventrevisions/EventRevisionTransformationRestApi`
- It opens its **own dedicated** Axon Server connection, replaces the affected events, and **applies** the
  transformation.

## What it does and does not touch

- **Events only.** Axon Server Event Transformation cannot transform snapshots. (AF5 also does not build a
  `MessageType` from a snapshot's version, so snapshots do not trigger #4625.)
- Only events whose payload revision is **empty** are replaced; events that already have a revision are left alone.
- Only the payload **revision** changes — identifier, aggregate id/type/sequence, timestamp and metadata are preserved.
- Re-running is safe and idempotent: once every event has a revision, it returns `NOTHING_TO_FIX`.

## Prerequisites

1. **Axon Server running and licensed.** Event Transformation is a licensed feature; place a license at
   `axonserver/axoniq.license` (already wired into `docker-compose.yaml`).
2. **Standalone internal hostname.** On a standalone dockerized node the transformation does internal node
   communication on port `8224`, so the node must resolve its own internal hostname from inside the container.
   `docker-compose.yaml` mounts `axonserver/axonserver.properties` which pins:
   ```properties
   axoniq.axonserver.internal-hostname=localhost
   ```
   Without this the call fails with `UNAVAILABLE: ... UnknownHostException: axon-server` (or a timeout).
3. **App configured for Axon Server.** Run with the `axonserver` Spring profile (`axon.axonserver.enabled=true`).
4. **Maintenance enabled.** `application.maintenance.enabled=true` (default in `application.yaml`).

The endpoint and operation only load when both `application.maintenance.enabled=true` **and**
`axon.axonserver.enabled=true`, so the default (JPA) startup is unaffected.

> **Back up the Axon Server event store before running** — the transformation rewrites it in place, and applying can
> temporarily need up to ~2× the store size on disk (reclaimed by `compact=true`).

## How to run

```bash
# 1. Start Axon Server (+ Postgres)
docker compose up -d

# 2. Run the app against Axon Server
./mvnw spring-boot:run -Dspring-boot.run.profiles=axonserver
```

Then trigger the transformation (app runs on port `3773`):

```bash
# Fill missing revision with the default "0.0.1" and apply
curl -X POST "http://localhost:3773/maintenance/event-store/transformations/fill-missing-revision"

# Custom revision + reclaim disk afterwards
curl -X POST "http://localhost:3773/maintenance/event-store/transformations/fill-missing-revision?revision=0.0.1&compact=true"
```

Ready-to-run requests are also in [`generated-requests.http`](../generated-requests.http).

### Parameters

| Param      | Default | Meaning                                                                 |
|------------|---------|-------------------------------------------------------------------------|
| `revision` | `0.0.1` | The revision assigned to events that currently have none.               |
| `compact`  | `false` | Compact the event store after applying, to reclaim disk.                |

### Response

```json
{ "transformationId": "…", "state": "APPLIED", "eventsReplaced": 12, "compacted": false }
```

- `state`: `APPLIED` on success, `NOTHING_TO_FIX` if no event needs a revision, `EMPTY_STORE` if the store is empty.
- `eventsReplaced`: number of events whose revision was filled in.

## Verify

Re-run the endpoint — it should report `NOTHING_TO_FIX` / `eventsReplaced: 0`, confirming every event now has a
revision. You can also read an affected aggregate's stream:

```bash
curl "http://localhost:3773/maintenance/event-store/streams/<streamId>/events"
```

and inspect existing transformations on Axon Server via Swagger at
`http://localhost:8024/swagger-ui/index.html` (list transformations endpoint).

## Notes / troubleshooting

- **Only one transformation may be active per context.** The operation cancels a lingering `ACTIVE` transformation
  from a previous run before starting a new one.
- **Event processors.** This is a pure revision fill; event global tokens are preserved, so processor tokens stay
  valid and no reset/replay is required. A streaming processor that was failing on the revision-less events recovers
  on its own once they are fixed. Stopping processors during the run is optional (only reduces error-log noise).
- **`CANCELLED: Not all nodes support transformation`** — the node is unlicensed; provide a license (prerequisite 1).
- **`UnknownHostException: axon-server` / timeout** — the internal hostname is not `localhost` (prerequisite 2).
