# 2026-07-27: Bound Initial Scheduler Priority Fairness To A Pure Admission-Ordered Burst Selector

Status: Accepted Decision

## Context

Gate 8's current single-worker queue selects the first dependency-ready admission in
FIFO order. Priority belongs to Scheduler selection, not to the Gate 7 work message or
Tool authority. Connecting priority directly to durable `QueuedWork` would also require
a queue schema revision, migration, and persisted fairness progress.

## Decision

Introduce a pure, store-free selector over caller-supplied admission-ordered ready
candidates. Initial priority has exactly two values, `NORMAL` and `EXPEDITED`.

Expedited work may precede normal work only below a bounded consecutive-expedited
burst. When that burst is exhausted and normal work is ready, the selector chooses the
oldest ready normal candidate and resets progress. If only expedited work is ready, it
remains selectable and progress stays capped at the configured burst. Admission order
breaks ties within each class.

The selector validates the complete bounded candidate set before choosing. It reads no
queue or clock, persists nothing, grants no authority, and does not modify
`SingleWorkerSchedulerQueue.claimNext` in this increment.

## Rationale

A pure selector fixes deterministic priority and starvation behavior without silently
creating non-durable scheduling state. It keeps the future persistence obligation
visible: restart-equivalent integration must store both each queued priority and the
fairness progress used by the next claim.

## Consequences

- `WorkItem`, `QueuedWork`, queue schema v2, admission, CLI, and recovery remain
  unchanged.
- `SingleWorkerSchedulerQueue.claimNext` is the next integration consumer.
- Durable integration requires a separate schema and stopped-Scheduler migration
  decision with a lossless schema-v2 default mapping.
- Time-based aging, additional priority classes, and distributed scheduling remain
  outside this decision.
