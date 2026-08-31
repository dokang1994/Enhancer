# Current Task

## Status

Completed

## Task

Implement RFC-0019 Scheduler model request/policy preparation and fresh RFC-0015/
RFC-0016 evaluation RED-first, stopping before candidate suitability, Tool execution,
or gateway activity.

## Task ID

implement-scheduler-model-invocation-preparation

## Context

Model RunRecord v2 persistence and exact active-task resolution are Contract Verified,
while typed ModelWork remains rejected before execution. RFC-0019 next requires one
invocation-scoped Scheduler boundary that receives explicit request and policy inputs,
reads one governed prompt snapshot, builds the profile-aligned request, evaluates the
exact task/policy/capability intersection freshly, and preserves the same policy object
for a later separately authorized candidate/invocation seam. Local `main` is
intentionally ahead of `origin/main`; this task grants no push.

## Justified By

- User continuation request on 2026-08-31 into Scheduler model request and admission preparation
- User continuation request on 2026-08-31 into exact Scheduler active-task resolution
- User continuation request on 2026-08-31 into the Model RunRecord v2 and Scheduler admission specification

## Approval

The user's explicit 2026-08-31 continuation authorizes only RFC-0019 implementation
sequence 3: RED-first Java tests; an immutable explicit Scheduler invocation-limits
value; minimum shared bounded prompt/request preparation over the current model Tool
containment and strict-UTF-8 rules; construction of one exact invocation-scoped
`ExecutionPolicy`; exact active-task resolution; RFC-0015 composition; fresh RFC-0016
evaluation with the unchanged WorkItem capability; a non-persistent preparation result
that preserves the exact task, policy, request/profile, and decision for the next
boundary; owning document synchronization; checkpoints; bounded read-only subagent
reviews; fresh Java 17 verification; and ordinary local commits at verified GREEN
boundaries. It authorizes no candidate selection or suitability claim, ToolExecutor or
ModelInvokeTool execution, gateway/fake/adapter/provider activity, Model RunRecord
writing, production runtime/process/worker/finalizer/recovery/caller wiring, submission/
receive/CLI changes, route, endpoint, network, transmission, credentials, spend, push,
merge, release, deployment, permission change, or destructive cleanup.

## Acceptance Criteria

- One immutable Scheduler invocation-limits value retains exactly the explicit gateway
  timeout and maximum response-character ceiling and validates them through existing
  ModelRequest bounds without profile, environment, provider, or ambient defaults.
- One concrete Scheduler preparer accepts an explicit project root, exact typed
  ModelWork `WorkItem`, correlation identity, invocation limits, denied-Tool set, read
  ceiling, Tool timeout, and cancellation token. It rejects legacy work through the
  existing exact-task resolver before prompt or policy-dependent activity.
- Each call freshly resolves the exact active `ApprovedTask`, constructs one
  `ExecutionPolicy` instance with only `model-invoke` allowed plus the explicit denied
  set and resource inputs, reads the typed target once under the existing real-path,
  regular-file, bounded mutable-read, and strict-UTF-8 prompt rules, and builds one
  `ModelRequest` from that snapshot, the exact profile model class, correlation, and
  explicit limits.
- The request and retained complete profile compose through RFC-0015 without timeout
  clamping or capability/model-class conflation. RFC-0016 receives the exact resolved
  task, the same policy object instance, and unchanged `WorkItem.requiredCapability`
  and returns its deterministic first-match decision freshly on every call.
- The returned immutable preparation result retains the exact task, same policy,
  profiled request, and ephemeral admission decision without copying, repairing,
  persisting, caching, transmitting, or converting the decision into authority.
- Missing, outside-root, symbolic-outside-root, non-regular, oversized, changing, or
  malformed-UTF-8 prompt input; invalid limits; RFC-0015 mismatch; and every RFC-0016
  rejection fail or return their existing typed result before candidate, Tool, gateway,
  evidence, or RunRecord activity. Mixed-scope typed ModelWork remains selected by
  payload kind, not Tool precedence.
- Existing ModelInvokeTool prompt behavior reuses the shared bounded component without
  behavioral drift. Current external receiver, in-process, process-parent, and child
  ModelWork execution guards remain unchanged and no production caller invokes the new
  preparer.
- Focused RED/GREEN preparation, prompt, policy-identity, RFC-0015/RFC-0016, execution-
  guard, and package-boundary tests plus full README-owned Java 17 regression pass
  freshly. Owning documents and append-only verification evidence are current.

## Out Of Scope

Candidate model value, capability registry, suitability/locality proof, outbound or
provider policy, route/provider/model/endpoint/destination selection; ToolExecutor,
ModelInvokeTool, gateway, fake, adapter, provider, network or remote transmission;
credentials, paid services, pricing, tokenizers, usage normalization, redaction,
fallback, retry, caching, streaming; Model RunRecord writer, lifecycle result,
finalizer, runtime/process/worker/recovery/status wiring; typed producer, submission,
receiver, CLI, Project Brain, runtime-event, migration, or durable schema changes;
capability maturity beyond this standalone preparation boundary; push, merge, history
rewrite, release, deployment, permission changes, and destructive cleanup.

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

Workflow ID: implement-scheduler-model-invocation-preparation
Mode: Sequential
Increment Limit: 2
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on prompt-containment drift, policy-object replacement, hidden
defaults, capability/model-class conflation, candidate/gateway requirement, failed
verification, task or checkpoint drift, new external authority, exhausted bounds, or
unsafe recovery.

### Increment 1 - fresh-model-invocation-preparation

State: Completed
Depends On: none
Scope: Reconcile bounded read-only API/prompt/test reviews, establish aligned RED, and
implement the minimum explicit limits, shared prompt/request preparation, exact policy,
RFC-0015 composition, RFC-0016 evaluation, and immutable result without a caller.
Exit Criteria: Exact success, policy identity, single prompt snapshot, every declared
failure/rejection, and execution-guard regressions pass; no candidate, gateway, writer,
or production caller exists; evidence is appended once; and the increment is committed
locally.
Verification: Focused preparer/model Tool/RFC-0015/RFC-0016/task-resolver/execution-
guard/package-boundary/governance tests and `git diff --check`.
Next Action: Completed; focused preparation and guard evidence was appended and the
standalone boundary is ready for its local GREEN commit.

### Increment 2 - verify-and-close-model-invocation-preparation

State: Completed
Depends On: fresh-model-invocation-preparation
Scope: Run the full README-owned Java 17 regression, synchronize only affected current-
state, architecture, task, changelog, handoff, and append-only verification owners, run
final governance, and close the task locally.
Exit Criteria: Full and final focused verification pass, capability claims remain
bounded to preparation, both increments are committed, and the worktree/checkpoint
reach the intended clean stable state.
Verification: Full `test`, focused governance, JUnit XML aggregation,
`git diff --check`, and final Git/checkpoint inspection.
Next Action: Separately specify candidate suitability and a proven-local gateway
boundary before any model invocation or runtime wiring.

## Next

Await separate user authority to specify candidate suitability and a proven-local
gateway boundary before any model invocation or runtime wiring.
