# Current Task

## Status

Completed

## Task

Specify RFC-0023: the typed ModelWork process-execution, ToolResult/evidence,
response-validation, Model RunRecord v2 finalization, retry, and recovery connection
that consumes the exact RFC-0022 deterministic-fake boundary.

## Task ID

specify-typed-model-work-process-execution

## Context

RFC-0018 retains the exact typed ModelWork profile through the durable transport,
submission, queue, and runtime family. RFC-0019 provides Model RunRecord v2, exact
active-task resolution, and fresh Scheduler request/policy/admission preparation.
RFC-0020 through RFC-0022 provide the closed deterministic-fake candidate,
Unicode-scalar semantics, suitability, actual-request budget decision, and exact
same-request/same-policy/same-candidate-gateway invocation seam. Those contracts remain
standalone and every current typed ModelWork execution guard still fails closed.
RFC-0022 requires a separately accepted connection contract before production process
execution, evidence, result validation, v2 finalization, retry, or recovery may be
implemented. The completed task names that specification as the next work, and the user
requested continuation on 2026-09-03.

## Justified By

- User continuation request on 2026-09-03 into typed ModelWork process-execution specification
- User continuation request on 2026-09-03 into RFC-0022 exact-request seam implementation
- User continuation request on 2026-09-02 into exact-request model budget and invocation seam specification

## Approval

The user's 2026-09-03 continuation authorizes a documentation-only RFC and accepted
decision defining the typed ModelWork process-execution connection from exact Scheduler
preparation through deterministic-fake suitability and exact-request invocation,
ToolResult/evidence and response validation, Model RunRecord v2 publication and result
binding, finalization, pre-reference retry, and post-reference recovery. It authorizes
the minimal architecture, compact mirror, Roadmap, RFC index, task, decision/index,
verification, handoff, and Changelog synchronization plus ordinary local GREEN commits.
It authorizes no Java or test-source change, model or Tool invocation now, production
caller or supported entry point, typed ModelWork producer/receiver, schema migration,
provider/router/network, credential or spend work, push, merge, release, deployment,
permission change, destructive cleanup, or external effect.

## Acceptance Criteria

- The RFC names the exact existing upstream and downstream production boundaries and
  one identity-preserving call chain from typed `WorkItem` and fresh RFC-0019
  preparation through RFC-0020/RFC-0021 suitability and RFC-0022 invocation.
- The contract proves the exact `SchedulerModelInvocationPreparation.executionPolicy()`
  instance reaches admission, suitability, budget preparation, invocation, and the
  process-isolation control envelope without request, prompt, policy, candidate, or
  gateway reconstruction.
- Closed pre-call refusal, gateway failure, untrusted response, ToolResult/evidence,
  expected-response validation, lifecycle status, and queue-disposition ownership are
  ordered without treating model output or a gateway return as verification authority.
- Model RunRecord v2 construction, persistence, Result-message binding, parent-side
  validation, reference checkpointing, and finalization define exact identities and
  persist-before-exposure boundaries without weakening the v1 type boundary.
- Crash points distinguish pre-reference at-least-once re-execution from post-reference
  no-invocation recovery, including deterministic AgentRun-bound identity, exact record
  replay, source drift, corrupt/foreign records, and partial spool/result prefixes.
- The RFC states whether the current v2 record and existing envelope/result/checkpoint
  schemas are sufficient; any required provenance or schema change is separated behind
  a later explicit compatibility and migration task rather than implied.
- The implementation sequence is RED-first and bounded, with named focused and
  integration evidence, while producer/receiver, provider/router/network, credentials,
  spend, MCP, and broader runtime changes remain excluded.
- RFC/decision indexes, Architecture and compact mirror, Roadmap, task cursor,
  Changelog, handoff, and append-only verification evidence are synchronized according
  to document ownership without changing capability maturity.
- Focused Markdown-sensitive governance, `git diff --check`, and the full
  README-owned Java 17 regression pass freshly before completion.

## Out Of Scope

Java or test-source implementation; actual gateway or Tool execution; production
caller or supported entry point; typed ModelWork producer, submission, receiver, CLI,
or runtime-event ingress; existing read-file execution behavior change; provider
selection, router, endpoint, remote transmission, network, credentials, pricing or
spend; MCP; schema/codec migration implementation; push, merge, release, deployment,
permissions, destructive cleanup, and external effects.

## Allowed Tools

- read-file
- write-docs
- build-output
- verify
- checkpoint
- git-inspect
- git-stage
- git-commit

## Verification

Evidence is appended once per completed increment to `docs/verification-log.md`.
Documentation verification must cover RFC indexing, architecture/document ownership,
dynamic workflow, accepted-task justification, and canonical task planning. Subagent
recommendations are not verification evidence.

## Dynamic Workflow

Workflow ID: specify-typed-model-work-process-execution
Mode: Sequential
Increment Limit: 2
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on contract conflict, identity or ownership ambiguity, implicit
execution or verification authority, unclosed refusal/crash/recovery behavior, silent
schema widening, failed verification, checkpoint drift, new authority, exhausted
bounds, or unsafe recovery.

### Increment 1 - specify-typed-model-work-execution-contract

State: Completed
Depends On: none
Scope: Review the existing typed ModelWork, process-isolated execution, Tool/evidence,
verification, Model RunRecord v2, finalization, retry, and recovery boundaries and
accept one RFC defining their exact connection without implementation or caller.
Exit Criteria: The RFC, accepted decision, indexes, architecture mirrors, Roadmap,
task cursor, Changelog, and focused evidence are synchronized and focused governance
passes.
Verification: RFC/decision/architecture/index/ownership/dynamic-workflow/approved-task/
task-justification/planner tests plus `git diff --check`.
Next Action: Commit the verified documentation increment and run the fresh full
regression.

### Increment 2 - verify-and-close-typed-model-work-execution-specification

State: Completed
Depends On: specify-typed-model-work-execution-contract
Scope: Run the full Markdown-sensitive Java 17 regression, record fresh evidence,
close the task/handoff, and commit the verified specification closure.
Exit Criteria: The full regression passes with results read, canonical documents are
current, the intended local commits are clean, and the checkpoint is stable and clear.
Verification: Full `.\scripts\gradle.ps1 test`, focused final governance,
diff/commit/status inspection, and checkpoint reconciliation.
Next Action: Await separate authority to implement the accepted typed ModelWork
execution connection RED-first.

## Next

Await separate user authority to implement RFC-0023 RED-first without adding a typed
ModelWork producer, receiver, or supported entry point.
