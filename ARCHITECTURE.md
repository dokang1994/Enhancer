# Architecture

## Status

The first two product architecture slices, Repository Context Reader and deterministic Task Planner, are implemented as a simple single Gradle project.

The accepted product direction is Self-hosting AI Development Operating System.

## Target Architecture

Enhancer will evolve toward these major components:

- Kernel: constitution, AI rules, scheduler, and core operating policies.
- Prompt Engine: converts tasks into model-specific prompts.
- Context Builder: reads repository documents and code context.
- Memory: treats Git repository state as durable memory.
- Planner: proposes next tasks from current project state.
- Task Queue: tracks selected and pending work.
- Prompt Builder: creates prompts for AI agents and tools.
- Tool System: exposes file, terminal, search, Git, and external tools.
- Skill System: stores reusable agent workflows.
- Agent Loop: coordinates context, planning, tool use, verification, and handoff.
- MCP and Plugin System: integrates external capabilities.
- Agent Runtime: supports single-agent and future multi-agent operation.
- CLI, VSCode Extension, and Web Dashboard: user-facing control surfaces.
- SDK: allows plugins such as Oracle, MyBatis, Spring, WebSquare, React, and Vue.

## Operating System Model

Enhancer is modeled as an AI Development Operating System:

```text
Kernel
↓
Scheduler
↓
Planner
↓
Memory
↓
Tool
↓
Skill
↓
Agent
↓
Plugin
↓
LLM
```

Cursor-like behavior is treated as an application-level capability on top of Enhancer, not the identity of Enhancer itself.

## Specification Architecture

`docs/` contains Codex-ready architecture and implementation guides. These documents are part of the product operating system, not secondary notes.

- `docs/00-Project-Overview.md`: project identity and scope
- `docs/01-Development-Environment.md`: environment checks and bootstrap target
- `docs/02-Agent-Loop.md`: Agent Loop design
- `docs/03-Tool-System.md`: Tool System design
- `docs/04-Skill-System.md`: Skill System design
- `docs/05-Memory.md`: repository-backed memory design
- `docs/06-Planner.md`: task planning design
- `docs/07-MCP.md`: MCP integration direction
- `docs/08-Multi-Agent.md`: multi-agent collaboration model
- `docs/09-Background-Agent.md`: background agent safety model
- `docs/10-Roadmap.md`: 30-day self-hosting plan
- `docs/11-Architecture.md`: expanded architecture guide

## RFC Architecture

Major design areas are tracked in `docs/rfcs/`.

- `RFC-0001`: Constitution
- `RFC-0002`: AI Behavior Specification
- `RFC-0003`: Prompt Contract
- `RFC-0004`: Context Builder
- `RFC-0005`: Planner
- `RFC-0006`: Tool Specification
- `RFC-0007`: Skill Specification
- `RFC-0008`: Memory Specification
- `RFC-0009`: Multi Agent
- `RFC-0010`: AI Operating System
- `RFC-0011`: Plugin SDK
- `RFC-0012`: Self Improvement

## First Architecture Slice

The first product slice should be a Repository Context Reader.

Expected responsibility:

- Read the required project documents in constitution order.
- Preserve source order and document identity.
- Report missing required documents clearly.
- Produce a structured context object that later components can use.

Implemented package: `com.enhancer.context`

The slice uses immutable context records, an enum as the canonical required-document order, and a filesystem reader. It has no Spring wiring because the current behavior does not require an application container.

## Planner Slice

The first Planner slice is implemented in `com.enhancer.planner` and consumes `ProjectContext` directly.

It is deterministic: an active `CURRENT_TASK.md` blocks a new proposal; otherwise the first ready phase in `ROADMAP.md` becomes one structured proposal. Proposal state is explicit and remains separate from accepted decisions and implementation state. The slice does not call an LLM, mutate documents, rank alternatives, or execute work.

## Architectural Principles

- Repository documents are product inputs, not only project management notes.
- Keep the first implementation minimal.
- Prefer Java 17, Spring Boot 3, Gradle, JUnit5, and Mockito.
- Do not introduce DDD early.
- Do not add abstractions until duplication or complexity justifies them.
- Accepted decisions belong in `DECISION_LOG.md`.
- Implemented state belongs in `PROJECT_STATE.md`.
- Repository Skills use validated `skills/<name>/SKILL.md` definitions with least-privilege capability categories.
- Proposed Skill catalog entries are design candidates, not loadable runtime inputs.
- Repository memory is distilled by promoting reusable procedures to Skills and repository-local rationale to decisions or ADRs.

## Open Architecture Questions

- CLI entry point is not selected yet.
- Context size and token budgeting strategy are not selected yet.
- Future LLM-backed Planner input/output schema is not selected yet.
