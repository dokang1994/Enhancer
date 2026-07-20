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

## RunRecord Execution Memory

Gate 4 stores each finalized run as a typed, versioned, SHA-256-checked binary envelope. A record preserves task and request inputs, the policy bound to the worker execution, Tool result and bounded evidence, the expected content digest, verification decision, iterations, and worker/final stop reasons. Its constructor rejects lifecycle combinations that the governed worker and verification flow cannot produce. A new store instance can replay the record without chat history. RunRecords are execution history; they do not override canonical project decisions or task authority.

## Distillation

- Promote project-independent repeatable procedures to validated Skills.
- Promote repository-specific rationale and pitfalls to `DECISION_LOG.md` or an ADR.
- Append per-increment verification evidence to `docs/verification-log.md`. It is written once and never revised, and it is not a session-start document.
- Do not duplicate promoted knowledge in `SESSION_HANDOFF.md`.
- Write each fact only to its owning document, per Constitution Section 4. Delete duplicates rather than synchronizing them.
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
