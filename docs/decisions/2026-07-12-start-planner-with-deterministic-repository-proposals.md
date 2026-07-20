# 2026-07-12: Start Planner With Deterministic Repository Proposals

Status: Accepted Decision

Decision:

The first Planner consumes `ProjectContext`, blocks proposals while `CURRENT_TASK.md` is active, and otherwise proposes the first ready roadmap phase. Its output has explicit `PROPOSAL` state and structured reason, scope, acceptance criteria, out-of-scope items, and risks.

Rationale:

This reaches the first self-hosting planning behavior without introducing an LLM, hidden chat context, document mutation, or premature ranking logic.

Consequences:

- Planner behavior is deterministic and unit-testable.
- A proposal cannot be confused with an accepted decision.
- Natural-language planning, proposal ranking, persistence, and execution remain future work.
