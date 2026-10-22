# User continuation request on 2026-10-22 into governed remote capability-fact source specification

Status: Accepted Decision

## Context

RFC-0030 is Contract Verified for its opaque remote capability facts and pure
suitability evaluator, but production contains no creator of those facts and no caller
of the evaluator. The completed delivery task records governed production capability-
fact acquisition before a same-invocation RFC-0029 route-option consumer as the next
dependency-ordered Gate 9 boundary. The user requested continuation on 2026-10-22.

## Decision

Specify RFC-0031 as the first production-capable, provider-neutral acquisition boundary
for `RemoteModelCandidateCapabilityFacts`. The source will consume one independently
provisioned absolute canonical catalog path and whole-file SHA-256 pin as trusted
construction inputs, read one bounded no-link strict-UTF-8 canonical snapshot, and
point-resolve an exact candidate/destination record without discovery or fallback.

The specification must define the catalog authority, provenance, record cardinality
and ordering, exact field mapping, trust and refresh model, closed acquisition outcome
and refusal order, safe rendering, and the later same-invocation RFC-0030 evaluator /
RFC-0029 route-option consumer. Request, eligibility, provider labels, repository
content, environment, working directory, CLI fields, and ambient state cannot supply
or override the catalog trust inputs or capability values.

This decision authorizes documentation, bounded read-only subagent review, fresh
governance and full regression verification, and ordinary local GREEN commits required
by repository policy. It does not authorize Java implementation, a production caller,
provider or adapter construction, endpoint or protocol access, credentials, network
transmission, spend, persistence of invocation-local facts or proofs, route execution,
runtime or Scheduler integration, durable schema, MCP, push, merge, release,
deployment, permission change, destructive cleanup, or another external effect.

## Rationale

A pinned canonical local catalog gives the first source an explicit operator-owned
trust root and deterministic bounded acquisition without treating request data or a
provider response as independent evidence. Keeping acquisition separate from the pure
evaluator and Router preserves their no-I/O contracts and leaves provider execution,
availability, quality, latency, credentials, spend, and transmission to later
boundaries.

## Consequences

- RFC-0031 may specify one production-capable source contract, but no remote route
  becomes reachable until separate implementation and consumer tasks are accepted.
- Catalog rotation and provisioning require a later trusted composition; the source
  cannot discover or self-authorize a replacement pin.
- A missing, indirect, oversized, noncanonical, corrupt, unpinned, unsupported, or
  nonmatching snapshot fails closed with no capability facts.
- RFC-0014, RFC-0016, RFC-0028 through RFC-0030, and the deterministic-fake local path
  retain their current ownership and behavior.
- No capability maturity changes and no push, merge, release, or deployment is
  authorized.
