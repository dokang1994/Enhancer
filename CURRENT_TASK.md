# Current Task

## Status

In Progress

## Task

Implement RFC-0033 RED-first as one read-only protected fixed-binding capability-catalog
source that reuses RFC-0032's canonical binding codec, delegates exact selected path and
pin acquisition once to a fresh RFC-0031 source, and exposes the result only after an
unchanged second complete binding sample.

## Task ID

implement-rfc-0033-protected-runtime-binding-composition

## Context

RFC-0033 is Accepted and the preceding specification task is Completed. RFC-0032
provides the fixed canonical binding grammar and an uncomposed package-private
publisher. RFC-0031 is the sole production creator of RFC-0030 capability facts and has
no production caller. This task implements only the read-only bridge between those two
contracts.

The implementation must retain the fixed binding path as protected construction input,
perform a fresh bounded no-follow canonical sample for every acquisition, create one
fresh RFC-0031 source from the exact selected catalog path and pin, delegate once, then
collapse every failed or changed second sample to the RFC-0033 acquisition-drift
refusal before exposing the delegated result.

## Justified By

- User continuation request on 2026-11-03 into RFC-0033 protected runtime binding implementation

## Approval

The user's continuation authorizes repository-local Java production and test changes,
architecture/source guards, decision and owning project-document updates, checkpoint
state, fresh repository verification, and ordinary local commits required for verified
GREEN increments of this Active Task.

It does not authorize production construction or an installed binding/catalog,
publisher or evidence composition, deployment configuration, permission mutation,
platform-support claims, a same-invocation RFC-0030/RFC-0029 consumer, route selection
or execution, provider, endpoint, credential, network transmission, spend, fallback,
retry, durable provenance, Tool/Evidence/RunRecord/Scheduler/runtime integration, MCP,
destructive action, push, merge, tag, release, or deployment.

## Acceptance Criteria

- Immutable closed RFC-0033 acquired/refused results retain only the exact RFC-0031
  facts, stable binding digest, top-level refusal, and nested RFC-0031 refusal where the
  accepted specification permits it; rendering is path-, byte-, value-, and
  exception-safe.
- One read-only source accepts only protected construction-time fixed binding path plus
  candidate, destination, and evaluation-time acquisition input, performs a fresh
  1-byte-through-4-KiB no-follow canonical binding read on every sample, and enforces
  the RFC-0033 first-applicable refusal order.
- Installation root, fixed catalog directory, binding point, and selected path are
  exact-real and link/junction/reparse-safe within the bounded Java filesystem claim;
  the canonical RFC-0032 binding codec remains the sole binding grammar.
- Every valid first sample constructs one fresh RFC-0031 source from the exact selected
  path and pin and delegates exactly once without mapping capability values or adding a
  second facts creator.
- Every second-sample failure or unequal canonical binding becomes
  `BINDING_CHANGED_DURING_ACQUISITION` and hides both an earlier acquisition and an
  earlier delegated refusal; repeated calls do not cache, scan, fall back, write,
  repair, or use the publisher lock.
- Focused tests cover programming input, refusal precedence, fixed derivation, missing
  and unsafe filesystem state, bounds, malformed/noncanonical/foreign binding state,
  before/after drift, exact delegation, acquired and delegated refusal results,
  rotation before/during/after delegation, second-sample failure, safe rendering, and
  fresh repeated acquisition.
- Architecture and source guards preserve RFC-0031 as the sole production facts
  creator, allow only the RFC-0033 internal delegation caller, prove no external
  production caller, and exclude forbidden provider, Router/evaluator, credential,
  network, spend, execution, persistence, runtime, CLI/configuration, publisher,
  permission, cleanup, rollback, and anti-rollback dependencies.
- Architecture, Project State, Roadmap, compact mirror, Changelog, verification log,
  Active Task, decision index, and session handoff remain consistent with document
  ownership; focused checks and the complete README-owned Java 17 regression are fresh
  before completion.

## Out Of Scope

Production construction, installed artifacts, publisher/evidence implementation or
composition, real installation, deployment configuration, operator CLI/launcher,
broker, authenticated transport, permission/ownership mutation, ACL/DACL/mode or
platform-identity enforcement, cleanup, retention, uninstall, rollback, privileged
anti-rollback, catalog signing, key or secret handling, source/evaluator/Router caller
beyond RFC-0033's internal RFC-0031 delegation, route selection or execution, provider,
endpoint, credential, network, transmission, spend, billing, retry, fallback, cache,
durable route provenance, Tool/Evidence/RunRecord/Scheduler/runtime-event integration,
durable schema or migration, MCP, plugin, interface, background work, capability
promotion beyond the implemented contract, push, merge, tag, release, deployment, and
unrelated external effects.

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

Use RED-first focused JUnit tests for observable behavior. Run focused RFC-0031 through
RFC-0033 model tests and architecture/source/governance guards at each GREEN increment,
plus `git diff --check`. Before task completion run the complete README-owned Java 17
test task, inspect generated JUnit evidence, and review final Git status and diff.

## Dynamic Workflow

Workflow ID: implement-rfc-0033-protected-runtime-binding-composition
Mode: Sequential
Increment Limit: 3
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on failed verification, authority conflict, task/checkpoint drift,
scope expansion into a consumer/provider/external effect, exhausted workflow bounds, or
unsafe recovery.

### Increment 1 - define-rfc0033-result-and-first-sample-boundary

State: Completed
Depends On: none
Scope: Record the accepted implementation continuation; add RED-first immutable result,
refusal, protected source, and bounded first binding-sample behavior; establish exact
RFC-0031 delegation without capability-value mapping; add focused source guards.
Exit Criteria: The contract and first-sample/delegation behavior are GREEN with closed
safe outcomes and no forbidden caller or dependency.
Verification: Focused RFC-0031/RFC-0032/RFC-0033 unit and architecture tests plus
`git diff --check`.
Next Action: Completed; stable second-sample, rotation, and drift behavior is selected.

### Increment 2 - enforce-stable-acquisition-and-rotation

State: In Progress
Depends On: define-rfc0033-result-and-first-sample-boundary
Scope: Add RED-first second-sample dominance, complete before/during/after rotation and
read-drift coverage, repeated fresh acquisition, and no-cache/no-fallback behavior.
Exit Criteria: The same canonical binding selection is required around exactly one
delegation, and all second-sample failure or change hides the delegated outcome.
Verification: Focused filesystem, rotation, delegation, source-guard, and architecture
tests plus `git diff --check`.
Next Action: Reconcile owning documents and run full verification.

### Increment 3 - reconcile-and-verify-rfc0033-implementation

State: Pending
Depends On: enforce-stable-acquisition-and-rotation
Scope: Reconcile Architecture, Project State, Roadmap, compact mirror, Changelog,
verification log, Active Task, decision index, and session handoff; run complete fresh
verification and commit the closure.
Exit Criteria: RFC-0033 is Contract Verified only at its read-only uncomposed boundary,
all owning documents are current, full Markdown-sensitive regression is GREEN, Git is
clean after required local commits, and the stable checkpoint is cleared.
Verification: Focused governance tests, full README-owned Java 17 regression, generated
JUnit evidence inspection, `git diff --check`, and Git status/log review.
Next Action: Await separate authority for the same-invocation RFC-0030/RFC-0029 consumer
or another explicitly approved Gate 9 continuation.

## Next

After this implementation is Completed, do not compose the RFC-0030/RFC-0029 consumer,
a production publisher or deployment source, installed artifact, provider, credential,
network, spend, execution, persistence, runtime, or MCP path, and do not push local
commits without explicit authority.
