# User continuation request on 2026-08-20 into the ModelRequest execution-profile composition specification

Status: Accepted Decision

## Context

RFC-0014 and its pure immutable value layer are Accepted and Contract Verified, while
the existing RFC-0013 `ModelRequest`, gateway, Tool, CLI, and Scheduler paths remain
unchanged. RFC-0014 requires a separately authorized composition contract before a
complete `ModelExecutionProfile` may enter the request path, and forbids implicit
defaults, routing, provider selection, or outbound authority.

The user requested continuation on 2026-08-20 after the value-layer task was closed.
The recorded next action is to specify the smallest Gate 9 composition that attaches a
complete profile to `ModelRequest` while preserving RFC-0013 compatibility and the
required task, execution, and future outbound-policy intersection.

## Decision

Authorize a documentation-only task to define and accept RFC-0015, Profiled Model
Request, as that minimum composition contract, including its fail-closed alignment
rules, authority boundary, exclusions, and test-first follow-up. The task may add the
RFC and synchronize its owning architecture, roadmap, decision, task, changelog,
handoff, and verification documents.

This decision does not authorize Java implementation; changes to `ModelRequest`,
`ModelGateway`, Scheduler, CLI, Tool, provider adapters, command or durable schemas;
routing or provider selection; network or remote transmission; credentials or paid
services; push, merge, release, deployment, or destructive cleanup.

## Consequences

- The contract must preserve every RFC-0013 request field and semantic without
  manufacturing a profile for existing callers.
- A profile remains untrusted requirement data and cannot grant execution or outbound
  authority.
- Any implementation, runtime integration, provider policy, or remote execution needs
  later explicit authority.
