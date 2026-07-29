# 2026-07-29: Route Isolated Worker Work Through The Message Bus Before More Gate 7 Infrastructure

Status: Accepted Decision

## Context

The supported Work and Control paths now have explicit file-spool producers, exact point
receivers, real Message Bus queue delivery, and durable consumers. The isolated Result
path likewise publishes the decoded result through a fresh real Message Bus queue before
the parent may expose its RunRecord reference.

The remaining Gate 7 branches do not yet have equivalent application ownership:

- no production code constructs a topic destination or `HandoffPayload`;
- `redeliver`, `cancel`, re-entrant cascade ordering, and in-process pending-queue
  backpressure have only bus-contract callers, not a production state owner;
- directory consumption needs discovery order, claim/ownership, partial-progress,
  concurrent-consumer, and restart semantics;
- durable journaling needs stable subscriber identities, delivery checkpoints,
  truncation/compaction ownership, and cross-store recovery ordering;
- retention would delete diagnostic or recovery evidence and therefore needs explicit
  bounded cleanup authority and audit/replay policy.

The process-isolated Work ingress has the opposite shape. The parent already produces
one exact queue-addressed Work spool point, `IsolatedWorkerMain` already consumes that
point to run the real Gate 1-4 execution pipeline, and the invocation namespace is
bounded to one Goal/AgentRun. The child currently decodes the transport message and
calls execution directly rather than delivering it through the Message Bus.

## Decision

Defer topic, Handoff, bus re-delivery/cancellation/cascade/backpressure production
behavior, directory consumption, durable journaling, and retention until an owning
producer/consumer or policy exists.

As the next bounded implementation, route the already-decoded isolated-worker Work
transport message through one fresh `InProcessMessageBus` queue before execution.

Extract a child-side Work handler that accepts only the existing `queue("work")`
subscription, constructs the same exact `WorkItem` from the unchanged envelope and
parent-supplied work identity/capability, invokes the unchanged
`AgentLoopAgentRunExecution` boundary, resolves the resulting RunRecord verification
status, and exposes the reference/status only after handler success. The child will
publish to the destination carried by the decoded transport message and proceed only
when exactly one delivery outcome is `DELIVERED`.

The focused RED contract will make a foreign Work destination produce the Message Bus
`UNROUTED` path without execution or a Result point, while the named real child-process
fixture will prove the valid Work point crosses the bus, persists one RunRecord, and
returns through the already Message-Bus-validated Result path.

The implementation must preserve every current filesystem, identity, RunRecord,
evidence, exit-code, result-publication, restart, and parent-validation contract. It
must not add retry/dead-letter policy, cancellation, topic fan-out, a second Work
protocol, durable bus state, directory discovery, cleanup, or authority.

## Rationale

Routing the child Work point is the smallest remaining message-mediated worker
connection because both real collaborators and the exact queue route already exist.
It closes a direct-call seam inside the isolated worker without inventing a new event
or treating transport acceptance as delivery.

Using a remaining Gate 7 reliability primitive first would require a production owner
for the resulting state. A durable journal without checkpoints and truncation would be
an incomplete protocol, while scanning or retention would introduce broader ownership
and destructive behavior. The isolated Work connection instead mirrors the already
accepted Result-side pattern and leaves all execution authority where it is now.

## Consequences

- The isolated worker's Work and Result boundaries will both use real Message Bus queue
  delivery around the existing file-spool hops.
- A foreign child Work route will fail as unrouted before execution, RunRecord
  persistence, or Result publication.
- Successful delivery still means only that the handler completed; the returned
  RunRecord remains a claim validated by the parent and durable finalizer.
- Topic, Handoff, retry/dead-letter re-delivery, cancellation, cascade ordering,
  in-process backpressure production flows, directory consumption, durable journaling,
  and retention remain deferred.
- No whole-gate maturity changes automatically.
