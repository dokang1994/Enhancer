# 2026-07-15: Deliver Gate 7 In-Process Messaging As A Deterministic Journal-Replayable Bus

Status: Accepted Decision

Context:

- The envelope contract named deterministic in-process topic and queue delivery with idempotency and replay as its next consumer, and the orchestration invariants require authorization and provenance to survive every hop.
- Competing queue consumers, retry, dead-letter, ordering, backpressure, threading, and IPC transport introduce non-determinism or scope the envelope contract does not yet need.

Decision:

- Add `InProcessMessageBus` under `com.enhancer.bus`: synchronous, single-threaded, deterministic delivery over `MessageEnvelope`, with a typed `DeliveryDestination` (`TOPIC` fan-out in registration order, `QUEUE` point-to-point to a single consumer that rejects a second consumer).
- Return an immutable ordered list of per-subscriber `DeliveryOutcome`s with a typed `DeliveryStatus` (`DELIVERED`, `DUPLICATE`, `UNROUTED`).
- Make delivery idempotent per `(destination, subscriber, message identity)` using a collision-free record key, and record every publication in an ordered immutable journal whose `replay` re-dispatches deterministically without appending, reproducing outcomes on a fresh bus and yielding only `DUPLICATE` with no side effect against a bus that already processed them.
- Defer retry, cancellation propagation, dead-letter, ordering beyond registration, backpressure, competing consumers, threading, journal persistence, and the IPC transport interface to later increments.

Rationale:

Synchronous single-threaded fan-out with a per-subscription idempotency key is the smallest surface that demonstrates deterministic delivery and replay without duplicate side effects, exactly the exit criterion, while carrying whole envelopes unchanged so consumers still validate authority against repository state rather than trusting the sender. Restricting a queue to one consumer keeps point-to-point delivery deterministic without a load-balancing policy the contract does not need yet.

Consequences:

- Multiple competing queue consumers require an explicit deterministic selection policy introduced through a later recorded decision.
- The in-memory idempotency and journal state is unbounded and process-local; durability, retention bounds, and cross-restart replay wait for the persistence and transport increments.
- Possessing or delivering an envelope still grants no authority; the bus never creates or widens task or Tool scope.
