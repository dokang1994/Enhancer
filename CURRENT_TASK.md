# Current Task

## Status

Completed

## Task

Add one durable, idempotent finalizer that connects a resolved RunRecord to the terminal AgentRun/Goal state and the matching Scheduler queue disposition in a recoverable order, so the verified outcome drives both stores without divergence and recovery idempotently finishes the missing suffix after an interruption.

## Task ID

gate-8-result-path-finalization

## Justified By

- 2026-07-17: Record RunRecord-Backed Result-Path Finalization Connecting Verified Outcome To Runtime Terminal State And Queue Disposition

## Context

Gate 8 held three separate durable facts with nothing connecting them: fence-checked execution acknowledgement (`EXECUTING -> AWAITING_VERIFICATION`), a durable Goal/AgentRun terminal transition (`DurableAgentRuntime.recordResult`), and a durable queue terminal disposition (`completeActiveVerified`/`failActive`). After a worker acknowledged execution, no component took the independently verified outcome, moved the AgentRun/Goal to its terminal state, and recorded the matching queue disposition, and no recovery finished that sequence after a crash.

## Acceptance Criteria

- Add `DurableAgentRunFinalizer` under `com.enhancer.runtime`, composing the durable queue, `AgentRuntimeStateStore`, and `RunRecordStore` with no new store and no schema change.
- Drive the recoverable order resolve RunRecord -> runtime terminal (`recordResult`) -> queue disposition, each step guarded by observed store state.
- Derive the queue disposition from the runtime terminal status (`COMPLETED -> completeActiveVerified`, `FAILED -> failActive`), never re-derived from the RunRecord, so the runtime and queue cannot diverge.
- Resolve the RunRecord by reference as an input; never persist it in the finalizer. Bind it to the Goal on `approvedTask.taskId()` and `sourceDocument()` and reject a mismatch.
- Provide `finalizeAgentRun(goalId, agentRunId, runRecordReference)` for the forward path and `recoverFinalization(goalId)` for autonomous post-terminal recovery that applies only the queue disposition with no reference.
- Fail closed on a missing/corrupt RunRecord (run stays `AWAITING_VERIFICATION`, recoverable); reject re-finalize with a different reference and finalize before execution acknowledgement.
- A verified outcome completes the runtime and releases dependents; a failed outcome fails the runtime and leaves dependents blocked.

## Out Of Scope

- The Scheduler worker / Tool execution and the production of the RunRecord (connection 3).
- `completeExecution` (`EXECUTING -> AWAITING_VERIFICATION`), which already exists and is driven by the future worker.
- Retry through additional AgentRuns, automatic failure propagation to dependents, external-effect ledger, controls, and multi-agent handoff.
- Any new durable store, schema change, or capability-maturity promotion beyond Contract Verified.
- Commit, push, PR, merge, release, or deployment without a new explicit user request.

## Approval

Approved by the user's 2026-07-17 request to continue the project on the roadmap's next increment (RunRecord-backed result finalization), confirmed against ROADMAP.md connection backlog item 2.

## Verification

- Task 1 RED: `DurableAgentRunFinalizerTest` failed to compile with the missing `DurableAgentRunFinalizer` and nothing else; GREEN passed the verified and failed forward-path scenarios after implementing the finalizer.
- During GREEN the forward path exposed the durable queue's recovery-requeue contract: a freshly recovered queue holds no active work, so the finalizer re-claims the requeued WorkItem before recording the disposition; this is the same claim-then-dispose pattern the queue's own recovery already mandates.
- Task 2 RED: the class failed to compile with the missing `recoverFinalization`; GREEN passed autonomous post-terminal recovery (disposition applied with no reference) and idempotent re-finalize.
- Task 3: the fail-closed, task/document-binding, different-reference, and finalize-before-acknowledgement guards implemented in Task 1 were pinned and passed on first run.
- Focused suite: 8 of 8 `DurableAgentRunFinalizerTest` cases passed with no skips, failures, or errors.
- Full regression passed 60 suites and 269 tests: 267 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors under `--warning-mode all`; Java 17 strict lint passed across 151 production sources.
- Structural: exactly one `Status: Specified - Next` gate marker (Gate 8) in `ROADMAP.md`; `git diff --check` reported no whitespace errors.

## Next

Integrate the process-isolated Scheduler worker and a selected local IPC adapter (connection 3): the worker executes the Tool, produces the RunRecord, and drives `finalizeAgentRun`, while durably retaining the `runRecordReference` across the pre-terminal recovery window. Transport acceptance never means bus delivery or work completion.
