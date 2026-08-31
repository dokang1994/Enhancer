# User continuation request on 2026-08-31 into Scheduler model request and admission preparation

Status: Accepted Decision

## Context

RFC-0019 Model RunRecord v2 persistence and exact active-task resolution are verified.
Its required sequence next names explicit Scheduler request/policy preparation and fresh
RFC-0015/RFC-0016 evaluation before a separately accepted candidate-suitability and
proven-local gateway boundary. The user requested continuation on 2026-08-31.

## Decision

Authorize the minimum RED-first standalone preparation boundary described by the new
Active Task. It may add explicit invocation limits, share the existing bounded prompt
read rules, construct one exact policy, prepare one exact profile-aligned request,
evaluate admission freshly with the exact task and unchanged WorkItem capability, and
return an invocation-local non-persistent result that preserves exact object identity.

This decision does not authorize candidate suitability, Tool or gateway invocation,
provider or route selection, external effects, Model RunRecord writing, runtime caller
wiring, durable-schema or command changes, network, credentials, spend, push, merge,
release, deployment, permission change, or destructive cleanup.

## Consequences

- Typed ModelWork remains blocked before execution even when RFC-0016 admits it.
- The preparation result can be the input to a later candidate-suitability contract but
  cannot be persisted or treated as reusable authorization.
- Existing prompt containment and model request validation remain the source of truth;
  the new boundary must share them rather than introduce divergent filesystem rules.
