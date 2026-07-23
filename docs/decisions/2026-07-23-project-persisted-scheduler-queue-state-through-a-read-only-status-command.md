# 2026-07-23: Project Persisted Scheduler Queue State Through A Read-Only Status Command

Status: Accepted Decision

Context:

- The supported Scheduler commands can submit, execute one cycle, or drain ready work,
  but operators cannot inspect which admitted work is dependency-ready, blocked, active,
  verified, or failed without running an execution command.
- `DurableSingleWorkerSchedulerQueue.recover` is not an inspection boundary. It
  intentionally requeues persisted active work and advances the durable revision, so a
  status command that calls it would mutate recovery state while claiming to be read-only.
- `SchedulerQueueStore.resolve` already returns the complete integrity-checked persisted
  queue snapshot without creating or updating the queue.
- Dependency readiness belongs to Scheduler policy. Reimplementing it directly in the CLI
  would allow inspection semantics to drift from `SingleWorkerSchedulerQueue.claimNext`.

Decision:

- Add a pure runtime-owned `SchedulerQueueStatus` projection over one resolved
  `SchedulerQueueState`.
- Preserve exact admission order and classify every admission into exactly one state:
  `VERIFIED` from `completedWorkItemIds`, `FAILED` from `failedWorkItemIds`, `ACTIVE`
  from the persisted active slot, `READY` when pending dependencies are all verified, and
  `BLOCKED` for every other pending item.
- Add a separate `scheduler-status` CLI command requiring explicit `--queue-root`,
  canonical `--queue-id`, and a 1-through-48 `--limit`.
- Read only through `FileSystemSchedulerQueueStore.resolve`. Never call queue recovery,
  claim, disposition, update, or creation from status inspection.
- Report bounded machine-readable `AVAILABLE` or `EMPTY` status, queue revision and
  capacity, total and per-state counts, the requested limit, and an admission-ordered
  prefix of `workItem.<n>=<canonical-id>,<state>` entries.
- Treat a missing queue as a usage/configuration error without creating the queue root.
  Corrupt or unreadable persisted state retains the existing bounded internal-failure
  behavior.
- Keep runtime, effect-ledger, cycle-checkpoint, RunRecord, submission, and invocation
  stores outside this command. A persisted `ACTIVE` classification is an observation, not
  proof that a worker process is currently alive.

Alternatives considered:

- Reuse `DurableSingleWorkerSchedulerQueue.recover`: rejected because inspection would
  requeue active work and persist a new revision.
- Print only aggregate queue counts: rejected because pending work would remain ambiguous
  between dependency-ready and blocked.
- Compute readiness inside `EnhancerCli`: rejected because the CLI would duplicate
  Scheduler policy.
- Correlate queue, runtime, cycle checkpoint, effects, and RunRecords immediately:
  deferred because that is a separate recovery projection with cross-store consistency
  and partial-prefix semantics.

Rationale:

A resolved queue snapshot already contains every fact needed for a truthful queue-local
view. Keeping derivation in the runtime package makes the CLI a formatting boundary,
while resolving rather than recovering guarantees that observation cannot change the
state being observed.

Consequences:

- Operators can distinguish runnable, blocked, in-flight, successful, and failed work
  before deciding whether to invoke a cycle or drain.
- Status remains snapshot-local and read-only. It does not establish worker liveness,
  recover an interrupted cycle, explain a RunRecord, or authorize execution.
- Cross-store Scheduler recovery inspection, richer filtering, pagination, waiting,
  polling, and interactive presentation remain separate work.
