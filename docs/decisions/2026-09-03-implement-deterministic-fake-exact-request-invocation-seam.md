# User continuation request on 2026-09-03 into RFC-0022 exact-request seam implementation

Status: Accepted Decision

## Context

RFC-0020 and RFC-0021 have Contract Verified the closed deterministic-fake candidate,
Unicode-scalar token semantics, fixed capacities, and standalone suitability. RFC-0022
specifies the remaining request-local budget preparation and exact-identity invocation
seam, and the completed specification task names its two RED-first implementation
sequences as the sole next action. The user requested continuation on 2026-09-03.

## Decision

Implement RFC-0022 sequences 1 and 2 sequentially and RED-first. Sequence 1 adds the
field-free exact-request preparation, opaque private-construction decision variants,
closed ordered budget reasons, exact identity/count retention, checked arithmetic,
redacted rendering, total-budget invariant proof, and zero-activity refusal. Sequence
2 adds the field-free invoker and opaque result variants, ordered pre-call policy and
cancellation refusal, exactly one candidate-bound deterministic-fake call on the
eligible path, untrusted returned response retention, and one-to-one code-only gateway
failure mapping.

The implementation remains standalone in `com.enhancer.model`. Focused tests may call
the deterministic fake through the new invoker, but production source outside the new
definition types must not reference or consume the seam. Existing fake rendering and
generic `ModelUsage`, public `ModelInvokeTool`, Scheduler preparation, Tool/evidence/
verification, Model RunRecord schemas, runtime, retry, and recovery remain unchanged.

This decision authorizes Java and test-source changes required by those two sequences,
minimal boundary guards and canonical document synchronization, fresh focused/full
verification, and ordinary local GREEN commits. It authorizes no production caller or
supported entry point, Scheduler/process integration, ToolResult/evidence/response-
verification/Model RunRecord writing, schema/runtime/finalizer/retry/recovery wiring,
typed ModelWork producer or receiver, provider/router/network, credential or spend
work, push, merge, release, deployment, permission change, destructive cleanup, or
external effect.

## Consequences

- Actual-request budget readiness becomes a closed process-local value instead of
  caller-supplied numeric claims.
- The standalone invoker can demonstrate exact request/policy/candidate/gateway
  identity and fail-closed pre-call behavior without creating a production execution
  path.
- Contract Verified maturity may be claimed only after fresh focused and regression
  evidence; higher maturity still requires separately authorized ModelWork process,
  evidence, verification, record, finalization, retry, and recovery integration.
- Every new attempt before a durable Model RunRecord v2 reference must reconstruct the
  full fresh chain later; these standalone types add no persistence or retry authority.
