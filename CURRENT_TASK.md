# Current Task

## Status

Completed

## Task

Implement RFC-0029 RED-first as the first pure provider-neutral `ModelRouter`, without
adding governed fact sources, a provider, adapter, credential, network, spend,
persistence, runtime, or MCP path.

## Task ID

implement-rfc-0029-pure-provider-neutral-model-router

## Context

RFC-0028 is Contract Verified and RFC-0029 is Accepted. The completed specification
task names a separately authorized pure implementation as the next work. The user
requested continuation on 2026-10-07, which authorizes that bounded implementation.
The implementation must preserve exact RFC-0028 eligibility, add only invocation-local
remote-suitability and route-selection values, validate the entire bounded ordered set,
and keep every production caller and external effect absent.

## Justified By

- Select provider-neutral outbound model route admission before router or provider integration
- User continuation request on 2026-10-07 into RFC-0029 pure ModelRouter implementation

## Approval

The user's 2026-10-07 continuation request authorizes the RED-first pure RFC-0029 Java
values, field-free Router evaluator, focused tests, source/locality/dependency/reflection
guards, the exact RFC-0028 no-consumer-guard narrowing needed for this Router, necessary
Architecture/mirror/state/roadmap/task/evidence/Changelog/handoff synchronization,
fresh README-owned verification, and ordinary local commits at verified GREEN increment
boundaries.

This approval does not authorize a governed candidate, destination, inspection,
redaction, pricing, token, capacity, or suitability fact source; discovery or registry;
provider or adapter construction; endpoint, protocol, credential, secret, network,
transmission, spend, or paid use; Tool execution; Scheduler/runtime/RunRecord/schema
integration; persistence, retry, fallback execution, MCP, push, merge, release,
deployment, permission change, destructive cleanup, or another external effect.

## Acceptance Criteria

- One accepted decision records the bounded implementation authority, with an exact
  matching `DECISION_LOG.md` entry.
- RED-first tests define the opaque remote-suitability proof, immutable route option,
  zero-through-256 defensive option set, opaque selected route, and sealed decision.
- The field-free Router validates the complete set in RFC-0029's closed order, rejects
  every exact-instance, content, profile, candidate, destination, freshness, and
  duplicate mismatch, and selects only ordinal zero after all options pass.
- Zero, one, 256, and 257 options, defensive copying, immutable order, strict freshness,
  equal-but-not-identical substitutions, later malformed entries, duplicate positions,
  exact identity retention, and safe rendering are covered.
- Source, locality, dependency, and reflection guards prove zero infrastructure or
  production-caller coupling; the RFC-0028 consumer guard permits only the exact pure
  Router dependency.
- RFC-0016, RFC-0020, RFC-0028, and the deterministic-fake path remain unchanged.
- Fresh focused and full Java 17 verification passes, owned documents are synchronized,
  the task is completed, required local GREEN commits exist, and no push occurs.

## Out Of Scope

Changes to RFC-0013 through RFC-0028 behavior; production fact sources, registries,
discovery, scanners/redactors, pricing or capacity acquisition, provider/adapter/model/
endpoint/protocol/network/credential/secret/spend paths; request transmission, response
validation, fallback/cache execution, Tool, Scheduler, runtime, RunRecord, durable
schema, migration, MCP, capability promotion, push, merge, release, deployment,
permission change, destructive cleanup, and unrelated external effects.

## Allowed Tools

- read-file
- write-code
- write-tests
- write-docs
- build-output
- verify
- checkpoint
- git-inspect
- git-stage
- git-commit
- development-subagents-read-only

## Verification

Use the README-owned Java 17 wrapper. Run focused RFC-0029 value/Router tests and
architecture/source/locality/dependency/reflection guards for each increment, plus
relevant RFC-0014, RFC-0016, RFC-0020, RFC-0028, and deterministic-fake regressions.
Run the full Markdown-sensitive `powershell -ExecutionPolicy Bypass -File
.\scripts\gradle.ps1 test` command and `git diff --check` before closure. Read fresh
output before lifecycle claims. Subagent reports are recommendations and never
verification evidence.

