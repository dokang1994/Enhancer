# RFC-0009: Multi Agent

Status: Accepted

## Purpose

Define agent roles and communication rules.

## Roles

```text
Architect
↓
Planner
↓
Coder
↓
Reviewer
↓
Tester
↓
Document Writer
```

## Rules

- Agents communicate through structured artifacts.
- Repository documents remain source of truth.
- One owner must control final task state.
- Reviewer can reject incomplete work.
- Human owner approves push and major direction.

## Reference Alignment

- Sequential handoff remains the first implementation model; parallel execution is deferred.
- Review stages may later use repository-defined requesting-review and receiving-review Skills.
- Agents exchange structured repository artifacts and keep coordination context small.
- Parallel-agent and subagent-driven workflows remain future references until the single-agent loop is stable.

## Prompt Book

### Codex Prompt

Do not implement Multi-Agent runtime until single-agent flow works. Start with sequential handoff artifacts.

### Claude Prompt

Review role boundaries, ownership, and handoff failure risks.

### GPT Prompt

Explain the agent roles and propose a safe first Multi-Agent workflow.
