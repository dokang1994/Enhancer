# Current Task

## Status

Completed

## Task

Persist exact ordered durable work-admission history inside Scheduler queue state and
make restart replay of an identical work message idempotent without accepting
message-identity reuse with changed content.

## Task ID

persist-exact-durable-work-admission-history

## Context

The production `DurableWorkItemAdmissionHandler` derives a stable WorkItem identity and
persists the first delivery into `DurableSingleWorkerSchedulerQueue`. The queue retains
the complete `QueuedWork` only while it is pending or active; after terminal disposition
it retains only the WorkItem identity. A fresh Message Bus therefore cannot prove that a
re-delivered message is exactly the original admission and currently dead-letters every
restart replay.

An end-user submission manifest cannot safely solve that gap first. A process may stop
after queue persistence but before a separate manifest or admission receipt is written,
leaving the restarted submitter unable to distinguish an interrupted identical
submission from changed-content identity reuse. Exact history owned by the queue is the
smallest durable prerequisite because queue persistence is already the admission
authority.

## Justified By

- 2026-07-22: Connect Work Admission To The Durable Scheduler Queue Through A Persist-First Handler
- 2026-07-16: Persist Gate 8 Queue Transitions Before Exposing State
- 2026-07-16: Separate IPC Transport Acceptance From Message-Bus Delivery

## Acceptance Criteria

- Scheduler queue state advances to one explicit new schema that retains every admitted
  `QueuedWork` in immutable admission order, including completed and failed work, while
  preserving the existing pending/active/verified/failed partition and one-logical-run
  invariants.
- Durable admission persists a new exact `QueuedWork` before success. Re-admitting the
  exact same value after recovery succeeds without a queue revision or second WorkItem;
  reusing its WorkItem/message identity with any changed capability, envelope,
  authorization, provenance, execution input, or dependency content fails closed.
- `DurableWorkItemAdmissionHandler` uses the exact idempotent durable-admission boundary.
  Same-bus replay remains a bus duplicate; fresh-bus exact replay succeeds without a
  dead letter both before and after terminal queue disposition.
- Filesystem serialization round-trips the exact ordered history with strict bounds and
  integrity checks. Prior queue schema artifacts fail explicitly; no in-place migration
  or silent reinterpretation is claimed.
- A named real-filesystem integration admits through the production handler, runs the
  existing process-isolated Scheduler cycle to terminal state, restarts the queue and
  Message Bus, re-delivers the exact envelope, and observes unchanged terminal
  disposition, queue revision, and RunRecord count.
- Focused RED/GREEN tests, the full Gradle build, strict Java lint, structural-document
  checks, and diff checks pass with fresh evidence.

## Out Of Scope

- Queue creation, an end-user submission manifest or submission CLI, admission receipts
  outside queue state, durable Message Bus journaling, worker polling/service loops,
  arbitrary dependency submission, concurrent writers, or multi-process locking.
- Queue schema migration from the prior version, history compaction/cleanup, priority,
  fairness, broader budgets, authenticated control application, external adapter/effect
  execution, Gate 9, or whole-Gate Operational promotion.
- Commit, push, PR, merge, release, or deployment unless separately requested.

## Approval

The user explicitly asked to continue the project on 2026-07-22. The previous completed
task required assessment of exact cross-bus admission recovery and an end-user submission
manifest; the assessment selected exact queue-owned admission history as the prerequisite
that makes a later submission manifest restart-safe.

## Verification

- Focused RED failed only on the absent `admitIdempotently` and `admittedWork`
  contracts.
- Focused GREEN passed the queue-state, durable-queue, filesystem-store, admission-handler,
  and named process-isolated recovery integration suites.
- Fresh full verification passed 85 suites and 457 tests: 454 passed, 3 existing
  privilege-dependent Windows symbolic-link setup cases skipped, 0 failures, and 0
  errors under build-enforced Java 17 `-Xlint:all -Werror`.
- Post-document structural checks and final diff checks are recorded in
  `docs/verification-log.md`.

## Next

After this task, design the smallest durable end-user submission manifest and queue
creation boundary over the exact idempotent admission contract, without adding polling,
authenticated controls, external-effect adapters, or Gate 9.
