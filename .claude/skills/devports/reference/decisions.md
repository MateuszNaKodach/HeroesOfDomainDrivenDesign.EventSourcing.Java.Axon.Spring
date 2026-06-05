# devports — design decisions

Lightweight decision log (ADR-style). Each entry: **what** was decided, **why**,
and the **alternatives rejected**. Newest first. Keep this in sync when a
decision changes — and explain the "why" in the commit that changes it.

---

## 8. Prepare/Isolate as two modes in ONE skill, gated by `suggest --check`
**Date:** 2026-06-05
**Decision:** Keep a single `devports` skill. Expose two explicit modes — PREPARE
(one-time, edits tracked files) and ISOLATE (per worktree, never edits tracked
files) — and gate them with `suggest.mts --check` (exit 1 = needs prepare,
exit 0 = ready).
**Why:** The two operations differ in cadence and blast radius, so they deserve
to be *named* — but splitting into two skill directories would force duplicating
the shared `lib/scan.mts` (Claude Code skills are self-contained dirs with no
shared-code mechanism). The scripts are already separated by concern
(`suggest` = prepare; `allocate`/`release`/`status` = isolate), so the split
lives in docs + a deterministic gate, with zero duplication and one cohesive
concept to discover.
**Rejected:** Two skills (`devports-prepare` / `devports-isolate`) — duplicates
`scan.mts`, two overlapping trigger descriptions, more to maintain. A single
skill with no explicit modes — loses the user's mental model and the safety
distinction (prepare mutates committed files).

## 7. App port env var is `APP_PORT` (generic), not `SERVER_PORT` (Spring)
**Date:** 2026-06-05
**Decision:** Name the app's port var `APP_PORT`.
**Why:** devports is framework-agnostic and every other var is service-scoped
and neutral (`POSTGRES_PORT`, `JAEGER_*_PORT`, …); `APP_PORT` fits that family.
A `${...}` placeholder in `application.yaml` is required for discovery anyway,
so `SERVER_PORT`'s one real advantage — Spring relaxed binding mapping
`SERVER_PORT → server.port` with no placeholder — buys nothing here.
**Rejected:** `SERVER_PORT` — couples the generic tool's vocabulary to Spring and
is the odd one out in an otherwise neutral set. (Would only win if we wanted
Spring relaxed binding as a safety net for a missing placeholder.)

## 6. `.http` files: env-file override pattern, not in-file constants
**Date:** 2026-06-05
**Decision:** `.http` requests reference `{{APP_PORT}}`; a committed
`http-client.env.json` holds the default; ISOLATE writes the per-worktree value
into the gitignored `http-client.private.env.json` (private overrides public for
the same environment). Drop hardcoded `@serverPort = …`.
**Why:** The app runs on the host (not in compose) but its port is still
per-worktree. The public/private env-file split is the canonical pattern shared
by JetBrains HTTP Client and VS Code REST Client, keeps the dynamic value out of
tracked files, and `release` can remove exactly the keys it added. context7 did
not surface authoritative HTTP-Client variable-precedence docs, so we avoided
guessing exotic `$env`-style syntax and used the well-established env-file model.
**Rejected:** Rewriting an in-file `@serverPort` per worktree (churns a tracked
file); `{{$dotenv}}` only (VS Code REST Client-specific).

## 5. Drop `container_name:` during PREPARE
**Date:** 2026-06-05
**Decision:** Remove fixed `container_name:` from compose services.
**Why:** `container_name` is globally unique on the Docker daemon, so it collides
across concurrent stacks regardless of project name — defeating isolation.
Inter-container DNS keeps working via the service name (a network alias), so
nothing breaks.
**Rejected:** Keeping `container_name` — blocks running two stacks at once.

## 4. Discovery is recursive and framework-agnostic
**Date:** 2026-06-05
**Decision:** Recursively scan for compose files and any generic config file
(yaml/properties/toml/json/…) for `${*_PORT:default}` placeholders; skip hidden
and build/vendor dirs; cap file size; sort for determinism.
**Why:** Nested stacks (`infra/db/compose.yaml`) must be found, and ports may be
declared in app config, not just compose. Hardcoding the Spring
`src/main/resources/application*.yaml` path was too narrow and framework-specific.
**Rejected:** Root-only compose glob + hardcoded Spring path.

## 3. Compose vs Spring placeholder syntax must differ
**Date:** 2026-06-05
**Decision:** Compose files use `${VAR:-default}` (shell, with dash); Spring
config uses `${VAR:default}` (single colon). Same VAR names on both sides.
**Why:** Correctness — in Spring, `${VAR:-default}` makes the default the literal
`-default`. The scanner accepts both forms (`:-?`) so either is discoverable.
**Rejected:** One syntax everywhere — silently breaks defaults on one side.

## 2. Global reservation registry behind an atomic lock
**Date:** 2026-06-05
**Decision:** Track `dir -> {projectName, ports}` in a single global registry
(`$XDG_CONFIG_HOME/devports/registry.json`), all reads-modify-writes wrapped in
an atomic `mkdir` lock.
**Why:** A Compose project name does NOT namespace host ports; verifying a port
is OS-free is not enough because two worktrees can both see it free before either
binds it. A shared registry closes that race and enables `release`/`--prune`.
**Rejected:** OS-free check only (races under parallel allocation); per-worktree
registry (loses cross-worktree collision protection).

## 1. Hybrid allocation keyed by absolute directory path
**Date:** 2026-06-05
**Decision:** Per port: deterministic candidate `base + hash(absDir)`, verify
free + unreserved, else probe upward. Key the environment by the absolute
(symlink-resolved) directory path.
**Why:** Deterministic = stable, predictable, greppable ports per worktree;
verification = collision-proof; abs-path key = each worktree (a distinct dir)
gets its own ports with no git introspection needed.
**Rejected:** Pure deterministic offset (can collide); pure free-port scan
(unstable/unpredictable across runs); keying by git branch (more edge cases,
needs git).

## 0. Zero-dependency TypeScript `.mts` on Node 24
**Date:** 2026-06-05
**Decision:** Scripts are `.mts`, run directly with `node` (native type
stripping), no build step, no npm dependencies (free-port check via `node:net`,
hashing via `node:crypto`).
**Why:** A skill that needs `npm install` before its scripts work is fragile and
non-portable; Node 24 strips types natively so we get TypeScript authoring for
free. Matches Claude Code's own Node/TS stack.
**Rejected:** `get-port`/`conf`/`env-paths` deps (install friction); a published
CLI (only wins if external users need `npx`, which they don't); Bun-only binary
(less ubiquitous, heavier artifact).
