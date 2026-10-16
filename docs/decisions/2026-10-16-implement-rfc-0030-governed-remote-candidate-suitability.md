# User continuation request on 2026-10-16 into RFC-0030 governed remote candidate suitability implementation

Status: Accepted Decision

## Context

RFC-0028 and RFC-0029 are Contract Verified, RFC-0030 is Accepted, and the completed
specification and delivery tasks record a RED-first pure capability-facts and
suitability-evaluator implementation as the singular next product work. The repository
still has no governed remote capability-facts value, closed RFC-0030 decision, or pure
evaluator that can create the existing RFC-0029 suitability proof. The user requested
continuation on 2026-10-16.

## Decision

Implement only the pure RFC-0030 definition and evaluation boundary. Add the opaque
immutable `RemoteModelCandidateCapabilityFacts`, closed eighteen-reason refusal enum,
sealed two-shape decision, and final field-free evaluator under missing-symbol RED
tests. The evaluator must accept only the exact profiled request, exact RFC-0028
`Eligible`, capability facts, and explicit evaluation time; apply the RFC's first-match
binding, freshness, capacity, timeout, classification, pricing, currency, and cost
rules; and create the existing RFC-0029 proof only after every check succeeds.

Restrict RFC-0029 proof construction to this evaluator success path and add source,
locality, dependency, reflection, construction, rendering, and zero-production-caller
guards. This decision authorizes necessary canonical-document synchronization, fresh
focused and full verification, bounded read-only development-subagent analysis, and
ordinary local GREEN commits required by repository policy.

This decision does not authorize a production fact source or caller, discovery,
registry, provider or adapter construction, endpoint, protocol, credential, network
transmission, spend, persistence, retry, fallback execution, runtime or Scheduler
integration, durable schema, MCP, push, merge, release, deployment, permission change,
destructive cleanup, or another external effect.

## Rationale

RFC-0028 proves outbound policy eligibility and RFC-0029 selects only among already
eligible and suitable options, but neither proves remote capacity. The accepted RFC-
0030 contract supplies that missing pure evidence boundary while keeping acquisition
and execution authority outside the evaluator. Closed ordering and exact identity
retention make combined failures deterministic and prevent reconstructed facts from
silently widening a route.

## Consequences

- RFC-0030 may reach Contract Verified only for its pure values, evaluator, and guards.
- RFC-0016, RFC-0020 through RFC-0029, and the deterministic-fake local path remain
  unchanged.
- Test-owned package fixtures may construct capability facts; no production fact source
  or evaluator caller is introduced.
- Every governed production source and route-option or execution consumer requires a
  later accepted task and fresh verification.
- No push, merge, release, deployment, permission change, or external effect is
  authorized.
