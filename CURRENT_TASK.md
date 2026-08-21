# Current Task

## Status

Completed

## Task

Implement the RFC-0018 typed `ModelWorkPayload` and model-work-only envelope/spool v2
golden-wire boundary test-first without enabling Scheduler model execution.

## Task ID

implement-typed-model-work-golden-wire

## Context

RFC-0018 is Accepted and the completed delivery task names its first implementation
prompt as the next work. The current sealed bus algebra has four payload kinds and the
package-private codec always writes transport-spool v1 plus message-envelope v1. The
new value must retain target, expected-response digest, and one exact complete RFC-0014
profile while every legacy encoding and cancellation-signing byte remains unchanged.

## Justified By

- User continuation request on 2026-08-21 into the typed model-work golden-wire implementation
- User request on 2026-08-21 to commit, push, and merge the completed RFC-0016 through RFC-0018 work to main

## Approval

The user's 2026-08-21 continuation authorizes one bounded RED-first implementation of
the typed bus value and package-private model-work wire family, read-only bounded
contract/compatibility reviews, owning architecture/maturity/task/changelog/
verification synchronization, fresh Java 17 verification, development-session
checkpoints, and ordinary local commits at verified GREEN increment boundaries. It
authorizes no durable manifest/queue/runtime migration, artifact rewrite, WorkItem,
Scheduler, process-worker, CLI, Tool, RunRecord, admission, gateway, provider, route,
network, credential, spend, push, merge, release, deployment, or destructive cleanup.

## Acceptance Criteria

- A fifth sealed `ModelWorkPayload` retains exactly task revision, snapshot identity,
  immutable bounded Tool scope, and one mandatory typed execution input retaining
  bounded target path, expected-response SHA-256, and the exact complete
  `ModelExecutionProfile`.
- Construction requires `model-invoke` in the Tool scope, preserves capability and
  model-class independence, rejects null/partial/invalid values through existing
  contracts, and adds no optional, flattened, provider, routing, credential, network,
  spend, decision, result, gateway, or authority field.
- Model work alone encodes as transport-spool v2 plus message-envelope v2 with explicit
  `MODEL_WORK` and all profile fields in RFC-0014 constructor order; decoding
  reconstructs validated typed values and rejects unknown, malformed, truncated,
  trailing, corrupt, or cross-family input.
- Existing Work, Result, Control, and Handoff messages retain byte-for-byte v1 encoding,
  `MessageEnvelope.ENVELOPE_VERSION` remains unchanged, and detached cancellation
  canonical bytes remain unchanged.
- No current producer, receiver, submission, queue, runtime, WorkItem, process worker,
  CLI, Tool, RunRecord, admission, gateway, adapter, migration, or artifact behavior is
  connected to the new payload.
- Focused RED/GREEN evidence, bus/model/cancellation regression, full README-owned Java
  17 regression, documentation ownership, and `git diff --check` pass freshly; owning
  documents are synchronized without claiming Scheduler integration or operation.

## Out Of Scope

Submission manifest v3, Scheduler queue v4, AgentRuntime v5, WorkItem widening,
durable-store or process-spool migration, stopped-owner cutover, CLI/receiver/producer
support, Scheduler selection/execution, RunRecord v2, request sourcing, admission or
policy wiring, model candidate suitability, gateway or adapter execution, provider,
route, endpoint, destination, credential, network or remote transmission, spend,
pricing, tokenizer, migration of real artifacts, push, merge, release, deployment,
permission changes, history rewrite, and destructive cleanup.

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
- subagent-readonly

## Verification

Evidence is appended once per completed increment to `docs/verification-log.md` after
the declared checks complete.

- Increment 1: the aligned missing-type RED failed at `compileTestJava`; after the
  minimum value implementation, the focused payload/envelope selection passed, and
  the combined bus plus architecture selection passed 78 tests across 11 suites with
  zero failures, errors, or skips. `git diff --check` was clean.
- Increment 2: the aligned codec RED failed because the v1-only codec rejected
  `ModelWorkPayload`. After the minimum family implementation, focused codec, spool,
  and cancellation tests passed 33 tests across 3 suites; expanded bus, model,
  architecture, and cancellation regression passed 152 tests across 20 suites with
  zero failures, errors, or skips. `git diff --check` was clean.
- Increment 3: the fresh unfiltered README-owned Java 17 test task passed 949 tests
  across 175 suites: 939 passed, 10 existing environment-dependent cases skipped,
  zero failed, and zero errored. After architecture, maturity, Roadmap, changelog, and
  session-owner synchronization, final focused bus, model, architecture, and
  cancellation regression passed 152 tests across 20 suites with zero failures,
  errors, or skips; the stale current-document four-kind scan and `git diff --check`
  were clean.

## Dynamic Workflow

Workflow ID: implement-typed-model-work-golden-wire
Mode: Sequential
Increment Limit: 3
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on contract mismatch, legacy-byte drift, cancellation-signing
drift, cross-family ambiguity, failed focused or full verification, task or checkpoint
drift, new durable/runtime/external authority, exhausted bounds, or unsafe recovery.

### Increment 1 - add-typed-model-work-payload

State: Completed
Depends On: none
Scope: Reconcile bounded read-only value/compatibility reviews, establish aligned RED
tests, and add only the fifth immutable payload plus mandatory execution input and
sealed-hierarchy membership.
Exit Criteria: Shape, validation, immutability, equality, exact profile retention,
scope requirement, and authority-surface tests pass; existing bus payload tests remain
GREEN; the verified increment is committed locally.
Verification: Focused `ModelWorkPayloadTest`, existing bus payload tests, architecture
guards, and `git diff --check`.
Next Action: Add the model-work-only v2 codec family after reading Increment 1 evidence.

### Increment 2 - add-model-work-wire-v2

State: Completed
Depends On: add-typed-model-work-payload
Scope: Establish codec RED tests, implement canonical model-work v2 encoding/decoding,
pin existing v1 and cancellation golden bytes, and reject all cross-family/corrupt
representations without connecting any runtime consumer.
Exit Criteria: Exact profile round-trip, deterministic bytes, corruption and
cross-family refusal, legacy v1 golden bytes, cancellation canonical bytes, and spool
adapter regression pass; the verified increment is committed locally.
Verification: Focused codec/spool/cancellation tests, complete bus/model regression,
architecture guards, and `git diff --check`.
Next Action: Run full verification and synchronize lifecycle owners.

### Increment 3 - verify-and-close-model-work-wire

State: Completed
Depends On: add-model-work-wire-v2
Scope: Run the full README-owned Java 17 regression, synchronize only owning documents,
rerun final governance, and close the task without runtime or durable-schema claims.
Exit Criteria: Full and final focused verification pass, evidence is appended once,
owning state is truthful, local GREEN commits exist, and the worktree/checkpoint reach
the intended clean stable state.
Verification: Full `test`, focused governance and bus/model/cancellation regression,
JUnit XML aggregation, `git diff --check`, and final Git/checkpoint inspection.
Next Action: Define the coordinated durable manifest/queue/runtime migration task under
separate authority.

## Next

Define the coordinated submission-manifest v3, Scheduler-queue v4, AgentRuntime v5,
model RunRecord v2, process-isolation, recovery, and stopped-owner migration increment
under separate user authority; do not connect ModelWork to current execution paths by
default.
