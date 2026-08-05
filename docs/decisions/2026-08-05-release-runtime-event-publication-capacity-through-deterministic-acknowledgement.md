# 2026-08-05: Release Runtime Event Publication Capacity Through Deterministic Acknowledgement

Status: Accepted Decision

## Context

The first filesystem runtime-event consumer repeatably resolves one explicit pending
publication point and its exact canonical event without mutation. The publisher counts
every retained `.runtime-event-reference` point against caller-bounded pending capacity.
Leaving every successfully resolved point pending eventually refuses new publication.

Acknowledgement cannot mean arbitrary handler success under the current contracts. A
crash after a non-idempotent callback but before rename would repeat that side effect,
and no consumer identity or durable `(consumer,event)` receipt store exists. Conversely,
another producer composition would widen the Scheduler execution surface without
closing the current producer-to-consumer capacity loop.

## Decision

Add deterministic acknowledgement of the existing read-only resolution. The caller
continues to provide the runtime-event store root, publication root, and original
canonical `<64-lowercase-hex>.runtime-event-reference` filename. The acknowledger scans
neither root and accepts exactly one retained state: that pending point or its same-root
deterministic `<64-lowercase-hex>.runtime-event-received` sibling.

Before reporting either state, resolve the selected regular non-symbolic artifact with
the existing bounded schema-v1 integrity, strict UTF-8, deterministic pending filename,
canonical `runtime-event/<goal>/<event>` grammar, exact one-Goal stream, exact event, and
re-derived reference checks. Both states present, both absent, symbolic/non-regular,
corrupt, foreign, malformed, missing-stream, missing-event, or mismatched input fails
closed without a move or rewrite.

After exact resolution of a pending point, atomically rename it in the same directory
to the acknowledged sibling without replacement or non-atomic fallback. Failure before
that move leaves the pending point authoritative. Loss of the command result after the
move re-enters through the acknowledged sibling, repeats exact event resolution, changes
nothing, and reports `ALREADY_ACKNOWLEDGED`. The acknowledged point itself is the durable
receipt that this boundary resolved the exact event; it is not a receipt for handler
delivery, event application, or a runtime transition.

Extend publisher exact replay to recognize the acknowledged sibling before pending
capacity evaluation. Exact acknowledged replay returns without recreating or rewriting
the pending point, including when the released slot is occupied by another event.
Conflicting pending plus acknowledged state and corrupt or foreign acknowledged reuse
fail closed. Only the original `.runtime-event-reference` suffix contributes to the
existing pending-capacity count.

Expose the operation through a separate supported `runtime-event-acknowledge` command
using the same three explicit inputs as `runtime-event-read`. It reports acknowledgement
state, the retained acknowledged filename, and the same bounded event identity and
provenance metadata. The existing read command remains mutation-free and does not
reinterpret an acknowledged sibling as pending.

## Rationale

This contract makes acknowledgement restart-safe without inventing a handler protocol.
Its durable prefixes are bounded: unresolved or failed validation leaves pending state;
resolved-but-not-renamed re-entry repeats a read; atomic rename exposes one retained
state; and lost output revalidates the acknowledged state. Publisher awareness makes
capacity release stable across source-owner exact replay.

A general event handler requires a separate consumer identity and durable idempotency
receipt before side effects. Message Bus delivery, event application, and additional
transition-owner composition remain separate boundaries.

## Consequences

- One exact read-only observation can release one pending publisher-capacity slot.
- Acknowledged artifacts remain retained evidence and have no automatic deletion,
  cleanup, time-based retention, or global acknowledged-history bound.
- Old publisher binaries do not understand acknowledged replay; mixed-version access to
  one publication root is unsupported.
- Cross-process coordination, directory consumption, consumer offsets, handler side
  effects, application authority, MessageEnvelope evolution, and Gate promotion are not
  added.
