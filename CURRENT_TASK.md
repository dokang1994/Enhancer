# Current Task

## Status

Completed

## Task

Implement Gate 8 connection sub-increment 3c: give `MessageTransport` its first concrete adapter as `FileSpoolMessageTransport`, carrying one route and envelope to a local spool directory a peer process reads, with the wire format owned separately by `MessageEnvelopeCodec`.

## Task ID

gate-8-file-spool-transport

## Justified By

- 2026-07-20: Carry The First Transport Hop Through A Local File Spool

## Context

The 2026-07-16 transport decision deliberately deferred serialization, endpoint discovery, concrete adapters, and persistence, so `MessageTransport` had no implementation and no process boundary had been crossed. The Roadmap records that connection 3 requires a separately selected adapter, making the selection its own decision. A file spool was chosen over a Unix domain socket because it needs no capability the project does not already exercise and its tests are deterministic. Worker process isolation (3b) needs `ProcessBuilder`, which `.ai/workflow.md` step 6 forbids implementing inside a RED cycle without explicit authority, so it is deliberately not part of this increment.

## Acceptance Criteria

- `FileSpoolMessageTransport` implements `MessageTransport`, mapping a durably spooled message to `ACCEPTED`, capacity exhaustion measured against a `BackpressurePolicy` to `BACKPRESSURED`, and an unusable spool root to `UNAVAILABLE`, with a refused message spooling nothing.
- Each hop is published through a temporary file and an atomic move into its own freshly generated name, so a reader never observes a partial message and resending never overwrites an earlier hop.
- `MessageEnvelopeCodec` owns the frame, carries all four payload kinds, preserves nanosecond occurrence time, encodes deterministically, and fails closed with `CorruptedSpooledMessageException` on bad magic, invalid length, digest mismatch, malformed UTF-8, trailing bytes, or an envelope invariant violation.
- The codec is verifiable without a filesystem; the adapter's own tests cover publication only.
- No production path constructs the adapter, so no runtime behaviour changes.
- Full regression passes with 0 failures and 0 errors, and strict lint passes across all production sources.

## Out Of Scope

- Worker process isolation (3b) and the `ProcessBuilder` authority it requires.
- A receiving-side bus, endpoint discovery, authentication, framing for streamed transports, retry policy, and spool retention or cleanup.
- Wiring the adapter into the CLI, worker, or bus.

## Approval

Approved by the user's 2026-07-20 selection of 3c with a file spool adapter after being shown that 3b requires explicit external-command authority, followed by their direction to adopt the stronger elements of an independently written implementation of the same contract.

## Verification

Recorded in `docs/verification-log.md` under File Spool Transport Verification.

## Next

Gate 8 connection sub-increment 3b, worker process isolation, which requires explicit user authority for process spawning before it can be designed or implemented.
