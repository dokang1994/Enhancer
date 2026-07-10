# 02 - Agent Loop

## Codex Prompt

Implement only the Agent Loop slice.

Do not implement tools, LLM integration, memory, or planner in this task unless `CURRENT_TASK.md` explicitly says so.

## Goal

The Agent Loop coordinates repeated work until a task is complete or a maximum iteration count is reached.

## Concepts

- `Agent`: owns execution behavior.
- `AgentLoop`: runs the loop.
- `AgentContext`: carries current state.
- `Task`: describes the work.
- `TaskStatus`: tracks pending, running, completed, failed.

## Rules

- Use a `while` based loop.
- Limit max iterations to 20.
- Stop on task completion.
- Stop on max iteration.
- Keep implementation deterministic and testable.

## Candidate Classes

```text
Agent
AgentLoop
AgentContext
Task
TaskStatus
AgentLoopResult
```

## Sequence

```text
Task
↓
AgentLoop
↓
Agent
↓
AgentContext update
↓
TaskStatus check
```

## Tests

Cover:

- completes before max iteration
- stops at max iteration
- failed task stops the loop
- iteration count is reported

## Out Of Scope

- Tool execution
- Prompt building
- LLM calls
- Multi-agent routing
