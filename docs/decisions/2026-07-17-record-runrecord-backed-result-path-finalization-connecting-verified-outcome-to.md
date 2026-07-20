# 2026-07-17: Record RunRecord-Backed Result-Path Finalization Connecting Verified Outcome To Runtime Terminal State And Queue Disposition

Status: Accepted Decision

Context:

- Gate 8 had three separate durable facts with nothing connecting them: fence-checked execution acknowledgement (`EXECUTING -> AWAITING_VERIFICATION`), a durable Goal/AgentRun terminal transition (`DurableAgentRuntime.recordResult`), and a durable queue terminal disposition (`completeActiveVerified`/`failActive`).
- After a worker acknowledges execution, no component took the independently verified outcome, moved the AgentRun/Goal to its terminal state, and recorded the matching queue disposition; no recovery finished that sequence after an interruption.
- `FileSystemRunRecordStore.persist` assigns a random `run-record/<uuid>` reference, so persist is not idempotent; `ApprovedTask` carries a taskId and sourceDocument but no source SHA; the durable queue's recovery contract requeues an interrupted active WorkItem to pending, so a recovered queue exposes no active slot until the item is claimed again.

Decision:

- Add one durable, idempotent coordinator, `DurableAgentRunFinalizer` under `com.enhancer.runtime`, over the existing durable queue, `AgentRuntimeStateStore`, and `RunRecordStore`; it performs no cross-store transaction and adds no new store or schema.
- Resolve the RunRecord by reference as an input; never persist it in the finalizer (persist is non-idempotent, and the verified outcome originates outside this increment).
- Drive one recoverable order: resolve RunRecord -> runtime terminal (`recordResult`) -> queue disposition, each step guarded by observed store state.
- Derive the queue disposition from the runtime terminal status, never re-derived from the RunRecord (`COMPLETED -> completeActiveVerified`, `FAILED -> failActive`), so the two stores cannot diverge; read `verificationStatus` from the resolved RunRecord and carry it in the `ResultPayload`.
- Bind the RunRecord to the Goal on `approvedTask.taskId()` and `approvedTask.sourceDocument()` (no SHA available) before finalizing; reject a mismatch. On re-`finalizeAgentRun` after the runtime is already terminal, require the stored result's `runRecordReference` to equal the supplied reference; a mismatch is an explicit error, never a silent overwrite.
- Provide two entry points: `finalizeAgentRun(goalId, agentRunId, runRecordReference)` drives the forward path; `recoverFinalization(goalId)` is pure post-terminal recovery that applies only the queue disposition from an already-terminal runtime and needs no reference.
- Honour the queue's recovery contract when applying the disposition: if the terminal work is already in the completed/failed set, treat it as recorded (idempotent); otherwise re-claim the requeued active item before recording the terminal disposition.
- Fail closed on a missing or corrupt RunRecord: record no disposition and leave the run `AWAITING_VERIFICATION` (recoverable). Reject `finalizeAgentRun` on a run that has not acknowledged execution.
- Leave the Scheduler worker/Tool execution and RunRecord production (connection 3), retry through additional AgentRuns, and automatic failure propagation to dependents out of this contract.

Rationale:

Deriving the queue disposition from the runtime terminal status makes divergence between the two durable stores unrepresentable, and resolving (not persisting) the RunRecord keeps the coordinator idempotent under a non-idempotent store. Re-claiming the requeued active item before disposition is the same claim-then-dispose pattern the durable queue's own recovery contract already mandates, so recovery composes without a cross-store transaction.

Consequences:

- The verified outcome now drives both the AgentRun/Goal terminal state and the queue disposition in a recoverable, idempotent order; a failed or unverifiable outcome fails the run and blocks dependents rather than satisfying them.
- Post-terminal recovery is autonomous; only the pre-terminal window still relies on the connection-3 worker/driver to re-supply the same `runRecordReference`, and retaining it durably across a crash in that window is that connection's responsibility.
- Correctness rests on the idempotent, ordered, guarded steps, not on atomicity; no released format or capability maturity beyond Contract Verified changes.
