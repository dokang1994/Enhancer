# 2026-08-07: Compose Result-Side Runtime Event Publication Across Supported Scheduler Execution Commands

Status: Accepted Decision

## Context

`DurableAgentRunFinalizer` already owns source-first derivation and exact replay for
`VERIFICATION_RECORDED`, Tool `TIMEOUT_DETECTED`, `STAGNATION_DETECTED`, and
`WORK_ITEM_TERMINATED`. Focused filesystem tests prove their Result-before-derived-event
and queue-disposition-before-termination ordering, including append and publication
repair. `DurableAgentRunWorker` now exact-replays a checkpointed terminal Result before
retry decisions or terminal queue-disposition recovery.

The supported `scheduler-cycle`, `scheduler-drain`, and `scheduler-service` commands
already construct one optional filesystem `RuntimeEventRecorder` and pass it through the
shared process-isolated Worker composition to process timeout, lease timeout, and retry
owners. The same composition still constructs `DurableAgentRunFinalizer` through its
event-free constructor, so configured Scheduler execution cannot publish Result-side
facts.

## Decision

- Reuse the existing optional all-or-none Scheduler event configuration and its one
  recorder across all three supported execution commands; add no CLI option, root,
  schema, publisher, event kind, or authority.
- In the shared `DurableAgentRunWorker.processIsolated` composition, construct the
  finalizer with the same injected clock and recorder only when that recorder is present.
  Preserve the existing event-free finalizer construction when it is absent.
- Preserve the established durable order: Result transition -> verification -> optional
  Tool timeout -> optional stagnation -> retry decision/replacement as applicable ->
  terminal queue disposition -> WorkItem termination. Each later step may proceed only
  after all earlier configured publication work succeeds or exact-replays.
- Pin production composition and recovery with one named real-filesystem parameterized
  CLI integration over cycle, drain, and service. A capacity-one publication prefix must
  retain the durable Result, queue disposition, events, RunRecord, and cycle checkpoint;
  acknowledgement plus exact command re-entry must publish the retained termination
  fact without child re-execution or another source/event revision.
- Update the existing retry CLI integration to assert the full interleaving introduced
  by finalizer composition. Continue relying on the focused finalizer suite for Tool-
  timeout and stagnation source variants that the normal CLI read scenario cannot
  synthesize safely.

## Rationale

The finalizer already contains the owner-specific durability and replay logic, and the
Worker checkpoint now provides the exact Result re-entry cursor before later side
effects. Selecting the finalizer constructor in the existing shared composition is the
smallest change that exposes those verified contracts without duplicating event logic or
widening the CLI.

## Consequences

Configured Scheduler execution can publish multiple Result-side facts in addition to
process, lease, and retry events. A bounded publication-capacity failure may occur after
the source Result or queue disposition is durable; the retained checkpoint and exact
event identity make that prefix recoverable after acknowledgement.

Event-free omission, retry and queue authority, RunRecord and event schemas, consumers,
cleanup, retention, authenticated control, external adapters, commit, push, release,
and deployment remain unchanged.
