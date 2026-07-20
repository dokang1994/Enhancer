# 2026-07-10: Operate Enhancer As A Real Open Source Project

Status: Accepted Decision

Decision:

Enhancer will be operated as a real open source project, not as a one-off chat artifact or documentation-only repository. The project will include documentation, code, ADRs, tests, inline specification examples, and shared prompts for Codex, Claude, and GPT.

Rationale:

The expected scope is too large for a single chat session. A Git-managed, chapter-based, reviewed workflow allows the project to grow over months without losing architectural consistency.

Consequences:

- Work proceeds by Sprint and small tasks.
- Documentation and code evolve together.
- ADR review is required for meaningful design changes.
- AI roles are explicit: Codex implements; ChatGPT supports architecture, backend design, agent research, documentation, and review.
- Git repository documents remain the source of truth.
