# `ROADMAP.md` and the completed delivery task identify lease-timeout recovery through

Status: Accepted Decision

## Context

`ROADMAP.md` and the completed delivery task both identify recovery of retained lease
timeouts through the shared Scheduler runtime seam as the current bounded runtime-event
candidate. The repository already owns the lease-timeout source fact, event derivation,
and filesystem recorder contracts; only supported Scheduler composition is absent.

## Decision

Select that recorded candidate as the sole Active Task. Limit the work to composing the
existing optional recorder through Scheduler-owned AgentRuntime recovery while
preserving event-free omission and excluding other runtime-event owners.

## Rationale

The selection follows the repository's recorded next action and advances one explicit
owner composition without opening a second task or broad event-mode switch.

## Consequences

The selected task may define and verify the minimum lease-recovery composition. The
roadmap remains capability-state context rather than implementation or delivery
authority, and subsequent owner selection remains separate work.
