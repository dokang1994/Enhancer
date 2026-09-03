# Current Task

## Status

Completed

## Task

Implement RFC-0023 RED-first as the internal deterministic-fake typed ModelWork
process-execution connection, while keeping every Model RunRecord v2 writer unreachable
until all named v2 consumers and recovery readers are installed.

## Task ID

implement-typed-model-work-process-execution

## Context

RFC-0018 through RFC-0022 are Contract Verified at their current boundaries. RFC-0023
is the accepted connection contract for child-local exact preparation, candidate and
request identity, one invocation, ToolResult/evidence materialization, independent
verification, v2-only lifecycle publication, parent validation, durable finalization,
retry, and crash recovery. The completed specification task names this RED-first
implementation as the next work, and the user requested continuation on 2026-09-03.

## Justified By

- User continuation request on 2026-09-03 into RFC-0023 typed ModelWork process-execution implementation
- User continuation request on 2026-09-03 into typed ModelWork process-execution specification
- User continuation request on 2026-09-03 into RFC-0022 exact-request seam implementation

## Approval

The user's 2026-09-03 continuation authorizes the minimum RED-first Java/test
implementation of RFC-0023's internal deterministic-fake process connection, including
the additive pure/lazy evidence-run boundary, child-local composition, returned-outcome
Tool/evidence and verification path, v2-only lifecycle publication, payload-kind-aware
child/parent/finalizer/worker/status/recovery consumers, and one internal integration
fixture. It authorizes architecture, state, Roadmap, task, decision/index, verification,
handoff, and Changelog synchronization and ordinary local GREEN commits.

It authorizes no typed ModelWork producer, submission or receiver change, supported
CLI/API/runtime-event ingress, general model router or provider, endpoint, remote
transmission, network, credentials, pricing or spend, MCP, durable schema version or
migration, push, merge, release, deployment, permission change, destructive cleanup,
or external effect. Focused tests may invoke only the deterministic fake inside
temporary test-owned storage and process boundaries.

## Acceptance Criteria

- A pure canonical Goal/AgentRun-bound evidence-run identity and exact contained lazy
  namespace operation preserve zero evidence-store activity on every pre-call refusal
  while supporting exact replay and long-output persistence.
- One child-local composition preserves the exact RFC-0019 task/request/policy/admission
  chain through the exact RFC-0020/0021 candidate and RFC-0022 `Ready`/invocation result,
  with no second prompt read, reconstructed authority, or same-AgentRun invocation retry.
- Only returned `Succeeded` or `GatewayFailed` outcomes reach a one-shot same-policy
  result-materialization Tool; response structure, code-only failures, evidence ordering,
  independent digest verification, lifecycle mapping, and sensitive-data redaction match
  RFC-0023.
- A model-specific lifecycle boundary persists only one complete Model RunRecord v2 at
  the deterministic Goal/AgentRun identity before publishing a child Result and never
  persists or projects RunRecord v1 for typed work.
- Child and parent paths select by payload kind and validate the exact Work, Result,
  record, task, capability, profile, request, policy, evidence, identity, and status
  closure. A valid v2 record prevents launch even beside a timeout fact; missing,
  cross-kind, corrupt, foreign, changed, or partial prefixes fail as specified.
- `DurableAgentRunWorker`, durable finalization, retry, and Scheduler status/recovery
  readers are explicitly v2-aware before writer reachability while existing v1 behavior
  and every durable byte format remain unchanged.
- The internal process integration proves verified completion, failed-result retry,
  replacement-AgentRun fresh preparation, pre-reference at-least-once and post-reference
  no-invocation recovery without adding a producer, receiver, or supported entry point.
- Observable behavior is RED-first, each selected increment passes its focused Java 17
  verification and `git diff --check`, the full README-owned regression passes before
  completion, documents are synchronized, and every GREEN increment is committed locally.

## Out Of Scope

Typed ModelWork producer, submission, receiver, CLI, API, supported entry point, or
runtime-event ingress; general/provider model selection, router, registry, endpoint,
remote transmission, network, credentials, pricing or spend; MCP; new durable schema
version or migration; changing RunRecord v1 bytes or legacy read-file behavior; adding
candidate/count/Ready/response-usage/refusal provenance to v2; durable terminal pre-call
refusal; automatic tight retry; push, merge, release, deployment, permissions,
destructive cleanup, and external effects.

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

Observable implementation behavior is RED-first. Evidence is appended once per
completed increment to `docs/verification-log.md`. Each RED must be classified against
RFC-0023 and existing v1 compatibility before production code changes. Subagent reports
are recommendations, never verification evidence.

## Dynamic Workflow

Workflow ID: implement-typed-model-work-process-execution
Mode: Sequential
Increment Limit: 6
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on contract conflict, unclassified RED, identity or containment
ambiguity, raw diagnostic leakage, v1 compatibility failure, reachable v2 writer before
all readers, unclosed crash/retry prefix, silent schema widening, failed verification,
checkpoint drift, new authority, exhausted bounds, or unsafe recovery.

