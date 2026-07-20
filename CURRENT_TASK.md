# Current Task

## Status

Completed

## Task

Implement Gate 8 connection sub-increment 3d: add `ProcessIsolatedAgentRunExecution`, which spools one dispatched WorkItem to a per-cycle work spool, runs it in a child process through `IsolatedWorkerMain`, and returns the persisted RunRecord reference after validating the child's published result against the resolved record.

## Task ID

gate-8-process-isolated-execution

## Justified By

- 2026-07-20: Return Isolated Worker Results Through A Correlated Per-Cycle Spool With RunRecord As Authority

## Context

Connection 3's adapter (3c) and process lifecycle (3b) both existed but neither was wired, because `AgentRunExecution` must return a `run-record/<uuid>` string and the child's only channel back was an exit code. The accepted decision fixed the return path as a reverse spool carrying an existing `ResultPayload`, keyed by a per-cycle invocation root, with the RunRecord as the authority the child's claim is checked against.

## Acceptance Criteria

- `ProcessIsolatedAgentRunExecution` implements `AgentRunExecution` and returns a reference resolvable in the shared `RunRecordStore`.
- Work and result travel through separate spools under an invocation root private to the Goal and AgentRun; exactly one valid message is expected in each direction.
- The child runs the same Gate 1-4 pipeline as the in-process path through a shared `AgentLoopAgentRunExecution.executeWork` seam rather than a second implementation.
- Store roots reach the child as parent-supplied launcher arguments, never as payload data.
- The result is validated before a reference is returned: correlation, logical-run, causation, and task identities match the dispatched work; the payload is exactly a `ResultPayload`; the reference resolves; and the claimed verification status equals the resolved record's own. Any mismatch fails closed.
- A non-completed launcher outcome or a non-zero exit fails closed.
- Re-entry returns an already-published valid result without launching a second child.
- The launcher is reached through a `WorkerProcessLauncher` port so the parent's failure paths are provable without spawning a process.
- Full regression passes with 0 failures and 0 errors, and strict lint passes across all production sources.

## Out Of Scope

- Wiring the execution into `DurableAgentRunWorker`.
- Spool retention and cleanup; nothing removes an invocation root today.
- Retry policy, cancellation of a running child, and concurrent cycles sharing one invocation root.
- Closing the window where a child persists a RunRecord and dies before publishing; that orphan and its re-execution stay documented at-least-once behaviour.

## Approval

Approved by the user's 2026-07-20 direction to proceed with the reverse result spool after review added the execution-request, result-authority, and spool-consumption contracts to the decision.

## Verification

Recorded in `docs/verification-log.md` under Process Isolated Execution Verification.

## Next

Wire `ProcessIsolatedAgentRunExecution` into `DurableAgentRunWorker` and decide spool retention, since an invocation root currently persists for every cycle with nothing to remove it.
