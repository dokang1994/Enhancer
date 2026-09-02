# User continuation request on 2026-09-02 into exact-request model budget and invocation seam specification

Status: Accepted Decision

## Context

RFC-0020/RFC-0021 sequence 1 now provides a closed token-aware fake candidate and
standalone suitability, but `Suitable` does not validate one concrete request's
actual scalar count or predicted response against its request/profile budgets. The
current public model Tool reconstructs a request and accepts a generic gateway, so it
cannot preserve the admitted request and candidate-bound gateway identities required
by the accepted sequence. The completed implementation task named a separate
exact-request specification as the next action, and the user requested continuation
on 2026-09-02.

## Decision

Accept RFC-0022 as a documentation-only contract for one field-free exact-request
preparation and one field-free deterministic-fake invoker. Preparation must accept the
exact `Suitable` and exact RFC-0019 policy, count the retained prompt once, apply the
closed actual-input/response-length/output/checked-total order, and create an opaque
private-construction `Ready` only after every check passes. The invoker must accept
only `Ready`, recheck retained policy eligibility and cancellation, and call the
exact candidate-bound gateway with the exact admitted request at most once.

The total-budget refusal remains a defensive stable reason even though RFC-0014's
`maxInput + maxOutput <= maxTotal` invariant makes it unreachable after the earlier
individual checks for every valid current profile. The contract must state and verify
that theorem instead of changing order or forging an invalid profile.

Standalone code cannot prove that its received policy was historically used by
RFC-0016 because existing admitted/suitable values do not retain policy. Later
Scheduler integration must pass its retained preparation policy by identity in one
fresh call chain. The seam is not a production Tool, authorization token, verification,
or exactly-once boundary.

This decision authorizes only RFC/architecture/index/task/state-neutral documentation,
focused governance, full regression verification, and ordinary local GREEN commits.
It authorizes no Java or test-source implementation, gateway or Tool call, production
caller, evidence or RunRecord write, schema/runtime/finalizer/retry/recovery wiring,
provider, network, credential, paid service, push, merge, release, deployment,
permission change, external effect, or destructive cleanup.

## Consequences

- Actual request budgets and profile/candidate suitability remain separate closed
  decisions with explicit identity flow.
- Budget refusal and pre-call policy refusal produce zero gateway, Tool, evidence,
  RunRecord, runtime, queue, or retry activity.
- Gateway success remains an untrusted invocation outcome; later Tool/evidence,
  response-verification, persistence, and finalization owners remain mandatory.
- The later standalone implementation can be tested without enabling any production
  caller, while process execution and recovery remain a separate accepted task.
