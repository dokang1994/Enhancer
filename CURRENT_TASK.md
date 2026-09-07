# Current Task

## Status

Completed

## Task

Specify RFC-0025: the smallest supported deterministic-fake model-aware Scheduler
composition that supplies every existing process-execution configuration source without
yet exposing typed ModelWork submission, publication, or receive.

## Task ID

specify-supported-model-aware-scheduler-composition

## Context

RFC-0023 is internally Integrated for typed deterministic-fake process execution and
RFC-0024 is internally Integrated for governed manifest-first typed submission, with
their only connection owned by tests. The supported `scheduler-cycle`,
`scheduler-drain`, and `scheduler-service` commands still construct only the legacy
process worker and do not supply `ModelProcessExecutionConfiguration`. RFC-0024 orders
an explicit supported model-aware Scheduler composition before any interface-owned
complete-profile format or supported typed submission/transport ingress. The completed
RFC-0024 task named this separately authorized boundary, and the user requested
continuation on 2026-09-07.

## Justified By

- User continuation request on 2026-09-07 into supported model-aware Scheduler composition specification
- User continuation request on 2026-09-07 into RFC-0024 governed deterministic ModelWork submission implementation
- User continuation request on 2026-09-04 into governed deterministic ModelWork submission specification

## Approval

The user's 2026-09-07 continuation authorizes a documentation-only RFC and accepted
decision defining one supported deterministic-fake model-aware composition for the
existing foreground Scheduler execution commands. The contract may define the exact
command-selection signal, complete all-or-none process-configuration inputs, fixed
repository-owned fake implementation sources, existing filesystem store sources,
payload-kind dispatch, legacy compatibility, runtime-event compatibility, recovery and
refusal behavior, output disclosure, and a bounded RED-first implementation sequence.
It authorizes the minimal Architecture, compact mirror, Project State, Roadmap, RFC
index, task, decision/index, verification, handoff, and Changelog synchronization and
ordinary local GREEN commits.

It authorizes no Java or test-source change, actual Scheduler/model execution now,
typed submission or spool publication/receive, complete-profile interface format,
legacy submission/receiver widening, provider/router/registry, endpoint, remote model
transmission, network, credentials, pricing or spend, MCP, durable schema version or
migration, new runtime-event kind, push, merge, release, deployment, permission change,
destructive cleanup, or external effect.

## Acceptance Criteria

- RFC-0025 selects one explicit optional all-or-none deterministic-fake model execution
  group shared by `scheduler-cycle`, `scheduler-drain`, and `scheduler-service`; omission
  preserves the exact current legacy-only composition and partial or unknown model
  configuration fails during CLI validation before queue/store access.
- The group names exactly one closed execution selector plus bounded gateway timeout,
  maximum response characters, maximum prompt-read bytes, Tool timeout, and a bounded
  repeatable denied-Tool set. It accepts no capability, profile, task, target, digest,
  candidate, endpoint, provider, credential, price, or network value.
- The contract names every `ModelProcessExecutionConfiguration` source and keeps them
  distinct: caller-supplied Scheduler resource/policy limits, repository-owned fixed
  deterministic gateway/candidate/token semantics, exact queued ModelWork profile,
  manifest-retained required capability, current task authority, and no-cancellation
  execution token until separately authenticated propagation exists.
- The model-aware composition reuses the command's exact project, queue, runtime,
  external-effect, cycle-checkpoint, evidence, RunRecord, invocation, owner, retry,
  lease, process-timeout, clock, and optional runtime-event sources. The same
  `FileSystemRunRecordStore` and evidence root serve the existing v1/v2 resolver and
  model validation; no sidecar or second record format is introduced.
- Enabling the group selects the existing payload-kind-aware worker and permits legacy
  Work and typed ModelWork to retain their existing distinct paths. It does not create,
  publish, receive, discover, or rewrite queued work and does not infer model authority
  from legacy payloads.
