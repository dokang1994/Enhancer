# 2026-07-10: Manage Major Designs As RFCs

Status: Accepted Decision

Decision:

Enhancer will manage major design areas as RFC-style Markdown documents under `docs/rfcs/`, starting with RFC-0001 through RFC-0012.

Rationale:

The project is large enough that design topics need stable identifiers, reviewable history, and clear references. RFC-style documents make long-term architecture easier to maintain across multiple AI agents and sessions.

Consequences:

- Major architecture changes should add or update an RFC.
- Accepted direction should still be summarized in `DECISION_LOG.md`.
- RFC statuses distinguish Draft, Accepted, Implemented, and Superseded.
- The initial RFC track covers Constitution, AI Behavior, Prompt Contract, Context Builder, Planner, Tool, Skill, Memory, Multi-Agent, AI Operating System, Plugin SDK, and Self Improvement.
