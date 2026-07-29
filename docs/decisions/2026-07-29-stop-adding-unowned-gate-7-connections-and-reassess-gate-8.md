# 2026-07-29: Stop Adding Unowned Gate 7 Connections And Reassess Gate 8

Status: Accepted Decision

## Context

Gate 7 now has named real queue connections for governed Work publication and durable
admission, Control publication and durable request admission, isolated child Work
execution, and isolated parent Result validation. Each connection preserves the
unchanged envelope and reports success only after its existing downstream owner
succeeds.

Production-call inspection found no equivalent owner for the remaining branches:

- no production code constructs a topic destination or `HandoffPayload`;
- no production caller invokes Message Bus cancellation or dead-letter re-delivery;
- no production handler publishes re-entrantly, so cascade ordering and in-process
  pending-queue backpressure have no application state to own;
- the point receivers create fresh buses, while their retained spool points and durable
  consumers already own restart recovery; an ephemeral dead letter would add no durable
  recovery fact;
- a durable journal still needs stable subscriber identity, per-consumer checkpoints,
  truncation or compaction ownership, and cross-store recovery ordering;
- directory consumption still needs discovery order, claim ownership, concurrent
  consumer rules, partial-progress recovery, and restart semantics;
- retention would delete diagnostic or recovery evidence and needs explicit bounded
  cleanup authority, audit policy, and replay consequences.

`HandoffPayload` belongs to the later multi-agent topology in Gate 13. Authenticated
cancel, pause, and resume application belongs to Gate 12. Building any of those
mechanisms inside Gate 7 now would create an owner or authority that the current
application does not have.

The isolated Work ingress also adds new evidence relevant to the Gate 8 exit criterion
that workers communicate through messages rather than a direct parent-to-child
execution call. Gate 8 remains the repository's `Specified - Next` delivery gate, but
its whole-gate maturity has not been reassessed against this new connection.

## Decision

Stop adding Gate 7 production connections until a real upstream/downstream owner or an
accepted durability, recovery, or retention policy exists.

Keep Gate 7 at Contract Verified. Its connected Work, Result, and Control sub-paths
retain their recorded Integrated maturity, but unowned topic, Handoff, reliability,
durable-journal, directory-consumption, and retention branches do not receive synthetic
production callers merely to exercise their contracts.

A bounded Gate 8 reassessment is accepted against every scope item and exit criterion
using the current queue, runtime, process-isolated Work/Result Message Bus, recovery,
migration, effect, retry, service, and supported CLI evidence. The assessment may
promote only evidence-backed sub-capabilities or the whole gate; otherwise it must
identify each remaining blocker and its owning gate without implementing it.

## Rationale

The smallest safe next step is an evidence reassessment, not another mechanism. All
remaining Gate 7 candidates either lack a production consumer, require a policy with
durable state and cleanup consequences, or belong to a later gate with its own
authorization boundary.

Gate 8 is the next dependency-ordered gate and the new child Work Message Bus path
changes evidence for one of its exit criteria. Reassessing it prevents the Roadmap from
remaining stale while avoiding speculative durable-bus, Handoff, or control behavior.

## Consequences

- Gate 7 remains Contract Verified; no whole-gate promotion is implied by the connected
  Work, Result, and Control sub-paths.
- Topic, Handoff, re-delivery, cancellation, cascade/backpressure production flows,
  durable journaling, directory consumption, and retention remain deferred to a real
  owner and accepted policy.
- Gate 12 remains the owner of authenticated control application, and Gate 13 remains
  the owner of Handoff and multi-agent execution.
- Gate 8 maturity reassessment is an accepted follow-on direction; task activation
  remains owned only by `CURRENT_TASK.md`.
- No runtime behavior, persistence, schema, dependency, authority, or external state
  changes as a result of this decision.
