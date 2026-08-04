# 2026-08-04: Persist Process Timeout Facts At The Isolated Execution Boundary

Status: Accepted Decision

## Context

`IsolatedWorkerLauncher` already distinguishes `TIMED_OUT` from `START_FAILED` and
completed process outcomes, but `ProcessIsolatedAgentRunExecution` currently converts
every non-completed outcome into an `IOException`. The durable cycle checkpoint retains
only identities and an optional RunRecord reference, while no RunRecord exists when the
parent watchdog terminates the child. Exception text, elapsed wall time, and later lease
reclamation are not authoritative timeout facts and cannot support exact event repair.

The process-isolated execution boundary owns the exact dispatch binding, configured
timeout, typed launcher outcome, and first post-outcome clock read. It is therefore the
narrowest owner that can persist the fact before failure is exposed without changing
AgentRun lifecycle or inventing a Tool/RunRecord result.

## Decision

- Add one immutable `ProcessTimeoutFact` and a `ProcessTimeoutFactStore` port. A fact
  retains its occurrence time, exact runtime-event Work/Goal binding, AgentRun identity,
  positive configured timeout, and bounded launcher reason. Its deterministic opaque
  reference is `process-timeout/<goal>/<agent-run>`.
- Add a bounded `FileSystemProcessTimeoutFactStore` rooted below the existing invocation
  root but outside per-cycle spool trees. It point-persists one integrity-checked fact per
  Goal/AgentRun, exact-replays identical content without rewrite, and rejects changed
  identity reuse, foreign reference/binding, corruption, oversized state, unsupported
  schema, symbolic storage boundaries, and trailing data. It adds no scan, retention,
  cleanup, migration, or cross-process transaction claim.
- `ProcessIsolatedAgentRunExecution` checks that deterministic point before launching.
  On a fresh `TIMED_OUT` outcome it reads its injected clock once, persists the fact,
  optionally records the derived event, and only then exposes execution failure. Exact
  re-entry resolves and revalidates the fact, repairs or republishes the same event when
  event-aware, and fails again without spooling or launching another child.
- `START_FAILED`, completed non-zero exit, missing/invalid result publication, and normal
  successful execution create no process-timeout fact or event. Persistence failure
  reaches no event; event or publisher failure leaves the fact available for exact
  re-entry.
- Add `PROCESS_TIMEOUT` as an authoritative runtime-event reference kind. The process
  event uses the fact's occurrence time, exact retained binding and AgentRun, the
  dispatched Work message as causation, producer `process-isolated-agent-run-execution`,
  detail `TimeoutDetected(PROCESS)`, and the fact reference plus its semantic SHA-256.
  The event never changes AgentRun, Goal, queue, retry, lease, or RunRecord state.
- Keep event publication caller-composed through the existing `RuntimeEventRecorder`
  port. The ordinary production constructor still persists process-timeout facts; the
  event-aware construction adds the recorder without introducing a MessageEnvelope or
  supported CLI event transport.

## Rationale

A dedicated point store preserves the typed watchdog fact at its real owner while
keeping the cycle checkpoint about recovery position and keeping RunRecords about
completed Agent Loop execution. The deterministic fact reference supplies restart
lookup and stable event identity, while exact replay prevents a later clock or changed
diagnostic from rewriting history. Deriving the event only from the stored fact follows
the existing source-first runtime-event ordering.

## Consequences

Process timeout detection becomes durable and restart-repairable without making the
timeout a verified result or automatically changing retry policy. One small filesystem
artifact family and one runtime-event reference enum value are added. Lease timeout
facts, lifecycle application after a process timeout, concrete event publication,
supported CLI event composition, migration, scanning, cleanup, retention, and
cross-store transactions remain separate work.
