# Gate 8 In-Process Scheduler Worker — Design

- Date: 2026-07-17
- Task ID: `gate-8-in-process-scheduler-worker`
- Gate: Delivery Gate 8 (Agent Runtime and Scheduler), sole `Specified - Next`
- Connection: ROADMAP backlog #3 (process-isolated worker and local IPC), sub-increment **3a** (in-process worker only)
- Status: Design (pending implementation plan)

## Problem

Gate 8 now has all the durable pieces of one work cycle as separate, tested
contracts — a durable queue claim plus fenced lease (`DurableAgentRunDispatcher`),
a durable Goal/AgentRun lifecycle with fence-checked execution acknowledgement
(`DurableAgentRuntime.completeExecution`), and a durable result-path finalizer
(`DurableAgentRunFinalizer`) — but nothing drives them end to end. The finalizer
has no production consumer, `completeExecution` has no real driver, and the
finalizer explicitly deferred the pre-terminal recovery window (retaining the
`runRecordReference` across a crash before finalization) to "the connection-3
worker/driver," which does not exist.

## Goal

Add one in-process worker that runs a single scheduling cycle — claim + lease,
execute the approved work to produce a durable RunRecord, acknowledge execution,
then finalize the runtime and queue disposition — and that recovers a cycle
interrupted at any seam, including the pre-terminal window the finalizer deferred.
This gives the finalizer and `completeExecution` their first real consumer and
closes the queue → runtime → result loop on the durable stores, without process
isolation or a concrete IPC adapter.

## Scope boundary within connection 3

Connection 3 in the ROADMAP bundles a process-isolated worker **and** a selected
local IPC adapter across Gate 7 transport, Gate 8 worker runtime, and Gate 11 Tool
controls. That is more than one bounded increment. This design is sub-increment
**3a only**: an in-process worker. Process isolation (3b) and a concrete
`MessageTransport` adapter (3c) are separate later increments and are out of scope.

## Confirmed decisions

- **In-process worker first (3a).** Smallest safe step; gives the finalizer its
  first consumer and proves the end-to-end loop on the durable stores before any
  process or IPC complexity.
- **Injected execution port, not the real Agent Loop.** The dispatched
  `WorkPayload` carries only `(taskRevision, snapshotId, allowedTools)` — no target
  path, expected content SHA, or concrete `ToolRequest`. Wiring the real
  `AgentLoop`/read-file pipeline would first require extending the work payload and
  its upstream publisher, crossing Gate 6/7 boundaries. The worker's novel contract
  is the orchestration and recovery, not the specific Tool, so execution is an
  injected port. A real `AgentLoop`-backed port is a named follow-on integration.
- **One claim → finalize cycle per call.** `runOneCycle` returns the disposition
  of the cycle it completed, or empty when the queue had no claimable work. Draining
  is a thin loop over this and is out of scope.
- **Worker-owned durable cycle-intent checkpoint** closes the pre-terminal window.
  The finalizer deferred retaining the `runRecordReference` across a crash before
  finalization; the worker owns that with a small durable store.
- **Caller-owned stable identity (unchanged dispatcher).** By the 2026-07-16 fenced-
  lease decision, Goal/AgentRun identities are caller-supplied and stable, and a
  repeated call with the same WorkItem/Goal/AgentRun/owner is idempotent recovery
  (`recoverMatching` validates exact WorkItem equality to prevent authority
  rebinding). The worker is that caller: it persists its generated identities in the
  cycle-intent checkpoint **before** claiming, so re-entry supplies the same
  identities and the dispatcher's existing idempotent recovery resumes the same
  Goal with no orphaned runtime state. **No dispatcher change is required.**

## Existing pieces (grounded)

- `DurableAgentRunDispatcher.claimAndLease(goalId, agentRunId, ownerId, leaseDuration)`
  returns `Optional<AgentRunDispatch>`. It uses `queue.activeWork()` if present else
  `claimNext()`, `recoverOrCreate`s the exact-WorkItem Goal, and advances to a lease.
  `advanceToLease` throws `IllegalStateException("AgentRun has advanced beyond lease
  acquisition")` when the run is already `AWAITING_VERIFICATION`, `COMPLETED`, or
  `FAILED`; a same-owner unexpired `EXECUTING` lease is returned unchanged.
