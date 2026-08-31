# Current Task

## Status

Completed

## Task

Implement the exact Scheduler active-task resolver for typed ModelWork RED-first,
binding the freshly loaded governed task to the retained WorkItem task revision and Tool
scope without preparing or executing a model request.

## Task ID

implement-exact-scheduler-active-task-resolution

## Context

RFC-0019 Model RunRecord v2 value, typed filesystem persistence, v1 compatibility, and
kind dispatch are implemented and verified. Typed ModelWork remains rejected before
execution. The next required sequence is a concrete Scheduler resolver that freshly
loads repository context and returns only the exact active `ApprovedTask` matching the
typed WorkItem's retained task ID, source document, complete source digest, and Tool
scope. Local `main` is intentionally ahead of `origin/main`; this task grants no push.

## Justified By

- User continuation request on 2026-08-31 into exact Scheduler active-task resolution
- User continuation request on 2026-08-31 into additive Model RunRecord v2 implementation
- User continuation request on 2026-08-31 into the Model RunRecord v2 and Scheduler admission specification

## Approval

The user's explicit 2026-08-31 continuation authorizes only the second RFC-0019
implementation sequence: RED-first Java tests, the minimum concrete Scheduler
active-task resolver over the existing `ProjectContextReader` and `ApprovedTaskReader`,
exact typed WorkItem revision/scope binding, owning architecture/state/task/changelog/
verification/handoff synchronization, development-session checkpoints, bounded read-
only subagent reviews, fresh Java 17 verification, and ordinary local commits at
verified GREEN boundaries. It authorizes no request or prompt preparation, policy
construction, RFC-0015/RFC-0016 invocation, production caller or execution wiring,
candidate suitability, ToolExecutor or gateway activity, model-record writing,
submission/receive/CLI changes, provider, route, endpoint, network, transmission,
credentials, spend, push, merge, release, deployment, permission change, or destructive
cleanup.

## Acceptance Criteria

- One concrete Scheduler resolver accepts an explicit project root and exact active
  typed ModelWork `WorkItem`, freshly reads the complete governed context through
  `ProjectContextReader`, and resolves `ApprovedTask` only through
  `ApprovedTaskReader` on every call.
- Legacy `WorkPayload` is rejected. The resolver does not construct, copy, default, or
  repair task description, approval evidence, Tool scope, source path, or identity from
  WorkItem, Goal, AgentRun, envelope, prompt, environment, or caller prose.
- The resolved task must be `In Progress` and match the retained
  `ApprovedTaskRevision` task ID, exact `CURRENT_TASK.md` source path, lowercase SHA-256
  of the complete strict-UTF-8 source content, and exact immutable allowed-Tool set.
- Missing, completed, malformed, changed-digest, task-ID, source-path, Tool-scope,
  legacy-work, symbolic/outside-root, oversized, or malformed-UTF-8 input fails closed
  before request construction, admission, Tool/gateway/evidence/RunRecord activity, or
  any repository mutation.
- Successful resolution returns the exact `ApprovedTask` produced by the existing
  reader and adds no registry, ambient lookup, cache, persistence, durable schema,
  generic context aggregate, source-of-authority value, or production runtime caller.
- Focused RED/GREEN resolver/context/reader/package-boundary tests, execution-guard
  regressions, architecture/governance checks, and the full README-owned Java 17
  regression pass freshly. Owning documents and append-only evidence are current.

## Out Of Scope

Model request or prompt preparation; invocation limits; `ExecutionPolicy` construction;
RFC-0015/RFC-0016 composition or admission; admission result mapping or persistence;
candidate suitability or locality proof; ToolExecutor, gateway, fake, adapter, provider,
route, endpoint, destination, network or remote transmission; credentials, paid
services, pricing, tokenizers, caching, fallback, streaming; Model RunRecord writer,
finalizer/result/recovery/process/worker wiring; submission, receiver, CLI, Project
Brain, runtime-event, status, migration, or artifact changes; capability maturity
beyond this pure resolver boundary; push, merge, history rewrite, release, deployment,
permission changes, and destructive cleanup.

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
- subagent-readonly

## Verification

Evidence is appended once per completed increment to `docs/verification-log.md` after
the declared checks complete.

## Dynamic Workflow

Workflow ID: implement-exact-scheduler-active-task-resolution
Mode: Sequential
Increment Limit: 2
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on ambiguous task source or digest semantics, authority-source
drift, a required request/admission/execution change, failed verification, task or
checkpoint drift, new external authority, exhausted bounds, or unsafe recovery.

### Increment 1 - exact-active-task-resolver

State: Completed
Depends On: none
Scope: Reconcile bounded read-only source/package/test reviews, add aligned failing
tests, then implement the minimum concrete resolver and typed mismatch failure without
adding a production caller.
Exit Criteria: Exact success and every declared mismatch fail-closed test pass, existing
context/task readers and typed execution guards remain GREEN, no preparation/execution
caller exists, evidence is appended once, and the increment is committed locally.
Verification: Focused resolver/context/task-reader/WorkItem/execution-guard/package-
boundary/governance tests and `git diff --check`.
Next Action: Completed; fresh focused verification evidence was appended and the
verified resolver boundary is ready for its local GREEN commit.

### Increment 2 - verify-and-close-exact-task-resolution

State: Completed
Depends On: exact-active-task-resolver
Scope: Run the full README-owned Java 17 regression, synchronize only affected current-
state, architecture, task, changelog, handoff, and append-only verification owners, run
final governance, and close the task locally.
Exit Criteria: Full and final focused verification pass, capability claims remain
bounded to the resolver, both increments are committed, and the worktree/checkpoint
reach the intended clean stable state.
Verification: Full `test`, focused governance, JUnit XML aggregation,
`git diff --check`, and final Git/checkpoint inspection.
Next Action: Implement explicit Scheduler request/policy preparation and fresh
RFC-0015/RFC-0016 evaluation under separate user authority while stopping before
candidate suitability and gateway activity.

## Next

Await separate user authority to implement explicit Scheduler request/policy
preparation and fresh RFC-0015/RFC-0016 evaluation while stopping before candidate
suitability and gateway activity.
