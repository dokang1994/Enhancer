# 2026-07-29: Publish Untrusted Control Intent From Existing Goal State

Status: Accepted Decision

## Context

The supported `scheduler-receive-control` command now carries one explicit local
Control spool point through the real Message Bus into an existing durable Goal's
bounded request ledger and acknowledges it only after persistence. Its upstream
transport artifact is still caller- or test-produced.

The durable runtime already retains the exact admitted Work envelope for every Goal.
`AgentRuntimeStateStore.resolve` can read that state without the lease-reclamation
behavior of `DurableAgentRuntime.recover`, and the retained Work envelope supplies the
Goal-bound correlation identity, logical-run identity, and causal message identity.
An upstream publisher can therefore bind one Control intent to existing state without
inventing runtime authority.

The remaining Gate 7 branches still have less complete collaborators or larger policy
prerequisites:

- Handoff has no production consumer and belongs to Gate 13 multi-agent execution.
- Topic delivery has no accepted production producer/consumer catalog.
- Cancellation, cascade ordering, and pending-queue backpressure lack a named
  production flow whose application semantics are already owned.
- A durable Message Bus journal still needs subscription checkpoints, truncation
  ownership, additional durable consumers, and cross-store recovery ordering.
- Retention requires destructive cleanup authority and replay/audit policy.

## Decision

Implement one separate `scheduler-spool-control` point command before those branches.

The command will take an explicit runtime-state root, canonical Goal identity, transport
spool root, queue destination, bounded pending-publication limit, canonical message
identity, bounded producer, occurrence time, Control signal, and bounded reason. It
will resolve the existing runtime state directly through
`FileSystemAgentRuntimeStateStore`, without runtime recovery, lease reclamation, queue
mutation, or worker interaction.

Publication is allowed only when the resolved Goal is `ACTIVE` and has a current
non-terminal AgentRun. The exact retained Work envelope is the sole binding source:

- `correlationId` equals the Work envelope's correlation identity;
- `logicalRunId` equals the Work envelope's logical-run identity;
- `causationId` equals the Work envelope's message identity;
- the new message identity, producer, occurrence time, signal, and reason remain
  explicit caller-owned intent metadata.

The command will construct one `ControlPayload`, carry the unchanged envelope through
`FileSpoolMessageTransport` to the explicit queue, and report only hop-level
`ACCEPTED`, `BACKPRESSURED`, or `UNAVAILABLE` plus the accepted point filename when
present. A named real-filesystem integration will connect an accepted point to the
existing separately invoked `scheduler-receive-control` command and prove the same
derived binding reaches the durable request ledger.

The producer is deliberately an **untrusted Control intent publisher**, not an
authentication or application boundary. Possessing runtime state, naming a Goal, or
successfully spooling a message grants no cancel, pause, resume, lease, queue, worker,
or filesystem-cleanup authority. Gate 12 remains responsible for any authenticated
interpretation and state transition.

The focused RED contract will require a production publisher that does not yet exist
and will cover exact binding derivation, active/non-terminal runtime eligibility,
hop-level refusal without a transport artifact, and the named publisher-to-receiver
durable integration. The minimum implementation must not scan a directory, receive or
acknowledge a point, reclaim a lease, apply a signal, add retry timing, create a durable
bus journal, or add retention behavior.

## Rationale

This is the smallest remaining Gate 7 connection with real upstream and downstream
state already present. The durable Goal supplies authoritative provenance without
granting transition authority, the file spool supplies the existing concrete hop, and
the Control receiver supplies the real Message Bus and durable consumer. Direct
read-only state resolution prevents publication from changing lease ownership merely
to discover the binding.

Selecting Handoff or topic first would invent a consumer. Selecting a reliability
branch would invent an application flow to make its behavior observable. Selecting a
durable journal or retention first would require broader persistence or destructive
policy. Calling this producer governed in the sense of authenticated control would be
misleading; its governance is limited to deriving immutable binding from existing
repository-owned runtime state.

## Consequences

- Gate 7 gains a selected upstream half for the supported Control point path without
  expanding Gate 12 authority.
- The publisher's success means transport acceptance only; durable admission remains a
  separate receiver result.
- Exact Work-derived binding and direct read-only state resolution become required
  contracts.
- Completed, failed, retry-pending, accepted-without-run, or terminal-run Goals cannot
  produce a supported Control intent through this command.
- Handoff, topic, authenticated application, cancellation/cascade/backpressure
  production flows, durable journaling, directory consumption, and cleanup/retention
  remain separate tasks.
