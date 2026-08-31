# User continuation request on 2026-08-31 into local model candidate suitability specification

Status: Accepted Decision

## Context

RFC-0019 request/policy/admission preparation is Contract Verified and stops before
candidate suitability. The only currently executed gateway is the deterministic fake,
but no accepted contract binds its exact instance to non-forgeable local provenance or
evaluates retained profile requirements. The fake also has character-based usage only,
not token semantics or proven token capacities. The user requested continuation on
2026-08-31.

## Decision

Authorize a documentation-only task to define and accept RFC-0020 as the minimum
closed deterministic-fake candidate and suitability boundary. It must bind the exact
final fake gateway instance through repository-owned facts, retain exact admitted and
candidate identity, evaluate requirements in deterministic first-match order, and fail
closed at `TOKEN_SEMANTICS_UNAVAILABLE` rather than invent context or token capacity.

This decision does not authorize Java implementation, tokenization, candidate
selection, Tool or gateway invocation, Model RunRecord writing, runtime or process
wiring, provider or route selection, network or remote transmission, credentials,
spend, push, merge, release, deployment, permission change, or destructive cleanup.

## Consequences

- A later RED-first implementation can prove one exact local fake binding while every
  typed ModelWork execution guard remains unchanged.
- No suitable path may become reachable until a separately accepted deterministic token
  semantics and capacity contract supplies truthful evidence.
- The current `ModelInvokeTool` cannot be reused as the later exact invocation seam
  because it reconstructs `ModelRequest` and owns a generic constructor-bound gateway;
  that integration remains separate work.
