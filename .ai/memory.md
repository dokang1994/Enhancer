# Memory Policy

AI memory is not trusted.

The Git repository is the durable memory.

The ignored `.enhancer/session-checkpoint/` artifact is durable execution position for
one local development session. It references the active task and evidence but is not
canonical project memory, verification, completion, or delivery history. Inspect it on
resume and clear it only from a stable, artifact-matched state after orderly close.

At minimum, a new session must recover state from:

- `CONSTITUTION.md`
- `AGENTS.md`
- `PROJECT_STATE.md`
- `CURRENT_TASK.md`
- `SESSION_HANDOFF.md`

For architecture work, also read:

- `ARCHITECTURE.md`
- `DECISION_LOG.md`

`docs/verification-log.md` holds the append-only evidence behind the state in
`PROJECT_STATE.md`. It is deliberately not a session-start document: it grows without
bound and is consulted only when a specific past verification claim matters.

`DECISION_LOG.md` is an index carrying each accepted decision's heading and status; the
reasoning lives in `docs/decisions/`. Those files are also deliberately outside the
session-start set — read the one you need rather than all of them.

- Promotion boundary: a project-independent repeatable procedure becomes a Skill under RFC-0007; repository-specific rationale or pitfalls remain repository memory. Execute distillation through `prompts/SESSION_CLOSE.md` Step 7.
