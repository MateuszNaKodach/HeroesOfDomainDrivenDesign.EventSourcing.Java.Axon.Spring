# Axon Framework 4 → 5 Migration — index

This directory holds the migration state for this project. The orchestrator
(skill `axon4-to-axon5-migration`) reads and writes here.

## Files

- **`progress.md`** — single source of truth for migration state. Read this
  first to see where the migration is and what's next. The ▶︎ RESUME HERE
  block tells you the next move. Rewritten alongside every code-changing
  commit so a fresh session can resume from the file alone.

- **`learnings.md`** — append-only narrative. Surprises, manual fixes,
  decisions worth remembering. Read on demand only.

- **`sql/`** — recipe-specific artifacts. `event-storage-engine` Path A
  (Spring Boot + JPA) emits DDL here; user runs it against their database.

This directory may grow over time. Keep `progress.md` and `learnings.md`
self-explanatory so a fresh session can reorient with no prior context.
