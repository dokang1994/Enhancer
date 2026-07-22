# 2026-07-22: Connect Work Admission To The Durable Scheduler Queue Through A Persist-First Handler

Status: Accepted Decision

Context:

- The real Gate 6/7 work-message path currently ends at `WorkItemAdmissionHandler` and
  an injected in-memory sink, while every durable Worker integration prepares
  `DurableSingleWorkerSchedulerQueue` directly in test setup.
- The downstream queue-to-AgentRun, process-isolated execution, retry, recovery, and
  terminal-disposition paths are Integrated, but a supported Scheduler entry point
  would still have no real production path for durable work admission.
- The existing handler was deliberately selected as a non-storing adapter. Changing it
  in place would rewrite the boundary established by its accepted decision and obscure
  the different failure semantics of an in-memory sink and a durable queue.

Decision:

- Add a separate `DurableWorkItemAdmissionHandler` that consumes one existing work
  envelope, derives a stable canonical WorkItem UUID by a fixed one-to-one transform of
  the canonical message UUID, and creates dependency-free `QueuedWork`.
- Persist through the caller-supplied `DurableSingleWorkerSchedulerQueue` before handler
  success. Translate checked storage failure to `UncheckedIOException` so the existing
  in-process bus retry and dead-letter policy remains the delivery failure boundary.
- Keep the original `WorkItemAdmissionHandler` unchanged for non-durable injected sinks.
- Prove a named real path from repository-derived approved work and Workspace snapshot
  through publication and bus delivery into a filesystem-backed durable queue, followed
  by fresh-instance recovery and claim of the exact unchanged WorkItem.
- Treat fresh-bus re-delivery after durable admission as a fail-closed duplicate for this
  increment. Do not call it idempotent success without an exact durable admission-history
  contract capable of rejecting message-identity reuse with changed content.

Rationale:

This is the smallest connection that removes test-only queue preparation from the real
upstream path while preserving persist-before-exposure semantics. A stable bijective
identity transform prevents retry attempts from inventing new WorkItem identities and
keeps the WorkItem distinct from its own delivery identity without adding a new store.
Fail-closed restart replay avoids duplicate logical work without overstating the queue's
current terminal-history schema.

Consequences:

- Gate 7 work publication gains one named durable Gate 8 queue consumer, but no supported
  entry point, durable bus journal, worker polling loop, or Gate 8 Operational claim is
  created.
- Same-process replay remains suppressed by the bus. Fresh-process replay is visible as
  failure/dead-letter rather than duplicate work or silent success.
- Arbitrary dependency submission, exact admission-history replay, and queue schema
  migration remain separate tasks.
