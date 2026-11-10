# Current Task

## Status

In Progress

## Task

Specify RFC-0035 as the provider-neutral same-invocation boundary that consumes one
bounded caller-ordered collection of exact RFC-0034 preparation results, fails closed
without dropping a refused preparation, constructs the exact RFC-0029 option set, and
invokes the existing pure `ModelRouter` once before any route execution.

## Task ID

specify-protected-remote-model-route-aggregation-and-selection

## Context

RFC-0034 is Contract Verified as an uncomposed one-option preparer with no production
caller. RFC-0029 is Contract Verified as a pure Router over a bounded ordered option
set, also with no production caller. No accepted contract currently owns how multiple
exact preparation outcomes retain caller order, how a refused preparation suppresses
selection, how prepared options enter `ModelRouteOptionSet`, or how the Router decision
and selected binding provenance remain associated.

The new contract must keep ordering explicit but non-authoritative, preserve every
exact prepared option and the same request and evaluation time, reject silent filtering
or reconstruction, and stop after the exact pure Router decision.

## Justified By

- User continuation request on 2026-11-10 into protected remote model route aggregation and selection specification

## Approval

The user's continuation authorizes this documentation-only RFC, accepted decision and
index entry, Architecture, Roadmap, compact mirror, Current Task, Changelog,
verification log and session-handoff reconciliation, checkpoint state, fresh repository
verification, bounded read-only development-subagent review, and ordinary local commits
required for verified GREEN increments of this Active Task.

It does not authorize Java production or test implementation, construction or calling
of the RFC-0034 preparer in production, installed binding/catalog artifacts,
publisher/evidence composition, production selection callers, route execution
preparation, provider or adapter construction, endpoint, credential, network
transmission, spend, billing, retry, fallback, cache, durable provenance,
Tool/Evidence/RunRecord/Scheduler/runtime/MCP integration, permission change,
destructive action, push, merge, tag, release, or deployment.

## Acceptance Criteria

- RFC-0035 defines one field-free composition receiving the exact profiled request, an
  ordered zero-through-256 collection of exact RFC-0034 preparation results, and one
  explicit evaluation time, with no ambient lookup, clock, or policy input.
- The collection order is caller-declared data and is preserved exactly. Null or
  oversized input is a programming error; the first acquisition or suitability refusal
  by lowest ordinal is returned exactly and suppresses option-set construction and
  Router evaluation, so no result is silently dropped, retried, replaced, or reordered.
- Only an all-`Prepared` collection yields a `ModelRouteOptionSet`, containing the exact
  option instances in the same order. Empty input reaches the existing Router and its
  exact `NO_ROUTE_OPTIONS` refusal; duplicate and malformed routes remain Router-owned.
- The existing field-free `ModelRouter` is invoked exactly once with the exact request,
  option set, and unchanged evaluation time. A Router refusal is preserved exactly; a
  selection retains the exact `SelectedModelRoute` and the exact ordinal-zero RFC-0034
  `Prepared` result so its redacted binding provenance remains associated by identity.
- The contract stops before preparer construction or invocation, route-execution
  preparation, provider/credential/network/spend authority, retry/fallback/cache,
  persistence, runtime, or MCP, and grants no authority through order, option, digest,
  or selection possession.
- Future RED-first result, composition, interaction, safe-rendering, and
  architecture/source-guard tests are specified without adding Java implementation or
  weakening current sole-creator and no-execution-consumer guards.
- RFC, decision/index, Architecture, Project State where current facts change, Roadmap,
  compact mirror, Changelog, verification log, Current Task, and session handoff remain
  consistent with document ownership; focused governance checks and the full README-
  owned Java 17 regression are fresh before completion.

## Out Of Scope

Java production or test implementation, preparer/source/evaluator behavior changes,
production preparer or selector construction or calling, installed artifacts,
publisher/evidence composition, candidate discovery, scoring, sorting, filtering,
preference derivation, dynamic priority, load balancing, retry, fallback, cache,
availability policy, route execution preparation, provider, adapter, endpoint,
protocol, credential, secret, network, transmission, spend, billing, response handling,
durable route or binding provenance, Tool/Evidence/RunRecord/Scheduler/runtime-event
integration, schema or migration, MCP, plugin, interface, background work, capability
promotion, destructive cleanup, push, merge, tag, release, deployment, permission
change, or unrelated external effects.

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

Workflow ID: specify-protected-remote-model-route-aggregation-and-selection
Mode: Sequential
Increment Limit: 2
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on failed verification, authority conflict, task/checkpoint drift,
scope expansion into implementation/execution/provider/external effects, exhausted
workflow bounds, or unsafe recovery.

### Increment 1 - specify-rfc0035-route-aggregation-and-selection

State: Completed
Depends On: none
Scope: Accept and write the exact ordered RFC-0034 result aggregation, fail-closed
preparation-refusal handling, RFC-0029 option-set construction and one-call Router
selection contract, closed results, provenance, safety boundary, future RED-first
verification, and exclusions.
Exit Criteria: RFC-0035 is Accepted, internally consistent with RFC-0028 through
RFC-0034, and grants no implementation, execution, provider, or external-effect
authority.
Verification: Focused RFC/decision/reference/Architecture governance tests plus
`git diff --check`.
Next Action: Completed; owning-document reconciliation and complete verification are
selected.

### Increment 2 - reconcile-and-verify-rfc0035-specification

State: In Progress
Depends On: specify-rfc0035-route-aggregation-and-selection
Scope: Reconcile owning documents, run complete fresh verification, inspect generated
evidence and final diff, commit the verified specification, and close the checkpoint.
Exit Criteria: The specification task is Completed, all owning documents are current,
the full Markdown-sensitive regression is GREEN, Git is clean after the required local
commit, and the stable checkpoint is cleared.
Verification: Focused governance tests, full README-owned Java 17 regression, generated
JUnit evidence inspection, `git diff --check`, and Git status/log review.
Next Action: Run complete fresh verification, inspect generated evidence and the final
diff, then close the specification task and checkpoint.

## Next

After this specification is Completed, do not implement RFC-0035, construct or call the
new composition in production, call the RFC-0034 preparer from production, prepare route
execution, construct a provider or credential, transmit content, authorize spend,
persist route provenance, add retry/fallback/cache/runtime/MCP behavior, or push local
commits without separate explicit authority.
