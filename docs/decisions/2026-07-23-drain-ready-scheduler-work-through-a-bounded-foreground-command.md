# 2026-07-23: Drain Ready Scheduler Work Through A Bounded Foreground Command

Status: Accepted Decision

Context:

- The durable queue accepts up to 4096 work items and releases dependency-ready work after
  verified completion, but the supported `scheduler-cycle` command intentionally runs only
  one recoverable cycle. Processing another already-ready item requires another explicit
  operator invocation even though the queue and Worker recovery contracts already exist.
- The explicit and generated submission commands must remain separate from execution.
  Submission approval cannot imply permission to start an unbounded service or wait for
  future work.
- A background polling service requires accepted waiting, backoff, shutdown, control,
  resource-budget, and operator-visibility contracts. None is necessary to drain work that
  is already ready in one foreground invocation.
- Schema migration and exact-history compaction are durability lifecycle work, but no
  supported older artifact or demonstrated capacity pressure currently consumes them.

Decision:

- Add a bounded foreground Scheduler drain boundary over the existing recoverable
  `DurableAgentRunWorker.runOneCycle` operation.
- Require an explicit positive maximum cycle count no greater than the durable queue's
  4096-work-item bound. Invoke at most that many cycles sequentially in the current process,
  with the existing process-isolated child execution and per-cycle durable recovery.
- Stop immediately on the first idle cycle, terminal failed disposition, or configured
  cycle limit. Continue only after a verified-completed disposition. Report the stop reason
  and verified/failed cycle counts explicitly; reaching the limit must not claim that the
  queue is empty.
- Expose the boundary through a separate `scheduler-drain` command over one already-existing
  caller-identified queue. Reuse the explicit roots, owner, retry, lease, and child-timeout
  inputs of `scheduler-cycle`, adding only the cycle bound.
- Keep `scheduler-cycle` unchanged. The drain command must not create a queue, submit work,
  merge either submission command with execution, sleep, wait for future work, retry an idle
  result, daemonize, or apply control requests.
- Recovery remains queue- and cycle-checkpoint-driven. A verified or failed disposition is
  already durable before another cycle begins, and an interrupted in-progress cycle resumes
  through the existing checkpoint. Do not add a second drain-progress store merely to retain
  diagnostic counts.
- The implementation consumer is a named real-filesystem CLI integration that prepares
  multiple ready and dependency-linked items, proves sequential verified draining, finite
  limit and idle stops, first-failure stop, and restart from an existing cycle checkpoint.

Alternatives considered:

- Keep one explicit `scheduler-cycle` invocation per item: preserves the smallest command,
  but leaves existing multi-item and dependency-release queue behavior unavailable through
  one bounded supported operation.
- Add a background polling or Scheduler service: deferred because waiting, backoff, shutdown,
  authenticated control, resource budgets, and long-running operator visibility must be
  designed together rather than inferred from a loop.
- Implement schema-v1-to-v2 migration first: deferred because current stores explicitly fail
  closed on unsupported artifacts and there is no released or supported schema-v1 upgrade
  path to consume a migration contract.
- Compact exact queue history first: deferred because exact admission replay and recovery
  currently depend on retained history, the queue is already bounded, and no measured
  storage pressure justifies tombstone, retention, or cleanup semantics.
- Drain only already-ready work in a bounded foreground invocation: selected because it
  directly consumes the durable queue, process-isolated Worker, and cycle recovery that
  already exist while preserving explicit execution authority and a finite stop.

Rationale:

A finite foreground drain makes the existing multi-item Scheduler useful without
introducing a service lifecycle. Stopping on idle instead of waiting preserves the
submission/execution boundary, and stopping on failure preserves operator visibility.
Reusing the queue disposition and cycle checkpoint avoids duplicating durable facts.

Consequences:

- The implementation may add one application result contract and one CLI command, but it
  must not change queue, runtime, effect-ledger, submission-manifest, or cycle-checkpoint
  schemas.
- The command can process work that is ready now; it is not a watcher, polling loop,
  background service, or automatic execution grant.
- A cycle limit is an explicit partial-progress boundary. Operators may invoke the command
  again against the same durable roots without replaying terminal work.
- Whole-Gate 8 maturity does not change from this decision or its isolated command alone.
- State migration, history compaction, authenticated controls, production external
  adapters, multi-process coordination, and background operation remain separate work.
