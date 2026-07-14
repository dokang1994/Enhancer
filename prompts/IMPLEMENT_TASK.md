# Implement Task Prompt

Use this prompt when implementing the current task.

## Required Steps

1. Read the required files listed in `AGENTS.md`.
2. Confirm the work follows `CONSTITUTION.md`.
3. Check `ARCHITECTURE.md` and update it first if the architecture must change.
4. Record accepted design decisions in `DECISION_LOG.md`.
5. Confirm the active task from `CURRENT_TASK.md`.
6. Inspect the relevant code and tests.
7. Make the smallest coherent change that satisfies the task.
8. For observable feature or bug-fix behavior, add a failing focused test first and confirm its expected failure. For excluded changes, record the alternative verification.
9. Implement the minimum scoped change and run fresh relevant verification commands.
10. Update project documents if state, architecture, roadmap, task, or decisions changed.
11. Report changed files, fresh verification evidence, and remaining risks.

## Scope Rules

- Do not expand beyond `CURRENT_TASK.md` without explicit user approval.
- Do not convert proposals into decisions unless the user accepts them.
- Do not push without explicit user approval.
