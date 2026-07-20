# 2026-07-10: Use Document Driven Development

Status: Accepted Decision

Decision:

Enhancer will follow Document Driven Development. New work must move through Constitution, Architecture, ADR / Decision Log, Task, Implementation, Test, and Documentation Update before it is considered complete.

Rationale:

Enhancer depends on repository documents as durable memory. If code changes happen before architecture, decisions, and tasks are clarified, future AI sessions will lose the reason behind the implementation.

Consequences:

- Agents must not jump directly from idea to code.
- Important architectural changes must be recorded before or during implementation.
- `CURRENT_TASK.md` remains the scope boundary for implementation.
- Documentation update is part of Definition of Done.
