# Current Task

## Status

Completed

## Task

Select `ProcessIsolatedAgentRunExecution` in the production composition of
`DurableAgentRunWorker` and retire each successful per-cycle invocation spool only
after its RunRecord reference is durably checkpointed.

## Task ID

wire-process-isolated-worker-and-retire-checkpointed-spools

## Context

Connection 3b, 3c, and 3d provide the bounded child-process lifecycle, local file
spool transport, and process-isolated `AgentRunExecution`, but the only real durable
worker composition still selects `AgentLoopAgentRunExecution` directly. Every
process-isolated cycle also leaves `work/` and `result/` messages below a private
Goal/AgentRun invocation root with no retirement boundary.

The durable worker already persists the returned RunRecord reference before execution
acknowledgement. That checkpoint is the earliest point at which the spool is no longer
needed for result recovery. Cleanup before it would reopen avoidable re-execution;
cleanup after it can be retried from the retained checkpoint without re-running the
child.

## Justified By

- 2026-07-21: Select The Process-Isolated Durable Worker And Retire Spools After Checkpoint
- 2026-07-17: Record In-Process Scheduler Worker Driving One Recoverable Claim-To-Disposition Cycle Through A Durable Cycle-Intent Checkpoint
- 2026-07-20: Carry The First Transport Hop Through A Local File Spool
- 2026-07-20: Isolate The Worker In A Bounded Self-JVM Child Process
- 2026-07-20: Return Isolated Worker Results Through A Correlated Per-Cycle Spool With RunRecord As Authority

## Acceptance Criteria

- One production composition constructs `DurableAgentRunWorker` with
  `ProcessIsolatedAgentRunExecution`, one shared durable queue instance for dispatch and
  finalization, the real child launcher, and the caller-supplied durable stores.
- A real filesystem integration drives a WorkItem through the durable worker, child JVM,
  work/result spools, Gate 1-4 execution, RunRecord resolution, runtime finalization,
  and the matching queue disposition.
- A successful cycle keeps its invocation spool until the RunRecord reference is
  durably recorded, then removes only that Goal/AgentRun-owned spool tree.
- If cleanup fails after checkpointing, the worker leaves the checkpoint intact and a
  resumed worker retries cleanup without launching or executing the work again.
- Empty, failed, corrupt, or not-yet-checkpointed cycles are not silently treated as
  retired, and the documented orphaned-RunRecord at-least-once window remains explicit.
- Focused tests, the full regression, strict Java lint, and document ownership checks
  pass with fresh output.

## Out Of Scope

- Closing the child-persisted RunRecord / result-publication orphan window.
- Retry through additional AgentRuns, cancellation, pause/resume, concurrent cycles,
  external-effect fencing, or schema migration.
- A supported Gate 8 CLI or promotion of the whole gate to Operational.
- Retention or deletion of Evidence and RunRecord artifacts.

## Approval

Approved by the user's 2026-07-21 request to continue with the next task recorded by
the completed strict-lint increment.

## Verification

Recorded in `docs/verification-log.md` under Process-Isolated Durable Worker Composition
And Spool Retirement Verification.

## Next

Define the first durable cancel/pause/resume control-state increment, preserving the
existing rule that a control message can request a transition but cannot create task or
Tool authority.