- Typed execution preserves RFC-0016 through RFC-0024 ordering and identity, performs at
  most one deterministic-fake call per AgentRun, publishes only verified Model
  RunRecord v2, retains crash/retry/finalization recovery, and exposes pre-call refusal
  only through the existing error/recovery prefix without inventing a terminal durable
  refusal.
- The existing optional runtime-event publication group remains composable with the
  model-aware worker and retains its exact event authority. RFC-0025 adds no model event
  kind and does not silently disable an already selected recorder.
- Supported command output adds at most a bounded non-secret execution-mode fact;
  prompt, response, profile, denied-Tool content, evidence content, and credentials are
  never printed.
- Existing message/spool v2, manifest v3, queue v4, runtime v5, pending-finalization v2,
  RunRecord v1/Model RunRecord v2, runtime-event, and legacy command bytes remain
  sufficient and unchanged.
- The RFC explicitly leaves the interface-owned complete-profile format and supported
  typed submission/publisher/receiver to later separately authorized work.
- RFC/decision indexes, Architecture and compact mirror, Project State, Roadmap, task
  cursor, Changelog, and append-only verification evidence are synchronized according
  to document ownership. Focused Markdown-sensitive governance, `git diff --check`, and
  the full README-owned Java 17 regression pass freshly before completion.

## Out Of Scope

Java or test-source implementation; actual model invocation or Scheduler mutation;
typed ModelWork submission, publisher, receiver, Message Bus or runtime-event ingress;
interface-owned profile file/schema/parser or profile CLI fields; legacy submission or
receiver widening; provider selection, router, registry, endpoint, remote transmission,
network, credentials, pricing or spend; MCP; new durable schema, migration, sidecar,
record provenance, event kind, cancellation propagation, or terminal pre-call refusal;
push, merge, release, deployment, permissions, destructive cleanup, and external
effects.

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
Documentation verification must cover RFC indexing, decision indexing, architecture and
document ownership, dynamic workflow, approved-task justification, canonical planning,
and source-boundary consistency. Subagent recommendations are not verification evidence.

## Dynamic Workflow

Workflow ID: specify-supported-model-aware-scheduler-composition
Mode: Sequential
Increment Limit: 2
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on configuration-source ambiguity, capability/profile/policy
conflation, implicit typed ingress, legacy behavior drift, runtime-event loss, durable
schema widening, unsupported provider/network authority, failed verification,
checkpoint drift, new authority, exhausted bounds, or unsafe recovery.

### Increment 1 - specify-supported-model-aware-scheduler-composition-contract

State: Completed
Depends On: none
Scope: Review the existing supported Scheduler composition and RFC-0016 through
RFC-0024 boundaries, then accept one RFC defining the smallest explicit model-aware
execution composition without implementation or typed ingress.
Exit Criteria: The RFC, accepted decision, indexes, architecture/state/roadmap/task/
Changelog synchronization, and focused evidence are current and focused governance
passes.
Verification: RFC/decision/architecture/index/ownership/dynamic-workflow/approved-task/
task-justification/planner/source-boundary tests plus `git diff --check`.
Next Action: Commit the verified documentation increment and select Increment 2.

### Increment 2 - verify-and-close-supported-model-aware-scheduler-specification

State: Completed
Depends On: specify-supported-model-aware-scheduler-composition-contract
Scope: Run the full Markdown-sensitive Java 17 regression, record fresh evidence, close
the task/handoff, and commit the verified specification closure.
Exit Criteria: The full regression passes with results read, canonical documents are
current, intended local commits are clean, and the checkpoint is stable and clear.
Verification: Full `.\scripts\gradle.ps1 test`, final focused governance,
diff/commit/status inspection, and checkpoint reconciliation.
Next Action: Await separate authority to implement RFC-0025 RED-first.

## Next

Await separate authority to implement RFC-0025 RED-first.
