# 2026-07-28: Bound Scheduler Service Polling With Caller-Owned Lifecycle And Finite Limits

Status: Accepted Decision

## Context

Gate 8 has recoverable one-cycle execution and a bounded foreground drain, but both stop
when no work is immediately ready. The remaining Gate 8 service-operation gap cannot be
closed by silently turning either command into an infinite daemon. A service lifecycle
must remain bounded, stoppable, recoverable through the existing cycle checkpoint, and
separate from Gate 12 authenticated controls.

## Decision

Introduce a caller-driven `BoundedSchedulerService` over the existing
`DurableAgentRunWorker.runOneCycle` boundary.

The service creates no thread and owns no process lifecycle. Its caller supplies a local
stop signal; a later supported entry point owns whether the blocking service runs on the
foreground thread or a supervised background thread.

One immutable policy bounds total cycle invocations, consecutive idle cycles, and the
positive idle-wait interval. The service checks stop before each cycle, invokes cycles
strictly sequentially, resets the consecutive-idle count after verified work, stops on
the first failed disposition, stops at either configured limit, and restores the thread
interrupt flag when idle waiting is interrupted.

Idle waiting is an injected boundary with a production monotonic duration wait and a
deterministic test seam. Waiting grants no execution authority and never changes queue,
runtime, effect, or checkpoint state.

The result reports a typed stop reason and exact invoked/verified/idle/failed counts. This
increment provides the bounded in-process lifecycle contract only; a separate task must
connect it to a supported entry point and prove real restart/orphan recovery.

## Rationale

This is the smallest Gate 8-owned step beyond the foreground drain. It permits controlled
idle polling without an unbounded loop, background thread, unauthenticated control
application, new durable state, or duplicate execution path. Reusing the existing durable
cycle preserves its checkpoint and fencing recovery authority.

## Consequences

- Existing `scheduler-cycle` and `scheduler-drain` behavior remains unchanged.
- The service may observe newly ready work after an idle wait, but it cannot create or
  admit work.
- A local stop signal is lifecycle input, not an authenticated Gate 12 control.
- No daemon, CLI command, service checkpoint, liveness claim, external adapter,
  multi-agent role, schema change, or automatic startup is added.
- The immediate integration consumer is a later explicitly authorized supported
  Scheduler service entry point using the same bounded policy.
