# Current Task

## Status

Completed

## Task

Specify RFC-0027: the smallest manifest-authorized typed `ModelWork` spool publication
and receive boundary that preserves the existing direct submission path, legacy Work
receiver, durable authority sources, and recoverable Scheduler admission semantics.

## Task ID

specify-manifest-authorized-model-work-spool-ingress

## Context

RFC-0026 completed the first supported direct typed-submission path and deliberately
left typed spool publication and receive as separate work. RFC-0018 already defines a
payload-sensitive transport-spool v2 format and forbids `scheduler-receive-work` from
accepting `ModelWorkPayload`, because its caller-supplied capability can neither
authenticate first-use authority nor replace a pre-existing exact submission manifest.
The Gate 9 Roadmap still names typed spool ingress as later work, and the user requested
continuation on 2026-09-10.

The existing file spool is an at-least-once transport point: publication is not
Scheduler admission, and acknowledgement follows durable admission. A specification
must therefore define manifest-before-publication ordering, receiver authorization,
exact replay and conflict behavior, crash prefixes, acknowledgement and capacity
release, and legacy compatibility before implementation can safely expose this path.

## Justified By

- User continuation request on 2026-09-09 into supported typed ModelWork submission specification
- User continuation request on 2026-08-21 into the Scheduler complete-profile transport specification
- 2026-07-14: Make Enhancer An Event-Driven Interoperable AI Operating Platform

## Approval

The user's 2026-09-10 continuation authorizes a documentation-only RFC and accepted
decision for one separate typed spool publisher and one manifest-authorized typed
receiver. The contract may define exact supported command surfaces, complete profile
and caller intent sources, immutable manifest creation and point resolution, transport
publication, destination binding, queue creation/admission, acknowledgement, replay,
crash recovery, capacity release, refusal classes, bounded output, source/locality
guards, compatibility, and a RED-first implementation sequence. It authorizes minimal
Architecture, compact mirror, Project State, Roadmap, RFC index, task, decision/index,
verification, handoff, and Changelog synchronization plus ordinary local GREEN
commits.

It authorizes no Java or test-source change, actual publication or receive, widening of
`scheduler-spool-work` or `scheduler-receive-work`, capability input or inference,
profile defaults, provider/router/registry, endpoint, remote transmission, network,
credentials, pricing or spend, MCP, new durable schema or migration, runtime-event
change, background service, push, merge, release, deployment, permission change,
destructive cleanup, or other external effect.

## Acceptance Criteria

- RFC-0027 defines separate typed publisher and receiver commands without changing the
  existing RFC-0026 direct-submission command or either legacy Work spool command.
- The publisher obtains one complete strict RFC-0026 profile input and the same
  caller-owned deterministic submission intent while accepting no capability, queue,
  correlation, logical-run, occurrence-time, candidate, provider, or execution-policy
  authority that belongs to another source.
- A current exact submission manifest is durable before a typed transport point becomes
  visible. The receiver point-resolves that manifest by the canonical message identity,
  requires exact destination and envelope equality, and obtains queue identity,
  capacity, required capability, and priority only from the manifest.
- Profile capability remains untrusted requirements data. It is never copied,
  normalized, pre-approved, or accepted from CLI or transport as the authoritative
  `WorkItem.requiredCapability`.
- Queue creation, exact durable admission, transport acknowledgement, and capacity
  release have an explicit fail-closed order. Every pre-manifest, post-manifest,
  post-publication, post-admission, and post-acknowledgement crash prefix has a bounded
  replay or operator-recovery outcome without duplicate queue work or false completion.
- Missing, inactive, mismatched, corrupt, unsupported, outside-root, link/reparse,
  collision, route, payload-kind, identity, manifest, queue, capacity, and durable
  content failures are classified before unauthorized mutation; corrupt or conflicting
  points are never acknowledged as successfully received.
- Existing message-envelope v1/v2, transport-spool v1/v2, submission manifest v3,
  Scheduler queue v4, runtime v5, pending-finalization v2, Model RunRecord v2, Result,
  and runtime-event formats are either proven sufficient unchanged or any required new
  durable fact is explicitly left blocked for separate compatibility authority.
- The RFC defines bounded non-secret output, no discovery/ambient fallback, explicit
  operator recovery, RED-first implementation increments, architecture guards, and
  real-filesystem/JVM evidence for a later implementation task.
- RFC/decision indexes, Architecture and compact mirror, Project State, Roadmap, task
  cursor, Changelog, append-only verification evidence, and handoff are synchronized
  by ownership. Focused Markdown-sensitive governance, `git diff --check`, and the full
  README-owned Java 17 regression pass freshly before completion.

## Out Of Scope

Java or test-source implementation; actual typed spool publication, receive, Scheduler
execution, provider invocation, or remote transmission; widening legacy commands;
caller capability input or inference; profile defaults or partial input; combined
publish/receive/execute wrappers; provider/router/registry, endpoint, network,
credentials, pricing or spend; MCP; background polling or service operation; new
durable schema, migration, sidecar, runtime-event kind, RunRecord provenance, or
cancellation propagation; push, merge, release, deployment, permissions, destructive
cleanup, and external effects.

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
typed/legacy command separation, manifest-owned authority, transport recovery, and
unsupported provider/network boundaries. Subagent recommendations are not verification
evidence.

## Dynamic Workflow

Workflow ID: specify-manifest-authorized-model-work-spool-ingress
Mode: Sequential
Increment Limit: 2
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on manifest/capability/profile conflation, legacy command
widening, unresolved publication idempotency or acknowledgement semantics, unsafe crash
recovery, durable schema widening, provider/network authority, failed verification,
checkpoint drift, new authority, exhausted bounds, or unsafe recovery.

### Increment 1 - specify-manifest-authorized-typed-spool-contract

State: Completed
Depends On: none
Scope: Review RFC-0018 through RFC-0026 and the existing transport, manifest, queue,
publisher, receiver, CLI, and recovery boundaries, then accept one RFC defining the
minimum separate typed publisher/receiver contract without implementation or execution.
Exit Criteria: The RFC, accepted decision, indexes, architecture/state/roadmap/task/
Changelog synchronization, and focused evidence are current and focused governance
passes.
Verification: RFC/decision/architecture/index/ownership/dynamic-workflow/approved-task/
task-justification/planner/source-boundary tests plus `git diff --check`.
Next Action: Commit the verified documentation increment and select Increment 2.

### Increment 2 - verify-and-close-typed-spool-specification

State: Completed
Depends On: specify-manifest-authorized-typed-spool-contract
Scope: Run the full Markdown-sensitive Java 17 regression, record fresh evidence, close
the task/handoff, and commit the verified specification closure.
Exit Criteria: The full regression passes with results read, canonical documents are
current, intended local commits are clean, and the checkpoint is stable and clear.
Verification: Full `.\scripts\gradle.ps1 test`, final focused governance,
diff/commit/status inspection, and checkpoint reconciliation.
Next Action: Await separate authority to implement RFC-0027 RED-first.

## Next

Await separate user authority to implement RFC-0027 RED-first.
