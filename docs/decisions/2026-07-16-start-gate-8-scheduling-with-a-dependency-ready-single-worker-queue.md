# 2026-07-16: Start Gate 8 Scheduling With A Dependency-Ready Single-Worker Queue

Status: Accepted Decision

Context:

- Gate 8 is the sole `Specified - Next` gate and already has immutable `WorkItem` admission, but no Scheduler consumer.
- A full durable AgentRun state machine, lease protocol, recovery mechanism, or worker implementation would exceed the smallest coherent next increment.
- Dependency readiness requires a completion signal, while forward references and arbitrary graph mutation would require broader cycle and persistence policy.

Decision:

- Add an immutable queued-work value containing one existing `WorkItem` and up to 256 unique canonical dependency work identities.
- Reject self-dependency and require every dependency to have been admitted earlier or already completed. This gives the first queue deterministic dependency validation and prevents cycles by construction without claiming general graph-cycle analysis.
- Add an in-memory run-scoped queue bounded to 4096 total work-item admissions, preserving admission order and selecting the first dependency-ready item.
- Permit at most one active work item. A claim while work is active returns no item; explicit completion of the matching active identity releases dependents.
- Preserve the exact `WorkItem` and its unchanged Gate 7 envelope. Queue admission and completion create no task approval, Tool authority, or execution result.
- Defer persistence, state versioning, failure/retry/cancellation, priority/fairness, leases/fencing, checkpoints, orphan recovery, worker execution, threading, and production wiring.

Rationale:

This is the smallest consumer that makes `WorkItem` useful to Gate 8 while preserving deterministic single-agent sequencing. Requiring dependencies to exist before their dependent prevents unknown and cyclic graphs without introducing a mutable graph service. One explicit active slot establishes the Scheduler boundary without pretending that a claim is a durable lease.

Consequences:

- Work must be admitted in dependency order in this first queue.
- Completion is an in-memory Scheduler fact, not a verified AgentRun terminal state or durable record.
- The queue sub-capability may become Contract Verified, but Gate 8 remains `Specified - Next`.
- Durable queue state and restart-safe recovery are the immediate next increment.
