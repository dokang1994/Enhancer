# Current Task

## Status

Completed

## Task

Persist bounded cancel/pause/resume control requests in the durable Agent runtime and
connect one real Gate 7 control-message queue delivery to that request boundary without
applying an unauthenticated runtime transition.

## Task ID

persist-bound-runtime-control-requests

## Context

Gate 7 already carries `ControlPayload(CANCEL|PAUSE|RESUME, reason)` and provides
deterministic delivery, retry, dead letter, and replay, but no real consumer connects a
control envelope to Gate 8 durable state. Gate 12 owns authenticated user controls and
does not exist yet, so treating possession of a control envelope as permission to pause,
resume, cancel, release a lease, or change queue state would create authority from
untrusted message data.

The first safe connection is therefore a durable, exact request ledger inside the
existing Goal state. It records what was requested and binds it to the Goal's immutable
work provenance. A later authenticated controller may evaluate and apply a request; the
current worker, dispatcher, finalizer, queue, and Tool policy ignore it.

## Justified By

- 2026-07-21: Persist Bound Runtime Control Requests Without Applying Unauthenticated Transitions
- 2026-07-16: Separate Execution Acknowledgement From Verified Queue Completion And Sequence Remaining Connections
- 2026-07-16: Propagate Cancellation As A Terminal Correlation-Scoped Delivery Refusal
- 2026-07-15: Start Gate 7 With Reference-Only Versioned Envelopes And Exactly Four Payload Kinds

## Acceptance Criteria

- `AgentRuntimeState` retains an immutable ordered ledger of at most 256 exact
  `MessageEnvelope` values carrying `ControlPayload`.
- Every admitted request matches the Goal work's logical run, correlation, and work
  message causation. Identity collisions, non-control payloads, mismatches, terminal
  Goals, and over-capacity requests fail closed.
- Re-delivery of the exact same message identity and envelope is idempotent and does not
  advance the runtime revision; reusing an identity with different content is rejected.
- The runtime store persists the complete control envelope before the request becomes
  visible. Store failure leaves the previous in-memory and durable revision authoritative.
- One `MessageHandler` connects the real in-process queue to the durable runtime. An I/O
  failure becomes a handler failure so existing bounded bus retry/dead-letter behavior
  remains observable.
- Recording requests changes no Goal status, AgentRun status, lease, fence, queue state,
  allowed Tool scope, execution input, or bus cancellation state. Producer and reason
  fields remain diagnostic data, not authority.
- A fresh filesystem store instance recovers the exact ordered requests, including
  supplementary Unicode. The unreleased schema-v1 payload is revised in place and
  pre-existing snapshots without the control ledger fail closed.
- Focused RED/GREEN tests, the full build, strict lint, and document structural checks
  pass with fresh output.

## Out Of Scope

- Applying pause, resume, or cancel to a Goal, AgentRun, lease, worker process, queue, or
  Tool execution.
- Authenticating or authorizing a user control; Gate 12 owns that boundary.
- Persisting the Gate 7 bus journal/cancellation set, remote controls, concurrent
  consumers, retries through new AgentRuns, or external-effect fencing.
- Schema migration for pre-existing unreleased runtime artifacts.

## Approval

Approved by the user's 2026-07-21 request to continue the recorded next task and, once
verified, commit it, push the feature branch, and merge it into `main`.

## Verification

Fresh RED/GREEN, full-build, strict-lint, and structural evidence is recorded in
`docs/verification-log.md`.

## Next

Persist a bounded external-effect ledger with fence-checked, idempotent effect outcomes
before connecting retry through additional AgentRuns.
