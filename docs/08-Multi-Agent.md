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
- Agents communicate through typed messages and durable artifacts, never direct peer calls once the bus exists.
- Repository documents remain the source of truth.
- Human approval gates destructive actions.
- Keep sequential artifact handoff as the first model and defer parallel-agent execution until the single-agent loop is stable.
- Proposed review Skills are references only until their validated `SKILL.md` files exist.

## First Multi-Agent Slice

Start with a sequential handoff:

The early diagram below is conceptual. The target implementation routes every handoff through the Message Bus:

```text
Planner -> Queue -> Implementation -> Queue
        -> Review -> Queue -> Test -> Memory
```

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
- duplicate delivery does not duplicate side effects
- correlation, causation, provenance, and authority survive every queue hop

## Prompt Book

### Codex Prompt

Implement Multi-Agent support only after single-agent execution works. Start with sequential handoff and structured artifacts; do not build uncontrolled parallel agents.

### Claude Prompt

Review the Multi-Agent model for ownership ambiguity, handoff loss, and approval-gate failures.

### GPT Prompt

Describe the planned agent roles and propose the smallest safe Multi-Agent experiment.
