# 2026-07-23: Correlate Scheduler Recovery Prefixes Through A Read-Only Checkpoint-Anchored Projection

Status: Accepted Decision

## Context

The queue-local `scheduler-status` command can show persisted readiness and disposition,
but it cannot explain which durable worker prefix exists across the queue, AgentRuntime,
cycle checkpoint, and RunRecord stores. Calling Scheduler recovery to answer that
question would mutate an active queue slot and advance its revision.

The stores do not share a transaction. The single `PendingFinalization` record is the
only durable artifact that names the Goal, current AgentRun, optional RunRecord
reference, and optional replacement AgentRun together. The runtime store supports
point resolution by Goal identity, and the RunRecord store supports point resolution by
the checkpointed reference. Neither store provides a recovery-safe reason to scan for
an unrelated record.

## Decision

Add a runtime-owned read-only recovery-status projection and a separate
`scheduler-recovery-status` CLI command.

The command will require explicit queue, runtime, cycle-checkpoint, and RunRecord roots
plus one canonical queue identity. It will resolve the queue snapshot directly, use the
optional cycle checkpoint as the sole cross-store anchor, resolve only that Goal and
only its checkpointed RunRecord, and never invoke queue or runtime recovery.

The projection will expose one typed durable phase:

- no pending cycle;
- intent recorded;
- runtime recorded;
- RunRecord recorded;
- result recording pending;
- retry resolution pending;
- replacement recorded;
- queue disposition pending; or
- checkpoint clear pending.

It will validate exact checkpoint, runtime, WorkItem, queue admission, terminal result,
and RunRecord bindings. Missing runtime is valid only for an intent-only prefix.
Missing or corrupt referenced state and impossible cross-store combinations fail
closed.

Because the stores are independent, the read boundary will take a bounded second sample
of the queue revision, checkpoint value, and referenced runtime revision. Observed drift
will fail explicitly instead of publishing a mixed projection. This proves a stable
sequential observation, not an atomic multi-store snapshot.

The output will state that worker liveness is unknown. It will not interpret lease
expiry, inspect processes, apply recovery, or recommend that a write is safe.

## Rationale

Checkpoint anchoring follows the worker's existing recoverable ordering and avoids
inventing a store-wide join policy. Runtime ownership keeps phase and consistency policy
out of the CLI. A bounded stability check closes the most important usability defect of
sequential cross-store inspection without introducing a lock, retry loop, or mutation.

## Consequences

- Operators can distinguish durable recovery prefixes before choosing whether to invoke
  `scheduler-cycle` or `scheduler-drain`.
- An absent checkpoint does not authorize runtime scanning and does not turn a persisted
  active queue slot into a worker-liveness claim.
- Concurrent transitions may make one inspection fail; the caller may invoke the
  read-only command again.
- The command adds no cross-store transaction, repair, automatic recovery, process
  observation, control application, retention, cleanup, migration, or service loop.
