# 2026-07-14: Colocate Examples With Specifications And Tests

Status: Accepted Decision

Decision:

Enhancer will not maintain a standalone `examples/` directory. Conceptual examples belong in the `docs/` or RFC document that owns the contract, while executable examples belong in focused tests.

Rationale:

The standalone Agent Loop and Tool examples were already behind the implemented contracts. Colocation reduces duplicate descriptions, prevents conceptual samples from drifting away from code, and keeps the repository structure smaller.

Consequences:

- Remove `examples/agent-loop.md`, `examples/tool-example.md`, `examples/skill-example.md`, and the empty-directory marker.
- Do not treat examples as a separate source of truth.
- New conceptual examples must be updated with their owning specification.
- Observable executable behavior remains demonstrated and verified through tests.
