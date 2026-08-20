# User continuation request on 2026-08-20 into the model invocation admission specification

Status: Accepted Decision

## Context

RFC-0015 and its pure `ProfiledModelRequest` value are Accepted and Contract Verified,
but no production caller constructs the value and no runtime boundary intersects its
requirements with task, execution, or outbound authority before gateway execution.
The Scheduler has a retained required-capability value, while the current direct CLI
path has no complete-profile or capability source.

The user requested continuation on 2026-08-20 after the RFC-0015 value implementation
was verified and closed.

## Decision

Authorize a documentation-only task to define and accept RFC-0016 as the smallest
provider-neutral invocation-admission contract upstream of gateway execution. It must
name explicit complete-profile and capability-authority sources, fail closed when
either is absent, define the task/execution/profile/outbound intersection and typed
rejection behavior, and preserve current RFC-0013 through RFC-0015 compatibility.

This decision does not authorize Java or runtime implementation; changes to existing
request, gateway, Tool, CLI, Scheduler, adapter, command or durable schemas; routing,
provider or destination selection; network or remote transmission; credentials, paid
services, push, merge, release, deployment, or destructive cleanup.

## Consequences

- The contract may specify pure admission values and ports, but must not manufacture a
  profile or treat untrusted inputs as authority.
- Current callers without complete profile and capability authority remain unsupported
  rather than receiving defaults.
- Runtime composition and every external or provider effect require later authority.
