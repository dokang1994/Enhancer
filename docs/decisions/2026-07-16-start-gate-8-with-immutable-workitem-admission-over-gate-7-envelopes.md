# 2026-07-16: Start Gate 8 With Immutable WorkItem Admission Over Gate 7 Envelopes

Status: Accepted Decision

Context:

- Gate 8 owns `WorkItem`, dependency scheduling, leases, recovery, and the single worker first, while Gate 7 already owns the versioned envelope and work payload that carry task, snapshot, logical-run, provenance, and allowed-Tool data.
- The existing Gate 3 `AgentRunState` governs one pre-authorized Tool request through execution and verification. Reusing or duplicating it as the Scheduler's durable work identity would mix bootstrap execution state with the later event-driven runtime.
- A full Goal/AgentRun store, dependency queue, lease protocol, or recovery mechanism would exceed the smallest coherent first Gate 8 increment.

Decision:

- Add immutable `com.enhancer.runtime.WorkItem` as the first Gate 8 contract.
- Give each work item a canonical UUID identity distinct from the retained envelope's canonical message identity and one required-capability name bounded to 256 characters.
- Admit only an existing `MessageEnvelope` whose payload is `WorkPayload`; retain the exact envelope rather than copying or flattening its authority and provenance fields.
- Expose logical run identity, approved task revision, snapshot identity, and allowed Tools only as projections of the retained work payload and envelope.
- Create no approval, policy decision, Tool grant, queue state, lifecycle transition, lease, persistence, or worker execution behavior.
- Name the dependency-ready single-worker Scheduler queue as the immediate integration consumer in the next Gate 8 increment.

Rationale:

A Scheduler needs a stable work identity separate from a delivery attempt, but the Gate 7 envelope must remain the authoritative handoff container. Wrapping one unchanged work envelope creates that separation without reopening the message schema or inventing runtime state before queue, persistence, and recovery semantics are decided. Keeping authority fields as projections makes it impossible for the work item constructor to widen them.

Consequences:

- One logical work item may later be delivered or retried through multiple message identities without conflating work identity with transport identity; this first contract admits one initial work message only.
- Non-work result, control, and handoff envelopes cannot enter the Scheduler work-item path.
- Dependencies, states, versions, budgets, deadlines, attempts, leases, fencing, checkpoints, and persistence remain required later Gate 8 increments rather than implicit fields.
- The Gate 8 gate remains `Specified - Next`; only the `WorkItem` admission sub-capability may reach Contract Verified in this task.
