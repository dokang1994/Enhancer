# RFC-0012: Self Improvement

Status: Draft

## Purpose

Define how Enhancer helps develop itself.

## Loop

```text
Roadmap
↓
Planner
↓
Task 생성
↓
Codex
↓
Review
↓
Commit
↓
Roadmap Update
```

## Rules

- Enhancer may propose self-improvement tasks.
- Human approval is required before execution.
- Codex may implement approved tasks.
- Review is required before commit.
- Roadmap and handoff must be updated after completion.

## Target Outcome

Any LLM, including Codex, GPT, Claude, and Gemini, should be able to continue the same project at the same quality by reading repository documents.

## Prompt Book

### Codex Prompt

Implement self-improvement only after Planner, Context Builder, Tool System, and review workflow exist. Do not let Enhancer silently modify itself.

### Claude Prompt

Review self-improvement proposals for unsafe autonomy, missing approval gates, and poor rollback strategy.

### GPT Prompt

Given repository state, propose a self-improvement task and explain why it should or should not be accepted.
