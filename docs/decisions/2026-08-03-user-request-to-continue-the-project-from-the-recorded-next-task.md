# User request on 2026-08-03 to continue the project from the recorded next task.

Status: Accepted Decision

## Context

The runtime-event value/store delivery task was completed on `main`, and
`CURRENT_TASK.md` named the next bounded increment: connect one existing transition
owner through a persist-after-source `RuntimeEventRecorder` and publisher port without
changing the four-kind MessageEnvelope wire schema.

The user requested that project work continue. Repository authority still requires the
next increment to remain bounded, test-first, locally verified, and separate from
commit, push, merge, release, or deployment authority.

## Decision

Activate and implement the repository-recorded next runtime-event increment. Use
`RuntimeControlAdmissionHandler` as the first transition owner because exact Control
request replay can recover both a source-persisted/event-missing prefix and an
event-persisted/publication-missing prefix without inventing a second runtime fact.

Authorization covers the bounded local implementation, tests, verification, and owned
document synchronization. It does not authorize commit, push, merge, release, tag,
deployment, MessageEnvelope schema evolution, authenticated control application, or
another runtime-event owner.

## Rationale

Selecting the already-recorded next task preserves document-driven sequencing. The
Control admission path already persists the exact request before success and supports
identity-stable replay, making it the smallest owner that can prove the accepted
state-first event ordering and exact-replay recovery contract.

## Consequences

- `CURRENT_TASK.md` may activate the bounded cancellation-request recorder connection.
- A fresh RED test must observe the missing recorder, publisher port, opaque reference,
  and transition-owner connection before production implementation.
- `PAUSE`, `RESUME`, authenticated `CANCELLATION_APPLIED`, concrete Message Bus event
  publication, and every other runtime-event kind remain outside this task.
- No delivery or external-state authority is granted.
