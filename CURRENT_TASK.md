# Current Task

## Status

Completed

## Task

Implement the first RFC-0020 fail-closed deterministic-fake candidate binding and
stateless suitability evaluator RED-first, stopping at
`TOKEN_SEMANTICS_UNAVAILABLE` without any caller or invocation wiring.

## Task ID

implement-fail-closed-local-model-candidate

## Context

RFC-0020 is accepted and requires one opaque exact deterministic-fake binding plus a
field-free evaluator over the exact RFC-0016 `Admitted` value. Current fake usage is
character-based, so the initial evaluator may reject model class, capability, or
reasoning first but must otherwise stop at `TOKEN_SEMANTICS_UNAVAILABLE`. Typed
ModelWork execution remains guarded before Tool/gateway activity, and no current caller
constructs a candidate or invokes suitability.

## Justified By

- User continuation request on 2026-08-31 into fail-closed local model candidate implementation
- User continuation request on 2026-08-31 into local model candidate suitability specification
- User continuation request on 2026-08-31 into Scheduler model request and admission preparation
- User continuation request on 2026-08-31 into exact Scheduler active-task resolution
- User continuation request on 2026-08-31 into the Model RunRecord v2 and Scheduler admission specification

## Approval

The user's explicit 2026-08-31 continuation authorizes only RFC-0020 implementation
sequence 1: RED-first Java tests; one opaque final exact-fake candidate binding with
repository-owned fixed facts; one field-free deterministic suitability evaluator;
sealed exact-identity result shapes and the closed ordered rejection vocabulary; typed
`TOKEN_SEMANTICS_UNAVAILABLE` refusal before every later predicate; bounded read-only
API/security reviews; owning architecture/state/task/changelog/decision/verification
synchronization; checkpoints; fresh Java 17 verification; and ordinary local commits at
verified GREEN boundaries. It authorizes no token semantics or numeric capacity,
reachable suitable path, Tool or gateway invocation, generic candidate or registry,
runtime/process/worker/finalizer/recovery/caller wiring, Model RunRecord writing,
submission/receive/CLI or durable-schema change, provider, route, endpoint,
destination, network or remote transmission, credentials, paid service, spend, push,
merge, release, deployment, permission change, external effect, or destructive cleanup.

## Acceptance Criteria

- `DeterministicFakeModelCandidate` is an opaque final non-record with one private exact
  `DeterministicFakeModelGateway` field, no generic gateway/interface implementation,
  and one public factory whose only input is the exact final fake type. It retains and
  returns that same gateway instance and rejects null.
- Candidate identity, model class, required capability, maximum reasoning, token-
  semantics availability, provider-charge status, and maximum data classification are
  fixed repository-owned facts exactly as RFC-0020 specifies. No caller supplies or
  alters metadata, locality, provider, route, credential, price, tokenizer, capacity,
  registry, supplier, or default.
- `ModelCandidateSuitability` is public, final, field-free, and accepts exactly one
  `ModelInvocationAdmissionDecision.Admitted` and one exact fake candidate. Nulls are
  programming errors; evaluation performs no I/O, lookup, Tool, gateway, evidence,
  persistence, process, network, credential, or external activity.
- The sealed decision retains either the exact admitted and candidate object instances
  or one closed rejection reason. The reason vocabulary and repository order include
  model class, capability, reasoning, token semantics, later context/input/output/total
  capacities, free-only cost, and classification without making later predicates
  reachable.
- Evaluation returns the deterministic first mismatch for model class, capability, and
  reasoning, then always returns `TOKEN_SEMANTICS_UNAVAILABLE` for the current candidate.
  No evaluator call returns `Suitable`, assigns numeric context/token capacity, converts
  characters or `ModelUsage` to tokens, or evaluates the later cost/classification path.
- Focused tests prove exact gateway/admitted/candidate identity, opaque API shape, fixed
  facts, null failures, first-match precedence, unreachable suitable evaluation, and
  forbidden-dependency absence. Existing RFC-0013 through RFC-0020 model tests,
  Scheduler preparation, `ModelInvokeTool`, typed ModelWork execution guards, durable
  schemas, production-caller absence, and package boundaries remain unchanged.
- Relevant focused RED/GREEN, execution-guard, package/reflection/source-dependency,
  governance, and full README-owned Java 17 regression pass freshly. Owning documents
  and append-only verification evidence are current.

## Out Of Scope

Token semantics, token counting, tokenizer, usage normalization, or numeric context/
input/output/total capacity; reachable suitable evaluation; generic candidate port,
registry, discovery, selection, router, provider, provider model, endpoint, destination,
outbound policy, network or remote transmission; credentials, paid services, pricing,
currency conversion; request/profile/gateway/Tool signature or behavior changes;
gateway or Tool execution; prompt reread, redaction, scanning, injection resistance,
attribution, evaluation, caching, fallback, retry, or streaming; Model RunRecord writing,
lifecycle disposition, finalizer, result validation, runtime/process/worker/recovery/
status/caller wiring; typed producer, submission, receiver, CLI, durable schema,
migration, MCP, capability maturity beyond the standalone boundary, push, merge,
history rewrite, release, deployment, permission changes, external effects, and
destructive cleanup.

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

Workflow ID: implement-fail-closed-local-model-candidate
Mode: Sequential
Increment Limit: 2
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on generic or forgeable gateway binding, caller-supplied candidate
facts, numeric token/capacity claim, reachable suitable evaluation, Tool/gateway or
runtime activity, failed verification, task or checkpoint drift, new external
authority, exhausted bounds, or unsafe recovery.

### Increment 1 - implement-fail-closed-candidate-suitability

State: Completed
Depends On: none
Scope: Reconcile bounded read-only API/security reviews, establish aligned missing-
symbol RED, implement only the opaque exact-fake binding, fixed facts, sealed result,
closed reasons, and field-free evaluator through the token-semantics stop, synchronize
owning documents/evidence, and commit the verified GREEN increment locally.
Exit Criteria: Every acceptance criterion for the fail-closed standalone boundary
passes; no suitable result is produced, no production caller or external activity
exists, evidence is appended once, and the increment is committed locally.
Verification: Focused candidate/suitability/RFC-0013-through-RFC-0020/model/preparation/
execution-guard/package-boundary/governance tests, production-source searches, and
`git diff --check`.
Next Action: Use the appended Increment 1 evidence as the dependency input for the
full-regression closure increment.

### Increment 2 - verify-and-close-fail-closed-candidate

State: Completed
Depends On: implement-fail-closed-candidate-suitability
Scope: Read fresh Increment 1 evidence, run the full README-owned Java 17 regression,
synchronize only changed lifecycle owners, rerun final focused/governance verification,
and close the task locally.
Exit Criteria: Full and final verification pass, maturity remains bounded to the
standalone fail-closed candidate boundary, both increments are committed, and the
worktree/checkpoint reach the intended clean stable state.
Verification: Full `test`, focused governance, JUnit XML aggregation,
`git diff --check`, and final Git/checkpoint inspection.
Next Action: Separately specify deterministic token semantics and proven capacities
before making `Suitable` reachable or wiring any invocation.

## Next

Separately specify deterministic token semantics and proven context/input/output/total
capacities before making `Suitable` reachable or wiring any invocation.
