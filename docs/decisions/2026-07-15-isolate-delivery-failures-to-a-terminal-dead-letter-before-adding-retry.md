# 2026-07-15: Isolate Delivery Failures To A Terminal Dead-Letter Before Adding Retry

Status: Accepted Decision

Context:

- `InProcessMessageBus` delivered synchronously with per-subscription idempotency but had no failure semantics: a throwing handler aborted fan-out to the remaining subscribers, propagated out of `publish`, and left the failure unrecorded even though the idempotency key was already consumed.
- Retry, cancellation, ordering, and backpressure are the Roadmap's next Gate 7 concerns, but bundling them into one change would exceed the smallest-coherent-increment discipline.

Decision:

- Catch a subscriber handler's `RuntimeException` inside dispatch, record a `FAILED` `DeliveryOutcome`, capture an immutable `DeadLetter` (destination, subscriber, unmodified envelope, bounded reason), continue delivering to the remaining subscribers, and never let the exception escape `publish` or `replay`.
- Consume the idempotency key on a failed delivery so it is terminal for this increment: no automatic re-delivery, and re-publishing or replaying reports `DUPLICATE` with no further dead letter.
- Defer automatic retry, re-delivery from the dead-letter record, cancellation propagation, ordering, backpressure, and persistence to later increments.

Rationale:

Failure isolation plus a terminal dead-letter is the smallest change that makes delivery total — every subscriber gets a deterministic outcome and no failure is silently lost — while keeping the bus deterministic and free of any re-delivery policy the contract does not yet define. Consuming the idempotency key keeps the current at-most-once guarantee intact; the later retry increment will layer explicit bounded re-delivery on top of the dead-letter record rather than reinterpreting existing keys.

Consequences:

- A handler that must not lose work has to be idempotent or externally durable until the retry increment adds bounded re-delivery.
- The dead-letter record is in-memory and process-local; durability and replay-from-dead-letter wait for the persistence and transport increments.
- Only `RuntimeException` is isolated; `Error` still propagates, because it signals a condition the bus should not swallow.
