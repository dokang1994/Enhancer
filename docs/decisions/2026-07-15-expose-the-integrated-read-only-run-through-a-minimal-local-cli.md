# 2026-07-15: Expose The Integrated Read-Only Run Through A Minimal Local CLI

Status: Accepted Decision

Context:

- Delivery Gates 1 through 4 provide repository-derived approval, governed read-only Tool execution, complete evidence persistence, independent verification, and durable RunRecord replay, but no supported entry point connects them for a user.
- Gate 5 requires explicit project, task, target, expected digest, evidence-root, and RunRecord-root inputs plus stable exit codes and documented recovery.
- The future interface suite belongs to Gate 12; the first operational command should therefore remain deliberately small and dependency-free.

Decision:

- Add the Gradle `application` entry point `com.enhancer.cli.EnhancerCli` with two commands: `run` and `replay`.
- Make `run` require six named inputs and match the explicit task identity against the `ApprovedTask` derived from the project's active `CURRENT_TASK.md`.
- Wire only the existing `read-file` flow through `ExecutionPolicy`, `ToolExecutor`, `AgentRunController`, `DeterministicReadFileVerifier`, and `AgentRunFinalizer`.
- Use fixed documented defaults of the existing 64 MiB evidence ceiling, a five-second Tool timeout, a five-iteration loop ceiling, a three-transition stagnation threshold, and a 30-day retention declaration without automatic cleanup.
- Define stable process exit codes for completion, usage/configuration failure, verification failure, policy denial, Tool failure, stagnation, maximum iterations, and internal failure.
- Bound CLI diagnostics, print no complete target content, and report the RunRecord root and opaque reference for replay.
- Make `replay` accept only an explicit RunRecord root and opaque reference and print typed bounded metadata from the integrity-checked store.

Rationale:

This is the smallest supported control surface that proves the integrated vertical slice against a real repository without inventing a second execution path. Explicit arguments keep authority and expected results inspectable, while stable exit codes and replay make the command automatable and diagnosable.

Consequences:

- Successful Gate 5 evidence may promote the first read-only run to Operational, but it does not release a distribution or make the broader Agent Runtime Operational.
- The command does not infer task approval, expected content, storage roots, or target paths from ambient state.
- Interactive prompts, configuration discovery, shell/Git/network/LLM capabilities, and polished multi-interface behavior remain deferred.
- Gate 6 becomes the next specified capability only after temporary-project and actual-repository run/replay evidence passes.
