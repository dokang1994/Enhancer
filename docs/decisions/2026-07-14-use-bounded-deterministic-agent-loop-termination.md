# 2026-07-14: Use Bounded Deterministic Agent Loop Termination

Status: Accepted Decision

Decision:

The first repeated Agent Loop uses immutable state transitions and explicit `COMPLETED`, `FAILED`, `MAX_ITERATIONS`, and `STAGNATED` stop reasons. The default ceiling is 20 executed steps. Stagnation means the progress key remains unchanged for 3 consecutive executed steps; both limits are constructor-configurable for focused tests and later runtime configuration.

Rationale:

Explicit bounded exits prevent silent infinite work before Tool or LLM execution is introduced. A caller-provided deterministic step keeps the loop independently testable and avoids premature Agent, Tool, prompt, or provider abstractions.

Consequences:

- Terminal status wins over stagnation after a step.
- Maximum iteration wins when its ceiling and the stagnation threshold coincide.
- Iteration count reports executed steps, including the terminal step.
- Maximum-iteration and stagnation results retain the latest running state for diagnosis.
- Tool execution, verification evidence, independent verification, LLM calls, and multi-agent routing remain out of scope.
