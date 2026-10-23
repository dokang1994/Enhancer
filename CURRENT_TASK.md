# Current Task

## Status

In Progress

## Task

Implement RFC-0031 RED-first as the pinned, provider-neutral production acquisition
source for `RemoteModelCandidateCapabilityFacts`, while retaining zero production
source callers and zero RFC-0030 evaluator callers.

## Task ID

implement-rfc-0031-pinned-capability-catalog-source

## Context

RFC-0031 defines an independently provisioned absolute normalized catalog path and
whole-file lowercase SHA-256 pin, one bounded exact-real no-link snapshot, strict
canonical grammar, complete catalog validation, exact candidate/destination lookup,
complete RFC-0030 field mapping, explicit-time freshness, closed refusal precedence,
and safe rendering.

The dependency-ordered implementation boundary is the source alone. It may create
RFC-0030 capability facts within `com.enhancer.model`, but it must not acquire
eligibility, evaluate suitability, construct an RFC-0029 route option, or connect any
provider, credential, network, spend, execution, persistence, runtime, or MCP path.

## Justified By

- User continuation request on 2026-10-23 into RFC-0031 pinned capability catalog source implementation

## Approval

The user's continuation request authorizes the bounded RFC-0031 Java and test
implementation, source and architecture guards, owned document synchronization,
append-only verification evidence, bounded read-only development-subagent review,
fresh focused and full Java 17 verification, and ordinary local GREEN commits required
by repository policy.

This approval does not authorize an installed production catalog, catalog writer or
provisioner, rotation command, installation layout or permission change, signature
scheme, remote retrieval, production source caller, RFC-0030 evaluator caller,
RFC-0029 option consumer, provider or adapter, endpoint or protocol, credential,
secret, network, transmission, spend, billing, execution, Tool, persistence, retry,
fallback, runtime, Scheduler, RunRecord, durable schema or migration, MCP, plugin,
interface, background work, push, merge, release, deployment, destructive cleanup, or
another external effect.

## Acceptance Criteria

- One Accepted Decision and exact index entry record the implementation authority.
- A final pinned-file source exposes only an absolute normalized path and lowercase
  whole-file SHA-256 pin at construction and exact candidate identity, destination
  identity, and evaluation time at acquisition.
- A sealed two-shape result and closed ten-reason refusal enum preserve RFC-0031 order,
  programming-error boundaries, no-partial-facts behavior, and safe rendering.
- Each acquisition reads one fresh 1-byte-through-4-MiB exact-real no-link regular-file
  snapshot, detects size drift, verifies the pin before parsing, and uses the same bytes
  for strict UTF-8 parsing, full validation, canonical re-encoding, lookup, and mapping.
- The complete v1 catalog grammar, zero-through-256 contiguous records, canonical
  integers and instants, fixed field order, exact enums/constants, record ordering and
  uniqueness, every repository bound, and every cross-field relationship are enforced
  before exact pair lookup.
- Exact lookup, inclusive observation/exclusive expiry, source revision from the
  verified pin, complete RFC-0030 field mapping, reread-on-every-acquisition, and
  separately constructed rotation behavior are covered at boundaries.
- Source, construction, dependency, locality, reflection, no-ambient-input, and zero-
  production-caller guards prevent evaluator/Router/runtime/provider coupling and keep
  the deterministic fake unchanged.
- Fresh focused RED/GREEN evidence, applicable compatibility tests, the full README-
  owned Java 17 regression, and `git diff --check` pass; owned documents are current;
  and every verified GREEN increment is committed locally without push.

## Out Of Scope

An installed catalog, catalog writer, provisioner, rotation command, installation
layout, permissions, signature verification, anti-rollback anchor, remote retrieval,
discovery, registry, cache, fallback, production caller, evaluator composition, route-
option construction, provider, adapter, provider model, endpoint, protocol, credential,
secret, network, transmission, spend, billing, execution, response handling,
availability, quality, latency, score, tokenizer, Tool, evidence, RunRecord, runtime
event, Scheduler, finalization, recovery, persistence of invocation-local facts or
proofs, retry execution, durable schema or migration, MCP, plugin, interface,
background work, push, merge, release, deployment, permission change, destructive
cleanup, and unrelated external effects.

## Allowed Tools

- read-file
- write-docs
- write-code
- write-tests
- build-output
- verify
- checkpoint
- git-inspect
- git-stage
- git-commit
- development-subagents-read-only

