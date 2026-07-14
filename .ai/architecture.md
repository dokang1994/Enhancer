# AI Architecture Notes

The canonical architecture document is `ARCHITECTURE.md`.

Current direction:

- Enhancer is a self-hosting AI Development Operating System.
- Repository Context Reader, deterministic Task Planner, Assisted Development Loop, repeated-loop safety, and Tool evidence are Contract Verified, not Integrated or Operational.
- Capability maturity is Specified, Contract Verified, Integrated, Operational, or Released; do not use Implemented alone as a roadmap state.
- The next architecture slice is Delivery Gate 1: ToolRequest, Tool, ExecutionPolicy, ToolExecutor, and one allowlisted read-only filesystem Tool.
- Evidence persistence follows real Tool execution; Agent Loop integration and the sequential independent verifier follow persisted Tool results.
- New contracts must name their current or next-gate integration consumer.
- External agent-harness patterns are adopted only through provider-neutral Enhancer contracts in their owning roadmap phase.
- Repository documents are product inputs.
