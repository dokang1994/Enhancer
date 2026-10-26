# Current Task

## Status

In Progress

## Task

Specify RFC-0032 as the governed, provider-neutral provisioning and rotation contract
for the protected catalog path and independently derived whole-file SHA-256 pin consumed
by RFC-0031.

## Task ID

specify-rfc-0032-governed-capability-catalog-provisioning-and-rotation

## Context

RFC-0031 is Contract Verified but cannot truthfully produce production capability facts
until separately governed actors establish canonical catalog values from independent
evidence and publish a protected path-and-pin binding. The digest proves exact bytes;
it does not prove the truth, freshness beyond the declared interval, or provider
attestation of those bytes.

This task defines only that missing contract. It does not implement a provisioner,
install a catalog, mutate operating-system permissions, connect the source to RFC-0030
or RFC-0029, or authorize any provider, credential, network, spend, execution,
persistence, runtime, MCP, release, deployment, or push path.

## Justified By

- User continuation request on 2026-10-26 into governed capability catalog provisioning and rotation specification

## Approval

The user's continuation authorizes repository-local documentation changes, checkpoint
state, fresh verification, and the ordinary local commits required for verified GREEN
increments of this Active Task.

It does not authorize product or test-source changes; a provisioner, writer, command,
launcher, platform adapter, installed catalog, permission mutation, credential or
secret access, provider or network access, paid service, source/evaluator/Router
consumer, route execution, runtime or durable-state integration, MCP, destructive
action, push, merge, tag, release, or deployment.

## Acceptance Criteria

- One accepted RFC-0032 defines distinct evidence-author, operator-intent,
  installer/publisher, and runtime-reader authorities without granting any of them
  remote execution authority.
- Canonical candidate validation and internally derived digest bind the exact bytes,
  path, and pin without accepting a caller-supplied resulting pin or catalog
  self-approval.
- Installation and rotation have closed preconditions, cooperative locking,
  compare-and-swap, immutable artifact publication, fixed binding publication,
  exact-replay, stale-intent, failure, crash-recovery, and no-automatic-rollback
  semantics.
- Permission, durability, truthful evidence, audit, cleanup, revocation, and privileged
  anti-rollback claims are bounded explicitly and do not exceed documentation evidence.
- Architecture, Project State, Roadmap, RFC track, Changelog, verification log, Active
  Task, and session handoff remain consistent with their document ownership.
- Focused governance checks, `git diff --check`, and the full README-owned Java 17
  regression pass freshly before completion.

## Out Of Scope

Java or test-source changes; production provisioner/writer; installed catalog or source
composition; operator CLI/launcher; broker; platform adapter; real filesystem layout,
identity, ACL, ownership, mode, permission, durability, deployment, cleanup, uninstall,
or privileged anti-rollback enforcement; signed catalogs, keys, credentials, secrets,
remote retrieval or provider attestation; provider, endpoint, network, spend,
same-invocation RFC-0030/RFC-0029 consumer, route execution, persistence, runtime, MCP,
push, merge, tag, release, and unrelated external effects.

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

Run focused RFC/decision/index/document-ownership/Architecture-mirror/Roadmap/Planner/
Assisted-Loop/dynamic-workflow governance checks after documentation changes. Run
`git diff --check` and the complete README-owned Java 17 test task before completion,
then inspect generated test evidence and the final diff.

## Dynamic Workflow

Workflow ID: specify-rfc-0032-governed-capability-catalog-provisioning-and-rotation
Mode: Sequential
Increment Limit: 3
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on failed verification, authority conflict, task/checkpoint drift,
scope expansion into implementation or external effects, exhausted workflow bounds, or
unsafe recovery.

### Increment 1 - establish-provisioning-authority-and-threat-contract

State: Completed
Depends On: none
Scope: Reconcile RFC-0031 and installation precedents, obtain bounded read-only security,
state-machine, and governance reviews, and define RFC-0032's authorities, trusted inputs,
threat boundary, and rejected trust shortcuts.
Exit Criteria: The accepted RFC draft closes authority ownership and truthfulness without
granting implementation or remote execution authority.
Verification: Focused governance tests and `git diff --check`.
Next Action: Define installation and rotation state transitions and failure behavior.

### Increment 2 - specify-installation-and-rotation-state-machine

State: Completed
Depends On: establish-provisioning-authority-and-threat-contract
Scope: Specify canonical validation, immutable artifact and fixed binding publication,
lock/CAS/replay semantics, crash recovery, observability, and explicit platform limits.
Exit Criteria: Every admitted operation and failure has deterministic authority-preserving
behavior, with cleanup, permission enforcement, and anti-rollback kept separate.
Verification: Focused governance tests and `git diff --check`.
Next Action: Reconcile repository-owned architecture, state, roadmap, and lifecycle docs.

### Increment 3 - reconcile-and-verify-rfc0032

State: In Progress
Depends On: specify-installation-and-rotation-state-machine
Scope: Reconcile Architecture and its compact mirror, Project State, Roadmap and RFC
track, Changelog, verification evidence, Active Task, and session handoff; run complete
fresh verification and commit the closure.
Exit Criteria: RFC-0032 is accepted, all owning documents are current, the full
Markdown-sensitive regression is GREEN, Git is clean after required local commits, and
the stable checkpoint is cleared.
Verification: Focused governance tests, full README-owned Java 17 regression, generated
JUnit evidence inspection, `git diff --check`, and Git status/log review.
Next Action: Await separate authority for implementation or the same-invocation consumer.

## Next

Complete Increment 3 by reconciling the accepted RFC-0032 contract into its owning
architecture, state, roadmap, lifecycle, and delivery documents, then run the complete
fresh regression. Do not begin implementation or any external effect.
