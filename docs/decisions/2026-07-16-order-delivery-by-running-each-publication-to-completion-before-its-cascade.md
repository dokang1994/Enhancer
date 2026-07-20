# 2026-07-16: Order Delivery By Running Each Publication To Completion Before Its Cascade

Status: Accepted Decision

Context:

- Topic fan-out already follows registration order, the journal already follows publication order, and replay already re-dispatches in journal order, so the ordering the Roadmap names has no content unless it addresses the one real hazard left.
- That hazard is re-entrant publication: `publish` dispatches synchronously, so a handler that publishes during its own delivery causes a nested dispatch. The child is delivered in full before the parent's fan-out finishes, and every subscriber registered after the publishing one observes the effect before its cause.
- The bus is synchronous and single-threaded, so ordering can only be established by the order in which the bus itself admits work, not by any scheduler.

Decision:

- Give the bus a pending queue and a single drain loop. A publication is appended to the queue; a top-level call drains the queue to exhaustion, and a call made while a drain is already running only enqueues.
- Add `DeliveryStatus.ENQUEUED` for a re-entrant publication accepted for later delivery, and return the whole ordered cascade from the top-level `publish` or `replay` that drained it, so no outcome is lost.
- Route `publish` and `replay` through the same submission and drain path, distinguishing them only by whether the entry is journaled, so ordering holds identically on both.
- Move admission — the cancellation check and the journal append — into the drain loop, so an entry is journaled at the moment it is admitted for delivery and the journal's order is the bus's own total delivery order.
- Generalize the scope-level status concept onto `DeliveryStatus.isScopeLevel()`, now covering `UNROUTED`, `CANCELLED`, and `ENQUEUED`, and keep `DeliveryOutcome` validating that exactly the scope-level statuses name no subscriber.
- Abandon a cascade entirely if an `Error` escapes a drain, clearing the queue rather than leaking a dead cascade into an unrelated later publication.

Rationale:

Run-to-completion is the only ordering a synchronous bus can offer that means anything: it makes delivery order equal publication order and guarantees that a cause is delivered to every subscriber before any of its effects reaches one. Draining from a queue rather than recursing also removes unbounded stack growth from a deep cascade. Journaling at admission rather than at the `publish` call keeps the invariant established by the cancellation decision — the journal records exactly what was admitted, so a fresh-bus replay reproduces exactly the side effects that occurred — and it lets a cancellation raised mid-cascade refuse work already queued behind it, which enqueue-time checking alone could not do.

Consequences:

- A handler observes `ENQUEUED` rather than a delivery result for its own publication, so a handler that needs its child's outcome must read it from the cascade the top-level caller receives.
- A publication is admitted or refused as a whole, so a cancellation raised during a fan-out cannot stop that fan-out; it stops only entries still queued behind it.
- The pending queue is unbounded, which is precisely the gap the backpressure increment will close; it joins the idempotency keys, journal, and cancellation state as process-local in-memory state.
- Re-delivery is unaffected: it targets exactly one subscription, so a publication from its handler has no fan-out to be nested inside and drains normally.
- Ordering remains registration order within a destination; competing consumers and priority ordering stay out of scope.