- `AgentRunDispatch(queueId, workItem, goalId, agentRunId, lease)`; `lease().fenceToken()`,
  `lease().ownerId()`.
- `DurableAgentRuntime.recover(goalId, store, clock)`, `.agentRun()` (Optional),
  `.goal().workItem()`, `.completeExecution(agentRunId, ownerId, fenceToken)`
  (`EXECUTING -> AWAITING_VERIFICATION`). `recoverMatching(goalId, workItem, …)`
  rejects a different WorkItem for an existing Goal.
- `DurableAgentRunFinalizer.finalizeAgentRun(goalId, agentRunId, runRecordReference)`
  → `WorkItemDisposition`; `recoverFinalization(goalId)` →
  `Optional<WorkItemDisposition>` (autonomous, post-terminal only).
- `RunRecordStore.persist(record)` → `StoredRunRecord` with `.reference()`
  (`run-record/<uuid>`, non-idempotent); `resolve(reference)` → `ResolvedRunRecord`.
- `RuntimeAgentRunStatus`: `PLANNING`, `READY`, `EXECUTING`, `AWAITING_VERIFICATION`,
  `COMPLETED`, `FAILED`, with `isTerminal()`. `WorkItemDisposition`:
  `VERIFIED_COMPLETED`, `FAILED`.

## Architecture

Four new types under `com.enhancer.runtime`; no change to the dispatcher, runtime,
queue, finalizer, or any schema.

### `AgentRunExecution` (execution port, interface)

```
String execute(AgentRunDispatch dispatch) throws IOException;
```

Given the dispatched WorkItem and lease, do the approved work and persist a
RunRecord, returning its `run-record/<uuid>` reference. The finalizer resolves that
reference to read the `verificationStatus`, so the port returns only the reference.
The production `AgentLoop`-backed implementation is deferred; this increment tests
against a deterministic in-test port.

### `PendingFinalization` + `PendingFinalizationStore` + `FileSystemPendingFinalizationStore`

The worker's durable cycle-intent checkpoint. Single-worker means at most one
pending cycle globally, so the store holds at most one record.

```
record PendingFinalization(String goalId, String agentRunId,
                           Optional<String> runRecordReference) { }

interface PendingFinalizationStore {
    void record(PendingFinalization pending) throws IOException;   // write/overwrite the single intent
    Optional<PendingFinalization> findPending() throws IOException; // the single intent, if any
    void clear() throws IOException;                                // remove it (idempotent)
}
```

`FileSystemPendingFinalizationStore` persists the single intent to one bounded,
strict-UTF-8, integrity-checked, atomically published file and fails closed on
corrupt or oversized state, matching the existing filesystem stores. `findPending`
returns the ids so a restarted worker needs no in-memory context.

### `DurableAgentRunWorker`

Constructed with a `DurableAgentRunDispatcher`, an `AgentRunExecution`, a
`PendingFinalizationStore`, a `DurableAgentRunFinalizer`, an
`AgentRuntimeStateStore`, a stable `ownerId`, and a `Clock`. The four durable
stores (queue, runtime, RunRecord, checkpoint) stay separate boundaries; the worker
performs no cross-store transaction.

