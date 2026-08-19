# Current Task

## Status

Completed

## Task

Compose `model-invoke` into the durable Scheduler execution path test-first: give
`ModelInvokeTool` a governed contained `prompt-path` prompt source, derive the
executed pipeline in `AgentLoopAgentRunExecution` from the WorkItem's allowed-tool
scope with the declared execution input as prompt document and expected response
digest and the required capability as the model-class label, accept
`model-invoke`-scoped tasks at the governed submission surfaces, and promote the
slice with one real-filesystem Scheduler cycle executing a model WorkItem to its
verified terminal disposition.

## Task ID

compose-model-invoke-into-scheduler-execution

## Context

The RFC-0013 minimum slice is delivered and observed passing on the external
verification host. The Scheduler execution pipeline currently always executes
`read-file`; the WorkItem's allowed-tool scope is carried but never selects the
executed tool. The declared `ExecutionInput` and the required-capability field
already carry exactly the data a model invocation needs — prompt document,
expected digest, and model-class label — so no queue, runtime, or spool schema
changes. Model work without a declared execution input fails closed because the
source-document fallback digest names the document, not a response. The
process-isolated child reuses the same execution seam, so both execution paths
gain model work together.

## Justified By

- User continuation request on 2026-08-19 into scheduler-executed model invocations
- Accept RFC-0013 defining the Delivery Gate 9 model gateway minimum slice

## Approval

The accepted continuation decision authorizes test-first source and test authoring
for `com.enhancer.model`, the `AgentLoopAgentRunExecution` composition in
`com.enhancer.runtime`, and the `com.enhancer.cli` submission gate, focused and
full verification, development-session checkpoints, document synchronization, and
ordinary local commits at verified GREEN increment boundaries under Constitution
1.2.0.

It does not authorize push, merge, tag, release, deployment, any queue, runtime,
submission, or spool schema change, migration, MessageEnvelope or store change,
network connection, credential, paid-service invocation, MCP, routing, caching,
streaming, real provider invocation, force push, rebase, reset, amend, squash, or
destructive cleanup.

## Acceptance Criteria

- `ModelInvokeTool` accepts exactly one prompt source per request: inline `prompt`
  or `prompt-path`; a contained regular UTF-8 prompt file under the policy project
  root is read with the same containment and size bounds as governed read-file,
  and requests with both, neither, or an escaping/oversized/malformed path fail
  closed as typed failures.
- `AgentLoopAgentRunExecution` keeps a `read-file`-containing scope on the
  existing pipeline byte-for-byte unchanged, executes a `model-invoke` scope
  against the deterministic fake with prompt document, expected response digest,
  capability-derived model-class, and fixed budget values whose gateway timeout
  fits strictly inside a per-tool timeout, and fails closed on model work without
  a declared execution input or with a scope naming neither executable tool.
- Model-scoped work verifies through `DeterministicModelInvokeVerifier` and
  persists a lifecycle-valid RunRecord through the same finalizer and store as
  read-file work.
- The governed submission surfaces accept a task whose allowed tools name
  `model-invoke` without `read-file` and continue to reject a task naming neither
  executable tool.
- The promoting integration test submits a model WorkItem through the governed CLI
  and drives one real-filesystem Scheduler cycle to `VERIFIED_COMPLETED`, with the
  persisted RunRecord resolvable, its evidence reference resolvable to the exact
  deterministic response, and exact re-entry creating no second execution.
- No queue, runtime, submission, or spool schema version changes, no test opens a
  network connection, and a fresh full Java 17 Markdown-sensitive regression
  passes before the task completes.

## Out Of Scope

Queue/runtime/submission/spool schema evolution and migration, MessageEnvelope
changes, model routing, provider selection, MCP, caching, fallback, streaming,
quality evaluation, real provider invocation, credentials, paid services,
prompt-injection resistance and redaction, multi-tool runs within one AgentRun,
push, merge, tag, release, and deployment.

## Allowed Tools

- read-file
- write-docs
- write-code
- build-output
- verify
- checkpoint
- git-inspect
- git-stage
- git-commit

## Dynamic Workflow

Workflow ID: compose-model-invoke-into-scheduler-execution

Mode: Sequential

Increment Limit: 3

Selection Rule: Select the first dependency-ready Pending increment in document order.

