# 2026-07-28: Receive One Durable Work Spool Through The Message Bus Before Scheduler Admission

Status: Accepted Decision

## Context

Gate 7 has a durable local `FileSpoolMessageTransport`, but its accepted hop means only
that a message was spooled. Gate 8 has a durable work-admission handler and supported
Scheduler execution commands, while the only named bus-to-admission integration is
in-process and test-owned. A supported receiving boundary must connect the transport
artifact back through Message Bus delivery before Scheduler admission without treating
transport acceptance as delivery or inventing a second execution path.

## Decision

Add a separate `scheduler-receive-work` CLI command for one explicitly named local
transport-spool file.

The caller supplies the spool root, one canonical transport filename, the expected queue
destination, the existing Scheduler queue root and identity, required capability, and an
optional exact `NORMAL`/`EXPEDITED` Scheduler priority. The command:

1. resolves only the named regular non-symbolic `.transport` file beneath the explicit
   spool root;
2. decodes it through `FileSpoolMessageTransport.read`;
3. requires the exact expected queue destination and a `WorkPayload`;
4. constructs an `InProcessMessageBus` with one queue subscriber backed by
   `DurableWorkItemAdmissionHandler`;
5. publishes the unchanged envelope and reports success only after the durable queue
   handler succeeds.

The result distinguishes `ADMITTED` from exact durable `REPLAYED` by the queue revision
change and reports the derived WorkItem identity, queue identity/revision, and effective
priority. A changed envelope under a reused message identity fails closed through the
existing durable admission invariant.

The command does not delete, rename, acknowledge, scan, or dead-letter the spool file.
The retained transport artifact makes an uncertain caller acknowledgement replayable;
exact replay changes no queue revision. Spool acknowledgement/retention and cleanup need
their own durable protocol and policy.

Execution remains a separate explicit `scheduler-cycle`, `scheduler-drain`, or
`scheduler-service` invocation. A real-filesystem integration must send through
`FileSpoolMessageTransport`, receive through this command and the real Message Bus,
execute through `scheduler-service`, then prove exact re-receipt creates no second queue
admission, AgentRun, or RunRecord.

## Rationale

This is the smallest supported Gate 7/8 connection that preserves the distinction between
transport acceptance, Message Bus delivery, durable Scheduler admission, and worker
completion. Point resolution avoids granting directory-scan authority, and retaining the
spool avoids an uncheckpointed delete acknowledgement.

## Consequences

- The supported path is transport spool → Message Bus → durable Scheduler admission →
  separately invoked durable worker.
- Authorization and provenance remain the unchanged `MessageEnvelope`; the receiver adds
  no Tool or task authority.
- Existing submit, cycle, drain, and service behavior remains unchanged.
- No durable bus journal, spool acknowledgement/deletion, directory scan, background
  thread, remote transport, queue creation, combined receive-and-execute wrapper, schema
  change, commit, push, release, or deployment is added.
