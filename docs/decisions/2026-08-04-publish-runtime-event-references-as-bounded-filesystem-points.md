# 2026-08-04: Publish Runtime Event References As Bounded Filesystem Points

Status: Accepted Decision

## Context

Every implemented runtime-event transition owner now reaches `RuntimeEventRecorder`,
which persists the derived event before invoking a caller-supplied
`RuntimeEventPublisher` with only its deterministic opaque reference. There is no
concrete publisher. The existing Message Bus envelope is deliberately sealed to Work,
Result, Control, and Handoff, so using that wire contract would require a separate Gate
7 schema decision and would risk assigning an existing payload a false meaning.

## Decision

Implement the first concrete publisher as `FileSystemRuntimeEventPublisher`, a local
bounded point adapter for the existing port. Its caller supplies one normalized root and
a capacity from 1 through 4096. A publication carries exactly the validated
`runtime-event/<goal>/<event>` reference; it never resolves, copies, or interprets the
runtime-event body.

Derive the point name deterministically as the lowercase SHA-256 of the exact UTF-8
reference plus `.runtime-event-reference`. Encode one schema-v1 integrity envelope with
a fixed magic, version, declared reference length, SHA-256 digest, and strict UTF-8
reference. Publish through a same-root temporary file, forced file contents, and an
atomic non-replacing move. Success means only that the local point is durably accepted
to the adapter's current filesystem guarantee; it is not consumer delivery,
acknowledgement, event application, or Message Bus acceptance.

Before capacity evaluation, resolve an existing deterministic point. An exact valid
point is revision-free replay and is not rewritten, so capacity cannot block recovery.
A symbolic, non-regular, oversized, malformed, unsupported, digest-invalid, or
reference-mismatched existing point fails closed. A new reference is refused with
`IOException` when the bounded suffix-count has reached capacity. The implementation
serializes publication within one adapter instance but claims no cross-process lock,
directory fsync, scan/consumer API, cleanup, retention, or cross-store transaction.

The existing source-first order remains unchanged: authoritative transition -> event
append/exact replay -> reference-point publication. Publication failure leaves the
event available for the existing owner-specific exact re-entry.

## Consequences

- All existing event owners can receive a concrete local publisher without changing
  their constructors or the `RuntimeEventPublisher` port.
- The adapter creates no runtime authority and does not promote the whole Scheduler,
  Message Bus, or runtime-event path to Operational maturity.
- Supported application composition, a consumer/acknowledgement contract, Message Bus
  schema evolution, cleanup/retention, and broader multi-process reliability remain
  separate decisions and tasks.
