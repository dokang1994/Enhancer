# Current Task

## Status

Completed

## Task

Specify RFC-0021 sequence 2: one closed typed exact-request budget decision and the
same-request/same-policy/same-candidate-gateway invocation seam required before any
production caller may consume standalone candidate suitability.

## Task ID

specify-deterministic-fake-exact-request-invocation-seam

## Context

RFC-0020/RFC-0021 sequence 1 is Contract Verified: the closed
`deterministic-fake-v2` candidate exposes fixed Unicode-scalar semantics and
capacities, and standalone profile/candidate suitability can return ephemeral
`Suitable`. It deliberately does not count one actual request or authorize gateway
activity. RFC-0021 requires a separately accepted contract that validates the exact
admitted request and predicted fake response before invoking the exact candidate-bound
gateway. The completed sequence-1 task records this specification as the sole next
action, and the user requested continuation on 2026-09-02.

## Justified By

- User continuation request on 2026-09-02 into exact-request model budget and invocation seam specification
- User continuation request on 2026-09-02 into RFC-0021 sequence 1 implementation
- User continuation request on 2026-09-01 into deterministic fake token semantics and capacity specification

## Approval

The user's 2026-09-02 continuation authorizes a documentation-only RFC and accepted
decision defining the closed exact-request budget refusal algebra, deterministic
ordering, exact identity and call semantics, retry/recovery boundary, later RED-first
implementation sequence, and the stop before any production caller or Scheduler
integration. It authorizes no Java or test-source change, gateway or Tool behavior
change, invocation now, production caller, RunRecord/schema/runtime wiring, provider,
network, credential, paid service, push, merge, release, deployment, permission
change, destructive cleanup, or external effect.

## Acceptance Criteria

- The RFC names the exact input/output decision types and immutable identities retained
  by admitted and refused results without creating a general router or provider API.
- The refusal vocabulary and first-match order cover malformed prompt, actual input,
  predicted response UTF-16 length, predicted output tokens, checked total tokens, and
  any gateway failure without conflating profile suitability or generic `ModelUsage`.
- The contract proves the prompt/request is read and counted once, the exact admitted
  request, same RFC-0019 `ExecutionPolicy`, same suitable candidate, and candidate-
  bound gateway cross the invocation seam without reconstruction or caller override.
- Zero-activity refusal, checked arithmetic, exception mapping, diagnostics, retry,
  pre-reference replay, post-reference recovery, evidence/RunRecord ownership, and
  production-caller absence are explicit.
- The implementation sequence is RED-first and stops before Scheduler execution,
  finalization, recovery integration, typed ModelWork production/receive, provider,
  router, network, credential, or spend work.
- RFC index, architecture and compact mirror, Roadmap scope, accepted decision/index,
  task cursor, Changelog, handoff, and append-only verification evidence are
  synchronized according to document ownership without changing capability maturity.
- Focused Markdown-sensitive governance, `git diff --check`, and the full
  README-owned Java 17 regression pass freshly before completion.

## Out Of Scope

Java or test-source implementation; actual gateway or Tool call; production caller;
Scheduler execution, result validation, finalization, retry controller, recovery, or
runtime-event wiring; Model RunRecord/schema changes; typed ModelWork producer,
submission, receiver, or CLI; generic/provider tokenizer or usage normalization;
provider selection, router, endpoint, remote transmission, network, credentials,
pricing or spend; push, merge, release, deployment, permissions, and destructive
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

## Verification

Evidence is appended once per completed increment to `docs/verification-log.md`.
Documentation verification must cover RFC indexing, architecture/document ownership,
dynamic workflow, accepted-task justification, and canonical task planning. Subagent
recommendations are not verification evidence.

## Dynamic Workflow

Workflow ID: specify-deterministic-fake-exact-request-invocation-seam
Mode: Sequential
Increment Limit: 2
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on contract conflict, identity ambiguity, unclosed refusal or
exception behavior, implicit caller/invocation authority, retry/recovery drift, failed
verification, checkpoint drift, new authority, exhausted bounds, or unsafe recovery.

### Increment 1 - specify-exact-request-invocation-contract

State: Completed
Depends On: none
Scope: Review the existing request/profile/suitability/policy/gateway boundaries and
accept one RFC defining the closed exact-request decision plus same-identity invocation
seam, with no implementation or caller.
Exit Criteria: The RFC, accepted decision, indexes, architecture mirrors, Roadmap,
task cursor, Changelog, and focused evidence are synchronized and focused governance
passes.
Verification: RFC/decision/architecture/index/ownership/dynamic-workflow/approved-task/
task-justification/planner tests plus `git diff --check`.
Next Action: Commit the verified documentation increment and run the fresh full
regression.

### Increment 2 - verify-and-close-exact-request-specification

State: Completed
Depends On: specify-exact-request-invocation-contract
Scope: Run the full Markdown-sensitive Java 17 regression, record fresh evidence,
close the task/handoff, and commit the verified specification closure.
Exit Criteria: The full regression passes with results read, canonical documents are
current, the intended local commits are clean, and the checkpoint is stable and clear.
Verification: Full `./scripts/gradle.ps1 test`, focused final governance,
diff/commit/status inspection, and checkpoint reconciliation.
Next Action: Await separate authority to implement the accepted exact-request budget
and invocation boundary RED-first without a production caller.

## Next

Await separate user authority to implement RFC-0022 sequences 1 and 2 RED-first with
no production caller.
