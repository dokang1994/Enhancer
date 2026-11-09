# User continuation request on 2026-11-09 into RFC-0034 protected remote model route-option preparation implementation

Status: Accepted Decision

## Context

RFC-0034 is Accepted and its specification has been delivered to `origin/main`.
RFC-0033 is the uncomposed protected fixed-binding acquisition source, RFC-0030 owns
remote-candidate suitability proof creation, and RFC-0029 owns route-option and Router
semantics. No same-invocation consumer currently connects those three boundaries.

The user requested continuation on 2026-11-09. The completed delivery task names
RED-first RFC-0034 implementation as the next separately authorized product work while
keeping option aggregation, Router selection, provider construction, and execution
separate.

## Decision

Implement RFC-0034 RED-first as one final uncomposed preparer with a path-only public
construction boundary and package-private interaction seams. Each call will accept the
exact profiled request, exact RFC-0028 `Eligible`, and explicit evaluation time; derive
candidate and destination only from that eligibility; invoke RFC-0033 exactly once;
invoke RFC-0030 at most once with exact acquired facts and unchanged inputs; and create
one RFC-0029 `ModelRouteOption` only after suitability succeeds.

Add the accepted three-shape sealed result with exact upstream refusal retention,
lowercase binding-digest validation, and safe rendering. Narrow existing source and
architecture guards only to the exact preparer while preserving RFC-0031 as the sole
capability-facts creator, RFC-0030 as the sole suitability-proof creator, and zero
production caller for the preparer and `ModelRouter`.

This decision authorizes repository-local Java production and test changes,
architecture/source guards, owning project-document updates, checkpoint state, fresh
verification, bounded read-only development-subagent review, and ordinary local GREEN
commits required by repository policy. It does not authorize option-set aggregation,
Router invocation, a production preparer caller, provider or credential construction,
network or spend, execution, persistence, retry, fallback, runtime integration, MCP,
push, merge, tag, release, deployment, permission change, or another external effect.

## Rationale

The accepted contracts already own policy eligibility, protected acquisition,
suitability, and pure routing independently. A narrow same-invocation preparer connects
only their exact values without moving authority, reconstructing proofs, or introducing
selection policy. Closed staged refusals and binding provenance preserve the reason a
remote option was not prepared without exposing protected values.

## Consequences

- RFC-0034 may become Contract Verified only as an uncomposed one-option preparer.
- Every call reacquires and reevaluates; no cache, retry, fallback, or clock is added.
- The prepared option and binding digest remain invocation-local and are not persisted.
- A production caller, option aggregation, Router selection, and route execution each
  require later accepted work.
- No push, merge, release, deployment, permission change, or external effect is
  authorized.
