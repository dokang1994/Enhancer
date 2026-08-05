# 2026-08-05: Compose Process Timeout Runtime Event Publication Across Supported Scheduler Execution Commands

Status: Accepted Decision

## Context

`ProcessIsolatedAgentRunExecution` already persists a deterministic
`ProcessTimeoutFact` before optionally recording `TIMEOUT_DETECTED(PROCESS)`, and exact
re-entry repairs that event without launching another child. The supported
`scheduler-cycle`, `scheduler-drain`, and `scheduler-service` commands share one
Scheduler execution composition, but that composition does not expose the existing
filesystem runtime-event store and publication point adapter.

Injecting the same recorder into finalization, retry, runtime recovery, or other
timeout owners would activate independently owned events. The process-isolated
execution seam is the smallest boundary that can publish this one already-durable
fact without changing Scheduler lifecycle authority.

## Decision

- Add the same optional, all-or-none `--runtime-event-root`,
  `--runtime-event-publication-root`, and
  `--max-pending-runtime-event-publications` group to `scheduler-cycle`,
  `scheduler-drain`, and `scheduler-service`. The configured capacity remains between
  1 and 4096. Omission preserves the existing event-free composition.
- Validate the complete group during CLI parsing, before queue recovery or any
  filesystem construction. Partial groups, invalid paths, and out-of-range capacity
  are usage failures with no Scheduler or runtime-event side effect.
- Construct at most one `FileSystemRuntimeEventStore`,
  `FileSystemRuntimeEventPublisher`, and `RuntimeEventRecorder` in the shared
  Scheduler execution composition. Pass the optional recorder only through
  `DurableAgentRunWorker.processIsolated` to `ProcessIsolatedAgentRunExecution`.
- Preserve the durable order already owned by the isolated execution boundary:
  process-timeout fact persistence or exact replay, event append or exact replay,
  opaque point publication or exact replay, then the existing execution failure.
  Re-entry checks the retained fact before spooling or launching.
- Do not compose the recorder into lease recovery, retry control, result finalization,
  runtime transitions, Tool timeout, verification, cancellation, stagnation, or any
  other runtime-event owner.

## Rationale

One optional configuration shared by all three commands matches their existing
execution seam and keeps event-free compatibility explicit. Recorder injection at the
process-isolated boundary reuses the durable source-first and replay contracts already
verified there, while avoiding a broad event-mode switch whose effects would depend on
unrelated owner implementations.

## Consequences

A process watchdog timeout can now create a durable runtime-event stream and bounded
filesystem reference point from each supported foreground Scheduler command. Fact,
event, and point prefixes remain independently repairable, and an acknowledged exact
point is not recreated. The execution still fails internally and retains its cycle
checkpoint; it creates no RunRecord, retry decision, or terminal queue disposition.
All other runtime-event owner composition, multi-owner atomicity, transport delivery,
retention, cleanup, deployment, and background service authority remain separate work.
