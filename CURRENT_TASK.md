# Current Task

## Status

In Progress

## Task

Define and accept RFC-0018 as the minimum typed and versioned Scheduler complete-profile
source and transport contract across message, submission, queue, runtime, recovery,
migration, and no-default cutover boundaries without implementation.

## Task ID

specify-scheduler-model-profile-transport

## Context

RFC-0017 requires one complete `ModelExecutionProfile` and an independent governed
capability projection per invocation. The current Scheduler can later project the exact
active `WorkItem.requiredCapability`, but `WorkPayload.ExecutionInput` retains only a
target path and expected digest. Message-envelope v1, transport-spool v1, submission
manifest v2, Scheduler queue v3, and AgentRuntime v4 persist that incomplete WorkItem
shape, so a durable complete-profile source requires an explicit coordinated version
and migration contract rather than ambient or manifest-only lookup.

## Justified By

- User continuation request on 2026-08-21 into the Scheduler complete-profile transport specification
- User continuation request on 2026-08-21 into the complete model invocation input specification

## Approval

The 2026-08-21 continuation authorizes one documentation-only RFC-0018 specification,
bounded read-only schema/recovery/authority reviews, owning architecture/RFC-planning/
index/decision/task/changelog/verification synchronization, fresh Java 17 Markdown-
sensitive verification, development-session checkpoints, and ordinary local commits at
verified GREEN increment boundaries. It authorizes no Java or binary-schema change,
artifact migration, command or caller change, Scheduler/admission/gateway runtime
wiring, provider/network behavior, push, merge, release, deployment, external effect,
or destructive cleanup.

## Acceptance Criteria

- RFC-0018 defines the minimum typed model-work input that retains the existing target
  and expected-response digest plus one exact complete RFC-0014 profile without
  flattening, partial optionals, defaults, inference, registry, or ambient lookup.
- The profile remains untrusted requirements data and the exact active
  `WorkItem.requiredCapability` remains a separate unchanged governed projection;
  neither may source or self-certify the other or the profile model class.
- Every affected message-envelope, spool codec, submission manifest, Scheduler queue,
  AgentRuntime, WorkItem recovery, retry, and process-isolation representation is named
  with a coordinated version/cutover rule; no manifest-only or cross-store lookup is
  allowed.
- Legacy read-file work remains lossless and compatible. Existing model work without a
  complete profile is never silently upgraded or defaulted; migration/cutover behavior
  is deterministic, bounded, recoverable, and fail closed before admission or gateway.
- Exact replay, duplicate submission, queue/runtime recovery, isolated worker transfer,
  result verification, and RunRecord provenance requirements are explicit, while an
  RFC-0016 `Admitted` decision remains fresh and non-persisted.
- The contract changes no current Java, command, schema, artifact, or runtime behavior
  and adds no provider, route, endpoint, destination, credential, tokenizer, price,
  network, transmission, spend, or gateway-execution authority.
- Architecture, RFC index/planning, accepted-decision index, changelog, task cursor, and
  append-only verification evidence are synchronized, with focused governance and the
  full README-owned Java 17 regression passing freshly.

## Out Of Scope

Java implementation; binary schema changes or migrations; artifact rewrite; CLI,
submission, spool, queue, runtime, Tool, gateway, adapter, or process-worker changes;
caller cutover; profile parsing or registry; admission/runtime integration; model
suitability; routing, providers, endpoints, destinations, network or remote
transmission; credentials, paid services, pricing, tokenizers, usage normalization,
MCP, release, deployment, push, merge, history rewrite, permission changes, and
destructive cleanup.

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

- Increment 1: two bounded read-only reviews converged on a fifth typed model-work
  payload, exact independent WorkItem capability projection, payload-sensitive wire
  versioning, and stopped-owner fail-closed migration. Focused governance passed 21
  tests across 6 suites with zero failures, errors, or skips; `git diff --check` was
  clean and every changed path was Markdown.
- Increment 2 evidence is pending.

## Dynamic Workflow

Workflow ID: specify-scheduler-model-profile-transport
Mode: Sequential
Increment Limit: 2
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on schema-owner omission, recovery ambiguity, authority conflict,
incompatible legacy behavior, failed governance/regression verification, task drift,
checkpoint drift, new external authority, exhausted bounds, or unsafe recovery.

### Increment 1 - define-and-accept-rfc-0018

State: Completed
Depends On: none
Scope: Reconcile independent read-only schema/recovery/authority reviews, define and
accept the minimum RFC-0018 contract, and synchronize architecture, RFC planning/index,
decision, changelog, task, and append-only focused verification evidence.
Exit Criteria: The RFC resolves every acceptance criterion without implementation or
artifact change, focused governance passes, the diff is documentation-only and clean,
evidence is appended once, and the verified increment is committed locally.
Verification: RFC/decision/architecture/document-ownership/dynamic-workflow governance
tests and `git diff --check`.
Next Action: Commit the verified RFC-0018 specification, then select Increment 2.

### Increment 2 - verify-and-close-rfc-0018

State: In Progress
Depends On: define-and-accept-rfc-0018
Scope: Run the full README-owned Java 17 regression, synchronize only changed lifecycle
owners, rerun final Markdown governance, and close the task.
Exit Criteria: Full and final governance verification pass, no implementation or
maturity claim appears, the closure is committed locally, and the worktree/checkpoint
reach the intended clean stable state.
Verification: Full `test`, focused governance, JUnit XML aggregation,
`git diff --check`, and final Git/checkpoint inspection.
Next Action: Run the fresh full regression and close the RFC-0018 task.

## Next

Run the full regression and close RFC-0018 in Increment 2 after the Increment 1 commit.
