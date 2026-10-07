# User continuation request on 2026-10-07 into RFC-0029 pure ModelRouter implementation

Status: Accepted Decision

## Context

RFC-0028 is Contract Verified, RFC-0029 is Accepted, and the completed Router
specification task records a RED-first pure implementation as the next bounded work.
The repository still has no remote-suitability proof, route-option set, selection
decision, selected-route value, or Router implementation. The user requested
continuation on 2026-10-07.

## Decision

Implement only the pure RFC-0029 value and selection boundary. Add the opaque
invocation-local remote-suitability proof, immutable route option, zero-through-256
ordered option set, sealed selection decision, opaque selected route, and field-free
Router under missing-symbol RED tests. The Router must validate the complete option set
in the RFC's reason-first order, preserve exact object identities, refuse empty, stale,
cross-bound, or duplicate input, and select ordinal zero only after every option passes.

Narrow the RFC-0028 production-consumer guard only for this exact pure Router surface
and add source, locality, dependency, reflection, rendering, and no-caller guards. This
decision authorizes necessary canonical-document synchronization, fresh focused and
full verification, and ordinary local GREEN commits required by repository policy.

This decision does not authorize governed fact acquisition, a remote-suitability
evaluator or production proof source, discovery, a registry, provider or adapter
construction, credentials, network transmission, spend, persistence, retry, fallback
execution, runtime or Scheduler integration, MCP, push, merge, release, deployment, or
another external effect.

## Rationale

The accepted RFC already separates policy eligibility, candidate suitability, and
selection. Implementing only the pure boundary makes those bindings executable without
granting the Router authority to manufacture facts, choose an unproven route, invoke a
provider, or persist expiring authority. Whole-set validation prevents caller order
from hiding malformed fallback options.

## Consequences

- RFC-0029 may reach Contract Verified only for its pure values, evaluator, and guards.
- RFC-0016, RFC-0020, RFC-0028, and the deterministic-fake local path remain unchanged.
- Test-owned package fixtures may construct opaque facts; no production fact source or
  Router caller is introduced.
- Every governed fact source and every route-execution consumer requires a later
  accepted task and fresh verification.
- No push, merge, release, deployment, permission change, or external effect is
  authorized.