```
Optional<WorkItemDisposition> runOneCycle(Duration leaseDuration):
    pending = checkpoint.findPending()
    if pending.isPresent():
        return resume(pending, leaseDuration)          // finish the interrupted cycle
    goalId, agentRunId = freshCanonicalUuid(), freshCanonicalUuid()   // distinct
    checkpoint.record(goalId, agentRunId, ref=empty)   // intent BEFORE claim
    return driveFreshOrResumedExecution(goalId, agentRunId, ref=empty, leaseDuration)

resume(pending, leaseDuration):
    try:
        runtime = DurableAgentRuntime.recover(pending.goalId, runtimeStore, clock)
    catch MissingAgentRuntimeStateException:
        // intent was recorded but the cycle stopped before the dispatcher created
        // the runtime (or the queue was empty): re-drive with the SAME ids
        return driveFreshOrResumedExecution(pending.goalId, pending.agentRunId,
                                            pending.runRecordReference, leaseDuration)
    run = runtime.agentRun()                           // Optional — may be empty
    if run.isPresent() and run is terminal:
        d = finalizer.recoverFinalization(pending.goalId).orElseThrow()
        checkpoint.clear(); return Optional.of(d)
    if run.isPresent() and run is AWAITING_VERIFICATION:   // ref is always present here
        d = finalizer.finalizeAgentRun(pending.goalId, pending.agentRunId,
                                       pending.runRecordReference.orElseThrow())
        checkpoint.clear(); return Optional.of(d)
    // EXECUTING / READY / PLANNING / no AgentRun yet: re-drive with the SAME ids
    return driveFreshOrResumedExecution(
        pending.goalId, pending.agentRunId, pending.runRecordReference, leaseDuration)

driveFreshOrResumedExecution(goalId, agentRunId, maybeRef, leaseDuration):
    dispatch = dispatcher.claimAndLease(goalId, agentRunId, ownerId, leaseDuration)
    if dispatch.isEmpty():                              // no claimable/active work
        checkpoint.clear(); return Optional.empty()
    ref = maybeRef.orElseGet(() -> {                   // execute only if not already done
        String r = execution.execute(dispatch);
        checkpoint.record(goalId, agentRunId, ref=r);  // persist ref BEFORE completeExecution
        return r;
    })
    runtime = DurableAgentRuntime.recover(goalId, runtimeStore, clock)
    runtime.completeExecution(agentRunId, ownerId, dispatch.lease().fenceToken())
    d = finalizer.finalizeAgentRun(goalId, agentRunId, ref)
    checkpoint.clear(); return Optional.of(d)
```

`freshCanonicalUuid` produces two distinct canonical UUIDs (Goal ≠ AgentRun, per the
dispatcher's identity rule).

### Ordering and recovery

Authoritative order per cycle: **cycle-intent (ids) → queue claim + lease →
RunRecord persisted (ref) → intent updated with ref → runtime terminal
(`completeExecution` then `finalizeAgentRun`) → queue disposition → clear intent.**
Every underlying write persists before exposure (the dispatcher, runtime, finalizer,
and stores already guarantee this). Recovery finishes the interrupted suffix from
`findPending`, using the runtime state as the source of truth:

| Runtime state at recovery | `ref` in checkpoint | Action |
|---|---|---|
| terminal (`COMPLETED`/`FAILED`) | any | `recoverFinalization` (autonomous queue disposition); clear |
| `AWAITING_VERIFICATION` | present | `finalizeAgentRun(ref)`; clear |
| `EXECUTING`/`READY`/`PLANNING`/none | present | resume lease via `claimAndLease` (same ids); `completeExecution`; `finalizeAgentRun(ref)` — **skip re-execution**; clear |
| `EXECUTING`/`READY`/`PLANNING`/none | absent | resume via `claimAndLease` (same ids); execute; set `ref`; `completeExecution`; `finalizeAgentRun`; clear |

"None" covers two distinct durable states, and `resume` must route both to the
re-drive rows without dying:

- **No runtime state at all** — the cycle stopped after `checkpoint.record` but
  before the dispatcher's `recoverOrCreate` persisted the Goal (or the queue was
  empty and the crash hit before `checkpoint.clear`). `DurableAgentRuntime.recover`
  resolves through the store and **throws the checked
  `MissingAgentRuntimeStateException`** here; `resume` catches exactly that and
  falls through to `driveFreshOrResumedExecution` with the same ids — the same
  pattern the dispatcher's own `recoverOrCreate` uses.
- **Runtime exists but no AgentRun yet** — `create` persisted the initial state
  and the crash hit before `beginAgentRun`; `recover` succeeds but `agentRun()`
  is empty, which the empty-`Optional` guard routes to the re-drive rows.

An intent left behind by an empty-queue crash converges cleanly: the re-drive's
`claimAndLease` finds no claimable work, the intent is cleared, and the cycle
returns empty with no durable residue in any store.

Because the intent stores the worker's stable ids, `claimAndLease` on recovery
supplies the same Goal/AgentRun and the dispatcher's idempotent `recoverMatching`
resumes the exact prefix — no second Goal, no orphaned runtime state. Writing `ref`
before `completeExecution` guarantees the `AWAITING_VERIFICATION` window always has a
recoverable reference (that window is precisely what the finalizer deferred).

No cross-store transaction is claimed; the cycle is an idempotent, ordered,
recoverable prefix.

