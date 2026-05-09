# Before/after migration examples

Curated, real-world AF4→AF5 aggregate migrations the agent can pattern-match
against. Each file is self-contained and project-agnostic — no
project-specific package paths leak into the skill itself.

## Filename convention

`NN-<short-case-slug>.md` — e.g. `01-simple-aggregate.md`,
`02-creation-policy-create-if-missing.md`.

The leading number gives a stable reading order; the slug describes the case.

## File template

Each example follows this structure:

````markdown
# <Case title>

**Why this case is interesting:** <one or two sentences describing what makes
this migration non-trivial — e.g. `@CreationPolicy(CREATE_IF_MISSING)` plus
mutable child collection, or polymorphic subtype with shared sourcing handlers.>

**Variant:** simple | creation-policy | multi-entity | polymorphic | spring | core

## Before (AF4)

```java
// minimal, self-contained AF4 source
```

## After (AF5)

```java
// minimal, self-contained AF5 source
```

## What changed

- bulleted list of the concrete edits, each cross-referenced to the relevant
  step in `SKILL.md` (e.g. "Step 7: tagged `cardId` in every event with
  `@EventTag(key = \"GiftCard\")`")
- include any subtle/silent changes (e.g. lost `snapshotTriggerDefinition`)
- include the test-fixture migration for the corresponding test class when
  applicable

## Caveats

- anything the agent should NOT generalise from this example to other projects
  (project-specific decisions, opinionated naming, etc.)
````

## Suggested starter cases

The placeholders in this folder cover the variants the skill detects:

| File | Variant | Notes |
|---|---|---|
| `01-simple-aggregate.md` | simple, Spring | The vanilla "one root, no children, no creation policy" path — the canonical happy path. |
| `02-creation-policy.md` | creation-policy | `CREATE_IF_MISSING` is the highest-risk row in the decision table — make sure the example shows the instance handler + no-arg `@EntityCreator` shape and the test that distinguishes it from `ALWAYS`. |
| `03-multi-entity.md` | multi-entity, Spring | Show `List<Child>` with `@EntityMember(routingKey = ...)`. If you have a `Map`-based AF4 case, include it as a separate file (`03b-multi-entity-map.md`) to highlight the breaking change. |
| `04-polymorphism.md` | polymorphic | AutoDetected path with `@EventSourcedEntity(concreteTypes = { ... })` on the base. Skip the Declarative path — link to the docs URL instead. |

You may add more files (e.g. `05-spring-snapshot-trigger-removal.md`,
`06-revision-event.md`) following the same template.
