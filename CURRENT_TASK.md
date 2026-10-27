# Current Task

## Status

Completed

## Task

Implement RFC-0032 RED-first as the unexposed provider-neutral publisher boundary for
governed remote capability catalog installation and rotation, without composing a real
installer, operator surface, runtime reader, or remote route.

## Task ID

implement-rfc-0032-governed-capability-catalog-publisher

## Context

RFC-0032 is Accepted and names RFC-0031's protected catalog path and independently
derived whole-file SHA-256 pin as its real Gate 9 consumer. That narrow consumer
satisfies the accepted exception to the installation-subsystem freeze, but the
implementation must remain in the model-catalog boundary and must not extend the
generic Gate 16 installation subsystem.

The implementation begins with pure publication values and an uncomposed publisher
port. Later increments may share RFC-0031 canonical catalog validation and implement a
test-composed filesystem publisher with injected evidence approval, fixed artifacts,
lock/CAS/replay, and catalog-first/binding-last publication. No production composition
or platform permission claim follows.

## Justified By

- User continuation request on 2026-10-27 into RFC-0032 governed capability catalog publisher implementation

## Approval

The user's continuation authorizes bounded repository-local Java and test changes for
RFC-0032, behavior-preserving RFC-0031 parser extraction where required, architecture
guards, documentation synchronization, checkpoint state, fresh verification, and the
ordinary local commits required for verified GREEN increments of this Active Task.

It does not authorize extending the generic Gate 16 installation subsystem; a real
installer, operator command or launcher, authenticated transport, evidence store or
schema, installed production catalog, runtime binding composition, operating-system
permission mutation, deployment, cleanup, credential or secret access, provider or
network access, paid service, source/evaluator/Router consumer, route execution,
runtime or durable-state integration, MCP, destructive action, push, merge, tag,
release, or deployment.

## Acceptance Criteria

- Pure immutable publication intent, operation, success, refusal, and result contracts
  preserve RFC-0032's exact field restrictions, invariants, safe rendering, and closed
  refusal order behind an uncomposed publisher port.
- RFC-0031 and RFC-0032 share one package-private complete canonical catalog
  validation boundary without changing source behavior or widening capability-facts
  creation.
- A test-composed provider-neutral filesystem publisher validates one bounded candidate
  snapshot and injected independent evidence approval, derives the digest internally,
  uses the fixed artifacts and canonical binding, and implements INSTALL/ROTATE,
  nonblocking lock, immutable-catalog-first/fixed-binding-last publication, CAS, exact
  replay, closed refusal precedence, and fail-closed recovery.
- Architecture guards prove zero production composition and no CLI, runtime, Router,
  evaluator, provider, credential, network, spend, execution, MCP, generic installer,
  permission-adapter, cleanup, rollback, or anti-rollback authority.
- Architecture, Project State, Roadmap, compact mirror, Changelog, verification log,
  Active Task, decision index, and session handoff remain consistent with document
  ownership and make no platform-support or Operational claim.
- Focused RED/GREEN tests, architecture/governance checks, `git diff --check`, and the
  full README-owned Java 17 regression pass freshly before completion.

## Out Of Scope

Generic Gate 16 installation framework changes; production composition; real installer,
operator CLI/launcher, broker, authenticated transport, evidence schema/store/authoring,
installed production catalog or binding, runtime binding reader/composition, platform
identity or permission adapter, ACL/DACL/mode/ownership mutation, deployment, durability
beyond the implemented process-crash contract, cleanup, retention, uninstall,
revocation, audit persistence, rollback, privileged anti-rollback, signed catalogs,
keys, credentials, secrets, provider endpoint or adapter, network, transmission, spend,
RFC-0031 source caller, RFC-0030 evaluator caller, RFC-0029 route-option consumer, route
execution, Tool/Evidence/RunRecord/Scheduler/runtime integration, durable schema,
migration, MCP, plugin, background work, push, merge, tag, release, and unrelated
external effects.

## Allowed Tools

- read-file
- write-code
- write-docs
- build-output
- verify
- checkpoint
- git-inspect
- git-stage
- git-commit

## Verification

Use RED-first focused contract tests for each observable increment. Run relevant model
and architecture tests plus `git diff --check` at every GREEN boundary. Before task
completion run the complete README-owned Java 17 test task, inspect generated JUnit
evidence, and review final Git status and diff.

## Dynamic Workflow

Workflow ID: implement-rfc-0032-governed-capability-catalog-publisher
Mode: Sequential
Increment Limit: 4
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on failed verification, authority conflict, task/checkpoint drift,
scope expansion into production composition or external effects, exhausted workflow
bounds, or unsafe recovery.

### Increment 1 - establish-pure-publication-contract

State: Completed
Depends On: none
Scope: Add RED-first immutable operation, status, refusal, intent, result and safe-
rendering contracts plus an uncomposed publisher port and source/dependency guards.
Exit Criteria: The exact RFC-0032 public vocabulary and intent invariants are executable
without filesystem mutation or a production caller.
Verification: Focused contract and architecture tests plus `git diff --check`.
Next Action: Extract one shared package-private canonical catalog codec with parity tests.

### Increment 2 - share-canonical-catalog-and-binding-validation

State: Completed
Depends On: establish-pure-publication-contract
Scope: Extract RFC-0031's complete canonical catalog parsing into one package-private
pure codec, preserve source behavior, and add canonical RFC-0032 binding values/codec,
bounded snapshot facts, and the injected evidence-approval port.
Exit Criteria: Source parity and complete catalog/binding validation are GREEN without
filesystem publication or production composition.
Verification: Focused source, catalog, binding, and architecture tests plus
`git diff --check`.
Next Action: Implement the test-composed filesystem publication state machine.

### Increment 3 - implement-filesystem-publication-state-machine

State: Completed
Depends On: share-canonical-catalog-and-binding-validation
Scope: Implement bounded exact candidate acquisition, fixed installation artifacts,
nonblocking lock, immutable catalog publication, binding publication, INSTALL/ROTATE,
CAS, exact replay, refusal precedence, failure injection, and post-publication unknown
handling behind the uncomposed publisher boundary.
Exit Criteria: Isolated filesystem tests cover success, replay, contention, conflicts,
drift, atomic-publication failure regions, retained artifacts, and no rollback/cleanup.
Verification: Focused model/filesystem/security/architecture tests and
`git diff --check`.
Next Action: Reconcile owning documents and run full verification.

### Increment 4 - reconcile-and-verify-rfc0032-implementation

State: Completed
Depends On: implement-filesystem-publication-state-machine
Scope: Reconcile Architecture and compact mirror, Project State, Roadmap, Changelog,
verification evidence, Active Task, decision index, and session handoff; run complete
fresh verification and commit the closure.
Exit Criteria: RFC-0032 is Contract Verified only to the implemented boundary, all
owning documents are current, the full Markdown-sensitive regression is GREEN, Git is
clean after required local commits, and the stable checkpoint is cleared.
Verification: Focused governance tests, full README-owned Java 17 regression, generated
JUnit evidence inspection, `git diff --check`, and Git status/log review.
Next Action: Await separate authority for runtime binding composition or the same-
invocation RFC-0031/RFC-0030/RFC-0029 consumer.

## Next

Await separate authority for production publisher or runtime binding composition, or
for the same-invocation RFC-0031 source, RFC-0030 evaluator and RFC-0029 route-option
consumer. Do not install a catalog, mutate permissions, connect a provider, credential,
network, spend, execution, persistence, runtime or MCP path, or push these local commits
without explicit authority.
