# 2026-07-22: Persist Submission Intent Before Creating The Scheduler Queue

Status: Accepted Decision

Context:

- `scheduler-cycle` deliberately recovers only an existing queue, while the production
  admission handler requires a queue instance. An end user therefore has no restart-safe
  boundary for the earlier act of declaring work and creating that queue.
- The queue now retains exact immutable admission history and can treat exact replay as a
  no-op while rejecting changed content under an admitted identity.
- Creating an empty queue before retaining the submission inputs leaves a crash window in
  which recovery cannot reconstruct the intended work. Writing a mutable receipt after
  admission creates the inverse queue-to-receipt crash window.

Decision:

- Persist one immutable submission manifest before queue creation. Key it by the canonical
  message identity and include the target queue identity, fixed capacity, required worker
  capability, and exact `MessageEnvelope`.
- Treat exact manifest replay as success without rewriting the artifact. Reject any changed
  manifest under the same message identity.
- After manifest persistence, create the declared queue only when absent; otherwise recover
  it and require its fixed capacity to equal the manifest. Then pass the exact envelope to
  `DurableWorkItemAdmissionHandler` so the queue remains the admission authority.
- Derive completion by exact queue admission history. Do not add a mutable admission status
  or receipt to the manifest.
- Keep this application boundary independent from Scheduler execution. A later CLI may
  compose governed repository inputs into the manifest, but this task does not combine
  submission with `scheduler-cycle`.

Rationale:

The ordered steps form monotonic restartable prefixes: durable intent, durable empty queue,
and exact durable admission. The existing queue history makes the final step idempotent,
while the immutable manifest makes the pre-queue inputs recoverable. No cross-store commit
or mutable second authority is required.

Consequences:

- A process may restart after either durable prefix and converge on one unchanged admitted
  work item; full exact replay changes neither manifest content nor queue revision.
- Queue capacity becomes part of immutable submission intent and must be observable on a
  recovered durable queue so configuration drift fails closed.
- This boundary remains single-process and does not claim concurrent-writer locking,
  polling, authenticated control application, external effects, or Gate 9.
