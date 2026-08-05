# 2026-08-05: Compose Lease Timeout Runtime Event Publication Across Supported Scheduler Execution Commands

Status: Accepted Decision

## Context

`DurableAgentRuntime` already persists an exact `LeaseTimeoutRecord` when recovery
reclaims an expired `EXECUTING` lease, then optionally derives and publishes one
`TIMEOUT_DETECTED(LEASE)` event from every retained record. The event-aware recovery
overload can repair a missing event or filesystem publication point on exact replay
without another runtime source-state transition.

The supported `scheduler-cycle`, `scheduler-drain`, and `scheduler-service` commands
share one optional filesystem `RuntimeEventRecorder`, but current Scheduler composition
passes it only to `ProcessIsolatedAgentRunExecution`. Runtime recovery in the worker and
dispatcher therefore remains event-free even when the complete event option group is
present. Injecting the recorder into finalization, retry, or other owners would activate
independent event contracts beyond this lease-timeout boundary.

## Decision

- Reuse the existing optional, all-or-none `--runtime-event-root`,
  `--runtime-event-publication-root`, and
  `--max-pending-runtime-event-publications` group for lease-timeout recovery in all
  three supported Scheduler execution commands. Add no new CLI option or composition.
- Retain the optional recorder in the shared `DurableAgentRunWorker` composition and
  pass it only to `DurableAgentRuntime` recovery performed by the worker and its
  `DurableAgentRunDispatcher`. Keep the existing process-isolated execution injection.
- Add an event-aware `recoverMatching` path so recovery of an already persisted goal
  through dispatcher claim follows the same lease-timeout contract as direct worker
  recovery. Event-free overloads remain the omission path.
- Preserve source-first order owned by `DurableAgentRuntime`: persist the reclaimed
  runtime revision and exact lease-timeout record first, then append or exactly replay
  the derived runtime event, then materialize or exactly replay its opaque publication
  point. Exact re-entry uses the retained record and does not reclaim again.
- Do not pass the recorder to finalization, retry control, result handling, Tool
  timeout, verification, cancellation, stagnation, or any other runtime-event owner.

## Rationale

The worker and dispatcher are the two recovery entries used by the one Scheduler
execution composition. Supplying the same optional recorder at both entries closes the
lease-timeout gap without changing lease authority or depending on which recovery path
observes expiry first. Reusing the already validated CLI option group and existing
recorder also preserves process-timeout behavior and event-free compatibility.

## Consequences

An expired Scheduler execution lease recovered through cycle, drain, or service can
now produce its retained lease-timeout event and bounded filesystem reference point.
Missing event or pending-point suffixes remain repairable from the source record, while
an acknowledged exact point is not recreated. Publication-capacity, failure-prefix,
and acknowledgement semantics remain those of the existing recorder and publisher.

This decision does not compose any other runtime-event owner, add a scan or background
service, change runtime schema or queue/retry/finalization authority, or grant external
delivery, deployment, cleanup, retention, or multi-process execution authority.
