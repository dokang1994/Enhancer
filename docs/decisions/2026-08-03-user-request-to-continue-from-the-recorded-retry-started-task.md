# User request on 2026-08-03 to continue from the recorded retry-started task

Status: Accepted Decision

## Context

The completed retry-decision event task recorded its next bounded increment as
connecting `RETRY_STARTED` only after the admitted replacement AgentRun becomes durable.
The previous continuation authority explicitly excluded that replacement-state fact.

The user requested that project work continue.

## Decision

Activate and implement the recorded `RETRY_STARTED` transition-owner connection at
`DurableAgentRunRetryController.beginAdmittedRetry`, the same exact boundary re-entered
by the retry-aware Worker after checkpointing the replacement identity.

Authorization covers the bounded local implementation, test-first verification,
correction of the prior increment's omitted detailed Roadmap entry, and owned project
document synchronization. It does not authorize another event owner, supported
Worker/CLI event composition, commit, push, merge, release, tag, deployment, or
unrelated external effects.

## Rationale

Keeping the decision event and replacement-start event separate preserves the accepted
runtime lifecycle and crash-recovery prefixes. The controller already owns the durable
replacement append, and its idempotent active-state re-entry can repair event recording
without adding a second transition authority.

## Consequences

- `CURRENT_TASK.md` may activate the bounded replacement-state event connection.
- A fresh RED test must prove replacement persistence precedes event append/publication
  and that exact active-state replay repairs missing acknowledgement.
- Event identity uses stable retry-decision and replacement-AgentRun references rather
  than a mutable later runtime revision; the first event occurrence time remains the
  recorder's recovery responsibility.
- Stagnation, timeout, cancellation application, concrete publication, delivery
  operations, and every other runtime-event owner remain outside this authority.
