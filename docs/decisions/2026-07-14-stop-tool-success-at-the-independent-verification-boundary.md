# 2026-07-14: Stop Tool Success At The Independent Verification Boundary

Status: Accepted Decision

Context:

- Delivery Gate 3 must connect a real `ToolResult` to the bounded Agent Loop without implementing the Gate 4 independent verifier.
- Treating Tool success as task completion would let worker output bypass the accepted rule that completion requires successful independent verification.
- Retry behavior must be deterministic without parsing diagnostic prose or allowing a Tool to grant itself execution authority.

Decision:

- Add immutable `AgentRunState` with an externally approved task, a caller-supplied pending `ToolRequest`, the last `ToolResult`, loop status, and deterministic progress key.
- Add `AWAITING_VERIFICATION` as an explicit terminal loop state and stop reason; a successful Tool result reaches this boundary but cannot reach `COMPLETED` in Gate 3.
- Add `AgentRunController` as the orchestration owner. It consumes an existing `ToolExecutor`, immutable `ExecutionPolicy`, and external `ToolFailureClassifier`; it does not implement Tools, create requests, expand policy, or approve tasks.
- Keep terminal failures as `FAILED`; retain the same pending request only for failures classified `RETRYABLE`.
- Derive progress keys from canonical Tool request/result content so identical retry results activate the existing maximum-iteration and stagnation rules.
- Reuse the existing bounded loop engine for both the small `AgentLoopState` contract and the richer Agent run state without weakening the 20/3 defaults or precedence rules.

Rationale:

The verification-wait boundary preserves separation of duties while still proving the real Context -> approved task -> Tool -> evidence -> loop transition. External retry classification avoids unreliable message parsing and prevents a Tool from deciding its own authority or finality.

Consequences:

- Gate 3 can stop successfully executed work without claiming it is verified or completed.
- Gate 4 becomes the only component allowed to turn an independently accepted result into `COMPLETED`.
- Repeated identical retryable failures remain bounded and observable as `STAGNATED`.
- A denied Tool remains uninvoked even if its implementation would attempt mutation; the controller cannot alter the caller's allow/deny policy.
- LLM decisions, Tool mutation, Git or network authorization, independent verification, RunRecord persistence, CLI wiring, and multi-agent routing remain deferred.
