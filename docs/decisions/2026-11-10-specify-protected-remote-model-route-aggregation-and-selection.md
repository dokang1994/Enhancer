# User continuation request on 2026-11-10 into protected remote model route aggregation and selection specification

Status: Accepted Decision

## Context

RFC-0034 is Contract Verified as an uncomposed one-option preparer and RFC-0029 is
Contract Verified as a pure Router over a bounded ordered option set. Both have no
production caller. No accepted contract owns how exact RFC-0034 results retain caller
order, how preparation refusal suppresses partial selection, how exact prepared options
enter the Router, or how selected binding provenance stays associated after selection.

The completed RFC-0034 task required separate authority before option aggregation or
Router invocation. The user requested continuation on 2026-11-10. A documentation-only
specification is the smallest dependency-safe next step because ordering, refusal
precedence, identity retention, provenance, and the exact pre-execution stopping point
must be accepted before Java or no-caller guards change.

## Decision

Specify RFC-0035 as one provider-neutral, field-free, same-invocation aggregation and
selection composition. It receives the exact profiled request, zero through 256 exact
RFC-0034 preparation results in caller-declared order, and one explicit evaluation
time. It validates complete collection shape, returns the first exact preparation
refusal by lowest ordinal without constructing an option set, and only for an all-
prepared collection builds one `ModelRouteOptionSet` from the exact option instances in
unchanged order.

The composition invokes the existing pure `ModelRouter` exactly once with the exact
request and unchanged time. It preserves an exact Router refusal, or preserves the
exact Router selection with the exact ordinal-zero RFC-0034 `Prepared` result so safe
binding provenance remains associated by identity. Empty input delegates to the
Router's existing `NO_ROUTE_OPTIONS` refusal. RFC-0029 continues to own all binding,
freshness, duplicate-route, whole-set, refusal-order, and selection semantics.

This decision authorizes the documentation-only RFC, architecture and owning project-
document updates, checkpoint state, fresh verification, bounded read-only development-
subagent review, and ordinary local GREEN commits required by repository policy. It
does not authorize Java implementation, production preparer or selection callers,
candidate-plan authority, route execution, providers, credentials, network, spend,
persistence, runtime integration, push, release, or deployment.

## Rationale

Consuming exact preparation results instead of raw options lets the composition refuse
an incomplete declared attempt rather than silently filter upstream failure into
implicit fallback. Delegating all prepared sets to the existing Router preserves one
owner for route validation and deterministic selection. Retaining the exact selected
preparation associates its already-redacted binding provenance without widening the
opaque selected route or a durable schema.

## Consequences

- Collection order remains explicit caller data, never newly derived authority.
- Lowest-ordinal preparation refusal suppresses option aggregation and Router activity.
- All-prepared results enter one exact ordered option set; the Router alone validates
  and selects or refuses it.
- The composition cannot prove an unauthorized caller's external candidate-plan
  completeness, so production caller construction remains separately accepted.
- Selection, option, order, or binding digest grants no provider, spend, transmission,
  execution, retry, fallback, persistence, or recovery authority.
- No Java implementation, production caller, capability promotion, push, release, or
  deployment is added.
