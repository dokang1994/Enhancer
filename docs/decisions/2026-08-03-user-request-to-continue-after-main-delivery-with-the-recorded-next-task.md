# User request on 2026-08-03 to continue after main delivery with the recorded next task

Status: Accepted Decision

## Context

The completed runtime-event owner connections were committed, pushed, fast-forward
merged, and synchronized on `main`. `CURRENT_TASK.md` recorded the next bounded
increment as connecting `DurableAgentRunRetryController` after durable retry-decision
persistence while keeping replacement-attempt recording separate.

The user requested that project work continue after the `main` merge completed.

## Decision

Activate and implement the recorded `RETRY_DECISION_RECORDED` transition-owner
connection. Authorization covers the bounded local implementation, test-first
verification, and owned project-document synchronization.

It does not authorize `RETRY_STARTED`, another event owner, commit, push, merge,
release, tag, deployment, or unrelated external effects.

## Rationale

Continuing from the repository-owned next task preserves document-driven sequencing.
The retry controller is already the sole durable decision application boundary, so it
can derive one observation after persistence without adding transition authority or
collapsing a decision into a replacement attempt.

## Consequences

- `CURRENT_TASK.md` may activate the bounded retry-decision event connection.
- A fresh RED test must expose the missing event-aware controller construction and
  post-decision recorder behavior before production implementation.
- Decision and event persistence remain separate recoverable stores; publisher
  delivery remains at-least-once by event identity.
- `RETRY_STARTED`, concrete publication, Worker/CLI wiring, delivery operations, and
  every other runtime-event owner remain outside this authority.
