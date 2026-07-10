# 08 - Multi Agent

## Codex Prompt

Design Multi-Agent support after a single Agent Loop works.

## Goal

Multiple agents should collaborate by role, not by uncontrolled parallel chatter.

## Candidate Roles

- Architect Agent
- Planner Agent
- Implementation Agent
- Review Agent
- Test Agent

## Rules

- One agent must own final task state.
- Agents communicate through structured artifacts.
- Repository documents remain the source of truth.
- Human approval gates destructive actions.

## First Multi-Agent Slice

Start with a sequential handoff:

```text
Planner Agent
↓
Implementation Agent
↓
Review Agent
```

## Tests

Cover:

- handoff preserves task context
- review can reject incomplete work
- final state updates session handoff
