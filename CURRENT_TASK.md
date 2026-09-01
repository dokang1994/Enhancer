# Current Task

## Status

Completed

## Task

Specify the smallest truthful deterministic-fake token semantics and proven context,
input, output, and total capacity contract before any suitability implementation or
model invocation wiring.

## Task ID

specify-deterministic-fake-token-semantics-and-capacity

## Context

RFC-0020 intentionally stops candidate evaluation at
`TOKEN_SEMANTICS_UNAVAILABLE`. The deterministic fake currently reports generic
character-based `ModelUsage` units and enforces Java-string length bounds, while the
RFC-0014 profile expresses provider-neutral token requirements. No accepted contract
defines a fake token unit, malformed-Unicode behavior, exact counting boundary, or
numeric context/input/output/total capacities. Those facts must be specified and made
independently testable before a later task may change the candidate or make `Suitable`
reachable.

## Justified By

- User continuation request on 2026-09-01 into deterministic fake token semantics and capacity specification
- User continuation request on 2026-08-31 into fail-closed local model candidate implementation
- User continuation request on 2026-08-31 into local model candidate suitability specification

## Approval

The user's explicit 2026-09-01 continuation authorizes only a documentation contract
for deterministic-fake token semantics and proven capacities: bounded read-only
contract/security reviews; one accepted RFC and matching decision/index entries;
owning architecture/roadmap/task/changelog/verification synchronization; checkpoints;
fresh README-owned Java 17 verification; and ordinary local commits at verified GREEN
boundaries. It authorizes no Java or test-source change, tokenizer implementation,
candidate/evaluator behavior change, reachable `Suitable`, Tool or gateway invocation,
runtime/process/worker/finalizer/recovery/caller wiring, Model RunRecord writing,
submission/receive/CLI or durable-schema change, provider, route, endpoint,
destination, network or remote transmission, credentials, paid service, spend, push,
merge, release, deployment, permission change, external effect, or destructive cleanup.

## Acceptance Criteria

- An accepted RFC defines one exact repository-owned deterministic-fake token unit and
  counting algorithm, including null, empty, line-ending, supplementary-character,
  and malformed-surrogate behavior, without treating current `ModelUsage` values as
  provider-token evidence or claiming portability to any provider tokenizer.
- The RFC defines exact context, input, output, and total capacity values plus the
  invariants and executable evidence a later implementation must supply. Every number
  is justified by an explicit closed fake contract and overflow-safe bounds, not by a
  model label, ambient machine capacity, or an unsupported character-to-provider-token
  conversion.
- The RFC distinguishes candidate/profile capacity suitability from actual request
  counting and budget enforcement, preserves the exact admitted request and candidate
  identities, and specifies fail-closed ordering before `Suitable` can become
  reachable.
- The contract keeps `ModelUsage` generic unless a separately justified mapping is
  explicitly defined, creates no provider/route/credential/network/spend authority,
  and does not weaken classification, locality, task, Tool, timeout, evidence,
  verification, retry, or recovery boundaries.
- The RFC states the later RED-first implementation surface, focused verification,
  compatibility limits, and exact stop before the separately accepted same-request/
  same-policy/same-gateway invocation seam.
- RFC/decision/architecture/index/ownership/dynamic-workflow/approved-task/task-
  justification/planner governance, `git diff --check`, and the full README-owned Java
  17 regression pass freshly. Owning documents and append-only verification evidence
  are current.

## Out Of Scope

Java or test-source implementation; candidate/evaluator/request/profile/gateway/Tool
signature or behavior changes; reachable suitable evaluation; generic tokenizer,
provider tokenizer, usage normalization, provider model, registry, discovery,
selection, router, endpoint, destination, outbound policy, network or remote
transmission; credentials, paid services, pricing, currency conversion; gateway or
Tool execution; prompt reread, redaction, scanning, injection resistance, attribution,
evaluation, caching, fallback, retry, or streaming; Model RunRecord writing, lifecycle
disposition, finalizer, result validation, runtime/process/worker/recovery/status/caller
wiring; typed producer, submission, receiver, CLI, durable schema, migration, MCP,
capability maturity, push, merge, history rewrite, release, deployment, permission
changes, external effects, and destructive cleanup.

## Allowed Tools

- read-file
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

Workflow ID: specify-deterministic-fake-token-semantics-and-capacity
Mode: Sequential
Increment Limit: 2
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on provider-token claims, reuse of unproven usage units, numeric
capacity without a closed proof obligation, hidden invocation authority, Java/test
source change, failed verification, task or checkpoint drift, new external authority,
exhausted bounds, or unsafe recovery.

### Increment 1 - specify-deterministic-token-and-capacity-contract

State: Completed
Depends On: none
Scope: Reconcile bounded read-only token/API and security/lifecycle reviews, specify and
accept only the deterministic-fake token unit, counting behavior, proven capacities,
later suitability obligations, and non-expansion boundary; synchronize owning
architecture/index/roadmap/decision/evidence documents; and commit the verified
documentation increment locally.
Exit Criteria: Every documentation acceptance criterion passes, no Java/test source or
runtime behavior changes, verification evidence is appended once, and the increment is
committed locally.
Verification: Focused RFC/index/decision/ownership/dynamic-workflow/approved-task/task-
justification/planner/architecture governance tests, `git diff --check`, and changed-
path inspection.
Next Action: Use the appended Increment 1 evidence as the dependency input for the
full-regression closure increment.

### Increment 2 - verify-and-close-token-capacity-specification

State: Completed
Depends On: specify-deterministic-token-and-capacity-contract
Scope: Read fresh Increment 1 evidence, run the full README-owned Java 17 regression,
synchronize only changed lifecycle owners, rerun final focused/governance verification,
and close the task locally.
Exit Criteria: Full and final verification pass, maturity remains unchanged, both
increments are committed locally, and the worktree/checkpoint reach the intended clean
stable state.
Verification: Full `test`, focused governance, JUnit XML aggregation,
`git diff --check`, and final Git/checkpoint inspection.
Next Action: Separately implement the accepted token/capacity contract RED-first before
any same-request invocation seam.

## Next

Separately implement RFC-0021 sequence 1 RED-first: the pure well-formed Unicode-scalar
counter, fixed token-aware candidate-v2 facts, and remaining standalone suitability
predicates, with no production caller or invocation seam.
