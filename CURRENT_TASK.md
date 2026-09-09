# Current Task

## Status

In Progress

## Task

Specify RFC-0026: the smallest interface-owned complete-profile input and supported
deterministic ModelWork submission surface over the existing RFC-0024 producer and
RFC-0025 model-aware Scheduler composition.

## Task ID

specify-supported-model-work-submission-input

## Context

RFC-0024 provides a governed manifest-first deterministic ModelWork producer, and
RFC-0025 connects the supported foreground Scheduler commands to the typed execution
path. Both are Integrated, but RFC-0024 remains reachable only from tests because no
supported interface owns the complete `ModelExecutionProfile` input. Its ordered
follow-up selects an interface-owned complete-profile format plus either direct typed
submission or spool publication before any receiver work. Direct submission is the
smallest boundary because it reuses the existing producer and durable admission path
without adding a transport artifact, acknowledgement, or receiver recovery contract.
The completed RFC-0025 task requested separate authority, and the user requested
continuation on 2026-09-09.

## Justified By

- User continuation request on 2026-09-09 into supported typed ModelWork submission specification
- User continuation request on 2026-09-08 into RFC-0025 supported model-aware Scheduler composition implementation
- User continuation request on 2026-09-07 into supported model-aware Scheduler composition specification

## Approval

The user's 2026-09-09 continuation authorizes a documentation-only RFC and accepted
decision defining one interface-owned, complete, strict, bounded profile input and one
separate supported deterministic ModelWork submission surface over the existing
RFC-0024 producer. The contract may define the exact command and arguments, profile
format and read boundary, validation and authority sources, first-use and replay
ordering, output disclosure, compatibility, recovery, refusal behavior, downstream
RFC-0025 operator sequence, and a bounded RED-first implementation plan. It authorizes
minimal Architecture, compact mirror, Project State, Roadmap, RFC index, task,
decision/index, verification, handoff, and Changelog synchronization and ordinary local
GREEN commits.

It authorizes no Java or test-source change, actual submission or execution, spool
publisher or receiver, legacy submission/receiver widening, capability input or
inference, profile defaults or partial profiles, provider/router/registry, endpoint,
remote transmission, network, credentials, pricing or spend, MCP, durable schema
version or migration, runtime-event change, push, merge, release, deployment,
permission change, destructive cleanup, or external effect.

## Acceptance Criteria

- RFC-0026 selects direct supported typed submission rather than spool publication as
  the smallest next boundary and leaves any publisher/receiver protocol to later
  separately authorized work.
- The separate `scheduler-submit-deterministic-fake-model-work` interface owns exactly
  one complete `ModelExecutionProfile` input without widening `scheduler-submit`,
  `scheduler-submit-generated`, `scheduler-spool-work`, or `scheduler-receive-work`.
- Its explicit project-relative profile file is a no-link strict-UTF-8 input bounded to
  4,096 bytes. Thirteen ordered LF-terminated entries retain every RFC-0014 component
  exactly and accept no missing, partial, duplicate, unknown, reordered, defaulted,
  inferred, registry, environment, or fallback value.
- Capability remains independently fixed by the RFC-0024 repository-owned source and
  is absent from caller input. Profile/capability disagreement remains an observable
  later RFC-0016 refusal rather than being normalized or self-certified.
- Caller-owned submission identity, task, producer, target, expected response digest,
  profile, queue capacity, and explicit priority project into the unchanged RFC-0024
  request;
  the interface supplies no candidate, provider, endpoint, credential, price, network,
  execution-policy, or Scheduler process-configuration authority.
- First use and exact replay preserve RFC-0024 manifest-first ordering, context and
  clock consultation boundaries, durable admission semantics, identity comparison,
  refusal behavior, and unchanged manifest/queue bytes. Submission never implies
  execution or completion.
- The supported operator sequence remains separate typed submission followed by an
  explicitly model-configured RFC-0025 cycle, drain, or service invocation; no wrapper,
  polling, background work, implicit execution, or typed receiver is added.
- Existing message/spool v2, manifest v3, queue v4, runtime v5, pending-finalization v2,
  Model RunRecord v2, and runtime-event formats remain sufficient and unchanged.
- The RFC defines bounded non-secret output, failure classes, recovery instructions,
  RED-first implementation increments, source/locality guards, and real filesystem/JVM
  integration evidence for a later implementation task.
- RFC/decision indexes, Architecture and compact mirror, Project State, Roadmap, task
  cursor, Changelog, and append-only verification evidence are synchronized according
  to document ownership. Focused Markdown-sensitive governance, `git diff --check`, and
  the full README-owned Java 17 regression pass freshly before completion.

## Out Of Scope

Java or test-source implementation; actual typed submission or Scheduler execution;
spool publisher, receiver, Message Bus or runtime-event ingress; legacy command
widening; caller capability input or inference; profile defaults, partial input,
registry, environment lookup, or provider configuration; candidate/provider/router,
endpoint, remote transmission, network, credentials, pricing or spend; MCP; new durable
schema, migration, sidecar, RunRecord provenance, event kind, cancellation propagation,
or terminal pre-call refusal; push, merge, release, deployment, permissions,
destructive cleanup, and external effects.

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
Documentation verification must cover RFC and decision indexing, architecture and
document ownership, dynamic workflow, approved-task justification, canonical planning,
profile-source completeness, command separation, and unsupported-ingress/source
boundaries. Subagent recommendations are not verification evidence.

## Dynamic Workflow

Workflow ID: specify-supported-model-work-submission-input
Mode: Sequential
Increment Limit: 2
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on profile-source ambiguity, capability/profile/policy
conflation, legacy command widening, implicit execution or transport ingress, durable
schema widening, provider/network authority, failed verification, checkpoint drift,
new authority, exhausted bounds, or unsafe recovery.

### Increment 1 - specify-supported-model-work-submission-contract

State: Completed
Depends On: none
Scope: Review RFC-0014 through RFC-0025 and the existing CLI/producer/storage
boundaries, then accept one RFC defining the smallest complete-profile input and direct
supported typed submission surface without implementation, publication, receive, or
execution.
Exit Criteria: The RFC, accepted decision, indexes, architecture/state/roadmap/task/
Changelog synchronization, and focused evidence are current and focused governance
passes.
Verification: RFC/decision/architecture/index/ownership/dynamic-workflow/approved-task/
task-justification/planner/source-boundary tests plus `git diff --check`.
Next Action: Commit the verified documentation increment and select Increment 2.

### Increment 2 - verify-and-close-supported-model-work-submission-specification

State: In Progress
Depends On: specify-supported-model-work-submission-contract
Scope: Run the full Markdown-sensitive Java 17 regression, record fresh evidence, close
the task/handoff, and commit the verified specification closure.
Exit Criteria: The full regression passes with results read, canonical documents are
current, intended local commits are clean, and the checkpoint is stable and clear.
Verification: Full `.\scripts\gradle.ps1 test`, final focused governance,
diff/commit/status inspection, and checkpoint reconciliation.
Next Action: Await separate authority to implement RFC-0026 RED-first.

## Next

Commit Increment 1, run the full Increment 2 regression, and close the verified
RFC-0026 specification.
