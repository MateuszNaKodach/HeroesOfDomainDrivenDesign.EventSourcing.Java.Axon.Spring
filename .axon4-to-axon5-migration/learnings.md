# Axon Framework 4 → 5 Migration — Learnings

Append-only narrative. One dated entry per surprise, manual fix, or
non-obvious decision. Read on demand only — `progress.md` is the source
of truth for state.

Format per entry:

```
## YYYY-MM-DD — <one-line headline>

- Context: where in the migration this came up.
- Surprise: what was unexpected.
- Resolution: what was done. Link to commit `<sha>` if applicable.
```

---

## 2026-05-09 — Phase 1: `LATEST` resolves to AF4 recipe artifact

- Context: Phase 1 (openrewrite, Path B). Initial run with `recipeArtifactCoordinates=org.axonframework:axon-migration:LATEST`.
- Surprise: Maven resolved `LATEST` to the locally cached `axon-migration:4.13.1` (the AF4 line of the same artifact id), not the AF5 5.x line. Combined with `rewrite-maven-plugin:6.39.0`, this caused a bytecode `VerifyError` (`Type 'UsesType' is not assignable to 'JavaVisitor'`) before any rewrites ran. Spring milestones repo also returned 401 for non-authenticated metadata fetches, which made `LATEST` unable to discover AF5 versions over the wire.
- Resolution: pinned to explicit `5.1.1-SNAPSHOT` per recipe doc — fall-back path. `5.1.0` also failed because the `pom` is gated behind the auth-required `spring-milestones` repo. `5.1.1-SNAPSHOT` resolved successfully via `central.sonatype.com/repository/maven-snapshots/`.
- Takeaway for future runs in this project: always pass an explicit recipe version (recommend `5.1.1-SNAPSHOT` until a fully published 5.x release lands in unauthenticated central). `LATEST` is unsafe here.

## 2026-05-09 — Phase 1: `@CreationPolicy(CREATE_IF_MISSING)` silently dropped on 4 aggregates

- Context: Phase 1 (openrewrite, Path B) rewrote `Army`, `Astrologers`, `Calendar`, `Dwelling` to AF5 `@EventSourced(tagKey, idType)`.
- Surprise: the recipe REMOVED `@CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)` from all four aggregates (along with the import) without inserting an AF5 equivalent. The "create if missing on first command" behavior is now lost. Stranded source comments (e.g. `// performance downside in comparison to constructor`) no longer match the code.
- Resolution: deferred to Phase 2 (per-aggregate). Phase 2's recipe must restore equivalent semantics per aggregate via AF5's entity-creator model (`@EntityCreator` factory method or constructor-as-creator pattern). Until then, behavior diverges from AF4: commands that previously auto-created the aggregate will instead fail to load it.
- See: phase-1 commit (this commit).
