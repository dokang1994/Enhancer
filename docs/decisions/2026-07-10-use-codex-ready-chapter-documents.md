# 2026-07-10: Use Codex-Ready Chapter Documents

Status: Accepted Decision

Decision:

Enhancer will maintain feature documents under `docs/` as Codex-ready prompts. Each document should describe the goal, architecture, task boundary, tests, and out-of-scope items for a major platform capability.

Rationale:

The project is too large to drive from chat history. Chapter-based Markdown specifications allow Codex, Claude, GPT, and future Enhancer agents to implement one slice at a time from repository state.

Consequences:

- `docs/` is part of the operating system for development, not passive documentation.
- New major capabilities should receive a prompt-style specification before implementation.
- Implementation should proceed sprint by sprint rather than attempting a full Cursor-like platform at once.
