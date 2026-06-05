# devports — design notes

## Why a project name is not enough

Docker Compose isolates **containers, the default network, and volumes** by
project name (`-p` / `COMPOSE_PROJECT_NAME` / top-level `name:` / dir basename).
It does **not** namespace published host ports — those bind the host's single
network stack, so two stacks publishing the same `host:container` collide
regardless of project name. devports therefore manages two things together:

- a per-directory **`COMPOSE_PROJECT_NAME`** (isolates names/volumes), and
- per-directory **free host ports** (isolates the runtime bindings).

Both are written to the directory's `.env`, which Compose auto-loads.

## Allocation algorithm (hybrid)

For each `${NAME_PORT:-base}` discovered:

```
offset    = uint32(sha1(absDir)) % span        # span default 10000
candidate = previousAssignment[NAME] ?? base + offset
if candidate is reserved-by-another-dir
   or (it's a fresh candidate AND not OS-free):
       candidate = firstFreeUnreserved(from = base + offset)
assign NAME = candidate
```

- **Deterministic** start → same directory yields the same ports across runs
  (stable, predictable, greppable).
- **Verified** → a probe socket binds `0.0.0.0:candidate`; if that fails the
  port is in use and we move on. This is the only way to be collision-proof —
  deterministic offsets alone can still clash.
- **Registry-aware** → a port held by another directory is skipped even if it's
  momentarily unbound, closing the race where two worktrees both see a port as
  "free" before either binds it.
- **Idempotent + self-healing** → a directory reuses its previous ports (even if
  currently bound by its own running stack), but reallocates if another
  directory has since taken one.

## Registry

Location: `$DEVPORTS_REGISTRY`, else `$XDG_CONFIG_HOME/devports/registry.json`,
else `~/.config/devports/registry.json`.

```jsonc
{
  "version": 1,
  "envs": {
    "/abs/path/to/worktree": {
      "updatedAt": "2026-06-05T07:51:39.489Z",
      "projectName": "worktree-4df871",
      "ports": { "POSTGRES_PORT": 14021, "APP_PORT": 11348 }
    }
  }
}
```

All reads-modify-writes happen inside `withLock()` — an atomic `mkdir` lock next
to the registry file — so concurrent `allocate`/`release` from parallel
worktrees are serialized. If a process is killed mid-write, a stale
`registry.json.lock` directory may remain; remove it manually.

## `.env` managed block

Only the region between the markers is owned by devports; anything else in the
file is preserved on `allocate` and on `release`.

```
# >>> devports (managed — do not edit by hand) >>>
COMPOSE_PROJECT_NAME=...
NAME_PORT=...
# <<< devports <<<
```

## Tuning

- `--span N` (allocate): size of the deterministic offset window (default
  10000). Larger span spreads directories further apart before any probing is
  needed; ports stay within `[base+0, 65535]`.
- `--env-file PATH` (allocate/release): target a file other than `.env`.

## Limitations

- Host-run processes do not auto-load `.env`; export it (`set -a && . ./.env &&
  set +a`) before launching them.
- Port discovery covers compose files and Spring `application*.yaml`. For other
  frameworks, ensure the app reads the same `*_PORT` env vars (the registry/.env
  are framework-agnostic; only discovery is opinionated).
- `suggest.mts` is best-effort line parsing for the common compose shapes; it is
  read-only and never rewrites — review and apply its proposals yourself.
- The deterministic offset can theoretically place two directories' starting
  candidates near each other; verification + the registry resolve the actual
  collision, but ports are then not a single uniform offset.
```
