# Recipe: saga (currently not supported)

AF5 reframes long-running coordination as process-managers; no automatic
rewrite. Currently not supported by this skill. Workflow support is on the
Axoniq roadmap — contact Axoniq.

## Goal

Surface the unsupported state to the user and record the decision so future
sessions don't re-prompt. No code changes.

## Inputs

- target: file path (informational only — orchestrator passes the discovery
  hit). Optional.

## Preflight

Always returns `needs-user-decision=true` with the not-supported message.
There is no "already migrated" path — sagas remain on AF4 shape.

## Procedure

1. Build message:
   - file list: hits from the routing-table `Discovery` grep
     (`@Saga\b|@SagaEventHandler|@StartSaga|@EndSaga|SagaConfigurer`).
   - body: "AF5 reframes long-running coordination as process-managers;
     no automatic rewrite. Currently not supported by this skill. Workflow
     support is on the Axoniq roadmap — contact Axoniq."
2. AskUserQuestion: choose one
   - `accept-stays-af4` — saga code stays on AF4 shape; will fail to compile
     under AF5 deps; user accepts that slice stays AF4.
   - `pause-migration` — orchestrator stops; user removes saga code first or
     waits for upstream support.
   - `remove-feature-first` — user accepts they will rewrite the saga as a
     plain projection / event-handler before resuming.
3. Append a dated narrative entry to `learnings.md` (orchestrator-side):
   "Saga detected at <file list>. Decision: <choice>. Reason: <user words>."
4. Emit Output. The orchestrator commits a decision-only record
   (`recipe: saga`, `decisions: [saga: <choice>]`) — no code change.

## End condition

Never green automatically. The orchestrator commits the user's decision; the
saga code itself is not migrated. Stabilization will surface the residual
compile/runtime impact.

## Output

- target: <file list> | "n/a"
- decisions:
    - saga: <accept-stays-af4 | pause-migration | remove-feature-first>
- needs-user-decision: false   # already resolved by AskUserQuestion above
- notes: AF5 reframes long-running coordination as process-managers; no automatic rewrite. Currently not supported by this skill. Workflow support is on the Axoniq roadmap — contact Axoniq.

## Caveats

- Even with `accept-stays-af4`, sagas reference AF4-only types (`SagaConfigurer`, `@StartSaga`, `org.axonframework.modelling.saga.*`). Stabilization MUST exclude these files from the AF5 build path or the project won't compile.
- A saga slice that depends on `DeadlineManager` (typical) means the deadline-manager unsupported-recipe will fire too — record both decisions.

## When this recipe will be replaced

When AxonIQ ships official process-manager / workflow support and the
migration guide adds a saga → workflow path. At that point this recipe
moves to `Mode: iterative` with a real Procedure.
