# 2026-07-16: Make Runtime Persistence And Verification Dependencies Acyclic

Status: Accepted Decision

Context:

- Production packages currently form cycles: `loop` imports `run` through `AgentRunFinalizer`, `run` imports `loop` lifecycle types, `loop` imports verification decisions, and `verification` imports the approved-task contract from `loop`.
- The single Gradle module compiles, but the cycles obstruct later Kernel, Runtime, Verification, and Persistence module boundaries.
- Moving every Agent/runtime type or introducing new modules now would be larger than necessary and could disrupt the verified vertical slice.

Decision:

- Move `VerificationDecision`, `VerificationStatus`, and `VerificationCode` unchanged into neutral `com.enhancer.kernel` lifecycle contracts.
- Move `AgentRunFinalizer` unchanged in behavior from `com.enhancer.loop` to `com.enhancer.application`, where orchestration may depend on loop, verification, and run persistence.
- Preserve `ApprovedTask`, Agent run state, and stop reasons in `loop` for this increment. `verification` and `run` may depend on `loop`; `loop` must no longer depend on `verification` or `run`.
- Require `kernel` to depend on none of `application`, `loop`, `run`, or `verification`; require `run` not to import `verification`.
- Preserve enum constant names, RunRecord binary values, CLI behavior, verification decisions, public value invariants, and storage compatibility.
- Add a source-structure regression test that fails if the forbidden dependency directions return.
- Defer Gradle module extraction, broader Kernel naming, ApprovedTask relocation, API compatibility shims for unreleased package names, and persistence SPI separation.

Rationale:

Moving three neutral decision values and one orchestration service is the smallest extraction that turns the current strongly connected component into a directed dependency graph. It creates a credible future module seam without redesigning runtime behavior or durable formats.

Consequences:

- Source imports change, but persisted enum names and runtime semantics do not.
- The resulting direction is `application -> run/verification/loop/kernel`, `run -> loop/kernel`, `verification -> loop/kernel`, and `loop -> kernel`.
- The project remains one Gradle module; independent modules require a later task.
