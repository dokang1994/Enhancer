# 2026-07-16: Separate IPC Transport Acceptance From Message-Bus Delivery

Status: Accepted Decision

Context:

- Gate 7 already has a versioned `MessageEnvelope`, typed destinations, and deterministic in-process delivery semantics, but a later local-process or remote adapter has no provider-neutral boundary through which to carry that route and envelope.
- Reusing `JournaledMessage` would falsely imply that a transport attempt was admitted to the bus journal. Returning `DeliveryOutcome` from a transport would likewise conflate acceptance by one transport hop with remote subscription delivery, which may be asynchronous or unavailable to the sender.
- Endpoint discovery, serialization, framing, authentication, threading, persistence, and concrete adapters have no verified requirement in the current increment.

Decision:

- Add immutable `TransportMessage`, containing exactly one existing `DeliveryDestination` and one existing `MessageEnvelope`; it creates no new authority and does not copy, reinterpret, or flatten the envelope.
- Add a provider-neutral functional `MessageTransport` interface with one `send(TransportMessage)` operation. A configured transport instance owns any peer or channel configuration, so provider endpoints and lifecycle do not enter the domain contract.
- Return a typed `TransportOutcome` directly from the synchronous admission call without copying a second message identity into the result. `ACCEPTED` means only that the adapter accepted responsibility for attempting the hop; it does not mean that a remote bus admitted, journaled, dispatched, or delivered the message.
- Permit explicit `BACKPRESSURED` and `UNAVAILABLE` non-acceptance outcomes with a bounded reason. A non-accepted message consumes no Message Bus delivery, idempotency, cancellation, dead-letter, or journal state; retry timing remains higher-level scheduling policy.
- Keep serialization, protocol negotiation, endpoint discovery, authentication, concrete local-process or remote adapters, buffering, threading, persistence, and production wiring out of this increment.

Rationale:

The smallest honest IPC seam transports the existing route and envelope and reports only ownership of the attempted hop. Separating transport acceptance from subscriber delivery preserves all current bus semantics, avoids promising synchronous remote outcomes, and leaves provider-specific concerns behind later adapters.

Consequences:

- Later adapters can implement one interface without changing message identities, payloads, destinations, or authority semantics.
- A caller must not translate `ACCEPTED` into `DELIVERED`; receiving-side delivery outcomes remain owned by the receiving Message Bus and must travel as explicit messages if a workflow needs them.
- The contract alone crosses no process boundary and proves no adapter, wire format, durability, authentication, or production integration.
