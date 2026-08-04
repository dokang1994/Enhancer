# The recorded next task in the completed

Status: Accepted Decision

## Context

The completed Tool-timeout task recorded one next action: persist an authoritative
process-timeout fact at the process-isolated boundary before connecting
`RuntimeTimeoutKind.PROCESS`. The user then asked the project to continue.

## Decision

Select that recorded process-timeout follow-up as the one Active Task. Keep lease
timeouts, lifecycle/retry policy, cancellation application, unrelated Worker/CLI work,
and external delivery outside its boundary.

## Consequences

The active dynamic workflow can implement persistence before event derivation in a
deterministic order. No independent work is selected merely to keep the workflow moving.
