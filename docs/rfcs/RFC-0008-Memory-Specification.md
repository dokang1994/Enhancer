# RFC-0008: Memory Specification

Status: Accepted

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
- Distilled knowledge is promoted instead of accumulating raw session notes.

## Distillation Loop

- Promote a project-independent repeatable procedure to a Skill and synchronize `skills/INDEX.md`.
- Promote repository-local rationale or pitfalls to `DECISION_LOG.md` or an ADR.
- Do not duplicate promoted knowledge in `SESSION_HANDOFF.md`.
- Embeddings and vector search remain out of scope until Markdown memory is reliable.

## Skill And Memory Boundary

- Reproducible independently of Enhancer: Skill.
- Tacit knowledge specific to this repository: repository memory.

## Prompt Book

### Codex Prompt

Implement memory from repository documents first. Apply the session-close distillation loop. Do not add embeddings or vector search until Markdown memory is reliable.

### Claude Prompt

Review memory layering for conflicts, stale document risks, and missing update rules.

### GPT Prompt

Explain how Enhancer recovers from memory loss using repository documents.
