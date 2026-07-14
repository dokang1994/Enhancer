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
- Project Brain views over repository memory, Workspace snapshots, RunRecords, and indexed knowledge

## Project Brain Boundary

Project Brain is a provenance-preserving aggregate for reasoning. Repository documents remain canonical memory, Workspace snapshots remain time-sensitive observations, and RunRecords remain execution history. Project Brain must retain source, freshness, and authority class instead of flattening them into untrusted hidden memory.

Its graph projections are:

- Decision Graph;
- Architecture Graph;
- Dependency Graph;
- Task Graph;
- Execution Graph.

Every node and edge identifies its source and can be rebuilt. Graph storage does not become a second silent source of truth.

## Distillation

- Promote project-independent repeatable procedures to validated Skills.
- Promote repository-specific rationale and pitfalls to `DECISION_LOG.md` or an ADR.
- Do not duplicate promoted knowledge in `SESSION_HANDOFF.md`.
- Keep embeddings and vector search deferred until Markdown memory is reliable.

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
