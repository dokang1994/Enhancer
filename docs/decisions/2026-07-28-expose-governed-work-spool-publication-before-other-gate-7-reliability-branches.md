# 2026-07-28: Expose Governed Work Spool Publication Before Other Gate 7 Reliability Branches

Status: Accepted Decision

## Context

The supported Work point receiver now takes one retained transport artifact through the
real Message Bus, persists exact Scheduler admission, and acknowledges the spool only
after that durable boundary. `FileSpoolMessageTransport` already implements durable
point publication with explicit `ACCEPTED`, `BACKPRESSURED`, and `UNAVAILABLE` outcomes,
but no supported producer derives a governed Work envelope and invokes that transport.
Current CLI integration tests create the input spool directly as test setup.

Other unconnected Gate 7 branches do not share the same immediate collaborators.
Result delivery is currently private to the process-isolated worker and its checkpointed
finalization sequence; authenticated control application belongs to Gate 12; handoff and
multi-agent consumers belong to Gate 13; topic events lack an accepted application
catalog; and a durable Message Bus journal still lacks ownership, subscription,
truncation, and cross-store recovery policy.

## Decision

Expose one governed Work spool publication boundary before implementing another Gate 7
payload, topic, cancellation, or durable-journal branch.

The follow-up implementation will add a separate `scheduler-spool-work` command:

1. The caller supplies the governed project and active task identity, one explicit spool
   root and queue destination, explicit message/correlation/logical-run/producer/time
   metadata, one governed execution target and digest, and a finite pending-capacity
   bound.
2. The command loads the repository-approved task and captures the existing
   repository-memory Workspace snapshot. It constructs the same bounded `WorkPayload`
   authorization and provenance fields already used by the real work-message path;
   observations do not create task or Tool authority.
3. It sends one unchanged `TransportMessage` through `FileSpoolMessageTransport`.
4. Bounded output reports exactly the hop-level `ACCEPTED`, `BACKPRESSURED`, or
   `UNAVAILABLE` outcome and a bounded refusal reason where applicable. It never reports
   Message Bus delivery, durable admission, acknowledgement, execution, or completion.
5. An accepted artifact is consumed only by a separately invoked existing
   `scheduler-receive-work` command. A named real-filesystem integration must prove the
   complete supported publication -> point receipt -> durable admission ->
   acknowledgement path, exact authorization/provenance carriage, backpressure without
   a second artifact, and an unavailable root without a partial publication.

The new command does not scan for work, select a file for receipt, create a Scheduler
queue, invoke a worker, retry a refused transport outcome, or combine publication with
receipt or execution.

## Rationale

This is the smallest remaining connection with both real collaborators already present:
governed task/snapshot input upstream and the acknowledged point receiver downstream. It
turns the existing transport backpressure contract into observable supported behavior
without adding a second recovery store or pretending that transport acceptance is
delivery.

Selecting result, handoff, topic, or cancellation first would either duplicate an
existing private worker sequence or invent an upstream/downstream consumer not yet owned
by the current gate. Persisting the Message Bus journal first would add schema,
subscription checkpoints, truncation, retry/dead-letter recovery ordering, and another
cross-store authority boundary before a supported producer uses the current transport.

## Consequences

- The selected follow-up is a Gate 7 Work transport-publication connection whose
  downstream consumer is the existing acknowledged Work point receiver.
- `WorkMessagePublisher` remains the in-process bus publisher; the new transport path
  must reuse its task/snapshot validation semantics without translating transport
  acceptance into `DeliveryOutcome`.
- Result, handoff, topic, cancellation, durable retry/dead-letter recovery, directory
  consumption, cleanup/retention, durable bus journaling, remote transport, background
  lifecycle, and combined execution remain separate work.
- This assessment adds no production/test behavior, spool artifact, schema, dependency,
  authority, commit, push, merge, release, or deployment.
