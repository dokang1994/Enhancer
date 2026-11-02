# Current Task

## Status

In Progress

## Task

Specify RFC-0033 as the protected runtime composition that reads RFC-0032's sole fixed
catalog binding and supplies its exact catalog path and pin to RFC-0031 for one stable
acquisition, without composing a Router consumer, provider, network path, or writer.

## Task ID

specify-protected-remote-capability-catalog-runtime-binding-composition

## Context

RFC-0032 is Contract Verified through an uncomposed package-private publisher and fixed
canonical binding. RFC-0031 is the sole production creator of RFC-0030 capability facts,
but today its protected absolute catalog path and independent pin have no runtime
composition source. The completed task records runtime binding composition and the
later same-invocation RFC-0031/RFC-0030/RFC-0029 consumer as separate continuations.

The runtime binding composition is selected first because the later consumer must not
invent or accept catalog trust inputs from a request, Router option, provider, CLI,
environment, repository, or working directory. This task specifies only the read-only
trust-input bridge and leaves production publisher composition and remote route
selection or execution separate.

## Justified By

- User continuation request on 2026-11-02 into protected remote capability catalog runtime binding composition specification

## Approval

The user's continuation authorizes repository-local RFC, decision, architecture,
roadmap, project-state, compact-mirror, task, changelog, verification-log, handoff, and
related documentation changes needed to specify the protected runtime binding
composition; checkpoint state; fresh repository verification; and the ordinary local
commits required for verified GREEN increments of this Active Task.

It does not authorize Java production or test implementation, production publisher
composition, an installed catalog or binding, permission mutation, a real installer or
operator, evidence authoring or storage, runtime deployment configuration, a
source/evaluator/Router caller, route execution, provider, endpoint, credential,
network transmission, spend, Tool/Evidence/RunRecord/Scheduler/runtime integration,
MCP, destructive action, push, merge, tag, release, or deployment.

## Acceptance Criteria

- RFC-0033 defines one read-only protected composition source for RFC-0032's fixed
  binding, including exact trust-input ownership, bounded no-follow acquisition,
  canonical binding and selected-catalog validation, stable observation around one
  RFC-0031 acquisition, and closed fail-closed outcomes.
- The specification preserves RFC-0031 as the sole production capability-facts creator
  and names the later same-invocation RFC-0031/RFC-0030/RFC-0029 option-construction
  boundary as its only next consumer.
- Requests, Router options, providers, catalogs, CLI arguments, environment/JVM
  properties, repositories, working directories, filenames, fallback, scans, caches,
  logs, and publisher results cannot select or override the binding trust root.
- The contract makes no writer, permission, installation, anti-rollback, provider,
  network, spend, execution, persistence, runtime, MCP, Operational, or platform-support
  claim.
- Architecture, Project State, Roadmap, compact mirror, Changelog, verification log,
  Active Task, decision index, RFC index, and session handoff remain consistent with
  document ownership.
- Focused governance/reference checks, `git diff --check`, and the full README-owned
  Java 17 regression pass freshly before completion.

## Out Of Scope

Java production or test implementation; publisher/evidence implementation or
composition; real installation, installed artifacts, operator CLI/launcher, broker,
authenticated transport, permission or ownership mutation, ACL/DACL/mode enforcement,
platform identity, deployment configuration, cleanup, retention, uninstall, rollback,
privileged anti-rollback, catalog signing, keys, credentials, secrets, provider
endpoint or adapter, network, transmission, spend, source/evaluator/Router caller,
route selection or execution, fallback, retry, durable route provenance,
Tool/Evidence/RunRecord/Scheduler/runtime-event integration, durable schema or
migration, MCP, plugin, background work, push, merge, tag, release, deployment, and
unrelated external effects.

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

Use structural and reference checks appropriate to a documentation-only specification.
Run focused decision/index/document-ownership/dynamic-workflow/Planner/Assisted-Loop and
Gate 9 architecture tests plus `git diff --check` at each GREEN boundary. Before task
completion run the complete README-owned Java 17 test task, inspect generated JUnit
evidence, and review final Git status and diff.

## Dynamic Workflow

Workflow ID: specify-protected-remote-capability-catalog-runtime-binding-composition
Mode: Sequential
Increment Limit: 2
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on failed verification, authority conflict, task/checkpoint drift,
scope expansion into implementation or external effects, exhausted workflow bounds, or
unsafe recovery.

### Increment 1 - specify-protected-runtime-binding-composition

State: Completed
Depends On: none
Scope: Record the accepted continuation decision and RFC-0033; define protected
binding-root ownership, bounded canonical acquisition, RFC-0031 delegation, stable
observation, closed outcomes, and the exact next consumer; update architectural owners.
Exit Criteria: The read-only trust-input bridge is fully specified without a writer,
production caller, route consumer, provider, or external effect.
Verification: Focused governance, RFC/reference, Gate 9 architecture tests, and
`git diff --check`.
Next Action: Reconcile remaining owning documents and run full verification.

### Increment 2 - reconcile-and-verify-rfc0033-specification

State: In Progress
Depends On: specify-protected-runtime-binding-composition
Scope: Reconcile Project State, Roadmap, compact mirror, Changelog, verification log,
Active Task, decision/RFC indexes, and session handoff; run complete fresh verification
and commit the closure.
Exit Criteria: RFC-0033 is Accepted only as a specification, all owning documents are
current, the full Markdown-sensitive regression is GREEN, Git is clean after required
local commits, and the stable checkpoint is cleared.
Verification: Focused governance tests, full README-owned Java 17 regression, generated
JUnit evidence inspection, `git diff --check`, and Git status/log review.
Next Action: Await separate authority to implement RFC-0033 RED-first.

## Next

After this specification is Completed, implement RFC-0033 RED-first only under a
separate continuation. Do not compose the RFC-0031/RFC-0030/RFC-0029 consumer, a
publisher, installed artifact, provider, credential, network, spend, execution,
runtime, persistence or MCP path, and do not push local commits without explicit
authority.
