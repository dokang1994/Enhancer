# 2026-07-16: Propagate Cancellation As A Terminal Correlation-Scoped Delivery Refusal

Status: Accepted Decision

Context:

- The bus delivers, retries, dead-letters, and replays deterministically, but nothing can abandon work in flight: every published envelope is delivered regardless of whether its run was cancelled, and a dead letter stays re-deliverable forever.
- The Roadmap names cancellation propagation as the next Gate 7 concern, before ordering and backpressure.
- The bus is synchronous and single-threaded, so there is no concurrently executing handler to interrupt; cancellation can only decide whether a delivery is admitted at all.
- `ControlSignal.CANCEL` already exists, but it is a consumer-facing payload semantic. Making the bus interpret `ControlPayload` would turn the bus into a consumer and break the sealed-payload decision's intent that consumers, not the transport, exhaust payload kinds.

Decision:

- Key the cancellation scope on the envelope's own `correlationId`, the identity the envelope contract already defines for grouping related messages across hops.
- Add `cancel(String correlationId)` and `isCancelled(String correlationId)` to `InProcessMessageBus`. Cancellation is idempotent and monotonic: once a correlation is cancelled it stays cancelled, and there is no resume.
- Add `DeliveryStatus.CANCELLED` and treat cancellation as admission control that runs before subscription lookup, idempotency, and dispatch: a publication into a cancelled correlation invokes no handler, consumes no idempotency key, creates no dead letter, is not journaled, and reports one scope-level `CANCELLED` outcome.
- Propagate the refusal to every delivery path: `replay` reports `CANCELLED` and skips a journal entry whose correlation is cancelled, and `redeliver` reports `CANCELLED` without invoking the handler while retaining the dead-letter record.
- Generalize the `DeliveryOutcome` invariant from "`UNROUTED` carries no subscriberId" to "a scope-level status (`UNROUTED` or `CANCELLED`) carries no subscriberId; every other status must name the subscription it targeted".
- Keep the bus free of payload interpretation: a handler that receives a `CANCEL` `ControlPayload` may call `cancel(correlationId)` itself, but the bus never reads a payload to decide delivery.

Rationale:

Correlation-scoped refusal is the only cancellation a deterministic single-threaded bus can honestly express, and it is genuine propagation rather than a per-destination flag: one `cancel` reaches every topic, queue, replay, and re-delivery that shares the correlation, and a caused child carrying the parent's correlation inherits it without the bus tracking a causation graph. Refusing admission before journaling is forced by the existing replay contract — journaling a cancelled publication would make a fresh-bus replay produce a side effect that never originally happened, breaking the determinism the journal exists to guarantee. Monotonic cancellation keeps replay reproducible, because a scope's admission decision can never differ between two passes over the same journal.

Consequences:

- Cancellation is terminal: resuming an abandoned correlation requires a new correlation identity, and `ControlSignal.PAUSE`/`RESUME` therefore remain consumer-level semantics with no bus behavior.
- A cancelled publication is invisible to the journal, so the journal records exactly the publications that were admitted for delivery and replay reproduces exactly the side effects that occurred.
- Cancelling mid-fan-out is impossible by construction: a publication is admitted or refused as a whole, so subscribers never observe a partially delivered envelope.
- Cancellation state joins the idempotency keys and journal as unbounded, process-local, in-memory state; bounds and durability wait for the persistence increment.
- Cancellation is scoped to the correlation, not the logical run; cancelling every correlation of a run is the caller's composition until a run-scoped control surface exists.
