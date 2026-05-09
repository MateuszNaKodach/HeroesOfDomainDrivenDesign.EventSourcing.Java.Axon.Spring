# Recipe: deadline-manager (currently not supported)

Predominantly used with sagas (also unsupported). No direct AF5 successor.
Currently not supported by this skill — contact Axoniq for support.

## Goal

Surface the unsupported state to the user and record the decision so future
sessions don't re-prompt. No code changes.

## Inputs

- target: file path (informational only — orchestrator passes the discovery
  hit). Optional.

## Preflight

Always returns `needs-user-decision=true` with the not-supported message.

## Procedure

1. Build message:
   - file list: hits from `@DeadlineHandler|DeadlineManager|DeadlineMessage`.
   - body: "Predominantly used with sagas (also unsupported). No direct AF5
     successor. Contact Axoniq for support."
2. AskUserQuestion: choose one
   - `accept-stays-af4` — deadline code stays AF4; won't compile under AF5
     deps in the affected slice.
   - `pause-migration` — stop; user removes / replaces deadline code first.
   - `remove-feature-first` — user accepts they will redesign deadline-driven
     flows (e.g. Spring `@Scheduled` or quartz directly) before resuming.
3. Append a dated narrative entry to `learnings.md`.
4. Emit Output. Orchestrator commits a decision-only record
   (`recipe: deadline-manager`, `decisions: [deadline-manager: <choice>]`).

## End condition

Never green automatically. Decision recorded; no code change.

## Output

- target: <file list> | "n/a"
- decisions:
    - deadline-manager: <accept-stays-af4 | pause-migration | remove-feature-first>
- needs-user-decision: false
- notes: Predominantly used with sagas (also unsupported). No direct AF5 successor. Contact Axoniq for support.

## Caveats

- Deadline code typically lives inside sagas — when the saga decision is
  `pause-migration`, this one usually is too.
- Quartz-based deadline manager (`axon-deadline-quartz-spring-boot-autoconfigure`)
  brings extra deps that won't resolve under AF5; flag them in stabilization.

## When this recipe will be replaced

When AF5 publishes a deadline / scheduled-message replacement. Until then,
non-saga uses of deadlines (rare) usually map to plain `@Scheduled` —
recommend that path manually.
