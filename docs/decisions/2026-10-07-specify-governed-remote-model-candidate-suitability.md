# User continuation request on 2026-10-07 into governed remote model candidate suitability specification

Status: Accepted Decision

## Context

RFC-0028 outbound eligibility and RFC-0029 pure route selection are Contract Verified,
but the opaque `RemoteModelCandidateSuitability` proof still has no accepted evidence
contract or pure evaluator. Its package-local construction remains reachable only by
test fixtures, so no production remote route can become suitable. The user requested
continuation on 2026-10-07.

## Decision

Specify RFC-0030 as the provider-neutral contract for independently governed remote
candidate capability facts and the field-free evaluator that may create the existing
RFC-0029 suitability proof. The evaluator must accept only one exact profiled request,
one exact RFC-0028 `Eligible`, one immutable capability-facts value, and an explicit
evaluation time. It must validate exact content and route bindings, strict freshness,
model and capability agreement, token semantics and capacities, supported timeout,
classification, and exact-schedule cost evidence in a closed deterministic order.

The specification must keep facts, evaluation, route selection, and execution
preparation as separate boundaries. It may define source obligations and a later pure
implementation surface, but it adds no Java, production source, provider, adapter,
credential, network, transmission, spend, persistence, retry, fallback, runtime, MCP,
release, or deployment authority.

## Rationale

RFC-0029 intentionally refuses to infer suitability from provider names, catalog rows,
availability flags, or RFC-0028 policy eligibility. A separate evidence contract makes
every profile requirement and price relationship explicit while preserving the
Router's single responsibility: select only among options already proven eligible and
suitable. Keeping the evaluator pure also allows RED-first contract tests before any
external fact acquisition is approved.

## Consequences

- RFC-0030 may define the first creator of `RemoteModelCandidateSuitability`, but the
  proof remains invocation-local and useful only through RFC-0029's full revalidation.
- Candidate capability evidence must be independently sourced, versioned, fresh, bound
  to the exact route and price schedule, and incapable of carrying provider authority.
- RFC-0014 requirements, RFC-0028 eligibility, RFC-0029 routing, and the RFC-0020 local
  deterministic-fake path retain their existing ownership and behavior.
- A later accepted task is required for the pure value/evaluator implementation, and
  another later task is required for every production fact source or execution caller.
- No capability maturity changes and no push, merge, release, or deployment is
  authorized by this decision.