## Dynamic Workflow

Workflow ID: implement-rfc-0029-pure-provider-neutral-model-router
Mode: Sequential
Increment Limit: 4
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on failed verification, task or checkpoint drift, an RFC-0016,
RFC-0020, RFC-0028, or RFC-0029 compatibility conflict, a need for governed production
facts or external authority, unsafe recovery, or scope expansion.

### Increment 1 - define-pure-router-values

State: Completed
Depends On: none
Scope: Record the accepted implementation decision, add missing-symbol RED tests for
the RFC-0029 value layer, and minimally implement the opaque remote-suitability proof,
route option, bounded option set, selected-route value, refusal reasons, and sealed
decision without a production caller.
Exit Criteria: The decision/index pair exists; RED is classified against RFC-0029;
value tests are GREEN; exact identities, bounds, defensive copying, immutability, and
safe rendering are proven; focused regressions pass; evidence is appended once; and the
increment is committed locally.
Verification: Focused value, RFC-0028 boundary, decision/index/document-governance,
Architecture mirror, and `git diff --check` tests.
Next Action: Select Increment 2 only after reading fresh Increment 1 evidence.

### Increment 2 - implement-field-free-router

State: Completed
Depends On: define-pure-router-values
Scope: Add RED-first Router behavior tests and the minimal field-free evaluator for
whole-set validation, closed refusal precedence, strict freshness, duplicate refusal,
and ordinal-zero selection.
Exit Criteria: Every decision shape and refusal reason is covered alone and in combined
precedence cases; later invalid options prevent selection; exact instances are retained;
focused model regressions pass; evidence is appended once; and the increment is
committed locally.
Verification: Focused Router plus RFC-0014, RFC-0016, RFC-0020, RFC-0028, and
deterministic-fake tests and `git diff --check`.
Next Action: Select Increment 3 only after reading fresh Increment 2 evidence.

### Increment 3 - guard-pure-router-boundary

State: Completed
Depends On: implement-field-free-router
Scope: Add safe-rendering, reflection, source, locality, dependency, and no-production-
caller guards; narrow the RFC-0028 no-consumer guard only for the exact Router; and
reconcile Architecture/mirror wording with the accepted zero-through-256 contract.
Exit Criteria: Guards prove the Router is field-free and pure, suitability and selected
route remain opaque and invocation-local, no other production caller exists, canonical
architecture matches RFC-0029, focused governance tests pass, evidence is appended
once, and the increment is committed locally.
Verification: Focused architecture/source/locality/dependency/reflection/document tests,
relevant model regressions, and `git diff --check`.
Next Action: Select Increment 4 only after reading fresh Increment 3 evidence.

### Increment 4 - verify-and-close-pure-router

State: Completed
Depends On: guard-pure-router-boundary
Scope: Run the full Markdown-sensitive Java 17 regression on the exact final tree,
review the diff, synchronize maturity/task/roadmap/handoff/delivery documents, close the
task, and create the final required ordinary local GREEN commit if closure changes
remain.
Exit Criteria: Fresh full regression and diff checks are GREEN; owned documents are
current; the task is Completed; required local commits exist; Git is clean; and the
checkpoint is stable and clear.
Verification: Full README-owned test command, generated JUnit XML counts,
`git diff --check`, diff/status/log review, commits, and checkpoint reconciliation.
Next Action: Await separate authority for governed remote suitability fact acquisition
or a later Router consumer; do not add a provider, network path, persistence, or push.

## Next

After this pure implementation task is completed, await separate user authority for a
governed remote-suitability evaluator and its evidence sources or another explicitly
selected Gate 9 boundary. Every production caller, provider, credential, network,
spend, persistence, fallback, MCP, release, and deployment path remains separate later
work.
