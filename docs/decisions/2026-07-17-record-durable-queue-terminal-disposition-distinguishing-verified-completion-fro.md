# 2026-07-17: Record Durable Queue Terminal Disposition Distinguishing Verified Completion From Failure

Status: Accepted Decision

Context:

- The Scheduler queue recorded only `completedWorkItemIds`, a success-only set that both marked a work item finished and satisfied its dependents' dependencies, so it could not represent failure.
- The runtime lifecycle already distinguishes `COMPLETED` from `FAILED`, but the queue's `completeActive` operation forced every item leaving the active slot into the dependency-satisfaction set.
- The prior 2026-07-16 decision named terminal queue disposition as the next bounded contract and required verified completion and failure to stay distinct before Scheduler capacity or dependency state changes.
- The on-disk schema-v1 queue envelope is length-prefixed with no forward-compatibility marker and rejects trailing bytes; it is an unreleased, git-ignored local `.enhancer/` artifact.

Decision:

- Introduce a terminal `WorkItemDisposition` enum (`VERIFIED_COMPLETED`, `FAILED`); only `VERIFIED_COMPLETED.satisfiesDependencies()` is true.
- Split the single `completeActive` operation into `completeActiveVerified` (enters `completedWorkItemIds`, the dependency-satisfaction set) and `failActive` (enters a separate `failedWorkItemIds` set that never satisfies dependents), across the in-memory queue, the durable wrapper, and the filesystem store.
- Preserve `completedWorkItemIds` meaning exactly: verified-completed = dependency-satisfying. Extend the state partition invariant to `pending + active + verified + failed = admissionOrder`, with verified and failed disjoint.
- Keep the active slot occupied through verification (reaffirming the 2026-07-16 Option A decision); both terminal dispositions release the slot at the terminal point. Option B (a non-terminal waiting state) remains deferred.
- The queue records disposition only, never a failure reason; the inspectable cause remains in the runtime/RunRecord, linked by `workItemId`. A failed dependency leaves dependents blocked, not automatically failed.
- Revise schema-v1 in place with no version bump. Because the envelope rejects trailing bytes, any pre-existing local queue snapshot fails closed as `CorruptedSchedulerQueueStateException` on read; this is accepted for the unreleased local artifact and promises no backward compatibility.
- Leave `ResultPayload`/RunRecord result wiring, dispatcher-driven disposition recording, retry, automatic failure propagation, and Option B out of this contract.

Rationale:

Naming the terminal disposition at the type level closes the overloaded-"completion" ambiguity that caused the earlier connection conflict. A separate failed set makes dependent blocking a consequence of the existing dependency check rather than new propagation logic, and keeps the change minimal against the schema-v1 single-worker design.

Consequences:

- Failed work is durably distinct from verified completion and never satisfies dependents; the result path (next connection) has an unambiguous recoverable disposition target.
- The at-least-once requeue window, where a runtime failure precedes a persisted queue disposition, remains open until the RunRecord-backed result-wiring increment closes it with idempotent-suffix recovery.
- Pre-existing local schema-v1 queue snapshots become unreadable; no released format is affected.
