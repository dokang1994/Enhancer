# 2026-08-04: Compose Filesystem Runtime Event Publication In The Supported Control Receiver

Status: Accepted Decision

## Context

The supported `scheduler-receive-control` command resolves one explicit pending or
acknowledged Control transport point, publishes it through a fresh Message Bus queue,
persists the exact request through `RuntimeControlAdmissionHandler`, and acknowledges
the point only after handler success. The handler already has an event-aware constructor
that records `CANCELLATION_REQUEST_RECORDED` for `CANCEL`, and the concrete filesystem
event store and reference publisher now exist, but the supported receiver constructs the
event-free handler path.

Scheduler cycle/drain/service construction is not the minimum alternative. Those
commands share one worker composition containing several separately owned runtime-event
transitions, so wiring one recorder there would either cover only one owner or widen
across process timeout, lease recovery, retry, verification, stagnation, Tool timeout,
and terminal disposition.

## Decision

Extend `scheduler-receive-control` with one optional all-or-none configuration group:

- `--runtime-event-root` for `FileSystemRuntimeEventStore`;
- `--runtime-event-publication-root` for `FileSystemRuntimeEventPublisher`; and
- `--max-pending-runtime-event-publications` from 1 through 4096.

When all three options are absent, retain the existing request-only construction and
output. When all three are present, the CLI constructs the store, concrete publisher,
and `RuntimeEventRecorder`, then supplies that recorder through
`DurableControlMessageReceiver` to the existing event-aware admission handler. Any
partial group, non-integer/non-positive capacity, or capacity above 4096 is a usage
error before transport resolution or runtime mutation. Existing command output remains
unchanged and makes no event-delivery, authentication, or application claim.

For `CANCEL`, durable order is unchanged Control request persistence -> runtime-event
append/exact replay -> opaque reference-point publication -> transport-point
acknowledgement. Request-store failure reaches neither event store nor publisher. Event
append failure leaves the exact request durable and the transport point unacknowledged.
Publisher failure leaves both request and event durable and the transport point
unacknowledged. Re-entry with the same explicit roots repairs the durable prefix without
another request or event revision; exact point replay occurs before capacity evaluation.
The caller must retain the same event and publication roots as recovery inputs. Root
drift is unsupported configuration, not migration.

`PAUSE` and `RESUME` remain request-only even when the group is present and create no
event or publication point. Root creation remains lazy behind actual `CANCEL` event
recording. Unusable event or publication storage therefore follows the existing
internal-storage failure exit and preserves the source-first durable prefix and
unacknowledged transport evidence.

## Rationale

The optional group preserves existing supported invocations while making publication
explicit and recoverable for callers that opt in. The Control receiver is the narrowest
real producer because its acknowledgement already brackets handler success and its
event owner is already connected. Keeping output unchanged prevents local publication
acceptance from being confused with consumer delivery or authenticated control
application.

## Consequences

- The supported Control receiver becomes the first application/CLI construction of the
  concrete runtime-event publisher, but only for `CANCELLATION_REQUEST_RECORDED`.
- The old event-free receiver constructor and CLI syntax remain supported.
- Consumers, scans, cleanup/retention, event-body transport, MessageEnvelope evolution,
  authenticated cancellation application, Scheduler composition, and additional
  transition owners remain separate tasks.
