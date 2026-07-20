# Current Task

## Status

Completed

## Task

Harden Gate 8 connection sub-increment 3d at its authority and recovery boundaries, and repair the canonical-document ownership guard that allowed stale next-task declarations to survive.

## Task ID

gate-8-process-isolated-execution-hardening

## Justified By

- 2026-07-20: Return Isolated Worker Results Through A Correlated Per-Cycle Spool With RunRecord As Authority

## Context

The first 3d implementation proved a real child JVM execution path, but review found four fail-closed gaps: a pre-existing foreign work message was reused without identity validation; a resolved RunRecord was not bound back to the dispatched source and execution input; multiple result messages were treated as no result and could trigger another child; and the shared execution seam was unnecessarily public. The same review found stale canonical documents and showed that the document-ownership test recognized `## Next Task` but not the repository's actual `## Next` heading or declarative next-increment prose.

## Acceptance Criteria

- A pre-existing work spool entry is decoded and reused only when its destination and complete envelope exactly equal the current WorkItem; foreign work fails before launch.
- Result spool cardinality distinguishes zero, one, and several entries; several entries fail before launch instead of being treated as unpublished.
- A result is accepted only from the exact result destination and only when its resolved RunRecord binds back to the dispatched task, source document, execution target, expected digest, and claimed verification status.
- Adversarial tests prove foreign work, foreign RunRecord references, wrong result destinations, and multiple results cannot launch or advance execution.
- `AgentLoopAgentRunExecution.executeWork` is package-private, keeping the shared child seam inside the runtime package.
- `PROJECT_STATE.md`, `ARCHITECTURE.md`, `.ai/architecture.md`, `ROADMAP.md`, and `SESSION_HANDOFF.md` agree on 3d's current boundary and leave next-task ownership to `CURRENT_TASK.md`.
- `DocumentOwnershipTest` detects both canonical next-task headings and declarative next-task/next-increment prose outside `CURRENT_TASK.md`.
- Full regression passes with 0 failures and 0 errors, and strict lint passes across all production sources.

## Out Of Scope

- Wiring the execution into `DurableAgentRunWorker`.
- Spool retention and cleanup; nothing removes an invocation root today.
- Retry policy, cancellation of a running child, and concurrent cycles sharing one invocation root.
- Closing the window where a child persists a RunRecord and dies before publishing; that orphan and its re-execution stay documented at-least-once behaviour.

## Approval

Approved by the user's 2026-07-20 request to implement the four reviewed findings, repair the document consistency guard, verify the repository, and commit, push, and merge the completed work.

## Verification

Recorded in `docs/verification-log.md` under Process-Isolated Execution Hardening Verification.

## Next

After this hardening increment, wire `ProcessIsolatedAgentRunExecution` into `DurableAgentRunWorker` and decide spool retention, since an invocation root currently persists for every cycle with nothing to remove it.
