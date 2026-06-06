# devports

Per-directory / per-worktree **host-port isolation** for Docker Compose and
host-run apps. Run the same compose project from several git worktrees at once
without `address already in use` or container-name clashes.

> This directory is a Claude Code skill (`SKILL.md` drives the agent), **but the
> scripts are plain Node and run standalone** — see
> ["Running without an LLM"](#running-without-an-llm).

## Why

A Compose **project name** isolates containers, networks, and volumes — but
**not published host ports**. Two worktrees that both publish `5432:5432` fight
over the host's single network stack; the second `up` fails. devports gives each
directory its own free, stable ports via a generated `.env`, tracked in a global
registry so parallel worktrees never collide.

## Requirements

- **Node ≥ 23.6** (24+ recommended) — scripts are `.mts` and use Node's native
  TypeScript type-stripping. No build, **no npm dependencies**. `bun` also works.

## Commands

```bash
DP=.claude/skills/devports/scripts   # adjust if the skill lives elsewhere

node $DP/allocate.mts [dir]          # allocate free ports → write .env, register
node $DP/status.mts                  # list reservations (current dir marked)
node $DP/release.mts [dir]           # free this dir's ports, strip its .env
node $DP/suggest.mts [dir]           # READ-ONLY: how to parameterize ports
node $DP/suggest.mts --check         # exit 1 = needs prepare, exit 0 = ready
node $DP/prepare.mts [dir] [--write] # apply compose parameterization (dry-run without --write)
```

Flags: `allocate --dry-run`, `--http-env <name>` (default `dev`), `--json` (all
scripts), `release --prune` (drop entries for deleted worktrees), `--keep-env`.
Registry path: `$DEVPORTS_REGISTRY` or `$XDG_CONFIG_HOME/devports/registry.json`.

## Two modes

1. **PREPARE** (once per project): parameterize static ports into `${VAR}`
   placeholders and drop `container_name:`. Edits tracked files — review + commit.
2. **ISOLATE** (every worktree): allocate/release per-worktree ports. Never edits
   tracked files — only the gitignored `.env` / `http-client.private.env.json`.

`node $DP/suggest.mts --check` tells you which you need.

---

## Running without an LLM

**Short answer: yes — the everyday loop is 100% scripts.** The skill's `SKILL.md`
is only there to help the Claude Code agent; nothing in `allocate`/`release`/
`status` needs an LLM. Here's the split:

| Step | Frequency | LLM needed? |
|------|-----------|-------------|
| **ISOLATE** — allocate / release / status | every worktree, constantly | **No** — pure scripts |
| **PREPARE** — apply `${VAR}` placeholders to tracked files | once per project | Optional (see below) |

### One-time PREPARE (no LLM)

```bash
node $DP/suggest.mts --check    # exit 1 until prepared
node $DP/prepare.mts            # dry-run: print the planned compose edits
node $DP/prepare.mts --write    # apply them to compose files (idempotent)
```

`prepare.mts --write` scripts the **compose** parameterization end to end: static
ports → `${VAR:-default}`, `container_name:` removed, across all (recursive)
compose files. No LLM, no manual editing.

The **app-side** edits are left for you to apply by hand (they need judgement
about which ports the app uses, and the Spring placeholder syntax differs):
- Spring `application.yaml`/`.properties`: mirror shared `*_PORT` with
  `${VAR:default}` (single colon).
- `.http` files: use `{{APP_PORT}}`; commit an `http-client.env.json` default.

Then `docker compose config` should still show the original ports and
`node $DP/suggest.mts --check` should exit 0. (Why compose is scripted but
app/`.http` aren't: see `reference/decisions.md`.)

### Everyday ISOLATE for a Spring Boot project (no LLM)

Once prepared, the full per-worktree loop is just scripts + your usual tools:

```bash
DP=.claude/skills/devports/scripts

node $DP/allocate.mts                       # 1. pick free ports → write .env (+ http private env)

docker compose up -d                        # 2. compose auto-loads .env (ports + COMPOSE_PROJECT_NAME)

set -a && . ./.env && set +a \              # 3. export ports for the HOST app (Maven won't read .env)
  && ./mvnw spring-boot:run

docker compose down --remove-orphans        # 4. teardown
node $DP/release.mts                         #    free the reservation + strip .env
```

> **The one gotcha:** `docker compose` auto-loads `.env`, but a host process you
> launch yourself (Maven/Gradle/java) does **not**. Always
> `set -a && . ./.env && set +a` before it, or Spring's `${VAR:3773}` placeholders
> fall back to base ports and miss the running containers.

### Wrap it in a Makefile (still no LLM)

```makefile
DP = node .claude/skills/devports/scripts

up:    ; $(DP)/allocate.mts && docker compose up -d
run:   ; set -a && . ./.env && set +a && ./mvnw spring-boot:run
down:  ; docker compose down --remove-orphans && $(DP)/release.mts
ports: ; $(DP)/status.mts
```

`make up && make run` in each worktree → isolated stacks, no Claude involved.
The same works as a Taskfile, npm script, shell alias, or CI step.

---

## How it works (brief)

- **Discover** — recursively scan compose + config files for `${*_PORT:default}`
  placeholders (the default is the base port). Framework-agnostic.
- **Allocate (hybrid)** — deterministic candidate `base + hash(absDir)`, verified
  bindable and not reserved by another directory; probe upward on conflict.
- **Register** — `dir → {projectName, ports}` in a global registry behind an
  atomic lock (parallel-worktree safe).
- **Write** — managed `.env` block + `COMPOSE_PROJECT_NAME`; idempotent.

More detail: [`reference/design.md`](reference/design.md),
[`reference/app-config-examples.md`](reference/app-config-examples.md),
[`reference/decisions.md`](reference/decisions.md).
