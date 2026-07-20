# 2026-07-14: Bind Run Records To The Policy Used During Execution

Status: Accepted Decision

Context:

- Gate 4 records an immutable execution-policy snapshot, but the finalizer currently accepts an `ExecutionPolicy` separately after the worker run.
- A caller could supply a different still-allowing root, timeout, or size limit and produce an internally valid record that does not describe the actual execution.
- The public `RunRecord` constructor also permits some verification and stop-reason combinations that the governed Agent run path cannot produce.

Decision:

- Preserve the exact `ExecutionPolicy` used by `AgentRunController` in `AgentRunResult`.
- Remove the replaceable policy argument from finalization and derive the persisted `PolicyDecision` from the worker-bound policy.
- Make `RunRecord` enforce the Gate 3 and Gate 4 lifecycle: worker completion is impossible, verification is performed only after successful verification-wait, and non-verification terminal or bounded stops retain failed Tool output with verification Not Performed.

Rationale:

RunRecords are audit evidence. Their policy snapshot and lifecycle must be bound to the executed run rather than trusted as a later caller assertion. Enforcing these relationships in immutable contracts prevents Gate 5 or another future entry point from constructing replayable but historically false records.

Consequences:

- Finalization cannot substitute a different policy after Tool execution.
- Invalid persisted records are rejected during both direct construction and replay decoding.
- The change strengthens existing Gate 4 semantics without adding Tool authority, CLI behavior, LLM calls, or multi-agent execution.
- Gate 5 consumes one policy-bound worker result and no longer needs to repeat the policy at finalization.