## Error handling

- **No claimable work:** `claimAndLease` empty → clear the intent, return empty. A
  cycle that claimed nothing leaves no durable trace.
- **Execution port throws:** the intent remains (`ref` absent), `completeExecution`
  is never called, the runtime stays `EXECUTING`; the next cycle resumes with the
  same ids and re-executes. Because the worker's `ownerId` is stable, the
  dispatcher's `EXECUTING` branch hands the unexpired lease straight back to the
  same owner — no expiry wait; only a different owner would have to wait for the
  lease to expire. Fail closed; no disposition.
- **Finalizer fail-closed** (missing/corrupt RunRecord): propagate; the intent
  remains so recovery retries; the run stays `AWAITING_VERIFICATION`.
- **`completeExecution` fence/owner mismatch:** propagate; the durable wrappers
  retain the prior revision.
- **Stable `ownerId`** across the worker instance lets a resumed `EXECUTING` lease
  return with its fence so `completeExecution` proceeds.

## Testing strategy (test-first, RED classified before GREEN)

- **`DurableAgentRunWorkerTest`** (real filesystem stores under `@TempDir`, an
  injected deterministic `AgentRunExecution`, an injected fixed `Clock`):
  - verified execution → runtime `COMPLETED` + queue `completeActiveVerified`,
    dependent released, `runOneCycle` returns `VERIFIED_COMPLETED`, intent cleared;
  - failed (non-verified) execution → runtime `FAILED` + queue `failActive`,
    dependent blocked;
  - empty queue → `runOneCycle` returns empty and leaves no intent;
  - crash after `completeExecution`, before finalize (fresh worker over the same
    stores, runtime `AWAITING_VERIFICATION`, intent with `ref`): `runOneCycle`
    resumes and finalizes, dependent released;
  - crash after the runtime terminal transition, before queue disposition: fresh
    worker → `recoverFinalization` records the disposition;
  - crash after intent, before/at `EXECUTING` with `ref` absent: fresh worker
    re-drives with the same ids, and exactly one Goal/AgentRun exists in the runtime
    store (no orphan) — pins the caller-owned stable-identity resume;
  - crash after intent, **before the runtime state is created** (intent present,
    runtime store empty): fresh worker re-drives with the same ids without
    surfacing `MissingAgentRuntimeStateException`, exactly one Goal/AgentRun
    exists afterwards, and the cycle completes — pins the missing-runtime resume
    branch;
  - crash after intent **with an empty queue** (intent present, no work, no
    runtime): fresh worker's `runOneCycle` returns empty, clears the intent, and
    leaves no durable residue in any store;
  - execution port throws → intent remains, runtime not terminal, queue still
    claimable on recovery, no disposition recorded.
- **`FileSystemAgentRunWorkerIntegrationTest`**: real `FileSystemSchedulerQueueStore`,
  `FileSystemAgentRuntimeStateStore`, `FileSystemRunRecordStore`, and
  `FileSystemPendingFinalizationStore`; a full claim → execute → finalize cycle
  reaches runtime terminal and queue disposition, survives a fresh worker resume, and
  releases the next dependent WorkItem for a verified outcome (blocks it for a failed
  one).

## Out of scope

- Process isolation of the worker (sub-increment 3b): separate OS process, process
  lifecycle, and watchdog.
- A concrete local IPC adapter implementing Gate 7 `MessageTransport` (3c).
- The real `AgentLoop`-backed execution port and the `WorkPayload` execution-input
  extension it needs (target path, expected SHA, initial `ToolRequest`).
- Retry through additional AgentRuns, cancel/pause/resume, budgets, priority/fairness,
  multi-agent execution, and any dispatcher/runtime/finalizer/queue contract change.
- Any new schema migration or capability-maturity promotion beyond Contract Verified.

## Risks and boundaries

- Re-execution on retry orphans the earlier RunRecord (persist assigns a random
  UUID); this is an accepted at-least-once consequence with no cleanup contract.
- The worker adds a fourth durable store it owns; correctness still rests on the
  ordered, guarded, idempotent steps and the caller-owned stable identity, not on
  atomicity across stores.
- The execution port is deterministic and injected in this increment; production Tool
  execution and its work-payload inputs are a named follow-on before any Integrated
  or Operational claim.
