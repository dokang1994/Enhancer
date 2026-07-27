# 2026-07-27: Preserve Queue Recovery Claims In Priority-Aware Schema V3

Status: Accepted Decision

## Context

Queue schema v2 persists exact admission and disposition history but has no Scheduler
priority or fairness state. A direct priority-aware restart could also select a
different item after recovery requeues active work, while the separate
pending-finalization checkpoint remains bound to the interrupted WorkItem.

## Decision

Queue schema v3 retains every schema-v2 field and adds Scheduler priority to each
`QueuedWork`, a maximum expedited burst, consecutive expedited-claim progress, and an
optional one-shot recovery-preferred WorkItem identity. `WorkItem` remains unchanged.

The lossless schema-v2 mapping assigns `NORMAL` to every admitted item, maximum burst
`4`, progress `0`, and no migration-time recovery preference. When ordinary v3 recovery
requeues active work, it persists that exact identity as the preference. The next claim
must consume that ready pending item before ordinary selection, clear the preference,
and leave progress unchanged because it replays an already-counted durable claim.
Non-recovery claims remain FIFO until a later task connects the pure priority selector.

Migration is an explicit queue-root-and-identity-scoped maintenance operation that
requires the owning Scheduler to be stopped. It validates the complete v2 artifact,
prepares and rereads a same-directory v3 candidate, rechecks the source bytes, and
atomically replaces only the unchanged validated source. It returns typed `ABSENT`,
`ALREADY_CURRENT`, or `MIGRATED`; every earlier failure preserves the authoritative
source and removes the candidate when possible. Ordinary resolution and recovery reject
schema v2 and never migrate as a side effect.

## Rationale

Persisting priority without progress would change ordering across restart. Persisting
both without the recovery preference would break the exact WorkItem binding held by the
worker checkpoint. A one-shot preference preserves the existing at-least-once recovery
prefix while keeping priority metadata Scheduler-owned and migration explicit.

## Consequences

- Schema v3 becomes the only queue format accepted by ordinary store resolution.
- A two-argument `QueuedWork` construction remains a `NORMAL` compatibility path.
- Recovery preference must reference exact pending admission content and cannot coexist
  with an active item.
- The explicit migration surface grants no claim, recovery, execution, Tool, or
  external-effect authority.
- Priority-based non-recovery selection, claim-driven fairness updates, priority input,
  aging, and additional classes remain separate work.
