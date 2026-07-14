# Session Close Prompt

Read and execute this prompt before ending a Codex work session.

## Required Steps

1. Check changed files.
2. Run relevant tests.
3. Update `PROJECT_STATE.md`.
4. Update `CURRENT_TASK.md`.
5. Update `ARCHITECTURE.md` if architecture changed.
6. Add new accepted decisions to `DECISION_LOG.md`.
7. Distill reusable knowledge:
   - Promote project-independent repeatable procedures to a validated Skill and synchronize `skills/INDEX.md`.
   - Promote repository-specific rationale or pitfalls to `DECISION_LOG.md` or an ADR.
   - Do not duplicate promoted knowledge in `SESSION_HANDOFF.md`.
8. Update `SESSION_HANDOFF.md` so the next agent can continue.
9. Update `CHANGELOG.md` when notable changes occurred.
10. Review the final diff.
11. Commit with an appropriate message if commit is part of this session.
12. Confirm the recorded lifecycle state is supported by fresh evidence and does not imply release.

## Final Report

Report:

- Changed files
- Verification commands and results
- Lifecycle state and checks not run
- Commit hash, if committed
- Remaining risks or next task

## Constraint

Do not push unless the user explicitly asks.

## Handoff Requirement

`SESSION_HANDOFF.md` must be complete enough for a future AI session to recover without chat history.

At minimum, include:

- completed work
- current state
- next task
- relevant files
- decisions made
- open issues
- commands verified
- instructions for the next agent
