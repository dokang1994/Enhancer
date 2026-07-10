# Session Close Prompt

Read and execute this prompt before ending a Codex work session.

## Required Steps

1. Check changed files.
2. Run relevant tests.
3. Update `PROJECT_STATE.md`.
4. Update `CURRENT_TASK.md`.
5. Update `ARCHITECTURE.md` if architecture changed.
6. Add new accepted decisions to `DECISION_LOG.md`.
7. Update `SESSION_HANDOFF.md` so the next agent can continue.
8. Update `CHANGELOG.md` when notable changes occurred.
9. Review the final diff.
10. Commit with an appropriate message if commit is part of this session.

## Final Report

Report:

- Changed files
- Verification commands and results
- Commit hash, if committed
- Remaining risks or next task

## Constraint

Do not push unless the user explicitly asks.
