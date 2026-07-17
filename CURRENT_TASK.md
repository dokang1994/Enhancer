# Current Task

## Status

Completed

## Task

Define the Gate 8 durable queue terminal-disposition contract so execution acknowledgement stays distinct from verified completion, failed work never satisfies dependencies, and later ResultPayload integration has an unambiguous recoverable target.

## Task ID

gate-8-durable-queue-terminal-disposition

## Justified By

- 2026-07-17: Record Durable Queue Terminal Disposition Distinguishing Verified Completion From Failure

## Context

The Scheduler queue recorded only `completedWorkItemIds`, a success-only set that both marked a work item finished and satisfied its dependents. It could not represent failure, so any item leaving the active slot was forced into the dependency-satisfaction set. The runtime already distinguishes `COMPLETED` from `FAILED`; the queue needed the same terminal distinction before result and RunRecord wiring can record an unambiguous disposition.

## Acceptance Criteria

- Add a terminal `WorkItemDisposition` enum where only verified completion satisfies dependencies.
- Add a separate `failedWorkItemIds` set to schema-v1 `SchedulerQueueState`; extend the partition invariant to `pending + active + verified + failed = admissionOrder` with verified and failed disjoint.
- Split the queue's single completion into `completeActiveVerified` and `failActive` across the in-memory queue, the durable persist-before-exposure wrapper, and the filesystem store.
- Failed work never enters the dependency-satisfaction set, so its dependents stay blocked; the queue stores disposition only, not a failure reason.
- Persist the failed disposition in the schema-v1 on-disk format revised in place (no version bump) with exact restart recovery; a persisted terminal disposition is never re-run.
- Record the accepted decision, including the local-artifact fail-closed compatibility boundary.

## Out Of Scope

- `ResultPayload`/RunRecord result delivery and terminal runtime persistence wiring.
- Dispatcher-driven disposition recording from a terminal AgentRun.
- Retry, automatic failure propagation to dependents, and a non-terminal awaiting-verification queue state.
- Workers, effect fencing, external effects, multi-process coordination, and capability maturity promotion beyond Contract Verified.
- Commit, push, PR, merge, release, or deployment without a new explicit user request.

## Approval

Approved by the user's 2026-07-17 request to continue the project on the roadmap's next increment (terminal queue disposition), confirmed against ROADMAP.md connection backlog item 1.

## Verification

- Task 1 RED: `WorkItemDispositionTest` failed to compile with the missing enum; GREEN passed after adding the enum.
- Task 2 RED: `SchedulerQueueStateTest` failed on constructor arity; GREEN passed after adding the failed set, partition disjointness, and accessor.
- Task 3 RED: `SingleWorkerSchedulerQueueTest` failed on missing `completeActiveVerified`/`failActive`/`failedWorkItemIds`/`dispositionOf`; GREEN passed after the split and rename cascade.
- Task 4 RED: `FileSystemSchedulerQueueStoreIntegrationTest.roundTripsFailedDispositionAcrossStoreInstances` failed behaviorally (failed set dropped); GREEN passed after serializing the set.
- Task 5 RED: `DurableSingleWorkerSchedulerQueueTest` failed on missing durable `failActive`; GREEN passed with persist-before-exposure and recovery, plus a real filesystem failed round-trip.
- Full regression passed 59 suites and 261 tests: 259 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors under `--warning-mode all`; Java 17 strict lint passed across 150 production sources.
- `completedWorkItemIds` semantics, existing queue/runtime behavior, and every other contract are unchanged; only the queue gains the failed disposition.

## Next

Integrate the RunRecord-backed result path (connection 2): durable RunRecord resolution, a matching `ResultPayload`, persisted AgentRun/Goal terminal state, then the matching queue disposition, with idempotent-suffix recovery closing the at-least-once requeue window.
