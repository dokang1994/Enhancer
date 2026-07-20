# 2026-07-17: Record In-Process Scheduler Worker Driving One Recoverable Claim-To-Disposition Cycle Through A Durable Cycle-Intent Checkpoint

Status: Accepted Decision

Context:

- Gate 8 held every durable piece of one work cycle as separate, tested contracts — durable queue claim plus fenced lease (`DurableAgentRunDispatcher`), fence-checked execution acknowledgement (`DurableAgentRuntime.completeExecution`), and RunRecord-backed result finalization (`DurableAgentRunFinalizer`) — but nothing drove them end to end; the finalizer had no production consumer and explicitly deferred the pre-terminal recovery window (retaining the `runRecordReference` across a crash before finalization) to the connection-3 worker/driver.
- Roadmap connection 3 bundles a process-isolated worker and a selected local IPC adapter; that is more than one bounded increment.
- By the 2026-07-16 fenced-lease decision, Goal/AgentRun identities are caller-supplied and stable, and a repeated dispatcher call with the same WorkItem/Goal/AgentRun/owner is idempotent recovery; the dispatched `WorkPayload` carries only `(taskRevision, snapshotId, allowedTools)` — no target path, expected content SHA, or concrete `ToolRequest`.

Decision:

- Split connection 3 into sub-increments: 3a in-process worker (this delivery), 3b process isolation, and 3c a concrete `MessageTransport` local IPC adapter; deliver 3a only.
- Add `DurableAgentRunWorker` under `com.enhancer.runtime`, driving one scheduling cycle per `runOneCycle(leaseDuration)` call in the authoritative order: cycle-intent (ids) -> queue claim + lease -> RunRecord persisted (ref) -> intent updated with ref -> `completeExecution` -> `finalizeAgentRun` -> queue disposition -> clear intent. No dispatcher, runtime, finalizer, queue contract, or schema change.
- Add a worker-owned durable cycle-intent checkpoint (`PendingFinalization`, `PendingFinalizationStore`, `FileSystemPendingFinalizationStore`): a single bounded, strict-UTF-8, digest-checked, atomically published record holding the worker-generated distinct canonical Goal/AgentRun UUIDs plus the optional `runRecordReference`. The intent is written before the queue claim, so re-entry supplies the same identities and the dispatcher's existing idempotent `recoverMatching` resumes the exact prefix with no second Goal and no orphaned runtime state; no dispatcher change is required.
- Persist the `runRecordReference` into the intent before `completeExecution`, closing the pre-terminal `AWAITING_VERIFICATION` window the finalizer deferred: that window always holds a recoverable reference.
- Execute through an injected `AgentRunExecution` port (`execute(dispatch) -> runRecordReference`); the real `AgentLoop`-backed port is a named follow-on because it needs a `WorkPayload` execution-input extension crossing Gate 6/7 boundaries.
- Route recovery by the runtime state as the source of truth: terminal -> `recoverFinalization`; `AWAITING_VERIFICATION` -> `finalizeAgentRun(ref)`; `EXECUTING`/`READY`/`PLANNING`, an unstarted AgentRun, or a missing runtime (`MissingAgentRuntimeStateException` tolerated) -> re-drive with the same identities, skipping re-execution when the reference is already recorded.
- Fail closed everywhere: an execution or finalizer failure propagates with the intent retained (the stable owner re-acquires its unexpired lease on resume); a cycle that claimed nothing clears its intent and leaves no durable trace.
- Require the dispatcher and finalizer handed to the worker to wrap the same queue instance, because the queue's in-memory revision advances with each persisted mutation and a second instance over the same store diverges after the claim.
- Accept that re-execution on retry orphans the earlier RunRecord (`persist` assigns a random UUID) as an at-least-once consequence with no cleanup contract.

Rationale:

The in-process worker is the smallest step that gives the finalizer and `completeExecution` their first real consumer and closes the queue -> runtime -> result loop on the durable stores before any process or IPC complexity. Caller-owned stable identity persisted before the claim turns crash recovery into the dispatcher's existing idempotent re-entry rather than new coordination, and writing the reference before acknowledgement makes every recovery row derivable from durable state alone.

Consequences:

- One `runOneCycle` call now drives claim, execution, acknowledgement, finalization, and disposition end to end over the four separate durable stores (queue, runtime, RunRecord, checkpoint) with no cross-store transaction claimed; a cycle interrupted at any seam converges under a fresh worker.
- Out of scope: 3b process isolation, 3c IPC adapter, the real `AgentLoop`-backed execution port and its `WorkPayload` extension, retry through additional AgentRuns, cancel/pause/resume, budgets, priority/fairness, multi-agent execution, schema migration, and any capability-maturity promotion beyond Contract Verified.
