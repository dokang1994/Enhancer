# Gate 8 RunRecord-Backed Result Path Finalization — Design

- Date: 2026-07-17
- Task ID: `gate-8-result-path-finalization`
- Gate: Delivery Gate 8 (Agent Runtime and Scheduler), sole `Specified - Next`
- Connection: ROADMAP backlog #2 (RunRecord-backed result finalization)
- Status: Design (pending implementation plan)

## Problem

Gate 8 now has, as separate durable facts: fence-checked execution acknowledgement
(`EXECUTING -> AWAITING_VERIFICATION`), a durable Goal/AgentRun terminal transition
(`DurableAgentRuntime.recordResult`), and a durable queue terminal disposition
(`completeActiveVerified` / `failActive`). Nothing connects them. After a worker
acknowledges execution, no component takes the independently verified outcome,
moves the AgentRun/Goal to its terminal state, and records the matching queue
disposition — and no recovery finishes that sequence after an interruption.

## Goal

Add one durable, idempotent finalizer that connects a resolved RunRecord to the
terminal runtime state and the matching queue disposition, in a recoverable order,
so that:

- the verified outcome (RunRecord) drives both the AgentRun/Goal terminal state and
  the queue disposition, and the two stores can never disagree;
- recovery idempotently finishes the missing suffix of that order;
- failed or unverifiable outcomes never satisfy dependents.

## Existing pieces (grounded)

- `DurableAgentRuntime.recordResult(agentRunId, resultMessage)` moves
  `AWAITING_VERIFICATION -> COMPLETED|FAILED`. `AgentRuntimeState.recordResult`
  validates the `ResultPayload` envelope against the Goal's work message
  (logicalRunId, correlationId, causationId = work messageId, taskId; messageId
  distinct from goal/workItem/agentRun ids) and completes only on `VERIFIED`.
- `RuntimeAgentRun` exposes `Optional<MessageEnvelope> resultMessage()`; a terminal
  run's payload is the `ResultPayload`.
- `ResultPayload(taskId, runRecordReference, verificationStatus)`.
- `SingleWorkerSchedulerQueue` / durable wrapper: `completeActiveVerified(workItemId)`
  and `failActive(workItemId)`; the durable queue self-recovers via `recover`.
- `RunRecordStore.resolve(reference)` returns a `ResolvedRunRecord(metadata, record)`;
  `RunRecord` carries `ApprovedTask approvedTask` (taskId + sourceDocument; **no source
  SHA**) and a `verification` decision with a `VerificationStatus`.
- `FileSystemRunRecordStore.persist` assigns a **random** `run-record/<uuid>`
  reference, so persist is not idempotent.

## Confirmed decisions

