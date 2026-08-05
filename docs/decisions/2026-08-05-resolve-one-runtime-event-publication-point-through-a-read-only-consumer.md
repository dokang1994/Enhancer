# 2026-08-05: Resolve One Runtime Event Publication Point Through A Read-Only Consumer

Status: Accepted Decision

## Context

The runtime-event recorder persists each derived fact before publishing only its opaque
`runtime-event/<goal>/<event>` reference. `FileSystemRuntimeEventPublisher` retains that
reference as one deterministic bounded integrity point, and the supported Control
receiver can now create such a point. No production consumer proves that an explicitly
retained point still names the exact event in its canonical Goal stream.

The next boundary could acknowledge a point or compose publication into another event
owner. Acknowledgement is not a read-only extension: a crash after downstream handling
but before rename requires a durable consumer-idempotency contract, and retained
acknowledged artifacts introduce a separate cleanup/retention question. Scheduler-wide
composition would thread one recorder through several independently owned transitions.

## Decision

Implement the first consumer as repeatable read-only resolution of one caller-named
filesystem point. The caller supplies the runtime-event store root, publication root,
and exact canonical `.runtime-event-reference` filename. The consumer never scans either
root.

Resolution requires one regular non-symbolic point under the normalized publication
root. It decodes the existing bounded schema-v1 integrity envelope with strict UTF-8,
validates that the filename is the deterministic lowercase SHA-256 name of the decoded
reference, and parses exactly one canonical Goal UUID and event UUID from
`runtime-event/<goal>/<event>`.

The consumer then point-resolves only that Goal's `RuntimeEventStream`, searches its
already-bounded at-most-4096-event prefix for the exact event identity, and verifies that
`RuntimeEventPublicationReference.from(event)` equals the decoded reference. Missing,
corrupt, symbolic, malformed, foreign, or mismatched point, stream, and event state
fails closed. Successful and failed reads create, rename, rewrite, acknowledge, or
delete nothing.

Expose this consumer through one supported `runtime-event-read` CLI command with only
the three explicit inputs above. Its bounded output reports typed event identity, kind,
time, producer, Goal/AgentRun, task/snapshot/run/correlation provenance, stream revision,
and authoritative-reference count. Output does not include source content, credentials,
Tool scope, or an event application result and does not claim delivery,
acknowledgement, transition authority, or Gate promotion.

## Rationale

Read-only point resolution is the smallest real consumer of the existing concrete
publisher. It makes the published reference useful and fail-closed without introducing
the lost-acknowledgement window, handler idempotency, destructive retention authority,
MessageEnvelope evolution, or broad Scheduler construction changes. Exact repeated
reads are naturally restart-safe because both the point and canonical event stream
remain unchanged.

## Consequences

- The filesystem publisher gains one supported read-only downstream consumer.
- Point acknowledgement, capacity release, consumer offsets/receipts, scans, watchers,
  cleanup, and retention remain separate decisions.
- Event application and runtime mutation remain with their canonical transition owners;
  observing an event grants no authority.
- Additional publisher composition remains a separate bounded task.
