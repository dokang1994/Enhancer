# RFC-0008: Memory Specification

Status: Draft

## Purpose

Define Enhancer memory layers.

## Memory Layers

```text
Short Memory
→ Current Task

Mid Memory
→ Decisions

Long Memory
→ Architecture
→ Repository
→ ADR
```

## Rules

- AI model memory is not trusted.
- Repository documents are durable memory.
- `SESSION_HANDOFF.md` is short-term session memory.
- `DECISION_LOG.md` is mid-term design memory.
- `ARCHITECTURE.md` and repository state are long-term memory.

## Prompt Book

### Codex Prompt

Implement memory from repository documents first. Do not add embeddings or vector search until Markdown memory is reliable.

### Claude Prompt

Review memory layering for conflicts, stale document risks, and missing update rules.

### GPT Prompt

Explain how Enhancer recovers from memory loss using repository documents.
