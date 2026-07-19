# Current Task

## Status

Completed

## Task

Add one in-process `DurableAgentRunWorker` that drives a single scheduling cycle end to end — claim + lease, execute via an injected port to a durable RunRecord, acknowledge execution, finalize runtime and queue disposition — and recovers a cycle interrupted at any seam via a worker-owned durable cycle-intent checkpoint.

## Task ID

gate-8-in-process-scheduler-worker

## Justified By

- 2026-07-17: Record In-Process Scheduler Worker Driving One Recoverable Claim-To-Disposition Cycle Through A Durable Cycle-Intent Checkpoint

## Context

Gate 8 held every durable piece of one work cycle as separate, tested contracts — durable queue claim plus fenced lease (`DurableAgentRunDispatcher`), fence-checked execution acknowledgement (`DurableAgentRuntime.completeExecution`), and RunRecord-backed result finalization (`DurableAgentRunFinalizer`) — but nothing drove them end to end. The finalizer had no production consumer and explicitly deferred the pre-terminal recovery window (retaining the `runRecordReference` across a crash before finalization) to the connection-3 worker/driver, which did not exist. Connection 3 was split into 3a (in-process worker, this task), 3b (process isolation), and 3c (concrete local IPC adapter).

## Acceptance Criteria

- Add `PendingFinalization`, `PendingFinalizationStore`, `CorruptedPendingFinalizationException`, and `FileSystemPendingFinalizationStore` under `com.enhancer.runtime`: a single-record durable cycle-intent checkpoint, bounded, strict-UTF-8, digest-checked, atomically published, failing closed on corrupt/truncated/oversized state.
- Add `AgentRunExecution` (injected execution port returning a `runRecordReference`) and `DurableAgentRunWorker` with `runOneCycle(leaseDuration) -> Optional<WorkItemDisposition>`.
- Drive the authoritative order: cycle-intent (ids) -> queue claim + lease -> RunRecord persisted (ref) -> intent updated with ref -> `completeExecution` -> `finalizeAgentRun` -> queue disposition -> clear intent; the intent is written before the claim and the reference before acknowledgement.
- Route recovery by runtime state as the source of truth: terminal -> `recoverFinalization`; `AWAITING_VERIFICATION` -> `finalizeAgentRun(ref)`; `EXECUTING`/`READY`/`PLANNING`, unstarted AgentRun, or missing runtime (`MissingAgentRuntimeStateException` tolerated) -> re-drive with the same identities, skipping re-execution when the reference exists.
- Fail closed: an execution/finalizer failure propagates with the intent retained; a cycle that claimed nothing clears its intent and leaves no durable trace.
- No change to `DurableAgentRunDispatcher`, `DurableAgentRuntime`, `DurableAgentRunFinalizer`, `DurableSingleWorkerSchedulerQueue`, or any schema; the dispatcher and finalizer wrap the same queue instance.

## Out Of Scope

- Process isolation of the worker (3b) and a concrete `MessageTransport` local IPC adapter (3c).
- The real `AgentLoop`-backed execution port and the `WorkPayload` execution-input extension it needs.
- Retry through additional AgentRuns, cancel/pause/resume, budgets, priority/fairness, multi-agent execution, schema migration, and any capability-maturity promotion beyond Contract Verified.
- Commit, push, PR, merge, release, or deployment without a new explicit user request.

## Approval

Approved by the user's 2026-07-17 request to continue the project on the merged worker design/plan (PR #6), executing `docs/superpowers/plans/2026-07-17-gate-8-in-process-scheduler-worker.md` against the design spec as amended by commit `cf06808`.

## Verification

- Task 1 RED: `FileSystemPendingFinalizationStoreIntegrationTest` failed compilation with 19 errors naming only the absent `PendingFinalization`, `FileSystemPendingFinalizationStore`, and `CorruptedPendingFinalizationException`; GREEN passed 8 of 8 checkpoint round-trip, overwrite, idempotent-clear, corrupt, truncated, and oversized cases.
- Task 2 RED: `DurableAgentRunWorkerTest` failed compilation with 5 errors naming only the absent `AgentRunExecution` and `DurableAgentRunWorker`; GREEN passed the verified-cycle, failed-cycle, and empty-queue happy paths (3 of 3).
- Task 3 RED: the six added recovery tests failed behaviourally with the deliberate `UnsupportedOperationException` resume stub while the three Task 2 tests stayed green; GREEN passed all 9 after the recovery routing replaced the stub, covering resume-after-acknowledgement, post-terminal disposition recovery, re-drive while `EXECUTING`, re-drive with no runtime state, empty-queue intent convergence, and execution-failure recoverability.
- Task 4: `FileSystemAgentRunWorkerIntegrationTest` passed 2 of 2 on first run — an interrupted cycle resumed on a fresh worker and drove the dependent end to end, and a failed outcome blocked the dependent — composing all four real filesystem stores.
- Runtime package suite: 14 suites, 73 tests, 0 skips, 0 failures, 0 errors.
- Full regression and strict lint recorded in `SESSION_HANDOFF.md` and `PROJECT_STATE.md`.

## Next

Choose the next bounded sub-increment: worker process isolation (3b), the concrete local IPC adapter (3c), or the `AgentLoop`-backed execution port with its `WorkPayload` execution-input extension.
