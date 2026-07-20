# 2026-07-16: Add Bounded Synchronous Retry And Explicit Dead-Letter Re-Delivery

Status: Accepted Decision

Context:

- The dead-letter increment made a failed delivery deterministic and terminal: the idempotency key is consumed, re-publishing reports `DUPLICATE`, and no automatic re-delivery exists, so a transiently failing handler permanently loses the delivery.
- The Roadmap names automatic retry with a bounded attempt policy and re-delivery from the dead-letter record as the next Gate 7 increment, before cancellation propagation, ordering, and backpressure.
- The bus is synchronous, single-threaded, and deterministic; timers, delays, and asynchronous scheduling remain out of Gate 7 scope.

Decision:

- Add an immutable `RetryPolicy(maxAttempts)` bounded to 1 through 10 attempts; the bus keeps its no-argument constructor at a single attempt so every existing behavior is unchanged, and accepts a policy through a new constructor.
- Retry synchronously and immediately inside dispatch: a handler `RuntimeException` is retried until the policy's attempts are exhausted, in the same deterministic order with no delay between attempts; success within the policy yields `DELIVERED` with no dead letter, and exhaustion yields one `FAILED` outcome and one dead letter.
- Record the failed attempt count on the dead letter: `DeadLetter` gains a positive `attempts` component naming how many handler invocations failed for that delivery.
- Add explicit re-delivery from the dead-letter record: `redeliver(DeadLetter)` accepts only a dead letter this bus currently records, re-invokes the subscription's handler under the same bounded policy, resolves the dead letter on success (`DELIVERED`, entry removed), and on renewed exhaustion replaces the entry in place with the accumulated attempt count and latest reason (`FAILED`).
- Keep re-delivery outside the journal and outside idempotency: the original publication is already journaled, and the consumed idempotency key stays consumed, so publish and replay still report `DUPLICATE` after a successful re-delivery.

Rationale:

Immediate bounded synchronous retry is the only retry the deterministic single-threaded contract can express without timers, and it composes with the existing failure isolation rather than reinterpreting it: the dead letter remains the terminal record of an exhausted policy, and re-delivery is an explicit, auditable operator action over that record instead of an implicit reinterpretation of consumed idempotency keys. Bounding attempts at ten keeps a pathological policy from turning one publish into unbounded synchronous work.

Consequences:

- Retry storms are impossible by construction: one publish invokes one handler at most `maxAttempts` times, and re-delivery requires an explicit call per dead letter.
- There is no backoff or delay between attempts; a handler needing time-based recovery must wait for a later scheduling increment.
- A successful re-delivery leaves the idempotency key consumed, preserving at-most-once publish semantics; the dead-letter record is the sole re-delivery authority.
- `DeadLetter` gains an `attempts` component, so the record itself proves the bounded policy was applied.
