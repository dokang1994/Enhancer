# 2026-07-16: Persist One Goal And One AgentRun Lifecycle Before Adding Worker Ownership

Status: Accepted Decision

Context:

- Gate 8 has durable dependency-ready queue state, but it has no durable Goal or AgentRun lifecycle above a claimed WorkItem.
- The existing Gate 3 `AgentRunState` is a bootstrap Tool-loop state tied to one approved request. Reusing it as the event-driven Scheduler lifecycle would mix two different responsibilities and reopen the package boundary just made acyclic.
- Leases, fencing, retries, worker processes, budgets, effect idempotency, and schema migration are separately specified concerns. Combining them now would make restart and ownership semantics impossible to verify independently.
- Gate 7 already defines unchanged work and result envelopes. The runtime can preserve those messages as data without treating delivery or possession as authority.

Decision:

- Add one immutable schema-v1 Gate 8 runtime aggregate containing exactly one `RuntimeGoal`, its exact admitted `WorkItem`, and at most one `RuntimeAgentRun`.
- Give Goal and AgentRun distinct canonical UUID identities. The Goal retains the exact WorkItem as its authority and provenance source; runtime state cannot add or widen task, snapshot, logical-run, capability, or allowed-Tool data.
- Use deterministic Goal states `ACCEPTED`, `ACTIVE`, `COMPLETED`, and `FAILED`, and AgentRun states `PLANNING`, `READY`, `EXECUTING`, `AWAITING_VERIFICATION`, `COMPLETED`, and `FAILED`.
- Permit only the forward path `PLANNING -> READY -> EXECUTING -> AWAITING_VERIFICATION -> COMPLETED|FAILED`. This first schema supports one AgentRun and no retry or replacement.
- Require a terminal result envelope to carry `ResultPayload`, match the retained WorkItem's logical run, correlation, task, and work-message causation, and retain that exact result envelope. Only `VERIFIED` may complete; every other verification status produces explicit failure.
- Persist every successful lifecycle transition before exposing it, with a monotonic revision and immutable recovery of the exact last state. Persistence failure leaves the previous in-memory and durable revision authoritative.
- Store one bounded strict-UTF-8 integrity-checked binary state per Goal through atomic create/replace publication. Missing, corrupt, oversized, trailing, structurally invalid, or unsupported state fails closed.
- Defer retry, cancellation, pause/resume, leases, fencing, heartbeat, worker execution, effect records, RunRecord resolution, multiple AgentRuns, schema migration beyond v1, history retention, multi-process coordination, and parent-directory power-loss durability.

Rationale:

One reference-preserving Goal and one deterministic AgentRun are the smallest durable lifecycle that can sit above the existing queue without pretending that a queue claim is worker ownership. A typed result envelope closes terminal state structurally while keeping verification and RunRecord production outside this state store. Persist-before-exposure makes restart behavior observable before leases and workers are introduced.

Consequences:

- Restart restores the last persisted lifecycle state exactly and performs no automatic transition because no lease or ownership expiry exists yet.
- This increment consumes the existing WorkItem and ResultPayload contracts but does not integrate a Message Bus result path or supported runtime entry point.
- Failed or non-verified results remain terminal in schema v1; a later accepted retry design must introduce another AgentRun rather than mutating terminal history.
- Gate 8 remains `Specified - Next`; only the durable Goal/AgentRun lifecycle sub-capability may become Contract Verified.
