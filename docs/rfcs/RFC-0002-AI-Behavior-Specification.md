# RFC-0002: AI Behavior Specification

Status: Accepted

## Purpose

Define how AI agents must behave when users give requests.

## Core Questions

When the user requests work, the AI must decide:

- Is the requirement clear?
- Should the AI ask a question?
- Should the AI search or inspect local context?
- Should the task be split?
- Is an ADR required?
- Is tool execution required?
- Is a Skill applicable?
- Is a plan required before editing?
- Was user intent explored before creative implementation?
- Were applicable behavior tests written and observed failing first?
- Has completion been proven with fresh command output?

## Initial Rules

- If a requirement is unclear, ask before implementation.
- If a task is likely to take more than four hours, split it.
- If architecture changes, create or update an ADR in `DECISION_LOG.md`.
- If three or more files will be modified, write a plan first.
- If a tool action is risky or destructive, request approval.
- If a matching Skill exists, read it before acting.
- Explore intent and design before creative work when requirements or tradeoffs are not already fixed by repository context.
- For observable feature and bug-fix behavior, write a failing test first, observe the expected failure, and then implement the minimum change.
- Documentation-only and behavior-preserving mechanical changes may use an appropriate non-test verification method when the reason is reported.
- Never claim completion, passing tests, or a fix without running the relevant verification and reading its output.

## Prompt Book

### Codex Prompt

Before implementing, classify the request using this behavior specification. Ask for clarification when required, create a plan when scope is broad, apply test-first where behavior changes, and verify before completion claims.

### Claude Prompt

Review the AI behavior rules for missing escalation cases, unsafe assumptions, and ambiguity in when to ask questions.

### GPT Prompt

Given a user request, decide whether to ask, plan, split, search, use a skill, create an ADR, or implement.
