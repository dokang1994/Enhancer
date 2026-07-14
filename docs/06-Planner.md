# 06 - Planner

## Codex Prompt

Design the Planner after the Context Reader exists. The Planner must propose tasks from repository context, not from chat memory.

## Goal

The Planner turns project context into candidate next tasks.

## Input

- structured repository context
- current task
- roadmap
- decision log
- project state

## Output

Use a simple task proposal:

```text
title
reason
scope
acceptanceCriteria
outOfScope
risk
```

## Rules

- Planner proposes; it does not silently execute.
- Human approval is required before implementation.
- Proposals are not accepted decisions.
- Accepted decisions must be written to `DECISION_LOG.md`.
- Prefer tasks that fit one focused failing-check, minimal-change, passing-verification cycle.
- Do not force a commit per task; follow `AGENTS.md` and user instruction.
- Do not emit vague tasks, cross-task shorthand, or placeholders.

## Tests

Cover:

- proposes a next task from an open roadmap item
- does not override current task
- marks unknown information as risk
- keeps proposal separate from accepted decision

## Prompt Book

### Codex Prompt

Implement Planner behavior only after repository context reading exists. The Planner may propose tasks but must not execute them or convert proposals into accepted decisions.

### Claude Prompt

Review Planner output for scope creep, missing acceptance criteria, and confusion between Proposal, Accepted Decision, and Implemented.

### GPT Prompt

Given repository context, propose the next useful task with reason, scope, acceptance criteria, out-of-scope items, and risks.
