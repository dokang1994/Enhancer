# 2026-07-22: Retain Exact Work Admission History In The Durable Queue

Status: Accepted Decision

Context:

- `DurableWorkItemAdmissionHandler` persists the first work delivery through the durable
  queue, but a fresh bus replay currently reaches the queue's duplicate-identity rejection
  and dead-letters.
- Queue state retains complete `QueuedWork` only while pending or active. Terminal state
  retains the WorkItem identity but not enough content to distinguish exact replay from
  message-identity reuse with changed authorization, provenance, execution input, capability,
  or dependencies.
- A separate admission receipt or submission manifest would create a cross-store crash
  window after queue persistence and before receipt persistence. It cannot be the first
  authority for restart-safe submission.

Decision:

- Advance Scheduler queue state to schema v2 and retain every exact admitted `QueuedWork`
  in immutable admission order for the bounded lifetime of the queue, including terminal
  verified and failed work.
- Keep the existing pending, active, verified, and failed partition, one-logical-run
  binding, capacity, dependency ordering, and persist-before-exposure revision rules.
  Validate that exact history and the status partition name the same admissions in the
  same order.
- Add an explicit durable idempotent-admission operation. A new `QueuedWork` is staged and
  persisted before success. Re-entry with an exactly equal value for an existing WorkItem
  identity is a successful no-op with no revision. Any changed value under that identity
  fails closed.
- Keep strict `enqueue` behavior for ordinary Scheduler callers. Route only the durable
  work-message handler through the idempotent admission operation so bus and queue
  duplicate semantics remain distinct.
- Reject schema-v1 queue artifacts explicitly. Do not silently reinterpret or migrate
  them in this task.

Rationale:

The durable queue is already the authority that decides whether work was admitted.
Persisting exact history in the same artifact makes the admission and its replay evidence
one atomic revision and removes the otherwise unavoidable queue-to-receipt crash window.
Bounded full values avoid inventing a second canonical digest format and allow equality to
cover every authority and provenance field.

Consequences:

- Fresh-bus exact replay can complete without a second WorkItem, queue revision, or dead
  letter before or after terminal disposition; changed-content identity reuse remains
  visible failure.
- Queue artifacts retain bounded complete admission history until a separately designed
  compaction/retention policy exists, and the on-disk schema advances incompatibly to v2.
- An end-user durable submission manifest may later use this exact idempotent queue
  boundary, but queue creation, submission commands, polling, concurrent writers, and
  multi-process locking remain separate work.
