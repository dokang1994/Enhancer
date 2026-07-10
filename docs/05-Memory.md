# 05 - Memory

## Codex Prompt

Implement memory as repository-backed memory first. Do not add Vector DB, embeddings, or RAG until plain Markdown memory works.

## Goal

Enhancer must not depend on AI session memory. It must recover project state from files.

## Memory Sources

Priority:

1. `CURRENT_TASK.md`
2. `SESSION_HANDOFF.md`
3. `DECISION_LOG.md`
4. `PROJECT_STATE.md`
5. `ARCHITECTURE.md`
6. `ROADMAP.md`
7. `README.md`
8. Chat History

## First Implementation

Start with:

- read required Markdown files
- preserve file path
- preserve read order
- preserve content
- report missing files

## Later Implementation

Add later:

- context summarization
- token budgeting
- semantic search
- RAG
- historical commit lookup

## Tests

Cover:

- all required documents loaded
- missing document error
- priority order preserved
- empty document behavior

## Prompt Book

### Codex Prompt

Implement the repository-backed memory slice selected in `CURRENT_TASK.md`. Start with Markdown documents only. Preserve path, order, and content, and add tests for missing required files.

### Claude Prompt

Review the memory design for source-of-truth conflicts, priority mistakes, token-limit risks, and premature RAG complexity.

### GPT Prompt

Summarize the memory policy and explain how a new AI session should recover project context from repository files.
