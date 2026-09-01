# User continuation request on 2026-09-01 into deterministic fake token semantics and capacity specification

Status: Accepted Decision

## Context

RFC-0020's implemented candidate evaluator intentionally stops at
`TOKEN_SEMANTICS_UNAVAILABLE`. The current fake's `ModelUsage` values and safety
bounds use Java-string lengths, so they do not establish a provider token contract or
truthful numeric candidate capacities. The completed task records a separate
token-semantics and capacity specification as the next work, and the user requested
continuation on 2026-09-01.

## Decision

Authorize a documentation-only RFC defining one versioned deterministic-fake token
unit, exact malformed-Unicode behavior, exact response-count algebra, proven context/
input/output/total capacities, candidate identity versioning, later suitability
ordering, and the distinction between profile suitability and actual request budget
validation.

The accepted contract may describe a later RED-first standalone implementation that
makes the pure evaluator's `Suitable` branch reachable with no caller. It must stop
before actual request-budget enforcement and the separately accepted same-request/
same-policy/same-gateway invocation seam.

This decision authorizes no Java or test-source change, candidate/evaluator/gateway/
Tool behavior change, caller, invocation, RunRecord or schema write, runtime/process/
worker/finalizer/recovery wiring, provider, route, network, credential, paid service,
spend, push, merge, release, deployment, permission change, external effect, or
destructive cleanup.

## Consequences

- The fake token unit and all four capacity values can be reviewed and implemented
  later without relabeling generic `ModelUsage` or inventing provider evidence.
- The token-aware candidate must use a new stable identity rather than changing
  `deterministic-fake-v1` in place.
- Candidate/profile suitability remains separate from concrete request counting and
  grants no invocation authority.
- The current fail-closed implementation and every typed ModelWork execution guard
  remain unchanged until separately approved implementation and integration tasks.
