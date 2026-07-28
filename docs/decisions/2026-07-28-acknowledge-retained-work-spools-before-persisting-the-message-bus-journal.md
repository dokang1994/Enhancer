# 2026-07-28: Acknowledge Retained Work Spools Before Persisting The Message Bus Journal

Status: Accepted Decision

## Context

The supported Work point receiver already crosses the real Message Bus and reports
success only after idempotent durable Scheduler admission. The local transport artifact
nevertheless remains named with the pending `.transport` suffix, so it continues to
consume `FileSpoolMessageTransport` capacity after delivery. Exact re-receipt is safe but
does not distinguish pending responsibility from acknowledged retained evidence.

A durable Message Bus journal would create another recovery store for publication,
delivery, retry, dead-letter, cancellation, and subscription state. On the current Work
path it would not release transport capacity, while the downstream Scheduler queue
already persists the exact admitted envelope and rejects changed identity reuse.

## Decision

Implement explicit point acknowledgement of retained Work spool artifacts before adding
a durable Message Bus journal.

The follow-up implementation will extend only the existing
`scheduler-receive-work` point boundary:

1. The caller continues to identify one canonical pending `.transport` filename beneath
   one explicit spool root.
2. The receiver resolves exactly one regular non-symbolic point state: the pending file
   or its deterministic same-root `.received` acknowledgement name. Neither, both, a
   foreign type, or a symbolic/non-regular point fails closed without queue mutation.
3. The selected artifact is decoded and its exact expected queue route and `WorkPayload`
   are validated before queue recovery.
4. The unchanged envelope is published through the existing real Message Bus and must
   reach idempotent durable Scheduler admission.
5. Only after successful durable admission, a pending artifact is atomically renamed in
   the same directory to the acknowledgement name without replacement. No non-atomic
   fallback is permitted.
6. Acknowledged-point re-entry decodes and validates the retained artifact and repeats
   the exact durable admission check, which must be revision-free. This recovers a lost
   command result after the rename without trusting the filename alone.

The output distinguishes newly admitted versus replayed durable Work while separately
stating that the spool is acknowledged. A successful acknowledgement no longer carries
the pending `.transport` suffix and therefore releases one unit of the transport's
existing pending-capacity accounting.

The acknowledged artifact remains the exact transport evidence. This increment adds no
automatic deletion, time-based retention, global acknowledged-artifact bound, directory
consumer, or cleanup authority. The resulting acknowledged-file accumulation is an
explicit known limitation requiring a separate retention and cleanup decision.

## Rationale

Acknowledgement closes the immediate ownership gap with one additional durable state
transition over the already retained evidence. Its crash prefixes are bounded:

- failure before durable admission leaves the pending file unchanged;
- failure after admission but before rename replays admission without a queue revision;
- atomic rename exposes either the pending or acknowledged name, never a partial state;
- loss of the result after rename re-enters through the acknowledged point and verifies
  the exact queue admission again.

Persisting the bus journal first would add broader schema and recovery semantics without
solving pending-capacity exhaustion. A durable journal remains appropriate only when a
later task names the additional topic/result/handoff/retry/dead-letter consumers and
defines journal ownership, truncation, subscription checkpoints, and recovery ordering.

## Consequences

- The next implementation is Gate 7-owned spool acknowledgement composed with the
  existing Gate 8 durable admission invariant.
- Transport acceptance, Message Bus delivery, durable Work admission, spool
  acknowledgement, and worker completion remain separate observable states.
- The point receiver retains its explicit filename authority and does not gain directory
  consumption or background lifecycle.
- No production/test behavior, spool mutation, durable bus store, schema, dependency,
  commit, push, release, or deployment is added by this assessment.
