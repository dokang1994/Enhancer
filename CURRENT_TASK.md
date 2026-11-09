# Current Task

## Status

In Progress

## Task

Specify RFC-0034 as the uncomposed same-invocation boundary that connects one protected
RFC-0033 capability-fact acquisition to one RFC-0030 suitability evaluation and one
RFC-0029 route option, then stops before option-set aggregation or Router selection.

## Task ID

specify-protected-remote-model-route-option-preparation

## Context

RFC-0033 is Contract Verified without a production construction caller. RFC-0030 is the
sole creator of remote suitability proofs and RFC-0029 consumes those proofs through an
immutable route option, but their exact same-invocation composition is not specified.

The new contract must preserve protected binding authority, derive candidate and
destination selectors only from the exact RFC-0028 eligibility, retain exact instances
and one explicit evaluation time across the chain, and keep acquisition refusal,
suitability refusal, option preparation, and later Router selection distinct.

## Justified By

- User continuation request on 2026-11-09 into protected remote model route-option preparation specification

## Approval

The user's continuation authorizes this documentation-only RFC, accepted decision and
index entry, Architecture, Roadmap, compact mirror, Current Task, Changelog, verification
log and session-handoff reconciliation, checkpoint state, fresh repository verification,
and ordinary local commits required for verified GREEN increments of this Active Task.

It does not authorize Java production or test implementation, production construction,
installed binding/catalog artifacts, publisher/evidence composition, option-set
aggregation, Router selection, route execution preparation, provider or adapter
construction, endpoint, credential, network transmission, spend, billing, retry,
fallback, cache, durable provenance, Tool/Evidence/RunRecord/Scheduler/runtime/MCP
integration, permission change, destructive action, push, merge, tag, release, or
deployment.

## Acceptance Criteria

- RFC-0034 defines one exact sequence from protected RFC-0033 acquisition through
  RFC-0030 evaluation to one RFC-0029 `ModelRouteOption`, with explicit inputs,
  construction authority, identity retention, refusal precedence, and stopping point.
- Candidate and destination selectors come only from the exact RFC-0028 `Eligible`;
  callers cannot provide or override path, pin, selector, facts, proof, or route option.
- The source is invoked exactly once and the evaluator at most once with the exact
  request, eligibility, acquired facts instance, and unchanged evaluation time.
- Closed safe results distinguish prepared option, exact acquisition refusal, and exact
  suitability refusal while retaining only permitted binding-digest provenance.
- The contract forbids option-set aggregation, Router evaluation, provider/credential/
  network/spend/execution authority, retry/fallback/cache, persistence, runtime, and MCP.
- Future RED-first behavior and architecture/source-guard tests are specified without
  adding Java implementation or weakening current no-caller guards.
- RFC, decision/index, Architecture, Project State where current facts change, Roadmap,
  compact mirror, Changelog, verification log, Current Task, and session handoff remain
  consistent with document ownership; focused governance checks and the full README-
  owned Java 17 regression are fresh before completion.

## Out Of Scope

Java production or test implementation, source/evaluator/Router caller changes,
production construction, installed artifacts, publisher/evidence composition, real
installation, deployment configuration, permission enforcement, cleanup, retention,
rollback, anti-rollback, option-set aggregation, scoring, ordering, Router selection,
route execution preparation, provider, adapter, endpoint, protocol, credential, secret,
network, transmission, spend, billing, response handling, retry, fallback, cache,
durable provenance, Tool/Evidence/RunRecord/Scheduler/runtime-event integration, schema
or migration, MCP, plugin, interface, background work, capability promotion, push,
merge, tag, release, deployment, or unrelated external effects.

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
Assisted-Loop/dynamic-workflow governance tests plus `git diff --check`. Before task
completion run the complete README-owned Java 17 test task, inspect generated JUnit
evidence, and review final Git status and diff.

## Dynamic Workflow

Workflow ID: specify-protected-remote-model-route-option-preparation
Mode: Sequential
Increment Limit: 2
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on failed verification, authority conflict, task/checkpoint drift,
scope expansion into implementation/selection/provider/external effect, exhausted
workflow bounds, or unsafe recovery.

### Increment 1 - specify-rfc0034-route-option-preparation

State: Completed
Depends On: none
Scope: Accept and write the exact same-invocation RFC-0033 -> RFC-0030 -> one RFC-0029
option preparation contract, closed results, refusal order, provenance, safety boundary,
future RED-first verification, and exclusions.
Exit Criteria: RFC-0034 is Accepted, internally consistent with RFC-0028 through
RFC-0033, and grants no implementation, selection, provider, or external-effect
authority.
Verification: Focused RFC/decision/reference/Architecture governance tests plus
`git diff --check`.
Next Action: Reconcile owning documents and run full verification.

### Increment 2 - reconcile-and-verify-rfc0034-specification

State: In Progress
Depends On: specify-rfc0034-route-option-preparation
Scope: Reconcile owning documents, run complete fresh verification, inspect generated
evidence and final diff, commit the verified specification, and close the checkpoint.
Exit Criteria: The specification task is Completed, all owning documents are current,
the full Markdown-sensitive regression is GREEN, Git is clean after the required local
commit, and the stable checkpoint is cleared.
Verification: Focused governance tests, full README-owned Java 17 regression, generated
JUnit evidence inspection, `git diff --check`, and Git status/log review.
Next Action: Await separate authority for RED-first RFC-0034 implementation.

## Next

After this specification is Completed, do not implement RFC-0034, weaken no-caller
guards, construct a Router option set, call `ModelRouter`, or add a production
construction, provider, credential, network, spend, execution, persistence, runtime, or
MCP path without separate explicit authority.
