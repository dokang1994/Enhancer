# Architecture

## Status

No product architecture has been implemented yet.

The accepted product direction is Self-hosting AI Development Operating System.

## Target Architecture

Enhancer will evolve toward these major components:

- Context Builder: reads repository documents and code context.
- Memory: treats Git repository state as durable memory.
- Planner: proposes next tasks from current project state.
- Task Queue: tracks selected and pending work.
- Prompt Builder: creates prompts for AI agents and tools.
- Tool System: exposes file, terminal, search, Git, and external tools.
- Skill System: stores reusable agent workflows.
- Agent Loop: coordinates context, planning, tool use, verification, and handoff.
- MCP and Plugin System: integrates external capabilities.
- CLI, VSCode Extension, and Web Dashboard: user-facing control surfaces.

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

## First Architecture Slice

The first product slice should be a Repository Context Reader.

Expected responsibility:

- Read the required project documents in constitution order.
- Preserve source order and document identity.
- Report missing required documents clearly.
- Produce a structured context object that later components can use.

## Architectural Principles

- Repository documents are product inputs, not only project management notes.
- Keep the first implementation minimal.
- Prefer Java 17, Spring Boot 3, Gradle, JUnit5, and Mockito.
- Do not introduce DDD early.
- Do not add abstractions until duplication or complexity justifies them.
- Accepted decisions belong in `DECISION_LOG.md`.
- Implemented state belongs in `PROJECT_STATE.md`.

## Open Architecture Questions

- Exact package structure is not selected yet.
- CLI entry point is not selected yet.
- Context size and token budgeting strategy are not selected yet.
- Planner input/output schema is not selected yet.
