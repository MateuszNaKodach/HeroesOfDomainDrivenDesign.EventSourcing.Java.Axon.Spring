---
name: devports
description: >-
  Isolate host ports for Docker Compose / local dev stacks per directory or git
  worktree so multiple checkouts run concurrently without port or container-name
  collisions. Use when the user wants to run the same compose project from
  several worktrees/branches at once, gets "address already in use" / "port is
  already allocated", asks to allocate free ports for a worktree, generate a
  per-directory .env, parameterize compose ports into env vars, or release/free
  a worktree's ports when done. Allocates collision-free ports (deterministic,
  verified free, registered globally), writes a managed .env, and releases on
  teardown.
---

# devports

Per-directory / per-worktree port isolation for Docker Compose and host-run apps.

**The problem it solves.** A Compose project name isolates containers, networks,
and volumes — but **never host ports**. Two worktrees that publish `5432:5432`
collide on the host's single network stack (the second `up` fails with *address
already in use*). devports gives each directory its own free, stable ports via a
generated `.env`, and tracks reservations in a global registry so parallel
worktrees never pick the same port.

## Requirements

- **Node >= 23.6** (24+ recommended). Scripts are `.mts` and rely on Node's
  native TypeScript type-stripping — run them directly with `node`, no build, no
  dependencies. `bun` also works (`bun script.mts`).

## Commands

Scripts live in this skill's `scripts/` directory. From the repo root:

```bash
DP=.claude/skills/devports/scripts        # adjust if the skill lives elsewhere

node $DP/allocate.mts [dir]               # allocate free ports -> write .env, register
node $DP/status.mts                       # show all reservations (current dir marked)
node $DP/release.mts [dir] [--prune]      # free this dir's ports, strip its .env
node $DP/suggest.mts [dir]                # READ-ONLY: propose how to parameterize ports
```

`dir` defaults to the current directory. Useful flags: `allocate --dry-run`,
`--json` (allocate/status/release/suggest), `release --prune` (also drop
registry entries whose directory no longer exists — good after `git worktree
remove`), `release --keep-env`.

## How allocation works

1. **Discover** — **recursively** scan the directory for compose files and any
   generic config file (yaml / properties / toml / json / …) containing
   `${NAME_PORT:-default}` / `${NAME_PORT:default}` placeholders. The default is
   the base port. Framework-agnostic: a Spring `application.yaml` /
   `application.properties` is picked up like any other config file — see
   `reference/app-config-examples.md` for per-ecosystem snippets. Hidden dirs
   (`.git`, `.idea`, `.claude`, …) and build/vendor dirs (`node_modules`,
   `target`, `build`, …) are skipped.
2. **Hybrid pick per port** — start from a deterministic candidate
   (`base + hash(absDir)`), then **verify it's actually free** (binds a probe
   socket on `0.0.0.0`) and **not reserved by another directory**; if taken,
   probe upward to the next free+unreserved port.
3. **Pin + register** — record `dir -> {projectName, ports}` in the global
   registry (`$XDG_CONFIG_HOME/devports/registry.json`, override with
   `$DEVPORTS_REGISTRY`), behind an atomic lock so parallel runs don't race.
4. **Write `.env`** — a managed block (between markers) with
   `COMPOSE_PROJECT_NAME` + every allocated port. Re-running is idempotent:
   the same directory keeps the same ports unless they conflict with another
   directory, in which case they self-heal.
5. **Wire HTTP request files** — if any `*.http` / `*.rest` files exist under
   the directory, write the allocated ports into `http-client.private.env.json`
   (gitignored) under environment `dev` (override with `--http-env`). This
   merges into any user-authored environments/keys and is removed again on
   `release`. So `.http` requests that target the app — which is **not** in
   compose — still hit this worktree's allocated port. See "HTTP request files".

Keyed by **absolute directory path** — each git worktree is a distinct path, so
each gets its own ports automatically.

## Lifecycle (typical worktree session)

```bash
DP=.claude/skills/devports/scripts

# 1. allocate for this worktree
node $DP/allocate.mts

# 2. bring up infra — compose auto-loads .env, so ports + project name apply
docker compose up -d
docker compose -f docker-compose.observability-jaeger.yaml up -d

# 3. run a HOST process (NOT in compose) — it does NOT auto-load .env, so export:
set -a && . ./.env && set +a && ./mvnw spring-boot:run

# 4. when done with the worktree
docker compose down --remove-orphans
node $DP/release.mts            # frees the reservation, strips the managed .env block
```

> **Critical caveat.** `docker compose` auto-loads `.env` from the project dir,
> but a process you start yourself on the host (Maven, Gradle, node, …) does
> **not**. Always `set -a && . ./.env && set +a` before launching the host app,
> or its `${VAR:default}` placeholders fall back to the base ports and miss the
> running containers.

## "prepare" — parameterize a project's ports (one-time)

If a project still has static `host:container` mappings, convert them to env
vars so devports can drive them:

1. Run `node $DP/suggest.mts` (read-only) to get proposed
   `${SERVICE_ROLE_PORT:-default}` names and the `container_name:` lines to drop.
2. Apply the edits:
   - **Compose files:** replace static ports with `${VAR:-default}`
     (shell syntax, `:-`), and **remove `container_name:`** lines (they're
     globally unique and block concurrency; inter-container DNS still works via
     service names).
   - **Host app config (e.g. Spring `application*.yaml`):** mirror any port the
     app uses with the **same VAR name** but **Spring placeholder syntax**
     `${VAR:default}` (single colon — `${VAR:-default}` would make the default
     the literal `-default`).
3. Verify defaults still resolve: `docker compose config` with no env set should
   show the original ports.

Naming convention: `<SERVICE>[_<ROLE>]_PORT` — full service name, with a role
suffix (`HTTP`, `GRPC`, `UI`, `OTLP_HTTP`, …) when a service exposes more than
one port or the protocol adds clarity; role-less for single, unambiguous ports.

## HTTP request files (`.http` / `.rest`)

The app usually runs on the **host**, not in compose — but `.http` files that
call it still hardcode a port. devports keeps them in sync:

- Reference the app port in requests with **`{{APP_PORT}}`** (the same env-var
  name), e.g. `GET http://localhost:{{APP_PORT}}/health`. Do **not** hardcode a
  port or an in-file `@serverPort = …`.
- Commit an **`http-client.env.json`** with a default environment so the file
  works out of the box:
  ```json
  { "dev": { "APP_PORT": "3773" } }
  ```
- On `allocate`, devports writes/merges this worktree's allocated ports into
  **`http-client.private.env.json`** (gitignored) under the same environment.
  The private file overrides the committed default, so selecting the `dev`
  environment in the IDE points requests at this worktree's app port.
- `release` removes only the keys devports added, preserving any tokens/secrets
  or other environments you keep in the private file.

> JetBrains HTTP Client and the VS Code REST Client both resolve `{{VAR}}` from
> these env files. (VS Code REST Client can alternatively read the `.env`
> directly via `{{$dotenv APP_PORT}}`.)

See `reference/app-config-examples.md` for per-ecosystem snippets, and
`reference/design.md` for the registry format, race-safety, and tuning
(`--span`, `--http-env`).
