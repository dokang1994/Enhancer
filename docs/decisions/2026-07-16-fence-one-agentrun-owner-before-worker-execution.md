# 2026-07-16: Fence One AgentRun Owner Before Worker Execution

Status: Accepted Decision

Context:

- Gate 8 now persists one Goal and one AgentRun lifecycle, but `EXECUTING` has no durable owner, expiry, or stale-writer defense.
- A queue claim is not a worker lease. Process interruption after `EXECUTING` would otherwise leave the run blocked forever or allow a replacement owner to race the original owner.
- Worker execution, heartbeats, retry, external effects, and multi-process coordination remain separate concerns. The lease contract must be independently verifiable before any worker is connected.

Decision:

- Add one optional immutable `AgentRunLease` to the schema-v1 AgentRun state and one persisted monotonically increasing last-issued fence token to the aggregate.
- Permit lease acquisition only from `READY`. Acquisition atomically advances the fence token, records a bounded non-blank owner identity, an issued time, and an exclusive expiry, and moves the AgentRun to `EXECUTING`.
- Bound lease duration from 1 millisecond through 24 hours. Time comes from an injected UTC `Clock`; caller-supplied wall-clock timestamps do not enter transition authority.
- Require matching owner and fence token for lease renewal and the transition from `EXECUTING` to `AWAITING_VERIFICATION`. Both operations fail closed at or after expiry.
- Renewal retains the same fence token, replaces the issue/expiry window, and must extend the existing expiry.
- On explicit reclaim or runtime recovery, an expired executing lease is persistently cleared and the AgentRun returns to `READY`. A later owner receives a strictly greater fence token; the expired owner can never write with its stale token.
- Preserve persist-before-exposure and exactly-one revision advancement for acquisition, renewal, execution completion, and expiry reclamation. Persistence failure leaves the prior state authoritative.
- Defer worker/Tool execution, heartbeat, retry or a second AgentRun, cancellation/pause, effect commits, lease-aware external systems, multi-process locking, clock-skew protocol, and parent-directory power-loss durability.

Rationale:

A persisted monotonically increasing fence is the smallest mechanism that distinguishes a current owner from a delayed or partitioned former owner. Returning expired execution to `READY` provides observable orphan recovery without inventing a replacement worker or retry policy. Injected time keeps expiry tests deterministic and prevents callers from choosing their own authority timestamp.

Consequences:

- Restart before expiry preserves the current owner and executing state; restart at or after expiry durably reclaims the run to `READY`.
- Lease possession grants no Tool or task authority. It only authorizes lifecycle writes already allowed by the retained WorkItem.
- In-memory and filesystem adapters remain single-process implementations; cross-process mutual exclusion and effect fencing require later work.
- Gate 8 remains `Specified - Next`; only the fenced single-owner lifecycle sub-capability may become Contract Verified.