### Increment 1 - add-lazy-evidence-run-boundary

State: Completed
Depends On: none
Scope: Add RED-first pure canonical Goal/AgentRun-bound evidence-run identity and an
exact contained idempotent lazy namespace operation without changing evidence artifact
bytes or any production caller.
Exit Criteria: Identity domain separation, canonicality, exact replay, containment,
symbolic/non-directory refusal, long-output support, and zero refusal-path writes are
GREEN; existing evidence behavior is unchanged.
Verification: New identity/namespace tests, existing EvidenceStore/EvidenceRecorder and
filesystem evidence integration tests, relevant architecture/task governance, and
`git diff --check`.
Next Action: Commit the GREEN evidence-run increment and select Increment 2.

### Increment 2 - add-standalone-model-attempt-pipeline

State: Completed
Depends On: add-lazy-evidence-run-boundary
Scope: Add the uncalled child-local exact composition, one-shot returned-outcome Tool,
response structure validation, code-only sanitization, independent verification, and
model-specific lifecycle/v2 publisher without changing process handlers or production
reachability.
Exit Criteria: Same-instance chain, one-call maximum, all closed outcomes, evidence and
verification ordering, redaction, lifecycle mapping, exact v2 replay, and zero
production caller are GREEN.
Verification: New pipeline/materializer/finalizer tests, RFC-0019 through RFC-0022
regression, evidence/verifier/model-store tests, locality/source guards, governance, and
`git diff --check`.
Next Action: Commit the GREEN standalone pipeline and select Increment 3.

### Increment 3 - add-v2-aware-process-validation

State: Completed
Depends On: add-standalone-model-attempt-pipeline
Scope: Add payload-kind-aware child handling, parent exact v2 validation, point
recovery, Result/Work prefix checks, and timeout precedence while retaining a guard that
keeps the typed production branch unreachable.
Exit Criteria: Complete parent binding, v1 preservation, missing/corrupt/foreign/change
handling, work-before-result prefix, and valid-v2-before-timeout behavior are GREEN with
no reachable writer.
Verification: Isolated handler/main/launcher and process-execution tests, model-store and
identity tests, v1 integration regression, architecture guards, governance, and
`git diff --check`.
Next Action: Commit the GREEN process-validation increment and select Increment 4.

### Increment 4 - add-v2-aware-durable-consumers

State: Completed
Depends On: add-v2-aware-process-validation
Scope: Add explicit v2 paths to durable finalization, worker processing, retry, and
Scheduler recovery/status readers while retaining the typed execution reachability
guard.
Exit Criteria: Verified/non-Verified dispositions, replacement attempts, exact replay,
source drift, terminal clear, and every recovery/status prefix are GREEN for both record
kinds without schema changes.
Verification: Durable finalizer/worker/retry, Scheduler recovery/status, runtime-event,
queue and checkpoint tests, v1 regressions, architecture guards, governance, and
`git diff --check`.
Next Action: Commit the GREEN durable-consumer increment and select Increment 5.

### Increment 5 - connect-internal-typed-process-branch

State: Completed
Depends On: add-v2-aware-durable-consumers
Scope: Remove only the internal typed process guard and connect the tested child
pipeline through the now-v2-aware consumers in temporary integration fixtures without
adding a submission/receive/user entry point.
Exit Criteria: Verified completion, failed-result retry, fresh replacement preparation,
pre-reference re-execution, post-reference no-invocation recovery, cleanup, and timeout
precedence pass end to end with legacy behavior unchanged.
Verification: FileSystem AgentRun/AgentLoop/process-isolated integrations, all RFC-0023
focused suites, architecture/no-entry-point guards, governance, and `git diff --check`.
Next Action: Commit the GREEN internal integration and select Increment 6.

### Increment 6 - verify-and-close-rfc-0023-implementation

State: Completed
Depends On: connect-internal-typed-process-branch
Scope: Run the full Markdown-sensitive Java 17 regression, read results, synchronize
implementation maturity and canonical documents, close the task/handoff, and commit the
verified closure.
Exit Criteria: Full regression passes, documents and local commits are current, Git is
clean, and the stable checkpoint is cleared.
Verification: Full `.\scripts\gradle.ps1 test`, final focused governance and RFC-0023
integration suites, diff/commit/status inspection, and checkpoint reconciliation.
Next Action: Await separate authority for a governed typed ModelWork producer or
receiver, or the next Roadmap-selected Gate 9 boundary.

## Next

Await separate user authority for a governed typed ModelWork producer or receiver, or
the next Roadmap-selected Gate 9 boundary. RFC-0023 is internally Integrated, but no
supported typed entry point is implied.
