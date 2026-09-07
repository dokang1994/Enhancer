# Current Task

## Status

In Progress

## Task

Implement RFC-0024 RED-first as one internal deterministic-fake typed ModelWork
submission source, preserving closed capability provenance, manifest-first replay, and
the absence of any supported typed ingress.

## Task ID

implement-governed-deterministic-model-work-submission

## Context

RFC-0023 typed execution and the RFC-0024 producer are now internally Integrated in one
test-owned real-filesystem/JVM path. One complete caller profile remains untrusted data,
one closed repository-owned source independently supplies `deterministic-echo`, first
use builds exact typed intent through the existing durable submission service, and
replay resolves the manifest before clock or repository context. No production
composition or supported ingress constructs the producer. The prior delivery task was
Completed and local/remote `main` were synchronized before the user requested
continuation on 2026-09-07.

## Justified By

- User continuation request on 2026-09-07 into RFC-0024 governed deterministic ModelWork submission implementation
- User continuation request on 2026-09-04 into governed deterministic ModelWork submission specification

## Approval

The user's 2026-09-07 continuation authorizes the minimum RED-first Java/test
implementation of RFC-0024: a closed immutable request, an independent fixed
deterministic capability source, first-use governed task/snapshot/envelope/manifest
construction, exact manifest-first replay, delegation to the unchanged durable
submission service, and one test-owned producer-to-existing-internal-worker integration.
It authorizes architecture, compact mirror, Project State, Roadmap, task,
decision/index, verification, handoff, and Changelog synchronization and ordinary
local GREEN commits.

It authorizes no supported CLI/API/editor/Desktop entry point, typed spool publisher or
receiver, legacy submission/receiver widening, general model router or provider,
endpoint, remote model transmission, network, credentials, pricing or spend, MCP,
durable schema version or migration, runtime-event ingress, push, merge, release,
deployment, permission change, destructive cleanup, or external effect.

## Acceptance Criteria

- A separate immutable request retains exactly the canonical submission UUID, expected
  active task ID, bounded producer, relative target, lowercase expected-response digest,
  exact complete profile, bounded capacity, and `NORMAL`/`EXPEDITED` priority; it has no
  capability or execution-authority field.
- A closed repository-owned deterministic-fake capability source supplies exactly
  `deterministic-echo` independently of the request, profile, target, candidate,
  envelope, manifest, queue, CLI, environment, repository content, or ambient state.
- Capability/profile disagreement is persisted unchanged and remains observable only
  at later RFC-0016 admission; the producer neither repairs nor pre-refuses it.
- First use derives existing stable identities, point-resolves the manifest, loads
  governed context and exact active task once, requires `model-invoke`, captures one
  clock value and repository-memory snapshot, builds one exact ModelWork envelope and
  manifest, then delegates to unchanged `DurableWorkSubmissionService`.
- Replay resolves the manifest before clock/context/task/snapshot work, validates all
  caller intent plus derived identities and the fixed capability, delegates exact
  replay without manifest rewrite or queue revision, and fails closed on every drift.
- Failure ordering preserves zero queue/manifest mutation before valid first-use intent
  and existing manifest-before-queue recovery prefixes without a new store or format.
- One temporary-filesystem integration connects only this producer to the existing
  internal model-aware worker and proves verified completion plus one typed pre-call
  refusal without a supported caller.
- Source and architecture guards prove that no CLI, spool receiver, provider, network,
  credential, legacy execution path, or current supported Scheduler composition
  constructs the producer.
- Existing envelope/spool v2, manifest v3, queue v4, runtime v5, pending-finalization
  v2, Model RunRecord v2, and legacy v1 bytes remain unchanged.
- Each observable increment is RED-first, focused Java 17 verification and
  `git diff --check` pass, the final README-owned regression passes freshly, documents
  are synchronized, and every GREEN increment is committed locally.

## Out Of Scope

Supported CLI/API/editor/Desktop input; typed spool publisher or receiver; legacy
command or receiver widening; general/provider model selection, router, registry,
endpoint, remote transmission, network, credentials, pricing or spend; MCP; durable
schema or migration; runtime-event ingress; current Model RunRecord v2 provenance
widening; durable terminal pre-call refusal; push, merge, release, deployment,
permissions, destructive cleanup, and external effects.

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
`docs/verification-log.md`. Each RED is classified against RFC-0024, RFC-0017 through
RFC-0023, existing durable formats, and v1 compatibility before production changes.
Subagent reports are recommendations, never verification evidence.

## Dynamic Workflow

Workflow ID: implement-governed-deterministic-model-work-submission
Mode: Sequential
Increment Limit: 4
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on capability-source ambiguity, request authority widening,
profile repair or defaulting, replay-before-manifest violation, unclassified RED,
supported-ingress reachability, schema drift, v1 compatibility failure, failed
verification, checkpoint drift, new authority, exhausted bounds, or unsafe recovery.

### Increment 1 - add-closed-model-submission-request-and-capability-source

State: Completed
Depends On: none
Scope: Add RED-first closed request and repository-owned fixed deterministic capability
source values without a service caller.
Exit Criteria: Exact request shape, validation bounds, absence of a capability field,
fixed source value/closed construction, profile disagreement retention, reflection and
source guards, and zero production caller are GREEN.
Verification: New value/source tests, existing profile/payload/candidate locality tests,
relevant architecture governance, and `git diff --check`.
Next Action: Commit the GREEN value/source increment and select Increment 2.

### Increment 2 - add-manifest-first-model-submission-service

State: Completed
Depends On: add-closed-model-submission-request-and-capability-source
Scope: Add RED-first first-use construction and exact replay through the unchanged
durable submission service without connecting execution or a supported caller.
Exit Criteria: Governed context/task/Tool/snapshot/envelope/manifest ordering, fixed
capability projection, every caller-intent replay comparison, no replay recapture,
failure prefixes, and unchanged durable schemas are GREEN.
Verification: New producer service tests plus generated submission, manifest, durable
submission, queue/runtime format, architecture guard, and `git diff --check` regression.
Next Action: Commit the GREEN service increment and select Increment 3.

### Increment 3 - connect-test-owned-model-submission-to-internal-worker

State: Completed
Depends On: add-manifest-first-model-submission-service
Scope: Connect one temporary test-owned producer/manifest/queue path to the existing
internal model-aware worker, with no production or interface caller.
Exit Criteria: Verified completion and one typed pre-call refusal are proven end to end;
legacy paths and supported interfaces remain unchanged and source guards pass.
Verification: Producer-to-worker real-filesystem/JVM integration, RFC-0017-through-
RFC-0024 focused suites, locality/entry-point guards, and `git diff --check`.
Next Action: Commit the GREEN internal integration and select Increment 4.

### Increment 4 - verify-and-close-rfc-0024-implementation

State: In Progress
Depends On: connect-test-owned-model-submission-to-internal-worker
Scope: Run the full Markdown-sensitive Java 17 regression, read results, synchronize
capability state and canonical documents, close the task/handoff, and commit closure.
Exit Criteria: Full regression passes, documents and commits are current, Git is clean,
and the stable checkpoint is cleared.
Verification: Full `.\scripts\gradle.ps1 test`, final focused RFC-0024/governance
suites, diff/commit/status inspection, and checkpoint reconciliation.
Next Action: Await separate authority for supported model-aware Scheduler composition.

## Next

Run the full Increment 4 regression and synchronize the completed RFC-0024 task.
