# 00 - Project Overview

## Codex Prompt

You are building Enhancer.

Enhancer is not a Cursor clone. Enhancer is a self-hosting AI Development Operating System that helps AI agents remember a project through repository documents, plan the next task, and assist implementation.

Before writing code, read:

0. `.ai/`
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
- Agent Runtime and Scheduler
- Event Bus, Message Bus, and IPC adapters
- Workspace and Project Brain
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
- MCP Server and Client
- Model Gateway and Router
- Multi Agent
- Background Agent
- Plugin System
- CLI
- VSCode Extension
- Web Dashboard
- Desktop and API
- Plugin Marketplace
- Governed Cloud Sync

## Final Platform Direction

Enhancer OS is an event-driven platform, not a Chat -> Tool -> Stop wrapper. Its target execution form is:

```text
Event -> Planner -> Queue -> Coder -> Queue
      -> Reviewer -> Queue -> Tester -> Memory
```

Workspace supplies current files, Git state, diagnostics, terminal metadata, and selections. Project Brain combines those observations with governed repository memory. Skills provide reusable composable workflows. MCP allows external clients and servers to share governed Tools, resources, Workspace views, and memory. Agents communicate through typed messages rather than direct peer calls.

Product milestones:

- V1 provides an AI development experience with Cursor-level productivity over shared Workspace-aware interfaces.
- V2 provides the Agent, Workflow, Skill, Memory, MCP, model-routing, and plugin platform.
- V3 provides the AI Kernel, Project Brain knowledge graphs, multi-agent OS, hybrid model privacy routing, marketplace ecosystem, and governed synchronization.

These are product outcomes, not implementation order. Dependency-ordered Delivery Gates may build V2 platform foundations before every V1 interface is polished without claiming that either milestone is complete.

Self-hosting development means Enhancer applies its governed workflow to the Enhancer repository. Local or hybrid model execution means approved provider routing. Local inference does not by itself make Enhancer self-hosting, and self-hosting does not require Ollama, Qwen, or another particular provider.

Project Brain projects canonical documents, code, Git, tasks, decisions, tests, bugs, commits, issues, PRs, and RunRecords into provenance-bearing Decision, Architecture, Dependency, Task, and Execution graphs. The graph is rebuildable and cannot silently replace its sources.

## Non-Goal

Do not build an LLM, Transformer, fine-tuning system, or AI model.

## Technology Status

Current verified build and repository tools:

- Java 17
- Gradle 8.4 Wrapper
- JUnit 5
- Mockito
- Git

Planned or conditional integrations, not current runtime dependencies:

- Spring Boot 3, only when application wiring justifies it
- Ollama and Qwen as possible local-model adapters; no provider is selected
- Codex CLI and VS Code as development or future integration surfaces

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

## Foundation Status

- [x] Repository documents exist.
- [x] Context Reader exists and its tests pass.
- [x] Planner proposes the next Delivery Gate from the canonical Roadmap.
- [x] A governed read-only Tool result enters the Agent Loop and stops at `AWAITING_VERIFICATION` in an integration test.
- [x] Independent verification and durable RunRecord persistence are integrated.
- [ ] A supported CLI runs the flow against a real project.

## Prompt Book

### Codex Prompt

Read this document with `CONSTITUTION.md`, `AGENTS.md`, and `CURRENT_TASK.md`. Do not implement the whole platform. Identify the smallest next task that advances Enhancer toward self-hosting and update project documents after the work.

### Claude Prompt

Review this project overview as an architecture and product-scope document. Find unclear boundaries, missing decisions, and risks that should be recorded in `DECISION_LOG.md` before implementation.

### GPT Prompt

Use this document to explain the current Enhancer vision, scope, and next practical milestone. Separate Proposal, Accepted Decision, and Implemented state.
