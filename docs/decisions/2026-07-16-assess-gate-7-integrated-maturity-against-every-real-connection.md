# 2026-07-16: Assess Gate 7 Integrated Maturity Against Every Real Connection

Status: Accepted Decision

Context:

- Gate 7 is Contract Verified and now has one named integration path from a real Gate 6 approved Workspace input through the in-process queue into Gate 8 `WorkItem` admission.
- The Roadmap scope is broader than that path: it includes the four payload kinds, topic and queue delivery, idempotency/retry/cancellation/dead-letter/replay/ordering/backpressure, and a transport-neutral IPC interface.
- Architecture defines Integrated maturity as connection to real upstream and downstream collaborators. Contract tests, interface existence, and one work-message path cannot be silently generalized to branches that the path does not exercise.

Decision:

- Run a documentation-only Gate 7 maturity assessment with fresh focused, full-regression, strict-lint, and self-hosting evidence.
- Map each Roadmap scope item and exit criterion separately to Contract Verified or Integrated evidence.
- Treat a Gate 7 sub-path as Integrated only when a named test connects its real upstream and downstream production collaborators.
- Promote Gate 7 as a whole only if the evidence supports every connection required by the gate-level scope. Otherwise retain Contract Verified and record the precise missing connections.
- Do not implement a missing adapter, payload flow, reliability scenario, Scheduler behavior, or production entry point inside the assessment.

Rationale:

Maturity labels are evidence claims, not rewards for aggregate test count. Requiring scope-by-scope real connections prevents a work-only queue integration from overstating result, control, handoff, topic, reliability, or transport integration while still recognizing the value of the path that now exists.

Consequences:

- The assessment may conclude that the work-message queue path is Integrated while Gate 7 remains Contract Verified.
- A concrete IPC adapter is not automatically mandatory merely because the interface is in scope; however, the interface cannot be called Integrated until a real transport implementation consumes it.
- Gate 8 remains the sole `Specified - Next` product gate regardless of the assessment outcome.
