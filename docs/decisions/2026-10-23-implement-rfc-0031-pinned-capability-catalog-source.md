# User continuation request on 2026-10-23 into RFC-0031 pinned capability catalog source implementation

Status: Accepted Decision

## Context

RFC-0031 specifies the first provider-neutral production acquisition contract for
`RemoteModelCandidateCapabilityFacts`. Its completed specification task records a RED-
first source implementation as the dependency-ordered continuation, while production
still contains no capability-facts creator and no RFC-0030 evaluator caller. The user
requested continuation on 2026-10-23.

## Decision

Implement RFC-0031 RED-first as one final pinned-file source under `com.enhancer.model`
with its sealed acquisition result and closed refusal vocabulary. The implementation
will accept only an independently provisioned absolute normalized catalog path and
lowercase whole-file SHA-256 pin, defensively read one bounded exact-real no-link byte
snapshot, verify the pin before parsing, validate and re-encode the complete canonical
catalog, and resolve one exact candidate/destination record at an explicit evaluation
time.

The source may use the existing package-private RFC-0030 facts creator only after every
catalog record is valid. It must map every facts field without defaults or conversion,
derive source revision from the verified digest, reread on every acquisition, preserve
the RFC refusal order and safe rendering, and retain zero production acquisition
callers and zero RFC-0030 evaluator callers.

This decision authorizes the bounded Java and test implementation, source and
architecture guards, documentation synchronization, fresh focused and full Java 17
verification, bounded read-only development-subagent review, and ordinary local GREEN
commits required by repository policy. It does not authorize an installed catalog,
catalog writer or provisioner, production caller, RFC-0030 evaluation composition,
RFC-0029 option construction, provider or adapter, endpoint, credential, network,
transmission, spend, execution, persistence, retry, fallback, runtime, Scheduler,
RunRecord, MCP, push, merge, release, deployment, permission change, destructive
cleanup, or another external effect.

## Rationale

The accepted catalog contract is sufficiently closed for a small isolated filesystem
implementation with behavior observable through focused tests. Keeping the source in
the model package permits the existing package-private facts construction boundary
without widening public creation, while zero-caller and dependency guards prevent the
new I/O boundary from silently entering the pure evaluator, Router, or runtime.

## Consequences

- The source can acquire exact invocation-local RFC-0030 facts from an independently
  pinned canonical snapshot, but no production remote route becomes reachable.
- A matching digest proves exact operator-approved bytes only; truthful provisioning,
  installation, permissions, rotation, and anti-rollback remain separate.
- Missing, unsafe, changed, oversized, unpinned, invalid, noncanonical, unmatched, or
  stale acquisition fails closed with no partial facts.
- RFC-0014, RFC-0016, RFC-0028 through RFC-0030, RFC-0029 routing, and the deterministic-
  fake local path retain their existing ownership and behavior.
- No push, merge, release, deployment, or external state change is authorized.
