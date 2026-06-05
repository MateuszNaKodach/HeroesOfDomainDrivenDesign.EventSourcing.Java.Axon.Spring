---
name: devports
description: >-
  Isolate host ports for Docker Compose / local dev stacks per directory or git
  worktree so multiple checkouts run concurrently without port or container-name
  collisions. Two modes: PREPARE (one-time, parameterize a project's ports into
  env vars — edits tracked files) and ISOLATE (per worktree — allocate free
  ports, write a gitignored .env, release on teardown — never edits tracked
  files). Use when the user wants to run the same compose project from several
  worktrees/branches at once, gets "address already in use" / "port is already
  allocated", asks to allocate/free ports for a worktree, generate a per-worktree
  .env, parameterize compose ports into env vars, or set a project up for port
  isolation.
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
  native TypeScript type-stripping — run directly with `node`, no build, no
  dependencies. `bun` also works.

Scripts live in this skill's `scripts/` directory. From a project root:

```bash
DP=.claude/skills/devports/scripts   # adjust if the skill lives elsewhere
```

## Two modes — and how to choose

devports has two distinct jobs. **Always start by detecting which one applies:**

```bash
node $DP/suggest.mts --check        # exit 1 = NEEDS PREPARE, exit 0 = already prepared
```

- **exit 1** → the project still has static `host:container` ports / `container_name:`
  lines. Do **PREPARE** first (one-time).
- **exit 0** → ports are already parameterized. Go straight to **ISOLATE** (the
  everyday path).

---

## PREPARE mode — one-time project setup

⚠️ **Edits tracked files. Run once per project (e.g. on `main`), review the diff,
commit.** This is rare; most of the time a project is already prepared and you
skip straight to ISOLATE.

Converts static ports into env-var placeholders so devports can drive them:

1. **Analyze** (read-only): `node $DP/suggest.mts` prints proposed
   `${SERVICE_ROLE_PORT:-default}` names and the `container_name:` lines to drop.
2. **Apply the edits:**
   - **Compose files:** replace static ports with `${VAR:-default}` (shell
     syntax, with `:-`), and **remove `container_name:`** lines (globally unique
     → they block concurrency; inter-container DNS still works via service names).
   - **Host app config** (e.g. Spring `application.yaml`/`.properties`): mirror
     any port the app uses with the **same VAR name** but **Spring placeholder
     syntax** `${VAR:default}` (single colon — `${VAR:-default}` would make the
     default the literal `-default`).
   - **`.http` request files:** reference the app port as `{{APP_PORT}}` (drop any
     hardcoded `@serverPort = …`), and commit an `http-client.env.json` default.
     See "HTTP request files".
3. **Verify** defaults still resolve: `docker compose config` with no env set
   should show the original ports. `node $DP/suggest.mts --check` should now
   exit 0.

Naming convention: `<SERVICE>[_<ROLE>]_PORT` — full service name, with a role
suffix (`HTTP`, `GRPC`, `UI`, `OTLP_HTTP`, …) when a service exposes more than
one port or the protocol adds clarity; role-less for single, unambiguous ports.

---

## ISOLATE mode — per worktree (the common path)

Never touches tracked files — only the gitignored `.env` and
`http-client.private.env.json`.

```bash
node $DP/allocate.mts [dir]          # allocate free ports -> write .env, register
node $DP/status.mts                  # show all reservations (current dir marked)
node $DP/release.mts [dir] [--prune] # free this dir's ports, strip its .env
```

`dir` defaults to the current directory. Flags: `allocate --dry-run`,
`--http-env <name>` (default `dev`), `--json` (all scripts), `release --prune`
(also drop registry entries whose directory no longer exists — good after
`git worktree remove`), `release --keep-env`.

### How allocation works

1. **Discover** — recursively scan for compose files and any generic config file
   (yaml/properties/toml/json/…) containing `${NAME_PORT:-default}` /
   `${NAME_PORT:default}` placeholders; the default is the base port.
   Framework-agnostic (a Spring `application.yaml` is just one example —
   see `reference/app-config-examples.md`). Hidden and build/vendor dirs skipped.
2. **Hybrid pick per port** — start from a deterministic candidate
   (`base + hash(absDir)`), verify it's **actually free** (binds a probe socket)
   and **not reserved by another directory**; if taken, probe upward.
3. **Pin + register** — record `dir -> {projectName, ports}` in the global
   registry (`$XDG_CONFIG_HOME/devports/registry.json`, or `$DEVPORTS_REGISTRY`),
   behind an atomic lock so parallel runs don't race.
4. **Write `.env`** — managed block with `COMPOSE_PROJECT_NAME` + every port.
   Idempotent: a directory keeps its ports across runs, self-healing on conflict.
5. **Wire `.http` files** — if any exist, write the ports into
   `http-client.private.env.json` (gitignored), merging into user-authored
   environments; removed again on `release`.

Keyed by **absolute directory path** — each worktree is a distinct path → its
own ports automatically.

### Lifecycle (typical worktree session)

```bash
node $DP/allocate.mts                 # 1. allocate for this worktree

docker compose up -d                  # 2. compose auto-loads .env (ports + project name)
docker compose -f docker-compose.observability-jaeger.yaml up -d

# 3. run a HOST process (NOT in compose) — it does NOT auto-load .env, so export:
set -a && . ./.env && set +a && ./mvnw spring-boot:run

docker compose down --remove-orphans  # 4. teardown
node $DP/release.mts                  # frees reservation, strips managed .env block
```

> **Critical caveat.** `docker compose` auto-loads `.env`, but a host process you
> start yourself (Maven, Gradle, node, …) does **not**. Always
> `set -a && . ./.env && set +a` before launching it, or its `${VAR:default}`
> placeholders fall back to base ports and miss the running containers.

---

## HTTP request files (`.http` / `.rest`)

The app usually runs on the **host**, not in compose, but `.http` files still
hardcode its port. Keep them env-driven:

- Reference the app port with **`{{APP_PORT}}`** (the env-var name), e.g.
  `GET http://localhost:{{APP_PORT}}/health`. No hardcoded port or in-file
  `@serverPort = …`.
- Commit an **`http-client.env.json`** default so the file works out of the box:
  `{ "dev": { "APP_PORT": "3773" } }`.
- ISOLATE writes/merges this worktree's ports into **`http-client.private.env.json`**
  (gitignored) under the same environment; the private file overrides the
  committed default, so selecting `dev` in the IDE points requests at this
  worktree's app port. `release` removes only the keys devports added.

> JetBrains HTTP Client and VS Code REST Client both resolve `{{VAR}}` from these
> env files. (VS Code REST Client can also read the `.env` via
> `{{$dotenv APP_PORT}}`.)

See `reference/app-config-examples.md` for per-ecosystem snippets and
`reference/design.md` for the registry format, race-safety, and tuning
(`--span`, `--http-env`).
