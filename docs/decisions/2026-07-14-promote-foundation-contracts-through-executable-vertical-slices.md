# 2026-07-14: Promote Foundation Contracts Through Executable Vertical Slices

Status: Accepted Decision

Context:

- The repository has 21 production Java files and approximately 479 production lines centered on contracts and deterministic control rules.
- Context, planning, loop termination, and Tool evidence invariants are tested, but there is no application entry point, concrete Tool execution, Agent-Loop/Tool integration, evidence persistence, LLM boundary, or end-to-end runtime.
- The existing roadmap uses `Implemented` for narrow slices, which can be mistaken for operational product completeness.
- Continuing to add isolated contracts would increase skeleton breadth without proving that Enhancer can execute a useful governed workflow.

Decision:

- Use capability maturity states: Specified, Contract Verified, Integrated, Operational, and Released.
- Replace ambiguous standalone `Implemented` roadmap labels with the most precise verified maturity.
- Promote capabilities through executable vertical slices with explicit integration and operational exit gates.
- Supersede the sequential independent verifier as a standalone next task. First implement the bounded Tool execution boundary and evidence persistence, then integrate the Agent Loop and sequential verifier in the same E2E delivery track.
- Require every new foundation contract to name its current or immediately following integration consumer.

Rationale:

Control-plane safety contracts were necessary before Tool or LLM execution, but their value is realized only when they participate in an observable, recoverable run. Maturity gates prevent focused unit tests from being reported as product readiness and make the path from foundation to usable system explicit.

Consequences:

- The next product task is the first Tool Execution Boundary slice, not an isolated verifier record.
- The first operational milestone must run Context → Tool → Evidence → Verification → Stop → Run Record through a supported entry point.
- Independent verification remains mandatory but moves after real ToolResult production and evidence persistence.
- Skill, LLM, MCP, plugin, multi-agent, and self-improvement work remains gated behind the executable single-agent path.
- Roadmap, architecture guides, state, and handoff documents must use the new maturity vocabulary.
