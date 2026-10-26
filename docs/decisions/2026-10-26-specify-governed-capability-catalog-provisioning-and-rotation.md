# User continuation request on 2026-10-26 into governed capability catalog provisioning and rotation specification

Status: Accepted Decision

## Context

RFC-0031 is Contract Verified as the sole production creator of RFC-0030 remote
candidate capability facts, but it intentionally accepts only a protected absolute
catalog path and independently provisioned whole-file SHA-256 pin. No contract yet
defines how truthful catalog bytes are prepared, how that exact path-and-pin binding is
installed or rotated, which authority owns publication, or which failures must leave the
previous binding authoritative.

The completed Active Task names catalog provisioning/rotation or the same-invocation
consumer as the next separately authorized boundary. Provisioning precedes a truthful
production consumer because a matching digest establishes exact approved bytes, not the
truth or provenance of the remote-world claims encoded in them.

## Decision

Treat the user's continuation as authority for one documentation-only RFC-0032
specification increment. Define the operator intent, independent evidence obligations,
privileged publisher boundary, canonical catalog artifact and binding identities,
atomic installation and rotation state machine, cooperative concurrency and replay
semantics, crash recovery, safe observability, and explicit permission and anti-rollback
limits needed to provision the RFC-0031 path and pin without collapsing their trust
boundary.

The specification may reconcile Architecture, Project State, Roadmap, Changelog, the
decision index, verification log, Active Task, and session handoff. It may add no Java
or test source, installed catalog, writer implementation, command, launcher, platform
adapter, permission mutation, provider, credential, network retrieval, spend,
same-invocation consumer, route execution, runtime, persistence integration, MCP,
release, deployment, push, or other external effect.

## Rationale

Defining the trust and publication contract before adding a consumer prevents an exact
digest from being mistaken for factual truth and prevents request, runtime, or catalog
content from self-approving both bytes and pin. A documentation-only increment can close
the authority, state-transition, and failure semantics while retaining separate future
authority for any privileged implementation or real installation.

## Consequences

- RFC-0032 must distinguish evidence preparation, operator request, privileged
  publication, runtime reading, and remote execution authority.
- Atomic publication, concurrent rotation, exact replay, stale intent, crash recovery,
  permission assumptions, and privileged rollback limits must be explicit.
- Acceptance of RFC-0032 will not make provisioning Operational or make a remote route
  reachable.
