# 2026-07-21: Select The Process-Isolated Durable Worker And Retire Spools After Checkpoint

Status: Accepted Decision

Context:

- Connection 3b, 3c, and 3d provide the child-process lifecycle, local spool transport,
  and `ProcessIsolatedAgentRunExecution`, but no production composition selects them for
  `DurableAgentRunWorker`.
- A per-cycle `work/` and `result/` spool is required while execution is in flight and
  while the returned RunRecord reference exists only in the result message. Deleting it
  inside `execute` would race the worker's durable cycle-intent checkpoint.
- `DurableAgentRunWorker` already persists the RunRecord reference before
  `completeExecution`. Once that write succeeds, restart recovery no longer needs the
  spool to rediscover the reference and can skip child execution.
- Silent best-effort cleanup would make retained artifacts invisible. Failing the whole
  cycle before the reference checkpoint would instead discard its recovery advantage.

Decision:

- Add one production `DurableAgentRunWorker.processIsolated` composition that constructs
  `ProcessIsolatedAgentRunExecution` with the real `IsolatedWorkerLauncher`, shares one
  caller-supplied durable queue instance between dispatcher and finalizer, and uses the
  caller-supplied runtime, checkpoint, and RunRecord stores and artifact roots.
- Extend `AgentRunExecution` with an optional post-checkpoint cleanup operation. The
  durable worker invokes it only after the RunRecord reference is present in
  `PendingFinalization`, including on recovery when the reference was recorded by an
  earlier process. In-process implementations inherit a no-op.
- `ProcessIsolatedAgentRunExecution` implements that operation by deleting only the
  exact Goal/AgentRun-owned invocation tree and removing its Goal directory only when
  empty. It never deletes the invocation root, Evidence, or RunRecord artifacts.
- Cleanup is idempotent. A missing cycle tree is already retired. A symbolic-link cycle
  or Goal boundary is rejected rather than traversed.
- Cleanup failure propagates with the checkpoint intact. A fresh worker retries cleanup
  before execution acknowledgement and does not call `execute` again because the
  durable reference is already present.
- Work and result spools remain available before checkpointing and after failed,
  corrupt, timed-out, or incomplete execution. The existing child-persisted
  RunRecord/result-publication orphan window remains an explicit at-least-once case.

Rationale:

The checkpoint is the first durable artifact that replaces the result spool as the
reference recovery source, so it is the only ordering boundary that permits cleanup
without weakening restart behavior. A default cleanup hook keeps the execution port
small for in-process implementations while allowing the worker, which owns the durable
ordering, to invoke process-specific retirement at the correct time. The production
factory closes the missing selection without adding a Gate 8 CLI or a second worker
lifecycle.

Consequences:

- A real durable worker composition now executes through the child JVM and the two
  local spool directions.
- Successfully checkpointed cycles do not accumulate invocation spools.
- Cleanup failures are visible and recoverable without duplicate execution.
- Failed or abandoned current cycles retain their spool for recovery or diagnosis; no
  time-based retention scheduler is introduced.
- Out of scope remains retry through new AgentRuns, cancellation, concurrent cycles,
  spool history, Evidence/RunRecord cleanup, and closing the orphaned-RunRecord window.
