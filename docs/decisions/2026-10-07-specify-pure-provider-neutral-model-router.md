# User continuation request on 2026-10-07 into pure provider-neutral ModelRouter specification

Status: Accepted Decision

## Context

RFC-0028 is Contract Verified and deliberately has no production consumer. It names a
separately authorized pure `ModelRouter` as the first consumer of freshly eligible
outbound proposals, but the repository has no provider-neutral remote suitability
proof, bounded selection set, deterministic refusal algebra, or identity-preserving
selected-route value. RFC-0020 suitability remains deliberately bound to the one
closed deterministic fake and therefore cannot be reused for a remote proposal.

The user requested continuation on 2026-10-07. A specification is required before a
Router implementation or any governed candidate, policy, scanner, redactor, or pricing
source can be added.

## Decision

Accept RFC-0029 as a documentation-only pure provider-neutral `ModelRouter` contract.
The future Router receives one exact `ProfiledModelRequest` and a bounded ordered set
of options. Every option must retain one exact RFC-0028 `Eligible` instance and one
separately proven remote suitability instance bound to that exact eligibility, the
same request/profile, and the exact same candidate facts. The Router validates the whole set before selection,
refuses empty, duplicate, stale, or cross-bound input in a closed deterministic order,
and otherwise selects the first option in caller-declared order.

The selected result retains the exact request, option, eligibility, suitability,
candidate, destination, and ordinal identities. It is ephemeral selection only and
does not grant provider construction, credential access, transmission, spend,
invocation, persistence, retry, or fallback authority. Every later attempt or fallback
must acquire fresh RFC-0028 eligibility and fresh suitability for its exact option.

This decision authorizes only the RFC, decision/index, Architecture/mirror, Roadmap RFC
tracking, task, verification, Changelog, and necessary handoff documentation; focused
and full verification; and ordinary local GREEN commits required by repository rules.
It authorizes no Java or test-source implementation, governed fact source, provider or
network activity, push, merge, release, or deployment.

## Rationale

Selection is a pure ordering decision, not an authority source. Requiring exact
eligibility and separately proven suitability prevents a Router from rebuilding facts,
turning profile requirements into permission, or hiding policy in candidate order.
Validating the complete bounded set before choosing prevents malformed later entries
from becoming an implicit fallback channel.

## Consequences

- RFC-0029 is Accepted as a specification only; Gate 9 maturity does not change.
- RFC-0016, RFC-0020, the deterministic-fake path, and RFC-0028 remain unchanged.
- A later pure implementation must be RED-first and may introduce only the values,
  evaluator, and guards accepted by RFC-0029.
- Governed fact acquisition, provider construction, credentials, network transmission,
  spend, invocation, durable provenance, fallback execution, MCP, push, merge, release,
  and deployment remain separately authorized.
