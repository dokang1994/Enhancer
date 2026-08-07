# 2026-08-06: Compose Retry Runtime Event Publication Across Supported Scheduler Execution Commands

Status: Accepted Decision

## Context

`DurableAgentRunRetryController` already owns source-first derivation and exact replay
for `RETRY_DECISION_RECORDED` after a durable decision and `RETRY_STARTED` after a
durable replacement AgentRun. Focused filesystem tests cover source-store failure,
event and publisher recovery, first-occurrence retention, and identity-safe replay.

The supported `scheduler-cycle`, `scheduler-drain`, and `scheduler-service` commands
already construct one optional filesystem `RuntimeEventRecorder` from the shared
all-or-none event option group and pass it into the event-aware process-isolated worker
factory. `DurableAgentRunWorker` retains that recorder for process- and lease-timeout
owners but still constructs its retry controller through the event-free constructor.

The finalizer is not the next bounded candidate. Supplying it with the recorder would
activate verification, Tool-timeout, stagnation, and terminal-disposition publication
together, and its terminal recovery ordering needs separate analysis before missing
result-side events can be repaired safely.

## Decision

- Reuse the existing optional Scheduler event configuration and recorder in all three
  supported execution commands; add no CLI option, root, schema, or publisher.
- When the recorder is present, construct `DurableAgentRunRetryController` with the
  worker's injected clock and that recorder. Preserve the current event-free controller
  construction when the option group is omitted.
- Preserve source-first recovery: durable retry decision before
  `RETRY_DECISION_RECORDED`, and durable replacement AgentRun before `RETRY_STARTED`.
  Existing Worker checkpoint states remain the exact re-entry cursor after append,
  publication, or capacity failure.
- Pin the supported production connection with a named real-filesystem CLI integration
  across cycle, drain, and service, including the admitted-decision, retry-started, and
  refused-decision sequence.
- Do not inject the recorder into finalization, Result handling, verification, Tool
  timeout, stagnation, cancellation application, or another owner in this task.

## Rationale

The retry controller is the smallest dependency-ready owner: the CLI-to-worker recorder
path and owner-specific durable event contract already exist, while the production gap
is one conditional constructor selection. Its decision and replacement checkpoints
also align with the Worker's current retry recovery branches, so failure can replay the
same source and event identities without another decision or AgentRun.

## Consequences

Retrying Scheduler executions configured for runtime events may publish multiple
pending points in one cycle. A caller-selected capacity that fills between those facts
causes a recoverable failure at the already-durable prefix; acknowledgement and exact
command re-entry may continue without widening retry or queue authority. Event-free
omission, retry policy, attempt bounds, terminal queue disposition, consumers, cleanup,
retention, and application authority remain unchanged.
