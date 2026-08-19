# User continuation request on 2026-08-19 into the provider-neutral model execution profile specification

Status: Accepted Decision

## Context

The RFC-0013 model-gateway minimum slice and its deterministic `model-invoke`
Scheduler execution path are Completed on `main`. The completed task records a
provider-neutral execution-profile contract as the next bounded Delivery Gate 9 slice,
and the user requested that work continue. The existing request budget stub does not
express the Roadmap's capability, locality, reasoning, context, token, cost, time, and
data-classification requirements, while routing and real provider execution remain
unauthorized.

## Decision

Authorize one documentation-only specification increment that authors and accepts
RFC-0014: Model Execution Profile. The RFC defines a bounded immutable versioned value
hierarchy over the existing gateway boundary, fixes its field meanings, units,
vocabularies, cross-field invariants, authority limits, compatibility with RFC-0013,
and immediate RED-first implementation consumer, and connects it to the Gate 9
architecture and RFC indexes.

The profile is requirement data only. It cannot select or invoke a provider, grant
network or remote-transmission authority, approve a paid service, release classified
data, supply a credential or endpoint, widen Tool scope, or override task, execution,
or future outbound policy. Verified GREEN completion authorizes one ordinary local
commit under Constitution 1.2.0. This decision authorizes no Java implementation,
signature or durable-schema change, routing, provider call, credential, paid service,
MCP, push, merge, release, deployment, permission change, or destructive cleanup.

## Rationale

The execution profile is the smallest next value boundary that lets later routing and
policy work state requirements without leaking provider vocabulary or inventing
authority. Specifying durable meanings before code prevents the current temporary
Scheduler capability/model-class reuse, response-character limits, usage units,
locality, classification, and cost from being conflated in an implementation.

## Consequences

- RFC-0014 extends RFC-0013 without rewriting its accepted minimum-slice contract.
- Capability and model class become distinct requirements; Tool authority remains in
  the approved task and execution policy.
- Locality, classification, and cost remain constraints awaiting later policy
  intersection, not authorization.
- Implementation remains a separate test-first task and capability maturity does not
  change from accepting this specification.
