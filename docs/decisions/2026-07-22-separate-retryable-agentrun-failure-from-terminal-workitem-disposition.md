# 2026-07-22: Separate Retryable AgentRun Failure From Terminal WorkItem Disposition

Status: Accepted Decision

Context:

- The first bounded retry decision consumed `WorkItemDisposition.FAILED`, but that value
  is the Scheduler queue's terminal WorkItem outcome. Once recorded, the WorkItem leaves
  the active slot and cannot be retried as the same queued work.
- The first decision also treated `APPLIED` and `DEDUPLICATED` external effects as
  automatically retry-safe. The current ledger binds each request to one exact AgentRun
  and provides no cross-attempt effect-replay or idempotency contract.
- The proposed multi-attempt lifecycle added `RETRY_PENDING` but left the current
  finalizer and worker unchanged. Those components derive queue failure from a terminal
  failed AgentRun immediately, producing a retry-pending Goal beside a terminally failed
  queue item.
- Durable attempt history also needs storage-level prefix enforcement and a persisted
  typed retry decision; retaining result messages alone cannot explain why retry stopped.

Decision:

- Retry eligibility consumes the exact latest failed `RuntimeAgentRun`, never a terminal
  Scheduler `WorkItemDisposition`. Attempt failure and WorkItem failure are distinct
  lifecycle facts.
- Keep the Scheduler WorkItem active while a Goal is `ACTIVE` or `RETRY_PENDING`.
  RunRecord-backed attempt result recording stops at `RETRY_PENDING` after a failed
  attempt. A terminal queue disposition is written only after the Goal becomes
  `COMPLETED` or terminal `FAILED`.
- Split finalization into result recording and terminal queue disposition. Recovery must
  not derive WorkItem failure from a terminal AgentRun when its Goal is
  `RETRY_PENDING`.
- Admit automatic retry only when the exact Goal ledger is empty or every effect is
  `COMPENSATED`. `PREPARED`, `REQUIRES_USER_RECOVERY`, `APPLIED`, and `DEDUPLICATED`
  each refuse automatic retry with a typed reason until a separate cross-attempt effect
  contract exists.
- Persist Agent runtime state as schema v2 with one ordered immutable AgentRun list, a
  latest-attempt projection, Goal-wide monotonic fences, and one immutable typed retry
  decision per failed attempt. Do not revise the incompatible payload under schema v1.
- Require the filesystem store to enforce exact AgentRun and retry-decision prefixes and
  reject truncation, rewrite, reordering, invalid append, stale revision, and unsupported
  schema independently of runtime helper methods.
- Persist the retry decision before appending a checkpointed replacement AgentRun identity
  or terminally abandoning the Goal. The decision records the attempt, attempt count,
  policy bound, ledger revision/count/digest, and typed admitted/refused result.

Rationale:

The queue disposition is the terminal outcome of the whole WorkItem, while retry is a
choice about one failed attempt inside that work. Separating them preserves the existing
completion semantics and keeps the dependency-satisfaction partition truthful. Limiting
automatic retry to empty or fully compensated effect history is the only current rule
that does not assume an unimplemented cross-attempt adapter behavior. Schema v2 and
prefix validation make immutable terminal evidence enforceable at the storage boundary,
and durable typed decisions make recovery explainable without chat history.

Consequences:

- This decision supersedes the retry-input, automatically-safe effect-status, and
  unchanged-finalizer portions of the earlier decision
  `2026-07-22: Decide Bounded AgentRun Retry On Attempt Budget And External Effect Resolution`.
  That decision remains an immutable record of the first Contract Verified slice.
- The current production classes still implement the earlier pure contract. This
  documentation increment changes no code and grants no maturity promotion.
- Implementation must first correct the pure decider, then add schema-v2 history and
  decision records, split finalization, and finally wire the durable controller and
  worker recovery path with named integration evidence.
- Schema-v1 runtime migration, cross-attempt effect idempotency, automatic compensation,
  user override, delay/backoff, broader budgets, and multi-agent execution remain outside
  this decision.
