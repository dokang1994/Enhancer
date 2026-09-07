# User continuation request on 2026-09-07 into supported model-aware Scheduler composition specification

Status: Accepted Decision

## Context

RFC-0023 implements typed deterministic-fake execution and RFC-0024 implements governed
typed submission, but their only connection is test-owned. The supported foreground
Scheduler commands still construct the legacy-only worker and do not supply the
existing model process configuration. RFC-0024 orders a supported model-aware Scheduler
composition before any interface profile format or supported typed ingress. The
completed task named that work as requiring separate authority, and the user requested
continuation on 2026-09-07.

## Decision

Accept RFC-0025 as the documentation-only contract for an optional model-aware
composition shared by `scheduler-cycle`, `scheduler-drain`, and `scheduler-service`.
The exact all-or-none group selects only `deterministic-fake-v2` and supplies bounded
gateway timeout, maximum response characters, maximum prompt-read bytes, Tool timeout,
and at most 16 unique repeatable bounded denied-Tool values. Partial, unknown,
duplicate, or invalid input fails before queue/store access; omission retains the
legacy composition.

The composition reuses every existing Scheduler root, owner, retry, lease, outer
process-timeout, clock, and optional runtime-event source. One filesystem RunRecord
store serves both v1 and v2, the existing evidence root supports parent validation, and
the queued payload, manifest-retained capability, fresh active task, and closed fake
implementation retain their separate authority. The model-aware branch remains
payload-kind-aware and can execute legacy Work without projection.

No supported typed producer, publisher, receiver, or profile input is added. The first
implementation evidence will seed exact RFC-0024 intent only inside tests, invoke the
real supported Scheduler CLI, and prove completion, pre-call refusal, legacy behavior,
recovery, and runtime-event compatibility. Existing durable schemas are sufficient.

This decision authorizes only RFC, architecture, state-consistency, Roadmap, task,
decision/index, verification, handoff, and Changelog documentation, focused and full
verification, and ordinary local GREEN commits. It authorizes no Java or test-source
change, actual execution, typed ingress, provider, network, credential, spend, schema
migration, push, merge, release, deployment, permission change, destructive cleanup,
or external effect.

## Consequences

- Supported model execution gains an explicit bounded policy source without treating
  profile or capability data as authority.
- Legacy Scheduler behavior remains selected by omission, and all three foreground
  commands share one composition rule.
- Runtime-event publication remains compatible rather than being silently disabled.
- Typed input and complete-profile interface design remain the next separate boundary.
- Capability maturity does not change until separately authorized implementation and
  fresh supported CLI integration evidence exist.
