# 2026-07-16: Bound Pending Publications With Deterministic Non-Blocking Refusal

Status: Accepted Decision

Context:

- Run-to-completion ordering replaced recursive delivery with an explicit FIFO pending queue, but that queue is unbounded and a handler cascade can retain arbitrarily many envelopes before the drain catches up.
- The bus is deliberately synchronous and single-threaded. Blocking a publisher cannot create useful flow control when the publisher is the handler currently holding the drain; it would deadlock rather than relieve pressure.
- Cancellation already establishes that work refused before admission must not be journaled or consume delivery state, or a later replay could create a side effect that never originally occurred.
- The user authorized the next recorded Gate 7 increment on 2026-07-16.

Decision:

- Add an immutable `BackpressurePolicy(maxPendingPublications)` bounded from 1 through 4096; existing constructors use a finite default of 4096, and an overload accepts both retry and backpressure policies.
- Add scope-level `DeliveryStatus.BACKPRESSURED`. When the pending queue is at capacity, refuse the submission immediately without journaling it, invoking a handler, consuming an idempotency key, creating a dead letter, or changing cancellation state.
- Keep accepted work FIFO. A re-entrant publisher receives `ENQUEUED` for accepted work or `BACKPRESSURED` for refused work; the draining caller receives outcomes only for the admitted cascade, while the publisher that attempted refused work receives its refusal directly.
- Apply the same capacity to replay batch submission: accept the deterministic prefix that fits, refuse the remaining entries, drain the accepted prefix, and return the refusal outcomes without appending any replay entry or caused publication to the live journal.
- Make no thread wait, timer, retry, eviction, priority, persistence, IPC, or production-wiring policy in this increment. A caller may retry a refused envelope later under its own higher-level scheduling authority.

Rationale:

A finite admission bound closes the only memory-growth hazard introduced by run-to-completion ordering without pretending a synchronous handler can block safely. Immediate typed refusal preserves determinism and makes overload visible to the exact caller that attempted the publication. Treating refusal like cancellation before admission preserves journal replay truth and keeps a later explicit retry possible because no idempotency state was consumed.

Consequences:

- The pending queue never contains more than the configured bound, but the journal, idempotency keys, cancellation set, dead letters, and returned outcome collections remain process-local and require separate retention or persistence work.
- A handler must inspect the immediate result of a caused publication if it needs to react to overload; the top-level drain cannot fabricate an outcome for work it never admitted.
- Replay of a batch larger than the configured capacity is partial but deterministic and observable; the caller may retry the refused suffix explicitly.
- Gate 7 remains `Specified - Next` and Contract Verified only; no production caller uses the bus yet.
