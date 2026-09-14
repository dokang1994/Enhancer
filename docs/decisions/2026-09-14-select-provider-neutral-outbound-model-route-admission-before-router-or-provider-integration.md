# Select provider-neutral outbound model route admission before router or provider integration

Status: Accepted Decision

## Context

The post-RFC-0027 Gate 9 audit found a strong closed deterministic-fake path but no
provider-neutral route decision, Router, selected provider adapter, outbound data
policy, secret/redaction policy, provider usage/cost enforcement, or MCP path. The
package-private HTTP adapter is an intentionally inert wire-shape boundary and is not a
supported provider integration.

RFC-0016 already refuses `POLICY_CONSTRAINED` requests with
`OUTBOUND_POLICY_REQUIRED`. A Router or real provider connected before that missing
policy boundary would let candidate selection, an endpoint, or profile data become
implicit transmission authority.

## Decision

Select a specification-only provider-neutral outbound model route-admission boundary
as the next Gate 9 task. It precedes any second candidate, generic Router, provider
adapter construction, credential access, paid invocation, network transmission, or MCP
work.

The future specification must define one fresh default-deny decision over the exact
active task, execution policy, profiled request, and independently governed candidate,
destination, purpose, retention, data-classification, and pricing facts. Profile
classification remains untrusted requirement data rather than disclosure authority.
Sensitive-content inspection and redaction are prerequisites for any allowed remote
content. A cost ceiling is not spend authorization.

The result remains ephemeral eligibility. It cannot contain credentials, authorize
spend by itself, persist as reusable authority, widen Tool or task scope, select an
unapproved provider, or invoke a network adapter. The specification, its pure
implementation, Router composition, provider integration, credential provisioning,
paid use, and external verification remain separately authorized lifecycle steps.

## Rationale

The first missing functional scope is provider-neutral routing, but remote routing is
unsafe without an explicit outbound admission contract. Defining the narrowing policy
seam first preserves the Constitution's authority and least-privilege rules, keeps the
existing local deterministic fake valid, and provides a testable prerequisite for
later Router and provider work without performing an external effect.

## Rejected Alternatives

- Constructing `HttpMessageApiModelProviderAdapter` next is rejected because no
  destination, purpose, retention, classification, redaction, pricing, credential, or
  spend policy authorizes its use.
- Adding a generic Router or provider registry first is rejected because selection
  before outbound admission would hide authority inside candidate ordering.
- Starting MCP first is rejected because the Gate 9 model routing and outbound policy
  boundary is earlier in the accepted scope, while governed MCP resources and Tools
  require additional protocol-specific authority and verification contracts.

## Consequences

- Gate 9 whole-gate state does not change.
- The next task is to specify RFC-0028, not to implement it.
- RFC-0028 must name the exact future consumer and preserve the current fake-only local
  path, typed failures, provenance, evidence, verification, and runtime recovery.
- Provider/network access, credentials, paid services, Router/provider construction,
  MCP, production/test implementation, push, merge, release, deployment, and external
  effects remain separately authorized.
