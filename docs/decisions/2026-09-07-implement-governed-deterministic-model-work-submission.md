# User continuation request on 2026-09-07 into RFC-0024 governed deterministic ModelWork submission implementation

Status: Accepted Decision

## Context

RFC-0023 is internally Integrated for test-owned typed ModelWork execution and RFC-0024
defines the missing governed producer. The prior delivery task synchronized local and
remote `main`, and `CURRENT_TASK.md` named RFC-0024 implementation as separately
authorized work. The user requested continuation on 2026-09-07.

## Decision

Implement RFC-0024 RED-first through four sequential increments: the closed immutable
request and fixed repository-owned capability source; manifest-first first-use/replay
construction through unchanged `DurableWorkSubmissionService`; one test-owned producer
to internal model-aware worker integration; and final regression/document closure.

The request contains no capability field. Only the closed producer source supplies
`deterministic-echo`, independently of profile, request, candidate, manifest, CLI,
repository content, environment, or ambient configuration. Profile disagreement is
retained unchanged for later RFC-0016 admission.

Replay resolves exact durable intent before clock, context, task, or snapshot work and
compares every caller-owned value plus derived identities and fixed capability. The
implementation adds no supported interface, receiver, provider, network path, schema,
or execution authority beyond the existing internal test-owned composition.

This decision authorizes the minimum Java/test implementation, documentation
synchronization, verification, checkpoints, and ordinary local GREEN commits. It does
not authorize supported CLI/API/editor/Desktop ingress, typed spool publication or
receive, legacy path widening, provider/router/network, credentials, spend, MCP,
schema migration, push, merge, release, deployment, permission change, destructive
cleanup, or external effect.

## Consequences

- Observable behavior is introduced RED-first and the durable service and formats stay
  unchanged.
- Capability provenance remains independent from untrusted model requirements.
- The first real consumer remains test-owned and internal; supported typed ingress
  requires a later accepted task.
- Capability maturity changes only after fresh integration and full regression evidence.
