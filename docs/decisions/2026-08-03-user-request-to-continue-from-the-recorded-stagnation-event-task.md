# User request on 2026-08-03 to continue from the recorded stagnation-event task

Status: Accepted Decision

## Context

The completed retry-started event task recorded its next bounded increment as connecting
RunRecord-backed result finalization to `STAGNATION_DETECTED`. The accepted runtime-event
architecture already assigns detection to the Agent Loop and recording to
`DurableAgentRunFinalizer` after the corresponding Result transition is durable.

The persisted RunRecord retains the `STAGNATED` worker stop reason, the total iteration
count, and its own recorded occurrence time. The current bounded Agent Loop policy uses
the repository's default stagnation threshold of three. Timeout detection has several
possible owners and remains separate until one authoritative durable source is selected.

The user requested that project work continue.

## Decision

Activate and implement one bounded event-aware finalizer connection that records
`STAGNATION_DETECTED` only for a resolved RunRecord whose worker stop reason is
`STAGNATED`, and only after the matching Result transition is durably persisted or
exact-replayed.

The event uses the RunRecord occurrence time, iteration count, and current default Agent
Loop stagnation threshold, with stable Result-message and RunRecord references. Existing
verification-event behavior remains separate and ordered before the stagnation event.

Authorization covers local test-first implementation, fresh verification, and owned
project-document synchronization only. It does not authorize timeout or cancellation-
application events, a RunRecord/runtime/event schema change, supported Worker/CLI event
composition, concrete publication, commit, push, merge, release, tag, deployment, or
unrelated external effects.

## Rationale

The finalizer already resolves the RunRecord and owns the durable Result transition, so it
can preserve source-first ordering without introducing another transition authority.
Using the retained RunRecord occurrence and stable source references keeps event identity
and replay independent of later mutable runtime revisions.

Keeping timeout outside this task avoids choosing among Tool, process, and lease timeout
owners without a separately reviewed authoritative source.

## Consequences

- `CURRENT_TASK.md` may activate the bounded stagnation-event connection.
- Focused RED evidence must distinguish the missing stagnation observation from the
  already-connected verification event.
- Exact Result re-entry must repair a missing event or failed publication without another
  runtime revision, while non-stagnated RunRecords create no stagnation event.
- Timeout, cancellation application, schema evolution, concrete publication, and every
  other owner remain outside this authority.
