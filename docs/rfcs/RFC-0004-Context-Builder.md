# RFC-0004: Context Builder

Status: Draft

## Purpose

Define how Enhancer assembles relevant context for a task.

## Target Order

```text
Current Task
↓
Architecture
↓
Decision
↓
Code
↓
Git Diff
↓
Prompt
```

## Example

For a login-related task, the Context Builder should include only relevant files such as:

- `LoginController`
- `UserService`
- `SecurityConfig`
- `application.yml`

## Initial Scope

Start with repository Markdown documents before code-aware context selection.

## Future Scope

- code graph analysis
- Git diff awareness
- token budgeting
- relevance scoring
- RAG

## Prompt Book

### Codex Prompt

Implement the Context Builder incrementally. Start with required Markdown documents and preserve ordering before adding code relevance.

### Claude Prompt

Review context assembly order, missing source types, token-limit risks, and relevance-selection failure modes.

### GPT Prompt

Given a task, describe which context sources should be included and why.
