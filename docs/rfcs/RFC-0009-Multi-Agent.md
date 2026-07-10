# RFC-0009: Multi Agent

Status: Draft

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

## Prompt Book

### Codex Prompt

Do not implement Multi-Agent runtime until single-agent flow works. Start with sequential handoff artifacts.

### Claude Prompt

Review role boundaries, ownership, and handoff failure risks.

### GPT Prompt

Explain the agent roles and propose a safe first Multi-Agent workflow.
