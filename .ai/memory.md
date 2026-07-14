# Memory Policy

AI memory is not trusted.

The Git repository is the durable memory.

At minimum, a new session must recover state from:

- `CONSTITUTION.md`
- `AGENTS.md`
- `PROJECT_STATE.md`
- `CURRENT_TASK.md`
- `SESSION_HANDOFF.md`

For architecture work, also read:

- `ARCHITECTURE.md`
- `DECISION_LOG.md`

- Promotion boundary: a project-independent repeatable procedure becomes a Skill under RFC-0007; repository-specific rationale or pitfalls remain repository memory. Execute distillation through `prompts/SESSION_CLOSE.md` Step 7.