## Verification

Use the README-owned Java 17 Gradle command. Establish each observable boundary under
focused RED tests, classify the failure against RFC-0031 and existing contracts, add
the minimum implementation, and rerun focused GREEN verification. Cover RFC-0014,
RFC-0016, RFC-0028 through RFC-0031, deterministic-fake compatibility, filesystem and
architecture guards, then run the full Markdown-sensitive regression on the exact
final tree. Read fresh output and generated JUnit XML before lifecycle claims.
Subagent reports are recommendations and never verification evidence.

## Dynamic Workflow

Workflow ID: implement-rfc-0031-pinned-capability-catalog-source
Mode: Sequential
Increment Limit: 4
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on failed verification, task or checkpoint drift, conflict with
RFC-0014/RFC-0016/RFC-0028/RFC-0029/RFC-0030/RFC-0031, inability to preserve the
independent pin boundary, need for a production caller or external authority, unsafe
recovery, or scope expansion.

### Increment 1 - establish-source-result-contract

State: Completed
Depends On: none
Scope: Add missing-symbol RED tests for the closed refusal enum, sealed acquisition
result, and final source construction/acquisition surface; implement only those values
and programming-input validation needed to turn that contract GREEN.
Exit Criteria: Public shape, closed reasons/order, null and malformed programming
inputs, safe rendering, finality, and field boundaries are GREEN; evidence is appended
once; and the increment is committed locally.
Verification: Focused value/reflection tests, decision/index/document ownership,
dynamic-workflow governance, and `git diff --check`.
Next Action: Implement the defensive snapshot and complete canonical parser in
Increment 2.

### Increment 2 - implement-defensive-catalog-snapshot

State: Completed
Depends On: establish-source-result-contract
Scope: Add RED tests and implement exact-real/no-link bounded single-snapshot reading,
pin verification, strict UTF-8 decoding, the complete canonical v1 parser/re-encoder,
whole-catalog structural and semantic validation, and refusal reasons one through
eight.
Exit Criteria: Snapshot safety, drift, size, pin, UTF-8, schema, canonicality, content,
record-count/order/uniqueness, and invalid-unselected-record cases are GREEN; evidence
is appended once; and the increment is committed locally.
Verification: Focused filesystem/parser/security tests, relevant pinned-file precedent
tests, architecture guards, and `git diff --check`.
Next Action: Implement exact lookup, mapping, freshness, rotation, and production guards
in Increment 3.

### Increment 3 - complete-lookup-mapping-and-guards

State: Completed
Depends On: implement-defensive-catalog-snapshot
Scope: Add RED tests and implement exact pair lookup, complete facts mapping, explicit-
time freshness, reread and rotation behavior, safe acquired rendering, and construction/
dependency/locality/no-ambient-input/zero-caller guards.
Exit Criteria: Refusals nine and ten, all field/boundary mappings, freshness equality,
reread/rotation, safe rendering, RFC-0030 compatibility, and zero production caller
constraints are GREEN; evidence is appended once; and the increment is committed
locally.
Verification: Focused RFC-0031 behavior and boundary tests plus RFC-0014/RFC-0016/RFC-
0028/RFC-0029/RFC-0030/deterministic-fake compatibility and `git diff --check`.
Next Action: Synchronize owned documents and run full exact-tree regression in
Increment 4.

### Increment 4 - verify-and-close-source-implementation

State: In Progress
Depends On: complete-lookup-mapping-and-guards
Scope: Reconcile Architecture and compact mirror, current implementation state,
Roadmap, Changelog, task and session-only handoff if needed; run the full Markdown-
sensitive Java 17 regression; review the exact diff and Git state; and close with the
required local GREEN commit.
Exit Criteria: Fresh full regression and diff checks are GREEN; source implementation
and known absences are recorded by their owning documents; the task is Completed; Git
is clean; required commits exist; and the checkpoint is stable and clear.
Verification: Full README-owned test command, generated JUnit XML counts, `git diff
--check`, diff/status/log review, commits, and checkpoint reconciliation.
Next Action: Await separate authority for the same-invocation RFC-0030 evaluator and
RFC-0029 option-construction consumer; do not connect provider, credential, network,
spend, execution, persistence, runtime, or MCP.

## Next

Synchronize implementation state and delivery history, then run the full README-owned
Markdown-sensitive regression and close Increment 4.
