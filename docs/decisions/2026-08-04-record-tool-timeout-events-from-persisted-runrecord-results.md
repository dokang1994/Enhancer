# 2026-08-04: Record Tool Timeout Events From Persisted RunRecord Results

Status: Accepted Decision

## Context

The accepted runtime-event taxonomy distinguishes Tool, process, and lease timeouts, but
it deliberately requires an authoritative durable runtime or RunRecord fact before a
transition owner records `TIMEOUT_DETECTED`. Those timeout boundaries do not currently
retain equivalent facts.

The Tool execution path persists its structured `ToolFailureCode` inside the RunRecord.
`DurableAgentRunFinalizer` already resolves and binds that RunRecord, persists or exact-
replays the matching Result transition, and only then records the verification and
optional stagnation events. A process-isolated watchdog timeout currently returns
`IsolatedWorkerStatus.TIMED_OUT` to an execution path that throws before returning a
RunRecord reference. Lease-expiry recovery persists a return to `READY` and a later
greater fence, but does not retain a separate typed timeout occurrence fact.

The user requested on 2026-08-04 that the project continue from the recorded bounded
timeout workflow.

## Decision

Select a persisted RunRecord whose exact Tool result carries
`ToolFailureCode.TIMED_OUT` as the first authoritative timeout source. The event-aware
`DurableAgentRunFinalizer` is its transition owner because it already resolves that
source and establishes the durable Result transition before derived event recording.

After the Result transition persists or exact-replays, the finalizer may record one
`TIMEOUT_DETECTED` event with `RuntimeTimeoutKind.TOOL`. The event uses the RunRecord's
recorded occurrence time, the retained Result message as causation, the exact Goal,
WorkItem, AgentRun, task, snapshot, logical-run, and correlation binding, and ordered
stable references to the Result message and RunRecord. Verification remains the first
derived Result fact. Tool timeout follows it, and stagnation remains a separate later
fact when the same RunRecord also records `STAGNATED`.

Exact Result re-entry re-derives the same event identity and exact-replays the append
before publication. A non-timeout failure, a missing or mismatched RunRecord, or a
Result-transition persistence failure produces no Tool-timeout event.

Process and lease timeouts remain deferred. Neither may be projected from transient
status, exception text, elapsed wall time, a reclaimed fence alone, or another timeout
kind's source. Each requires its own accepted durable fact, transition owner, occurrence
semantics, and recovery test before its event connection exists.

## Rationale

The selected source is already structured, durable, integrity-checked, and available at
the existing source-first event owner. It therefore adds no schema, competing state
machine, inference from prose, or new authority. Separating process and lease sources
keeps the event truthful: the current artifacts can prove recovery behavior but cannot
yet prove one retained typed timeout occurrence for those boundaries.

## Consequences

- The active dynamic workflow may connect only Tool timeout detection in this task.
- Focused RED evidence must distinguish the missing Tool-timeout event from the existing
  verification and stagnation events.
- The minimal implementation belongs in event-aware
  `DurableAgentRunFinalizer.recordAgentRunResult` after Result persistence.
- No RunRecord, runtime-event, MessageEnvelope, queue, runtime, or store schema changes
  are authorized.
- Process and lease timeout persistence, concrete publication, supported Worker/CLI
  event composition, cancellation application, and Gate 8 promotion remain separate.
