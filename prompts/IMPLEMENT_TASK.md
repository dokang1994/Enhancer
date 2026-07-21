# Implement Task Prompt

Use this prompt when implementing the current task.

## Required Steps

1. Read the required files listed in `AGENTS.md`.
2. Confirm the work follows `CONSTITUTION.md`.
3. Check `ARCHITECTURE.md` and update it first if the architecture must change.
4. Record accepted design decisions in `DECISION_LOG.md`.
5. Confirm the active task from `CURRENT_TASK.md`.
6. Inspect the relevant code and tests.
7. Start or resume the development-session checkpoint. Before every mutating or
   verification step, record `STEP_PENDING` with the expected revision, current changed
   artifact paths, and the next executable action. Immediately afterward record
   `STEP_SUCCEEDED` or `STEP_FAILED` plus evidence references. A checkpoint does not
   promote lifecycle state.
8. Make the smallest coherent change that satisfies the task.
9. For observable feature or bug-fix behavior, add a failing focused test first and confirm its expected failure. Classify every RED failure against the active task, accepted decisions, Architecture, and repository build/runtime settings. For excluded changes, record the alternative verification.
10. If the RED contract is in scope and configuration-compatible, proceed directly with the minimum implementation needed to turn it GREEN. Missing production types or symbols are acceptable expected RED evidence. Do not absorb unrelated failures, flaky behavior, scope expansion, configuration conflicts, or work requiring new external/destructive authority; report those separately.
11. Run fresh relevant verification commands.
12. Update project documents if state, architecture, roadmap, task, or decisions changed.
13. Promote lifecycle state only when fresh evidence supports the promotion.
14. Report changed files, fresh verification evidence, checks not run, and remaining risks.

## Scope Rules

- Do not expand beyond `CURRENT_TASK.md` without explicit user approval.
- Do not convert proposals into decisions unless the user accepts them.
- Do not infer destructive or external-action authority from permission to implement.
- Do not treat the implementing Agent's claim as independent verification.
- Do not treat an expected missing-implementation RED failure as a blocker after its contract has passed the scope and configuration checks.
- Do not push without explicit user approval.
