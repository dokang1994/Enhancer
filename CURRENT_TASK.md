# Current Task

## Status

Completed

## Task

Implement RFC-0022 sequences 1 and 2 RED-first: the standalone deterministic-fake
exact-request budget preparation and the same-request/same-policy/same-candidate-
gateway invoker, without adding a production caller.

## Task ID

implement-deterministic-fake-exact-request-invocation-seam

## Context

RFC-0020/RFC-0021 provide the Contract Verified closed fake candidate, Unicode-scalar
counter, fixed capacities, and standalone suitability. RFC-0022 now specifies the
missing actual-request budget decision and exact-identity invocation seam. The
completed specification task names RED-first implementation of sequences 1 and 2 as
the sole next action, and the user requested continuation on 2026-09-03.

## Justified By

- User continuation request on 2026-09-03 into RFC-0022 exact-request seam implementation
- User continuation request on 2026-09-02 into exact-request model budget and invocation seam specification

## Approval

The user's 2026-09-03 continuation authorizes RFC-0022 sequences 1 and 2 as a
standalone RED-first Java/test implementation plus the minimal architecture, state,
task, verification, handoff, and Changelog synchronization and ordinary local GREEN
commits. It authorizes focused execution of the deterministic fake in tests only. It
authorizes no production caller or supported entry point, Scheduler/process execution,
ToolResult/evidence/verification/Model RunRecord writing, schema/runtime/finalizer/
retry/recovery wiring, typed ModelWork producer or receiver, provider/router/network,
credential or spend work, push, merge, release, deployment, permission change,
destructive cleanup, or external effect.

## Acceptance Criteria

- A field-free `DeterministicFakeExactRequestPreparation` accepts only exact
  `ModelCandidateSuitabilityDecision.Suitable` plus exact `ExecutionPolicy`, counts
  the retained prompt once, and applies RFC-0022's closed malformed/input/response-
  length/output/checked-total first-match order.
- A sealed opaque `DeterministicFakeExactRequestDecision` exposes private-construction
  final `Ready` and `Refused` variants with exact identity retention, complete derived
  counts where applicable, closed reasons, and non-revealing rendering.
- Reachable equality/one-over budget boundaries, combined precedence, supplementary
  Unicode, malformed-surrogate positions, checked arithmetic, the valid-budget total
  theorem, and zero gateway/Tool/evidence/runtime activity are covered RED-first.
- A field-free `DeterministicFakeExactRequestInvoker` accepts only evaluator-created
  `Ready`, rechecks the retained policy allowlist, strict timeout, and current
  cancellation in order, and invokes the exact candidate-bound gateway with the exact
  admitted request at most once.
- A sealed opaque invocation result retains exact identities and distinguishes
  untrusted success, closed pre-call refusal, and one-to-one `ModelFailureCode`
  failure without raw exception text or broad exception mapping.
- Reflection and source guards prove private construction, field-free services,
  non-revealing rendering, exact dependency and call shapes, unchanged fake rendering
  and generic `ModelUsage`, and zero production references outside the new definition
  types and focused tests.
- No production caller, Tool/evidence/RunRecord/schema/runtime/retry/recovery/provider/
  network/credential/spend path is added, and capability maturity advances only to the
  level supported by fresh evidence.
- Each sequential increment passes its declared focused tests and `git diff --check`,
  the full README-owned Java 17 regression passes before completion, canonical
  documents are synchronized, and every verified GREEN increment has a local commit.

## Out Of Scope

Production caller or supported entry point; Scheduler/process execution; ToolRequest,
ToolExecutor, ToolResult, evidence, response verification, Model RunRecord writing,
schema/runtime/finalizer/retry/recovery integration; typed ModelWork producer,
submission, receiver, or CLI; provider selection, router, endpoint, remote
transmission, network, credentials, pricing or spend; fake gateway rendering or generic
`ModelUsage` change; push, merge, release, deployment, permissions, destructive
cleanup, and external effects.

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

## Verification

Observable behavior is RED-first. Evidence is appended once per completed increment to
`docs/verification-log.md`. Subagent reports are recommendations, never verification.
Every GREEN claim requires the primary Agent to read the fresh focused result and
reconcile boundary/source/reflection tests plus `git diff --check`.

## Dynamic Workflow

Workflow ID: implement-deterministic-fake-exact-request-invocation-seam
Mode: Sequential
Increment Limit: 3
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on contract conflict, unclassified RED, identity ambiguity,
unclosed refusal or exception behavior, production-caller or lifecycle leakage,
unreachable-test fabrication, failed verification, checkpoint drift, new authority,
exhausted bounds, or unsafe recovery.

### Increment 1 - implement-exact-request-preparation

State: Completed
Depends On: none
Scope: Add RED-first exact-request preparation, opaque decision/reasons, exact identity
and count retention, ordered budget refusal, total-invariant proof, redacted rendering,
and preparation-specific boundary guards without any gateway call or caller.
Exit Criteria: All preparation behavior and structural constraints in RFC-0022 are
freshly GREEN, the focused diff is reviewed, evidence is appended, and the increment
is committed locally.
Verification: New preparation tests, existing token/candidate/profile/admission tests,
candidate locality/source guards, Markdown-sensitive task governance, and
`git diff --check`.
Next Action: Select Increment 2 and establish the invoker RED tests.

### Increment 2 - implement-exact-request-invoker

State: Completed
Depends On: implement-exact-request-preparation
Scope: Add RED-first field-free invoker and opaque invocation result/reasons with
ordered pre-call policy refusal, exact at-most-once fake invocation, untrusted success,
and code-only gateway failure, without a production caller.
Exit Criteria: All invoker behavior and structural constraints in RFC-0022 are freshly
GREEN, the focused diff is reviewed, evidence is appended, and the increment is
committed locally.
Verification: New invoker tests, preparation regression, fake gateway/candidate tests,
candidate locality/source guards, Markdown-sensitive task governance, and
`git diff --check`.
Next Action: Select Increment 3 and run the full regression.

### Increment 3 - verify-and-close-exact-request-implementation

State: Completed
Depends On: implement-exact-request-invoker
Scope: Run the full Markdown-sensitive Java 17 regression, record fresh evidence,
synchronize implementation maturity and canonical documents, close the task/handoff,
and commit the verified closure.
Exit Criteria: The full regression passes with results read, canonical documents are
current, the intended local commits are clean, and the checkpoint is stable and clear.
Verification: Full `.\scripts\gradle.ps1 test`, focused final governance,
diff/commit/status inspection, and checkpoint reconciliation.
Next Action: Await separate user authority for typed ModelWork process integration,
Tool/evidence/response validation, Model RunRecord v2 finalization, and recovery.

## Next

Await separate user authority for typed ModelWork process integration, Tool/evidence/
response validation, Model RunRecord v2 finalization, and recovery.
