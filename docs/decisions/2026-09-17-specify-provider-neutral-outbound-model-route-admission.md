# User continuation request on 2026-09-17 into provider-neutral outbound model route-admission specification

Status: Accepted Decision

## Context

The completed Gate 9 baseline audit selected a provider-neutral default-deny outbound
route-admission contract as the next dependency before any second model candidate,
Router, provider adapter, credential access, paid invocation, network transmission, or
MCP work. RFC-0016 deliberately refuses `POLICY_CONSTRAINED` requests with
`OUTBOUND_POLICY_REQUIRED`, and the current deterministic-fake path proves only its
closed local route.

The user requested continuation on 2026-09-17. A specification is required before an
implementation can define how exact task and policy authority intersects independently
governed destination, content-handling, retention, classification, and pricing facts.

## Decision

Accept RFC-0028 as a documentation-only provider-neutral outbound model route-admission
contract. The boundary begins from the exact active task, execution policy, profiled
request, and authoritative capability used for fresh RFC-0016 evaluation. Every
RFC-0016 refusal remains terminal except the exact
`OUTBOUND_POLICY_REQUIRED` result, which is the sole guarded continuation into remote
policy evaluation and is never reinterpreted as remote authority.

One remote proposal must also carry independently governed, fresh candidate,
destination-policy, purpose, retention, complete model-visible content inspection and
redaction, classification, and pricing facts. Missing, stale, ambiguous, mismatched, or
unknown facts refuse in a closed deterministic order. The profile remains untrusted
requirements data, effective classification cannot be lowered by caller assertion, and
a cost ceiling remains distinct from spend authority. With no separate spend grant in
this RFC, any positive worst-case charge refuses.

The successful result is ephemeral eligibility for one exact proposal. It retains
identity and provenance but contains no credential, adapter, invocation method, spend
grant, or reusable token and cannot be persisted, cached, replayed, transmitted, or
treated as durable authority. A future pure Router is the first named consumer and may
select only among separately and freshly eligible proposals; provider construction,
credential acquisition, network activity, and invocation remain later contracts.

This decision authorizes only RFC, decision/index, Architecture/mirror, Roadmap RFC
tracking, task, verification, Changelog, and necessary handoff documentation; focused
and full verification; and an ordinary local GREEN commit. It authorizes no Java or
test-source implementation, external provider activity, push, merge, release, or
deployment.

## Rationale

Treating a profile, endpoint, candidate order, free-looking price, or redacted-looking
prompt as transmission authority would hide policy inside data. A fresh exact proposal
decision makes every authority and fact source explicit, preserves the current local
path, and creates a testable prerequisite for routing without connecting an external
effect.

## Consequences

- RFC-0028 is Accepted as a specification only; Gate 9 capability maturity does not
  change.
- The current deterministic-fake local execution path and RFC-0013 through RFC-0027
  contracts remain unchanged.
- A later pure implementation must be RED-first and must leave every production caller
  absent until separately authorized.
- Router/provider construction, scanners and redactors, pricing acquisition, spend
  authorization, credentials, network transmission, MCP, durable provenance, push,
  merge, release, and deployment remain separately authorized.
