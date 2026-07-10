# RFC-0005: Planner

Status: Draft

## Purpose

Define how Enhancer converts user goals and repository context into tasks.

## Example

User request:

```text
회원가입 만들어
```

Planner output:

```text
Task 1: DTO
Task 2: Controller
Task 3: Service
Task 4: Repository
Task 5: Test
```

## Rules

- Planner proposes tasks; it does not execute.
- Planner must keep task scope small.
- Planner must include acceptance criteria.
- Planner must identify risks and dependencies.
- Planner must not override an active `CURRENT_TASK.md` without approval.

## Prompt Book

### Codex Prompt

Implement Planner only after Context Builder exists. Start with deterministic task proposals from roadmap and current state.

### Claude Prompt

Review task decomposition for missing dependencies, excessive scope, and unclear acceptance criteria.

### GPT Prompt

Break a user goal into small tasks with order, acceptance criteria, and out-of-scope boundaries.
