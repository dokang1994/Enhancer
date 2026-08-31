# User continuation request on 2026-08-31 into the Model RunRecord v2 and Scheduler admission specification

Status: Accepted Decision

## Context

RFC-0018's typed ModelWork retention and coordinated durable migration are implemented
and verified, but every execution path still blocks typed ModelWork because RunRecord
payload v1 cannot retain standalone complete-profile provenance. The legacy Scheduler
also synthesizes an `ApprovedTask`, reuses required capability as model class, and does
not evaluate RFC-0015/RFC-0016 with the exact active task and same execution-policy
instance.

RFC-0018 names the next design as defining Model RunRecord v2 and the exact task/policy
source required before a Scheduler model attempt can approach candidate suitability or
gateway execution. The user requested continuation on 2026-08-31.

## Decision

Authorize a documentation-only task to define and accept RFC-0019 as the minimum
additive Model RunRecord v2 and Scheduler admission-integration contract. It must keep
read-file RunRecord v1 byte-compatible, bind exact WorkItem/message/capability/profile/
request/policy/result provenance, resolve the exact active governed task rather than
synthesizing it, reuse one exact execution-policy instance, order fresh RFC-0015/
RFC-0016 evaluation and recovery, and persist no admission decision.

This decision does not authorize Java or binary-schema implementation, artifact
migration, command or caller changes, typed ModelWork execution, candidate suitability,
gateway or provider invocation, route or endpoint selection, network or remote
transmission, credentials, spend, push, merge, release, deployment, permission change,
or destructive cleanup.

## Consequences

- The task may add one accepted RFC and synchronize architecture, RFC planning/index,
  decision, task, changelog, and append-only verification owners.
- Existing RunRecord v1 writers/readers and every typed ModelWork execution guard remain
  unchanged until a later separately authorized RED-first implementation.
- RFC-0016 admission remains ephemeral eligibility only; without a separately accepted
  candidate-suitability and proven-local gateway contract, typed ModelWork cannot invoke
  even the deterministic fake gateway.
