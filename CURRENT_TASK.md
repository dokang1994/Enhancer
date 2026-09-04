# Current Task

## Status

In Progress

## Task

Specify RFC-0024: the smallest governed deterministic-fake typed ModelWork submission
source that can feed the completed internal RFC-0023 process path without enabling a
receiver, supported command, or external model boundary.

## Task ID

specify-governed-deterministic-model-work-submission

## Context

RFC-0023 is internally Integrated for test-owned typed ModelWork, including fresh
child-local preparation, one deterministic-fake invocation per AgentRun, Model
RunRecord v2 finalization, retry, and recovery. No production source constructs a
typed ModelWork envelope, and no supported command can supply the internal model-aware
Scheduler execution configuration. RFC-0017 through RFC-0019 require a separately
accepted caller-specific complete-profile source and an independent governed
capability projection before typed submission or receive. The completed task names a
governed producer or receiver as the next possible boundary, and the user requested
continuation on 2026-09-04.

## Justified By

- User continuation request on 2026-09-04 into governed deterministic ModelWork submission specification
- User continuation request on 2026-09-03 into RFC-0023 typed ModelWork process-execution implementation
- User continuation request on 2026-09-03 into typed ModelWork process-execution specification

## Approval

The user's 2026-09-04 continuation authorizes a documentation-only RFC and accepted
decision defining one internal deterministic-fake typed ModelWork submission source.
The contract may define exact task, snapshot, complete-profile, independent capability,
identity, manifest, queue-admission, replay, refusal, and later-consumer relationships,
plus a bounded RED-first implementation sequence. It authorizes the minimal
Architecture, compact mirror, Project State consistency correction, Roadmap, RFC index,
task, decision/index, verification, handoff, and Changelog synchronization and ordinary
local GREEN commits.

It authorizes no Java or test-source change, actual submission or model execution now,
typed spool publisher or receiver, supported CLI/API/editor/Desktop entry point,
general model router or provider, endpoint, remote transmission, network, credentials,
pricing or spend, MCP, durable schema version or migration, runtime-event ingress,
push, merge, release, deployment, permission change, destructive cleanup, or external
effect.

## Acceptance Criteria

- The RFC names the exact existing RFC-0017 through RFC-0023 inputs and the durable
  manifest/queue consumer, and defines one caller-specific typed submission boundary
  without adding a supported entry point in this task.
- One complete `ModelExecutionProfile` remains indivisible untrusted requirement data;
  no field is defaulted, repaired, inferred, flattened into Tool arguments, or used as
  capability authority.
- The independent `WorkItem.requiredCapability` source is repository-owned,
  deterministic-fake-specific, explicitly named, and fixed independently of the
  profile, request, candidate instance, envelope, manifest, queue, CLI text, or ambient
  state. Capability/profile mismatch remains observable at RFC-0016 admission.
- First use resolves the exact active task, requires `model-invoke`, captures one
  repository-memory Workspace snapshot, derives stable submission identities, creates
  one complete ModelWork envelope, persists manifest intent before queue creation, and
  admits only through the existing durable service.
- Replay resolves the manifest before clock or repository context, exact-validates all
  caller-owned intent and the fixed capability source, changes no manifest or queue
  revision, and fails closed on task, producer, target, digest, profile, capacity, or
  priority drift.
- The contract distinguishes an internal producer from execution authority and names
  the existing internal v2 process composition as its test-owned next consumer. A
  supported Scheduler composition, typed transport publisher/receiver, and interface
  profile format remain separately authorized work.
- Existing message/spool v2, manifest v3, queue v4, runtime v5, pending-finalization v2,
  Model RunRecord v2, and legacy v1 bytes remain sufficient and unchanged; any new
  provenance or durable refusal requires a separate compatibility decision.
- Stale architecture/state wording discovered during recovery is corrected without
  changing capability maturity or claiming supported typed ingress.
- RFC/decision indexes, Architecture and compact mirror, Project State, Roadmap, task
  cursor, Changelog, and append-only verification evidence are synchronized according
  to document ownership.
- Focused Markdown-sensitive governance, `git diff --check`, and the full
  README-owned Java 17 regression pass freshly before completion.

## Out Of Scope

Java or test-source implementation; actual typed submission, queue admission, gateway
or Tool execution; supported Scheduler composition or command; typed spool publisher
or receiver; existing legacy command widening; provider selection, router, registry,
endpoint, remote transmission, network, credentials, pricing or spend; MCP; new durable
schema or migration; runtime-event ingress; candidate/count/Ready/usage/refusal
provenance changes to Model RunRecord v2; durable terminal pre-call refusal; push,
merge, release, deployment, permissions, destructive cleanup, and external effects.

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
dynamic workflow, approved-task justification, and canonical task planning. Subagent
recommendations are not verification evidence.

## Dynamic Workflow

Workflow ID: specify-governed-deterministic-model-work-submission
Mode: Sequential
Increment Limit: 2
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on authority-source ambiguity, profile/capability conflation,
implicit supported-entry-point or execution authority, unclosed replay/refusal behavior,
silent schema widening, failed verification, checkpoint drift, new authority,
exhausted bounds, or unsafe recovery.

### Increment 1 - specify-governed-model-work-submission-contract

State: Completed
Depends On: none
Scope: Review the existing typed ModelWork, submission, authority, identity, replay,
and internal process boundaries and accept one RFC defining the smallest governed
deterministic-fake submission source without implementation or supported caller.
Exit Criteria: The RFC, accepted decision, indexes, architecture/state corrections,
Roadmap, task cursor, Changelog, and focused evidence are synchronized and focused
governance passes.
Verification: RFC/decision/architecture/index/ownership/dynamic-workflow/approved-task/
task-justification/planner tests plus `git diff --check`.
Next Action: Commit the verified documentation increment and select Increment 2.

### Increment 2 - verify-and-close-governed-model-work-submission-specification

State: In Progress
Depends On: specify-governed-model-work-submission-contract
Scope: Run the full Markdown-sensitive Java 17 regression, record fresh evidence,
close the task/handoff, and commit the verified specification closure.
Exit Criteria: The full regression passes with results read, canonical documents are
current, the intended local commits are clean, and the checkpoint is stable and clear.
Verification: Full `.\scripts\gradle.ps1 test`, focused final governance,
diff/commit/status inspection, and checkpoint reconciliation.
Next Action: Await separate authority to implement RFC-0024 RED-first.

## Next

Specify and verify RFC-0024, then await separate authority for its RED-first
implementation.
