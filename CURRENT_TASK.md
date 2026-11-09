# Current Task

## Status

In Progress

## Task

Implement RFC-0034 RED-first as one uncomposed same-invocation preparer that connects
the exact RFC-0033 acquisition, RFC-0030 suitability, and RFC-0029 route-option value
boundaries without aggregating options or calling the Router.

## Task ID

implement-rfc-0034-protected-remote-model-route-option-preparation

## Context

RFC-0034 is Accepted and delivered. RFC-0033 can acquire exact RFC-0030 capability
facts through one protected fixed binding, RFC-0030 can create an invocation-local
suitability proof, and RFC-0029 defines a pure route option. All remain uncomposed and
have no production consumer.

This task adds only the same-invocation preparation boundary. It retains protected path
ownership at construction, derives route selectors only from the exact RFC-0028
eligibility, preserves exact instances and evaluation time, and stops after one
prepared option or one closed staged refusal.

## Justified By

- User continuation request on 2026-11-09 into RFC-0034 protected remote model route-option preparation implementation

## Approval

The user's continuation authorizes repository-local Java production and test changes,
architecture/source guards, decision and owning project-document updates, checkpoint
state, fresh verification, bounded read-only development-subagent review, and ordinary
local commits required for verified GREEN increments of this Active Task.

It does not authorize option-set aggregation or ordering, `ModelRouter` invocation, a
production preparer caller, provider or adapter construction, endpoint, credential,
network transmission, spend, execution, retry, fallback, cache, durable provenance,
Tool/Evidence/RunRecord/Scheduler/runtime integration, MCP, destructive action, push,
merge, tag, release, deployment, permission change, or another external effect.

## Acceptance Criteria

- A sealed immutable three-shape result retains the exact prepared option or exact
  upstream refusal, validates lowercase binding SHA-256 provenance where present, and
  renders no option, digest, path, fact, prompt, content, price, capacity, time, secret,
  or exception value.
- One final preparer has exactly one path-only public construction boundary; protected
  construction creates one RFC-0033 source and one field-free RFC-0030 evaluator, while
  package-private interaction seams add no second public or production fact source.
- Every call requires the exact profiled request, RFC-0028 `Eligible`, and explicit
  evaluation time; derives candidate and destination only from the eligibility; and
  calls RFC-0033 exactly once with those exact derived identities and time.
- Acquisition refusal preserves the exact RFC-0033 refusal and suppresses evaluation;
  acquired facts enter RFC-0030 exactly once by identity with unchanged request,
  eligibility, and time; suitability refusal preserves its exact reason and binding
  digest while suppressing option construction.
- Success creates exactly one `ModelRouteOption` from the exact request, eligibility,
  and RFC-0030 proof, returns it with the exact binding digest, and repeated calls
  reacquire and reevaluate without cache, retry, fallback, scan, write, repair, lock, or
  clock behavior.
- Architecture and source guards allow only the exact preparer to construct RFC-0033
  and RFC-0030 boundaries and one existing option, preserve zero production caller for
  the preparer and Router, preserve RFC-0031 as sole facts creator and RFC-0030 as sole
  proof creator, and keep the deterministic fake isolated.
- Architecture, Project State, Roadmap, compact mirror, Changelog, verification log,
  Active Task, decision index, and session handoff remain consistent with document
  ownership; focused checks and the complete README-owned Java 17 regression are fresh
  before completion.

## Out Of Scope

Production preparer construction or calling, installed binding or catalog, publisher
or evidence composition, option-set aggregation, caller ordering, Router selection,
selected route, provider, adapter, endpoint, credential, network, transmission, spend,
billing, execution, response handling, retry, fallback, cache, durable route or binding
provenance, Tool/Evidence/RunRecord/Scheduler/runtime-event integration, durable schema
or migration, MCP, plugin, interface, background work, capability promotion beyond the
implemented contract, destructive cleanup, push, merge, tag, release, deployment,
permission change, and unrelated external effects.

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

Use RED-first focused JUnit tests for observable behavior. Run focused RFC-0028 through
RFC-0034 model tests and architecture/source/governance guards at each GREEN increment,
plus `git diff --check`. Before task completion run the complete README-owned Java 17
test task, inspect generated JUnit evidence, and review final Git status and diff.

## Dynamic Workflow

Workflow ID: implement-rfc-0034-protected-remote-model-route-option-preparation
Mode: Sequential
Increment Limit: 3
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on failed verification, authority conflict, task/checkpoint drift,
scope expansion into routing/provider/execution/external effects, exhausted workflow
bounds, or unsafe recovery.

### Increment 1 - define-rfc0034-result-contract

State: Completed
Depends On: none
Scope: Record the accepted implementation continuation and add the sealed three-shape
result under missing-symbol RED tests for exact value retention, digest validation, and
safe rendering.
Exit Criteria: The result contract is GREEN, closed, immutable, safe to render, and
contains only the exact RFC-0034 values.
Verification: Focused RFC-0033/RFC-0034 result tests, applicable architecture and
governance tests, and `git diff --check`.
Next Action: Completed; the final same-invocation preparer and exact interaction
behavior are selected.

### Increment 2 - implement-rfc0034-preparer-and-guards

State: In Progress
Depends On: define-rfc0034-result-contract
Scope: Add RED-first path-only construction, exact RFC-0033/RFC-0030 orchestration,
refusal dominance, one-option success, fresh repeated-call behavior, and narrowly
updated source/architecture guards.
Exit Criteria: The exact same-invocation sequence is GREEN with no Router, option-set,
production caller, deterministic-fake reuse, or external-effect surface.
Verification: Focused RFC-0028 through RFC-0034 model, source, locality, dependency,
architecture, and governance tests plus `git diff --check`.
Next Action: Reconcile owning documents and run complete verification.

### Increment 3 - reconcile-and-verify-rfc0034-implementation

State: Pending
Depends On: implement-rfc0034-preparer-and-guards
Scope: Reconcile Architecture, Project State, Roadmap, compact mirror, Changelog,
verification log, Active Task, decision index, and session handoff; run complete fresh
verification and commit the closure.
Exit Criteria: RFC-0034 is Contract Verified only at its uncomposed one-option boundary,
all owning documents are current, full Markdown-sensitive regression is GREEN, Git is
clean after required local commits, and the stable checkpoint is cleared.
Verification: Focused governance tests, full README-owned Java 17 regression, generated
JUnit evidence inspection, `git diff --check`, and Git status/log review.
Next Action: Await separate authority for option aggregation and Router selection or
another explicitly approved Gate 9 continuation.

## Next

After this implementation is Completed, do not add a production caller, aggregate or
order options, invoke `ModelRouter`, construct a provider or credential, transmit
content, authorize spend, execute or persist a route, add retry/fallback/cache/runtime
or MCP behavior, and do not push local commits without explicit authority.
