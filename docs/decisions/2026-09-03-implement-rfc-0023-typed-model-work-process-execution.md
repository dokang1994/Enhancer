# User continuation request on 2026-09-03 into RFC-0023 typed ModelWork process-execution implementation

Status: Accepted Decision

## Context

RFC-0023 is accepted and the completed specification task names its RED-first internal
implementation as the next work. All current model boundaries through the exact fake
invoker are standalone, typed ModelWork execution remains blocked, and the user
requested continuation on 2026-09-03.

## Decision

Implement RFC-0023 sequentially. Begin with the pure/lazy evidence-run identity
boundary, then add an uncalled child-local model attempt pipeline and v2-only publisher,
then make child/parent and durable finalizer/worker/status/recovery consumers explicitly
v2-aware. Only after every reader is installed may the internal deterministic-fake
typed process branch become reachable in test-owned integration fixtures.

Every observable change is RED-first. Exact child-local task/request/policy/admission/
candidate/suitability/Ready identity, one invocation per AgentRun, zero-write pre-call
refusal, code-only failure evidence, independent verification, deterministic v2
publication, complete parent binding, valid-record timeout precedence, replacement-
AgentRun retry, and post-reference no-invocation recovery are mandatory. RunRecord v1
behavior and every current durable format remain unchanged.

This decision authorizes the minimal Java, tests, internal process composition,
documentation, verification, and ordinary local GREEN commits required by RFC-0023. It
authorizes no typed ModelWork producer, receiver, supported entry point, provider,
router, endpoint, network, credential, spend, MCP, durable schema version or migration,
push, merge, release, deployment, permission change, destructive cleanup, or external
effect.

## Consequences

- The implementation is split into six dependency-ordered increments so no v2 writer
  becomes reachable before all required consumers.
- Focused deterministic-fake tests may invoke only inside test-owned process and
  filesystem boundaries.
- Any need for durable pre-call refusal or omitted v2 provenance stops for a separate
  compatibility decision rather than widening this task.
- Capability maturity changes only after fresh integration evidence supports it.
