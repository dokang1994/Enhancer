# User continuation request on 2026-08-31 into exact Scheduler active-task resolution

Status: Accepted Decision

## Context

RFC-0019 and the completed Model RunRecord v2 task define exact active-task resolution
as the next sequential boundary. Typed ModelWork already retains an
`ApprovedTaskRevision` and Tool scope, while existing `ProjectContextReader` and
`ApprovedTaskReader` own complete governed context loading and active task parsing.
Current production execution still synthesizes legacy inputs and rejects typed
ModelWork before execution.

## Decision

Treat the user's explicit continuation as authority to implement only a concrete
Scheduler resolver that freshly loads the governed repository task and requires exact
retained task ID, source path, complete source SHA-256, and Tool-scope equality for one
typed ModelWork `WorkItem`. Use RED-first tests, bounded read-only development reviews,
fresh Java 17 verification, owning-document synchronization, checkpoints, and ordinary
local GREEN commits.

Do not prepare a prompt or `ModelRequest`, construct execution policy, invoke RFC-0015
or RFC-0016, add a production caller, remove a typed execution guard, write a Model
RunRecord, invoke a Tool or gateway, or add provider/network/credential/spend or
delivery authority.

## Consequences

The repository gains a verifiable pure resolution boundary for the exact current task
but no model-execution path. Request/policy preparation and fresh admission remain the
next separately authorized RFC-0019 sequence. Push, merge, release, deployment, and
destructive cleanup remain unauthorized.
