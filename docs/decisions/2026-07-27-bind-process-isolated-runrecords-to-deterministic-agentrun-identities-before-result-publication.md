# 2026-07-27: Bind Process-Isolated RunRecords To Deterministic AgentRun Identities Before Result Publication

Status: Accepted Decision

## Context

The process-isolated child persists a randomly identified RunRecord before publishing
its result spool. A stop between those writes leaves a valid record that the stable Goal
and AgentRun cannot point-resolve, while scanning cannot prove attempt ownership and a
second sidecar repeats the same two-write failure window.

## Decision

Derive one versioned, domain-separated RunRecord UUID from the canonical Goal and
AgentRun identities already held in the durable cycle checkpoint. The filesystem store
persists at that identity, treats exact replay as non-writing success, and rejects
changed-content identity reuse. When no result is published, the parent point-resolves
only that reference, applies the existing task/source/target/digest binding checks, and
returns it without launching another child.

## Rationale

The RunRecord itself becomes the first point-addressable durable recovery artifact, so
there is no second post-persistence acknowledgement window and no store scan. Existing
random-reference callers and result-as-claim validation remain unchanged.

## Consequences

- The child-persisted/result-not-published prefix recovers without duplicate execution
  or another RunRecord.
- Corrupt or foreign content at the deterministic identity fails closed.
- Other at-least-once windows, external-effect recovery, retention, and universal
  exactly-once execution remain outside this decision.
