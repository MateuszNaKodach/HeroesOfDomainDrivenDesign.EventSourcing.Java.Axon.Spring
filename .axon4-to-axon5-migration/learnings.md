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

## 2026-05-09 — Phase 1: OpenRewrite went past "mechanical" — already produced correct AF5 shape for `CREATE_IF_MISSING`

- Context: Phase 1 (openrewrite, Path B) rewrote `Army`, `Astrologers`, `Calendar`, `Dwelling` to AF5 `@EventSourced(tagKey, idType)`.
- Initial impression (incorrect): "the recipe removed `@CreationPolicy(CREATE_IF_MISSING)` without an AF5 equivalent — behaviour is now lost".
- Correction (verified on Army during Phase 2): the recipe DID produce the AF5 equivalent. Per [aggregate/creation-policy-decision.md](../.claude/skills/axon4-to-axon5-migration/references/aggregate/creation-policy-decision.md), the AF5 shape for `CREATE_IF_MISSING` is "**instance** `@CommandHandler` (NOT static) + no-arg `@EntityCreator`". OpenRewrite produced exactly that:
  - Command handlers stayed instance (not made static).
  - A no-arg constructor annotated `@EntityCreator` was added to the aggregate.
  - `apply(...)` was rewritten to `eventAppender.append(...)`.
  - The `RemoveCreatureFromArmyTest.givenEmptyArmy_...` expectation needed updating: AF4 would throw `AggregateNotFoundException` for a missing aggregate, but AF5 with no-arg `@EntityCreator` materialises an empty entity and runs the instance handler — so the domain rule (`Can remove only present creatures`) fires instead. This is the documented gotcha in the decision-matrix doc.
- Stranded comment: the source still says `// performance downside in comparison to constructor` — was a note about `CREATE_IF_MISSING`'s performance cost on EVERY command. Still loosely accurate (instance handler still re-loads the aggregate) but the original referent is gone. Left in place; can be cleaned up during stabilization.
- Implication: each per-aggregate Phase 2 step is mostly **verification**, not heavy rewriting — confirm the entity-creator pattern matches what AF4's `CreationPolicy` value implied, fix any test expectations that asserted on AF4-only exceptions, and verify scoped tests pass. See Army (commit pending) as the canonical example.
- See: phase-1 commit `1911b46`, phase-2-Army commit (pending).
