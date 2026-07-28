# 2026-07-28: Route The Existing Isolated Worker Result Point Through The Message Bus Next

Status: Accepted Decision

## Context

The governed Work path now has a real producer, file-spool hop, Message Bus receiver,
durable Scheduler admission, and acknowledgement. The remaining Gate 7 branches do not
have equal implementation prerequisites.

The process-isolated Scheduler path already has both sides of one Result connection:
`IsolatedWorkerMain` publishes a correlated `ResultPayload` through
`FileSpoolMessageTransport`, and `ProcessIsolatedAgentRunExecution` point-reads that
result, validates it against the dispatched Work and resolved RunRecord, and returns the
reference to the durable Worker finalization sequence. The parent currently performs
those checks directly rather than through `InProcessMessageBus`.

Handoff and general topic events have no accepted production consumer. Durable journal
work still lacks an additional consumer, subscription checkpoint, truncation owner, and
cross-store recovery order. Acknowledged-point retirement requires a separate retention
policy and explicit destructive authority. Authenticated control application remains
Gate 12 work.

## Decision

Connect the existing isolated-worker Result point through the real Message Bus before
adding another payload, durable journal, reliability, or retention branch.

The follow-up implementation will:

1. Keep the child producer, result spool wire format, explicit point resolution, and
   `ResultPayload` envelope unchanged.
2. Extract the parent's existing exact result/RunRecord validation into one bounded
   queue handler with no execution, finalization, or persistence authority.
3. Publish the decoded result envelope to a fresh `InProcessMessageBus` using the
   spooled destination. Only delivery to the exact expected result queue and successful
   handler validation may return the RunRecord reference.
4. Preserve the current correlation, logical-run, non-empty causation, task,
   RunRecord-binding, and verification-status checks. An unrouted, failed, duplicate, or
   several-result condition fails the isolated cycle closed.
5. Prove the real child/file-spool/bus/handler/RunRecord path and exact restart re-entry,
   plus the existing foreign-route, foreign-identity, payload, record-binding, and
   claimed-status refusals.

The handler is an in-memory admission boundary inside the existing execution cycle. It
does not journal durably, retry beyond the bus's single-attempt default, mutate runtime
or queue state, acknowledge or delete the result spool, create a new CLI, or change
worker completion authority.

## Rationale

This is the smallest remaining branch with a real upstream producer and downstream
consumer already in production. It makes the Result payload, non-empty causation, queue
delivery, and transport-to-bus handoff observable without inventing a new application
catalog, recovery store, schema, or authority.

Persisting a journal without a second durable consumer would duplicate the retained
result/Work points and Scheduler state without defining checkpoint or truncation
ownership. Selecting handoff, topic, or authenticated cancellation would invent or
cross a consumer boundary owned by later gates. Selecting retention would require
destructive authority not needed by this connection.

## Consequences

- The next implementation is a bounded refactor-and-connection of the existing isolated
  Result path, not a second result protocol.
- Result transport acceptance, Message Bus delivery, RunRecord validation, AgentRun
  result recording, and terminal queue disposition remain distinct states.
- Durable journaling, retry/dead-letter recovery, cancellation, ordering,
  backpressure, handoff/topic consumers, result acknowledgement/retention, and remote
  transport remain separate work.
- This assessment adds no production/test behavior, spool mutation, schema, dependency,
  authority, commit, push, merge, release, or deployment.
