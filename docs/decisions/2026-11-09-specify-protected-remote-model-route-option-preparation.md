# User continuation request on 2026-11-09 into protected remote model route-option preparation specification

Status: Accepted Decision

## Context

RFC-0033 is Contract Verified as an uncomposed read-only bridge from one protected
fixed binding to exact RFC-0031 capability facts. RFC-0030 can evaluate those facts
for one exact RFC-0028 outbound eligibility, and RFC-0029 can retain the resulting
suitability proof in one route option, but no accepted contract connects those three
boundaries in one invocation.

The completed RFC-0033 task required separate authority before adding that consumer.
The user requested continuation on 2026-11-09. A documentation-only specification is
the smallest dependency-safe next step because the consumer's result vocabulary,
provenance retention, refusal precedence, and exact stopping boundary must be accepted
before Java implementation changes existing no-caller guards.

## Decision

Specify RFC-0034 as one provider-neutral, uncomposed, same-invocation route-option
preparation boundary. Protected construction supplies the fixed RFC-0033 binding path
or source. Each prepare call accepts only the exact `ProfiledModelRequest`, exact
RFC-0028 `Eligible`, and explicit evaluation time; derives candidate and destination
selectors only from that eligibility; invokes RFC-0033 exactly once; invokes RFC-0030
at most once with the exact acquired facts and unchanged inputs; and constructs exactly
one RFC-0029 `ModelRouteOption` only after suitability success.

The closed result will distinguish exact acquisition refusal, exact suitability
refusal, and prepared option while retaining only safe invocation-local binding-digest
provenance. It will stop before option-set aggregation, `ModelRouter` selection,
provider construction, credentials, network transmission, spend, execution,
persistence, retry, fallback, cache, runtime, or MCP integration.

This decision authorizes the documentation-only RFC, architecture and owning project-
document updates, checkpoint state, fresh verification, and ordinary local GREEN
commits required by repository policy. It does not authorize Java implementation,
production construction, installed artifacts, publisher/evidence composition, route
selection or execution, provider or network effects, push, release, or deployment.

## Rationale

Keeping acquisition, suitability evaluation, and option construction in one bounded
invocation preserves exact identity and freshness without moving fact acquisition into
the pure evaluator or selection into the source. Specifying one option before an option
set prevents caller ordering and Router policy from entering through an unreviewed
composition.

## Consequences

- RFC-0033 refusal dominates suitability and suppresses evaluator activity.
- Successful acquisition passes the exact facts, request, eligibility, and evaluation
  time once to RFC-0030; successful suitability creates one exact RFC-0029 option.
- The prepared option and binding digest remain ephemeral and non-authoritative.
- Option aggregation, Router selection, route execution preparation, production
  construction, providers, credentials, network, spend, persistence, and MCP remain
  separately authorized.
- No Java implementation, capability promotion, push, release, or deployment is added.
