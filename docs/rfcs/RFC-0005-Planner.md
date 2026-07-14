# RFC-0005: Planner

Status: Accepted

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
- Each implementation task should fit one focused verification cycle without placeholders.

## Task Granularity

- Prefer one cycle: failing test or check, minimal change, passing verification.
- Do not use vague instructions, references such as "same as Task N", or `TBD` placeholders.
- A task cycle does not require a commit. Commit behavior follows `AGENTS.md` and explicit user instruction.

## Plan Header

Every multi-step plan identifies the feature goal, architecture summary, technology constraints, and explicit scope boundaries.

## Execution Checkpoints

- Do not start implementation on `main` or `master` without user approval.
- When repository context cannot resolve a material ambiguity, stop and ask instead of guessing.

## Prompt Book

### Codex Prompt

Implement Planner only after Context Builder exists. Start with deterministic task proposals from roadmap and current state. Emit focused verification-cycle tasks with explicit scope boundaries.

### Claude Prompt

Review task decomposition for missing dependencies, excessive scope, and unclear acceptance criteria.

### GPT Prompt

Break a user goal into small tasks with order, acceptance criteria, and out-of-scope boundaries.
