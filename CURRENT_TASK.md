# Current Task

## Status

Ready

## Task

Implement the first self-hosting slice: Repository Context Reader.

## Context

Enhancer's 30-day goal is to propose next tasks from repository context. The first required capability is reading the repository documents that define project memory.

This task should create the smallest useful implementation that reads required project documents and returns structured context for later planning.

## Required Documents

The reader must support the constitution startup order:

1. `CONSTITUTION.md`
2. `AGENTS.md`
3. `ARCHITECTURE.md`
4. `PROJECT_STATE.md`
5. `ROADMAP.md`
6. `CURRENT_TASK.md`
7. `DECISION_LOG.md`
8. `SESSION_HANDOFF.md`

## Acceptance Criteria

- A minimal Java 17 project structure exists.
- A Repository Context Reader can read the required documents from a project root.
- The reader preserves document path, read order, and content.
- Missing required documents are reported with a clear error.
- Focused unit tests cover success and missing-document cases.
- Project documents are updated after implementation.

## Out Of Scope

- LLM integration
- Vector DB or RAG
- Planner implementation
- Tool execution
- VSCode Extension
- Web Dashboard
- Push to remote repository
