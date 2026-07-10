# 00 - Project Overview

## Codex Prompt

You are building Enhancer.

Enhancer is not a Cursor clone. Enhancer is a self-hosting AI Development Operating System that helps AI agents remember a project through repository documents, plan the next task, and assist implementation.

Before writing code, read:

1. `CONSTITUTION.md`
2. `AGENTS.md`
3. `ARCHITECTURE.md`
4. `PROJECT_STATE.md`
5. `ROADMAP.md`
6. `CURRENT_TASK.md`
7. `DECISION_LOG.md`
8. `SESSION_HANDOFF.md`

Follow Document Driven Development:

```text
CONSTITUTION
↓
Architecture
↓
ADR / Decision Log
↓
Task
↓
Implementation
↓
Test
↓
Documentation Update
```

## Goal

Build the core features needed for an AI coding platform:

- Agent Loop
- Context Builder
- Tool System
- Skill System
- Memory
- Planner
- Task Queue
- Prompt Builder
- Git Tool
- Terminal Tool
- File Tool
- Search Tool
- RAG
- MCP
- Multi Agent
- Background Agent
- Plugin System
- CLI
- VSCode Extension
- Web Dashboard

## Non-Goal

Do not build an LLM, Transformer, fine-tuning system, or AI model.

## Technology

- Java 17
- Spring Boot 3
- Gradle
- JUnit5
- Mockito
- Ollama
- Qwen
- Codex CLI
- VSCode
- Git

## Development Rules

- Always implement one small task.
- Always keep code compilable.
- Always add or update tests when code changes.
- Always update project documents after implementation.
- Do not add unnecessary abstractions.
- Do not apply DDD in the early phase.

## First Milestone

The first milestone is self-hosting context awareness:

Enhancer must read its own repository documents and produce a structured project context that later components can use to propose the next task.

## Checklist

- [ ] Repository documents exist.
- [ ] Context reader exists.
- [ ] Context reader tests pass.
- [ ] Planner can propose next tasks.
- [ ] Agent loop can execute a selected task with tools.