- **RunRecord is an input, resolved by reference — not persisted by the finalizer.**
  ROADMAP #2 says "durable RunRecord *resolution*"; persist is non-idempotent
  (random UUID), so a finalizer that persisted would duplicate on recovery; and the
  verified outcome originates outside this increment (no Scheduler worker yet —
  connection #3).
- **The finalizer is an idempotent coordinator** over the order: resolve RunRecord
  -> runtime terminal (`recordResult`) -> queue disposition, each step guarded by
  observed store state.
- **The queue disposition is derived from the runtime terminal status**, not
  re-derived from the RunRecord, so runtime and queue cannot diverge.
- **`verificationStatus` is read from the resolved RunRecord** (single source), then
  carried in the `ResultPayload`; `VERIFIED -> completeActiveVerified`, otherwise
  `failActive`.

## Design improvements folded in

1. **Two entry points.** `finalizeAgentRun(goalId, agentRunId, runRecordReference)` drives
   the forward path (needs the reference). `recoverFinalization(goalId)` is pure
   recovery: it recovers the runtime and, if the AgentRun is terminal, applies only
   the queue disposition from the terminal status — **no reference required**. The
   only window that needs the caller to re-supply the reference is before the
   runtime terminal transition; after it, recovery is autonomous.
2. **Single source of truth for disposition = runtime terminal status.**
   `COMPLETED -> completeActiveVerified`, `FAILED -> failActive`.
3. **RunRecord-to-Goal binding.** Before finalizing, the resolved RunRecord's
   `approvedTask.taskId()` and `approvedTask.sourceDocument()` must equal the Goal's
   `workItem.taskRevision().taskId()` and `sourceDocument()`; a mismatch is rejected.
   (`ApprovedTask` carries no source SHA, so the bind is task + document, not
   revision hash — enough to reject a RunRecord for a different task. The
   `ResultPayload` validation alone is insufficient because the finalizer constructs
   that payload from the Goal's own work message.)
4. **Consistency assert on terminal re-call.** If `finalizeAgentRun` is re-called after the
   runtime is already terminal, the stored result's `runRecordReference` must equal
   the supplied reference; a mismatch is an explicit error, never a silent overwrite.
5. **Fail closed on missing/corrupt RunRecord.** If `resolve` fails, no disposition
   is recorded and the run stays `AWAITING_VERIFICATION` (recoverable). Evidence is
   required before any terminal disposition.
6. **Named type and consumer.** New type `DurableAgentRunFinalizer` under
   `com.enhancer.runtime`; its next-gate consumer is the Scheduler worker/driver
   (connection #3). The result envelope's `messageId` is derived deterministically
   from the `agentRunId` so repeated pre-terminal attempts build the same envelope.

## Architecture

One new coordinator type; no new store and no schema change.

### `DurableAgentRunFinalizer` (new, `com.enhancer.runtime`)

Constructed with the durable queue, the `AgentRuntimeStateStore`, the
`RunRecordStore`, and a `Clock`. Queue and runtime remain separate durable
boundaries; the finalizer performs no cross-store transaction.

```
finalizeAgentRun(goalId, agentRunId, runRecordReference):
    runtime = DurableAgentRuntime.recover(goalId, runtimeStore, clock)
    run = runtime.agentRun()            // must exist and belong to agentRunId
    if run.status() == AWAITING_VERIFICATION:
        resolved = runRecordStore.resolve(runRecordReference)   // fail closed
        // improvement 3: bind on taskId + sourceDocument
        requireBinding(resolved.record.approvedTask, goal.workItem.taskRevision)
        status  = resolved.verification.status()
        result  = buildResultEnvelope(goal.workItem, runRecordReference, status)
        runtime.recordResult(agentRunId, result)                // runtime terminal
    else if run.status().isTerminal():
        assertStoredResultReference(run, runRecordReference)     // improvement 4
    else:
        throw IllegalState  // cannot finalize a run that has not acknowledged execution
    applyQueueDisposition(goal.workItem.workItemId(), run-after)  // improvement 2

recoverFinalization(goalId):
    runtime = DurableAgentRuntime.recover(goalId, runtimeStore, clock)
    run = runtime.agentRun()
    if run.status().isTerminal():
        applyQueueDisposition(goal.workItem.workItemId(), run)    // no reference
    // else: nothing to recover; the forward path still owns the run

applyQueueDisposition(workItemId, terminalRun):
    if queue.activeWork() is workItemId:
        if terminalRun.status() == COMPLETED: queue.completeActiveVerified(workItemId)
        else:                                 queue.failActive(workItemId)
    // else: disposition already recorded — skip (idempotent)
```

`buildResultEnvelope` constructs a `MessageEnvelope` carrying a `ResultPayload`,
with `correlationId`, `causationId` (= work messageId), and `logicalRunId` copied
from the Goal's work message, `taskId` from `workItem.taskRevision().taskId()`,
`runRecordReference` as given, `verificationStatus` from the RunRecord, and a
`messageId` deterministically derived from the `agentRunId` and distinct from every
runtime identity — exactly what `AgentRuntimeState.recordResult` already validates.

### Ordering and recovery

Authoritative order: (RunRecord persisted upstream) -> runtime terminal state ->
queue disposition. Each of the finalizer's own writes persists before it is exposed
(the underlying `DurableAgentRuntime` and `DurableSingleWorkerSchedulerQueue` already
guarantee persist-before-exposure). Recovery finishes the suffix:

- Crash after runtime terminal, before queue disposition: `recoverFinalization`
  reads the terminal status and records the disposition — no external input.
- Crash before runtime terminal: the run is still `AWAITING_VERIFICATION`;
  `finalizeAgentRun` is re-driven with the same reference (idempotent — see improvement 4).

No cross-store transaction is claimed; the sequence is an idempotent, recoverable
prefix, matching the 2026-07-16 accepted decision.

## Error handling

- Missing/corrupt RunRecord (`MissingRunRecordException` / `CorruptedRunRecordException`
  from `resolve`): propagate; no disposition recorded; run stays recoverable.
- RunRecord not bound to the Goal (taskId/sourceDocument mismatch): `IllegalArgumentException`.
- `finalizeAgentRun` on a run that is `PLANNING`/`READY`/`EXECUTING` (execution not yet
  acknowledged): `IllegalStateException`.
- Re-`finalize` after terminal with a different reference: `IllegalStateException`.
- Persistence failure in either store: the underlying durable wrappers retain the
  prior revision; a half-applied disposition is never exposed.

## Testing strategy (test-first, RED classified before GREEN)

- **`DurableAgentRunFinalizerTest`** (in-memory stores):
  - verified RunRecord -> runtime `COMPLETED` + queue `completeActiveVerified`,
    dependents released;
  - non-verified RunRecord -> runtime `FAILED` + queue `failActive`, dependents
    blocked;
  - crash-after-runtime-terminal simulated: `recoverFinalization` records the
    disposition with no reference;
  - re-`finalizeAgentRun` after terminal is a no-op; re-`finalizeAgentRun` with a different
    reference is rejected;
  - missing/corrupt RunRecord fails closed, run stays `AWAITING_VERIFICATION`,
    queue still active;
  - RunRecord whose approved task (taskId/sourceDocument) differs from the Goal is rejected;
  - `finalize` before `AWAITING_VERIFICATION` is rejected.
- **`FileSystemAgentRunFinalizerIntegrationTest`**: real `FileSystemSchedulerQueueStore`,
  `FileSystemAgentRuntimeStateStore`, and `FileSystemRunRecordStore`; a full
  claim -> lease -> completeExecution -> finalize flow reaches runtime terminal and
  queue disposition, survives a fresh `recoverFinalization`, and releases the next
  dependent WorkItem for a verified outcome (and blocks it for a failed one).

## Out of scope

- Scheduler worker / Tool execution and the production of the RunRecord
  (connection #3).
- `completeExecution` (`EXECUTING -> AWAITING_VERIFICATION`) — already exists and is
  driven by the (future) worker.
- Retry through additional AgentRuns, automatic failure propagation to dependents,
  external-effect ledger, controls, and multi-agent handoff.
- Any new durable store, schema change, or capability-maturity promotion beyond
  Contract Verified.

## Risks and boundaries

- The pre-terminal recovery window still relies on the caller/driver re-supplying the
  same `runRecordReference`; retaining it durably across a crash in that window is
  the connection-#3 worker/driver's responsibility. After the runtime terminal
  transition, recovery is fully autonomous.
- The finalizer coordinates two durable stores without a transaction; correctness
  rests on the idempotent, ordered, guarded steps, not on atomicity.
