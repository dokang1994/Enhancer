# 2026-07-29: Receive One Control Spool Through The Message Bus Into Durable Request State

Status: Accepted Decision

## Context

The remaining Gate 7 branches do not have equal prerequisites:

- Control already has a real queue consumer, `RuntimeControlAdmissionHandler`, which
  persists an exact bound request in Gate 8 runtime state and exposes storage failure
  through the Message Bus retry/dead-letter contract. Applying cancel, pause, or resume
  remains Gate 12 authenticated-control work.
- Handoff has no production consumer and belongs to Gate 13 multi-agent execution.
- Topic delivery has no accepted production event catalog or downstream application
  consumer.
- Cancellation, cascade ordering, and pending-queue backpressure are Contract Verified,
  but no accepted production flow currently causes those branches without inventing a
  producer or granting unauthenticated control authority.
- A durable Message Bus journal still lacks subscription checkpoints, truncation
  ownership, additional durable consumers, and cross-store recovery ordering.
- Retained `.received` Work points and failed/incomplete invocation spools have no
  cleanup policy. Deleting them requires a separate bounded retention design and
  destructive authority.

`FileSpoolMessageTransport`, the exact point-resolution and acknowledgement pattern, the
real `InProcessMessageBus`, and the durable control-request handler already provide both
sides of one smaller connection. Receiving an untrusted Control request records intent
only and therefore does not cross Gate 12's authority boundary.

## Decision

Implement one separate `scheduler-receive-control` point command before Handoff, topic,
durable-journal, or retention work.

The bounded connection will:

1. accept an explicit spool root, one canonical pending `.transport` filename, an exact
   expected queue destination, an explicit runtime-state root, and one canonical Goal
   identity;
2. resolve exactly one regular non-symbolic pending point or its deterministic
   same-directory `.received` point, rejecting neither/both/foreign point state before
   runtime mutation;
3. decode the unchanged `TransportMessage`, require the exact queue destination and
   `ControlPayload`, and publish it through a fresh `InProcessMessageBus` subscription
   backed by `RuntimeControlAdmissionHandler`;
4. report success only after the exact request is durable, distinguishing a newly
   recorded request from revision-free exact replay;
5. atomically rename a pending point to `.received` only after durable request admission,
   and revalidate acknowledged-point re-entry before reporting replay.

The focused RED contract will cover foreign destination and payload refusal before
runtime mutation, exact replay, changed-identity-content refusal, storage failure, and
pending-versus-acknowledged point ambiguity. A named real-filesystem integration will
send one Control envelope through `FileSpoolMessageTransport`, receive it through the
supported command and real bus into the existing Goal ledger, then prove acknowledged
re-entry changes neither runtime revision nor request history.

The command records untrusted intent only. It does not authenticate a producer, apply a
control signal, reclaim or release a lease, interrupt a worker, mutate a Scheduler
queue, call `InProcessMessageBus.cancel`, execute work, create a durable bus journal,
scan a directory, or delete retained evidence.

## Rationale

This is the smallest remaining Gate 7 connection with real implemented collaborators on
both sides and an observable durable outcome. It extends transport-to-bus coverage to
the existing Control consumer while preserving the accepted boundary that possession of
an envelope grants no runtime authority.

Selecting Handoff or topic first would invent a downstream consumer. Selecting
authenticated control application would cross into Gate 12. Selecting a durable journal
would introduce another recovery store before its ownership and lifecycle are specified.
Selecting retention would require deletion policy and authority that this connection
does not need.

## Consequences

- Gate 7 gains a supported local Control transport-to-bus-to-durable-request connection;
  the full authenticated Control lifecycle remains unimplemented.
- The existing Work point acknowledgement semantics become a shared transport-point
  pattern only to the extent needed to avoid divergent validation and atomic-rename
  behavior.
- A future governed Control producer remains separate from this receiver.
- Handoff, topic, authenticated application, cancellation/cascade/backpressure production
  flows, durable journaling, directory consumption, and cleanup/retention remain separate
  tasks with their existing owners.