Stop Conditions: Stop on failed verification, governance-test failure that cannot be
resolved inside the selected increment, task/checkpoint drift, scope expansion,
schema-change requirement, network or credential requirement, subagent bound
exhaustion, or insufficient authority.

### Increment 1 - model-invoke-prompt-path-argument

State: Completed

Depends On: none

Scope: Extend `ModelInvokeTool` RED-first with the `prompt-path` argument as an
exact alternative to inline `prompt`: containment against the real project root,
regular-file and bounded-size checks through the shared bounded read, strict UTF-8
decoding, and typed failure on both-or-neither prompt sources or an invalid path.

Exit Criteria: Focused unit tests cover the governed prompt-file round trip,
exclusivity of the two prompt sources, escaping, missing, oversized, non-regular,
and malformed-UTF-8 paths, and unchanged inline-prompt behavior; focused
governance tests pass.

Verification: Focused `com.enhancer.model` unit tests plus the architecture
governance suites, and `git diff --check`, before the increment commit.

Next Action: Derive the executed pipeline from the WorkItem scope.

### Increment 2 - scope-derived-model-execution-pipeline

State: Completed

Depends On: model-invoke-prompt-path-argument

Scope: Extend `AgentLoopAgentRunExecution` RED-first: keep `read-file`-containing
scopes on the unchanged existing pipeline, execute `model-invoke` scopes through
the deterministic fake and `DeterministicModelInvokeVerifier` with the declared
execution input as prompt document and expected response digest, the required
capability as model-class, and fixed per-tool budget values; fail closed on model
work without a declared input or a scope naming neither executable tool.

Exit Criteria: Focused execution tests cover the verified model run, the untouched
read-file path, absent-input and unknown-scope refusal, and a failed model
verification carried in the persisted RunRecord; focused governance tests pass.

Verification: Focused `com.enhancer.runtime` execution and `com.enhancer.model`
tests plus the architecture governance suites, and `git diff --check`, before the
increment commit.

Next Action: Promote the slice through the governed Scheduler cycle.

### Increment 3 - scheduler-model-work-promotion

State: Completed

Depends On: scope-derived-model-execution-pipeline

Scope: Accept `model-invoke`-scoped tasks at the governed submission surfaces
while still requiring at least one executable tool in scope, and author the
promoting real-filesystem integration test: submit a model WorkItem through the
governed CLI, drive one Scheduler cycle to `VERIFIED_COMPLETED`, resolve the
RunRecord and its evidence reference, prove exact re-entry creates no second
execution, and synchronize the owning documents.

Exit Criteria: The promoting integration test passes, owning documents are
synchronized once, and a fresh full Java 17 Markdown-sensitive regression passes.

Verification: Full Java 17 Markdown-sensitive Gradle regression including the new
focused and integration tests, `git diff --check`, and staged-boundary review
before the final increment commit.

Next Action: Record the follow-up task after the slice completes.

## Verification

Increment evidence is appended once per increment to `docs/verification-log.md`
when the increment's exit criteria and declared verification are satisfied.

- Increment 1: the RED-first governed `prompt-path` prompt source passed 29
  focused model tests and the 13 focused governance tests with zero failures,
  errors, or skips, and `git diff --check` was clean. Evidence is appended once in
  `docs/verification-log.md`.
- Increment 2: the RED-first scope-derived execution pipeline passed 9 focused
  execution tests with the read-file cases unchanged, the process-isolated suite,
  29 model tests, the 13 governance tests, and the complete runtime package
  regression with zero failures, errors, or skips, and `git diff --check` was
  clean. Evidence is appended once in `docs/verification-log.md`.
- Increment 3: the submission-gate relaxation, the scope-derived isolated-result
  and recovery-status expectations, and the promoting real-filesystem
  submit-plus-cycle integration test passed with the complete runtime and CLI
  package regressions, and the fresh full Java 17 Markdown-sensitive regression
  passed. Evidence is appended once in `docs/verification-log.md`.

## Next

Define the next bounded Delivery Gate 9 slice: specify the provider-neutral
execution-profile contract (capability, model class, locality, reasoning, context,
token, cost, time, and data-classification requirements) as an RFC-governed value
layer over the existing gateway port, without routing, providers, or remote
transmission.
