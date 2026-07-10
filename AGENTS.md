# Agent Instructions

## Highest Rule

`CONSTITUTION.md` is the highest-priority document in this repository. Every AI Agent must read it before planning or editing.

## Required Reading Order

Before planning or editing, read these files in order:

0. `.ai/`
1. `CONSTITUTION.md`
2. `AGENTS.md`
3. `ARCHITECTURE.md`
4. `PROJECT_STATE.md`
5. `ROADMAP.md`
6. `CURRENT_TASK.md`
7. `DECISION_LOG.md`
8. `SESSION_HANDOFF.md`

## Context Priority

When context conflicts, follow this order:

1. `CURRENT_TASK.md`
2. `SESSION_HANDOFF.md`
3. `DECISION_LOG.md`
4. `PROJECT_STATE.md`
5. `ARCHITECTURE.md`
6. `ROADMAP.md`
7. `README.md`
8. Chat History

## Working Rules

- Always read `.ai/` before starting work.
- Do not guess when repository documents can answer the question.
- Keep work small and scoped to `CURRENT_TASK.md`.
- Treat Enhancer as a self-hosting AI Development Operating System, not a Cursor clone.
- Follow Document Driven Development: Constitution, Architecture, ADR, Task, Implementation, Test, Documentation Update.
- Keep proposals, accepted decisions, and implemented state separate.
- Update project documents whenever implementation state, task state, roadmap, architecture, or decisions change.
- Run relevant tests before reporting completion when tests exist.
- Report any test that could not be run and why.
- Commit only when the user requests it or the session-close prompt explicitly requires it.
- Do not push unless the user explicitly asks.

## Document Driven Development

Every implementation must follow this sequence:

1. Confirm the work does not violate `CONSTITUTION.md`.
2. Check or update `ARCHITECTURE.md`.
3. Record important decisions in `DECISION_LOG.md`.
4. Define or confirm the active task in `CURRENT_TASK.md`.
5. Implement the smallest scoped change.
6. Run relevant compile and test checks.
7. Update documentation, state, roadmap, and handoff files.

## Definition Of Done

A task is done only when:

- Compile succeeds, when a build exists.
- Tests pass, when tests exist.
- Documents are current.
- `CURRENT_TASK.md` reflects task completion or the next task.
- `SESSION_HANDOFF.md` is updated.
- Commit is completed when required.

## Session Close Requirements

Before ending a work session:

1. Check changed files.
2. Run relevant tests.
3. Update `PROJECT_STATE.md`.
4. Update `CURRENT_TASK.md`.
5. Update `ROADMAP.md` if milestone state changed.
6. Update `ARCHITECTURE.md` if architecture changed.
7. Add new accepted decisions to `DECISION_LOG.md`.
8. Update `SESSION_HANDOFF.md`.
9. Update `CHANGELOG.md` when notable changes occurred.
10. Review the diff.
11. Commit if required.
