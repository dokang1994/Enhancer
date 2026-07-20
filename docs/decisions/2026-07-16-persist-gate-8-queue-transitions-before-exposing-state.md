# 2026-07-16: Persist Gate 8 Queue Transitions Before Exposing State

Status: Accepted Decision

Context:

- Gate 8 has a deterministic in-memory single-worker queue, but a process interruption loses pending, active, completed, dependency, and admission-order state.
- A claimed item cannot remain permanently active after restart because the current queue has no lease, heartbeat, worker process, or fence token.
- Persisting only identifiers would be insufficient because restart recovery must reproduce the exact existing `WorkItem`, unchanged Gate 7 envelope, dependency readiness, and Tool-scope provenance.
- A full AgentRun state machine, effect ledger, lease protocol, migration framework, or multi-process coordinator would exceed this increment.

Decision:

- Give each durable queue a caller-supplied canonical UUID and bind its first admitted item to one logical run identity; reject later work from another logical run.
- Define a versioned immutable queue snapshot containing the queue identity, revision, capacity, optional logical run identity, total admission order, ordered pending work, optional active work, and completed work identities.
- Persist the exact `WorkItem`, `MessageEnvelope`, `WorkPayload`, task revision, allowed-Tool scope, required capability, and dependency identities needed to reconstruct scheduling without creating new authority.
- Store one bounded integrity-checked binary snapshot per queue through strict UTF-8 encoding and atomic replacement. Creation must not overwrite an existing queue; updates must advance exactly one revision; missing, corrupt, structurally invalid, oversized, or unsupported-version state fails closed.
- Stage every enqueue, successful claim, and completion against a copy, persist the next snapshot before exposing or adopting the transition, and retain the prior in-memory and durable state when persistence fails.
- On restart, retain pending and completed state and move any previously active item back into pending position according to original admission order, persist that recovery transition, and allow later re-claim. This is at-least-once queue recovery, not exactly-once effect execution.
- Defer leases, fencing, heartbeat, worker execution, effect idempotency records, failure/retry/cancellation policy, history retention, schema migration beyond v1, and multi-process coordination.

Rationale:

Persist-before-exposure prevents a caller from observing a queue transition that cannot survive restart. Re-queuing an interrupted active item avoids hidden work loss or a permanently blocked queue while honestly accepting possible replay until later lease, fence, and effect-idempotency contracts exist. A complete bounded snapshot is simpler and safer than a partially recoverable event log at this maturity.

Consequences:

- A storage failure rejects the transition and leaves the last durable revision authoritative.
- Recovery may cause an interrupted item to be offered again; no external side-effect deduplication is claimed.
- The v1 store has one current snapshot and no automatic cleanup or historical revisions.
- The durable queue sub-capability may become Contract Verified while Gate 8 remains `Specified - Next`.
- Durable Goal/AgentRun lifecycle state and fenced worker ownership remain the next larger Gate 8 concerns.
