# User continuation request on 2026-08-21 into the Scheduler complete-profile transport specification

Status: Accepted Decision

## Context

RFC-0017 is Accepted and defines caller-side model invocation source obligations over
existing pure values. It leaves the current Scheduler unsupported because the retained
WorkItem path has an independently projected required capability but no complete
RFC-0014 profile source. The complete WorkItem is currently encoded through the Gate 7
message envelope, submission manifest v2, Scheduler queue v3, and AgentRuntime v4.

The completed RFC-0017 task names the next work as defining the typed, versioned, and
recoverable Scheduler profile source/transport contract. The user requested
continuation on 2026-08-21.

## Decision

Authorize a documentation-only task to define and accept RFC-0018 as the minimum
versioned Scheduler complete-profile source and transport contract. It must define one
typed model-work input, preserve all RFC-0014 fields as one exact untrusted value, keep
the active WorkItem capability projection independent, enumerate every affected durable
representation and version transition, and specify fail-closed migration, replay,
recovery, and cutover behavior without defaults.

This decision does not authorize Java implementation, schema or artifact migration,
command changes, caller cutover, admission or gateway wiring, model suitability,
routing, providers, network or remote transmission, credentials, paid services, push,
merge, release, deployment, or destructive cleanup.

## Consequences

- The task may add one accepted RFC and synchronize architecture, RFC planning/index,
  decision, task, changelog, and append-only verification owners.
- Existing message-envelope v1, transport-spool v1, submission manifest v2, Scheduler
  queue v3, AgentRuntime v4, current commands, and current runtime behavior remain
  unchanged until a later separately authorized implementation task.
- No migration may manufacture a missing profile or treat retained transport data as
  self-authenticating model capability authority.
