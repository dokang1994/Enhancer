# 2026-08-06: Replay Terminal Result Before Retry Or Queue Disposition Recovery

Status: Accepted Decision

## Context

`DurableAgentRunFinalizer.recordAgentRunResult` already exact-replays a retained terminal
Result and repairs `VERIFICATION_RECORDED`, Tool `TIMEOUT_DETECTED`, and
`STAGNATION_DETECTED` without another runtime revision. The Worker checkpoint retains the
exact AgentRun and RunRecord reference needed for that replay.

`DurableAgentRunWorker.resume`, however, currently branches on Goal state first. A
`RETRY_PENDING` Goal advances directly to retry control, while a `COMPLETED` or `FAILED`
Goal advances directly to queue-disposition recovery and checkpoint clearing. If the
previous call persisted the Result transition and then failed while recording or
publishing a derived event, restart can therefore skip Result replay permanently.

## Decision

- On Worker resume, after exact runtime recovery and before Goal-state retry or terminal-
  disposition branching, inspect the latest AgentRun.
- When that AgentRun is `COMPLETED` or `FAILED`, require the checkpoint's exact RunRecord
  reference and call `recordAgentRunResult` with the checkpointed Goal and AgentRun
  identities.
- Only after that call succeeds may the existing `RETRY_PENDING` branch decide retry or
  the existing terminal Goal branch recover queue disposition and clear the checkpoint.
- Preserve the existing `AWAITING_VERIFICATION`, execution, retry policy, queue mutation,
  finalizer API, and event-free behavior. `CANCELLED` does not enter Result replay.
- Pin the ordering test-first by proving that a checkpoint reference differing from the
  retained terminal Result fails before retry-decision, queue-disposition, execution, or
  checkpoint-clear side effects.

## Rationale

The finalizer already owns Result binding and exact event repair. Re-entering that owner
from the Worker's durable checkpoint is smaller than changing `recoverFinalization`,
adding another checkpoint, or teaching retry and queue branches to reconstruct Result
facts. It preserves the accepted source transition -> event -> publication ordering and
keeps queue disposition separate.

## Consequences

- A Result-derived event failure keeps the checkpoint as the re-entry cursor and blocks
  later retry or queue side effects until exact Result replay succeeds.
- Event-free recovery performs an additional exact retained-reference check but advances
  no runtime or queue revision.
- Existing event-aware finalizer tests remain the evidence that terminal Result replay
  repairs verification, Tool-timeout, and stagnation facts.
- Supplying the recorder to the production finalizer for `scheduler-cycle`,
  `scheduler-drain`, and `scheduler-service` remains a separate next task.
