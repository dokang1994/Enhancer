# User continuation request on 2026-08-21 into the typed model-work golden-wire implementation

Status: Accepted Decision

## Context

RFC-0018 is Accepted and names the first implementation prompt as adding a fifth typed
`ModelWorkPayload`, one mandatory complete profile-bearing execution input, and a
model-work-only envelope/spool v2 representation while preserving every existing v1
payload and cancellation-signing byte.

The completed delivery task names this implementation as the next work. The user
requested continuation on 2026-08-21.

## Decision

Authorize a bounded RED-first implementation of the pure bus value and wire boundary:
add the fifth payload kind and mandatory execution input, extend the package-private
message codec with a model-work-only v2 family, preserve exact legacy v1 encoding, and
add focused shape, validation, round-trip, corruption, cross-family, and golden-byte
compatibility tests.

The implementation may synchronize owning architecture, maturity, task, changelog, and
append-only verification documents after fresh focused and full verification.

This decision does not authorize manifest, queue, AgentRuntime, WorkItem, Scheduler,
process-worker, CLI, Tool, RunRecord, gateway, provider, route, network, credential,
spend, migration, artifact rewrite, push, merge, release, deployment, or destructive
cleanup changes.

## Consequences

- Existing `WorkPayload`, `MessageEnvelope.ENVELOPE_VERSION`, the four legacy payload
  encodings, transport-spool v1 frames, and cancellation canonical bytes remain exact.
- `ModelWorkPayload` is transportable pure untrusted data only; no current producer or
  runtime consumer accepts or executes it.
- Durable schema migration and Scheduler caller integration remain separately
  authorized follow-up work under RFC-0018.
