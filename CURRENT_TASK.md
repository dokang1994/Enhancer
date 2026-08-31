# Current Task

## Status

Completed

## Task

Define and accept RFC-0019 as the additive Model RunRecord v2 and exact Scheduler
task/request/policy/admission integration contract required before typed ModelWork may
approach candidate suitability or gateway execution, without implementation.

## Task ID

specify-model-run-record-v2-and-scheduler-admission

## Context

RFC-0018's typed ModelWork envelope, manifest v3, queue v4, AgentRuntime v5, coordinated
migration, and bounded operator are implemented and verified. Typed ModelWork remains
blocked before in-process execution, point recovery, child launch, and external receive.
RunRecord payload v1 cannot independently audit the WorkItem/message identity,
independent capability projection, complete profile, and model request. The current
legacy Scheduler also synthesizes `ApprovedTask`, reuses capability as model class, and
constructs fixed request/policy values without RFC-0015/RFC-0016 composition. The next
contract must resolve those gaps before any execution wiring.

## Justified By

- User continuation request on 2026-08-31 into the Model RunRecord v2 and Scheduler admission specification
- User continuation request on 2026-08-25 into the coordinated durable ModelWork migration implementation
- User continuation request on 2026-08-21 into the Scheduler complete-profile transport specification

## Approval

The user's explicit 2026-08-31 continuation authorizes one documentation-only RFC-0019
specification, bounded read-only RunRecord/schema/recovery and authority/wiring reviews,
owning architecture/RFC-planning/index/decision/task/changelog/verification
synchronization, correction of directly related stale architecture wording, fresh Java
17 Markdown-sensitive verification, development-session checkpoints, and ordinary
local commits at verified GREEN increment boundaries. It authorizes no Java or binary-
schema implementation, artifact migration, command or caller change, Scheduler
execution wiring, child launch, admission invocation, candidate suitability, gateway or
provider execution, network or remote transmission, credentials or spend, push, merge,
release, deployment, external effect, permission change, or destructive cleanup.

## Acceptance Criteria

- RFC-0019 defines an additive model-specific RunRecord payload v2 that retains the
  exact WorkItem and work-message identities, unchanged independent capability
  projection, complete RFC-0014 profile, exact RFC-0013 request, existing policy/result/
  digest/evidence/verification/lifecycle data, and no persisted admission decision.
- Existing read-file `RunRecord` payload v1, public v1 readers, reference namespace,
  envelope integrity, replay behavior, and encoded bytes remain unchanged. Cross-kind
  identity reuse, partial projection, and silent v2-to-v1 fallback fail closed.
- The exact active governed `ApprovedTask` source is point-resolved from the governed
  task document and must match the retained task identity, source path, source digest,
  and Tool scope; generated descriptions or approval evidence are not accepted.
- The Scheduler-specific source for every `ModelRequest` and `ExecutionPolicy` field is
  explicit. Model class comes from the profile, capability remains the unchanged active
  WorkItem projection, response-character and token ceilings stay independent, longer
  profiles are never clamped, and one policy instance reaches both RFC-0016 and the
  later `ToolExecutor`.
- Fresh RFC-0015/RFC-0016 evaluation, typed rejection before Tool/gateway activity,
  retry/re-entry behavior, process-parent/child binding, model-v2 result validation,
  and post-record deterministic recovery are fully ordered without persisting
  `Admitted` or reconstructing provenance from ambient stores.
- The contract keeps typed ModelWork execution blocked until separately accepted
  candidate-suitability and proven-local gateway boundaries exist, and grants no route,
  provider, endpoint, destination, credential, network, transmission, spend, cache,
  fallback, or external-receive authority.
- Architecture, RFC index/planning, accepted-decision index, changelog, task cursor, and
  append-only verification evidence are synchronized. Focused governance and the full
  README-owned Java 17 regression pass freshly with a documentation-only diff.

## Out Of Scope

Java implementation; RunRecord binary writer/reader changes; schema or artifact
migration; task resolver, request/policy factory, admission, finalizer, result handler,
recovery reader, process worker, submission, receiver, CLI, Tool, gateway, or adapter
changes; candidate suitability; routing, providers, endpoints, destinations, network or
remote transmission; credentials, paid services, pricing, tokenizers, usage
normalization, caching, fallback, streaming, MCP, capability maturity promotion,
release, deployment, push, merge, history rewrite, permission changes, and destructive
cleanup.

## Allowed Tools

- read-file
- write-docs
- build-output
- verify
- checkpoint
- git-inspect
- git-stage
- git-commit
- subagent-readonly

## Verification

- Increment 1: two bounded read-only reviews independently identified the existing
  RunRecord v1 compatibility constraints and Scheduler authority/wiring gaps. The
  primary Agent reconciled those recommendations against repository authority and
  corrected the draft so Model RunRecord v2 directly retains the exact prepared
  `ModelRequest`. Focused governance passed 21 tests across 6 suites with zero failures,
  errors, or skips, and `git diff --check` passed.
- Increment 2: the fresh unfiltered README-owned Java 17 Gradle `test` task completed
  with `BUILD SUCCESSFUL` in 2 minutes 19 seconds. JUnit XML aggregation found 180
  suites and 990 tests: 980 passed, 10 existing environment-dependent cases skipped,
  zero failed, and zero errored. After closure synchronization, final focused governance
  passed 21 tests across 6 suites with zero failures, errors, or skips, and
  `git diff --check` passed. No implementation or maturity state changed.

## Dynamic Workflow

Workflow ID: specify-model-run-record-v2-and-scheduler-admission
Mode: Sequential
Increment Limit: 2
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on v1 compatibility ambiguity, missing authority source,
admission/recovery ambiguity, candidate or gateway authority expansion, failed
governance/regression verification, task drift, checkpoint drift, new external
authority, exhausted bounds, or unsafe recovery.

### Increment 1 - define-and-accept-rfc-0019

State: Completed
Depends On: none
Scope: Reconcile the bounded read-only schema/recovery and authority/wiring reviews,
define and accept the minimum RFC-0019 contract, correct directly related architecture
drift, and synchronize RFC planning/index, decision, changelog, task, and append-only
focused verification evidence.
Exit Criteria: The RFC resolves every acceptance criterion without implementation or
artifact change, focused governance passes, the diff is documentation-only and clean,
evidence is appended once, and the verified increment is committed locally.
Verification: RFC/decision/architecture/document-ownership/dynamic-workflow governance
tests and `git diff --check`.
Next Action: Select Increment 2 and run the full README-owned regression.

### Increment 2 - verify-and-close-rfc-0019

State: Completed
Depends On: define-and-accept-rfc-0019
Scope: Run the full README-owned Java 17 regression, synchronize only changed lifecycle
owners, rerun final Markdown governance, and close the task.
Exit Criteria: Full and final governance verification pass, no implementation or
maturity claim appears, the closure is committed locally, and the worktree/checkpoint
reach the intended clean stable state.
Verification: Full `test`, focused governance, JUnit XML aggregation,
`git diff --check`, and final Git/checkpoint inspection.
Next Action: Implement the additive Model RunRecord v2 contract RED-first under
separate user authority while typed ModelWork execution remains blocked.

## Next

Await separate user authority to implement additive Model RunRecord v2 RED-first.
Typed ModelWork execution remains blocked before candidate suitability and a proven-
local gateway boundary.
