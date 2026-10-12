# Current Task

## Status

Completed

## Task

Specify RFC-0030 as the first governed provider-neutral remote model candidate
suitability contract after RFC-0029, without implementing Java, a production fact
source, provider, adapter, credential, network, spend, persistence, runtime, or MCP
path.

## Task ID

specify-governed-remote-model-candidate-suitability

## Context

RFC-0028 outbound eligibility and RFC-0029 pure route selection are Contract Verified,
but `RemoteModelCandidateSuitability` remains constructible only by package-local test
fixtures. The completed RFC-0029 implementation task names governed remote-suitability
evaluation and fact acquisition as the next missing Gate 9 boundary. The user's
2026-10-07 continuation request authorizes a bounded specification before any such
implementation or external integration.

## Justified By

- User continuation request on 2026-10-07 into RFC-0029 pure ModelRouter implementation
- User continuation request on 2026-10-07 into governed remote model candidate suitability specification

## Approval

The user's 2026-10-07 continuation request authorizes a documentation-only RFC for the
provider-neutral remote suitability evidence and evaluator boundary, one matching
accepted continuation decision and Decision Log entry, Architecture and mirror
synchronization, Roadmap RFC tracking, task/evidence/Changelog synchronization, fresh
README-owned verification, and ordinary local commits at verified GREEN increment
boundaries.

This approval does not authorize Java or test-source implementation; changes to
RFC-0013 through RFC-0029 behavior; a production candidate, destination, token,
capacity, pricing, availability, quality, or suitability source; discovery or a
registry; provider or adapter construction; endpoint, protocol, credential, secret,
network, transmission, spend, or paid use; route-execution preparation; Tool,
Scheduler, runtime, RunRecord, durable schema, persistence, retry, fallback, MCP, push,
merge, release, deployment, permission change, destructive cleanup, or another
external effect.

## Acceptance Criteria

- One accepted decision records the bounded continuation authority, with an exact
  matching `DECISION_LOG.md` entry.
- RFC-0030 defines the exact relationship among the RFC-0014 requirements, RFC-0028
  eligibility, RFC-0029 remote-suitability proof and Router, and the unchanged local
  deterministic-fake path.
- The contract defines one immutable independently governed remote-candidate
  capability-facts value with exact candidate binding, provenance, revision,
  freshness, token semantics, capacity, reasoning, timeout, cost, and classification
  evidence, without embedding a provider adapter or authority.
- One field-free evaluator accepts only the exact profiled request, exact RFC-0028
  `Eligible`, exact capability facts, and explicit evaluation time; it returns a
  closed suitable/refused decision and creates the existing opaque RFC-0029 proof only
  after complete ordered validation.
- Exact-instance and content bindings, strict freshness, checked token-capacity
  relationships, price/currency binding, classification support, safe rendering,
  retry lifetime, and deterministic first-match refusal behavior are specified.
- Governed source obligations and the first future pure implementation consumer are
  named, while production acquisition and route-execution preparation remain separate
  later work.
- Fresh focused document-governance and full Java 17 verification passes, owned
  documents are synchronized, the task is completed, required local GREEN commits
  exist, and no push occurs.

## Out Of Scope

Java or test-source implementation; changes to existing model admission, outbound
eligibility, Router, deterministic-fake suitability/execution, submission, transport,
or recovery behavior; production fact sources, registries, discovery, provider/
adapter/model/endpoint/protocol/network/credential/secret/spend paths; request
transmission, response validation, Tool, Scheduler, runtime, RunRecord, durable schema,
migration, persistence, retry, fallback execution, MCP, capability promotion, push,
merge, release, deployment, permission change, destructive cleanup, and unrelated
external effects.

## Allowed Tools

- read-file
- write-docs
- build-output
- verify
- checkpoint
- git-inspect
- git-stage
- git-commit
- development-subagents-read-only

## Verification

Run focused RFC/index/decision/document-ownership/Architecture-mirror/Planner/
Assisted-Loop/dynamic-workflow governance tests, then the README-owned full
`powershell -ExecutionPolicy Bypass -File .\scripts\gradle.ps1 test` command and
`git diff --check`. Read fresh output before lifecycle claims. Subagent reports are
recommendations and never verification evidence.

## Dynamic Workflow

Workflow ID: specify-governed-remote-model-candidate-suitability
Mode: Sequential
Increment Limit: 2
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on failed verification, task or checkpoint drift, an RFC-0014,
RFC-0028, or RFC-0029 compatibility conflict, incomplete evidence or evaluator
binding, newly required production or external authority, or unsafe recovery.

### Increment 1 - specify-remote-suitability-contract

State: Completed
Depends On: none
Scope: Record the accepted continuation decision and RFC-0030 provider-neutral remote
suitability evidence/evaluator contract, then synchronize Architecture, its mirror,
Roadmap RFC tracking, and delivery history.
Exit Criteria: The decision/index pair and RFC exist; exact evidence, evaluator,
decision, refusal, identity, freshness, token/cost/classification, source-obligation,
retry, and non-authority semantics are specified; focused document-governance
verification is GREEN; evidence is appended once; and the increment is committed
locally.
Verification: Focused RFC/index/decision/document ownership/Architecture mirror/
Roadmap/Planner/Assisted Loop/dynamic-workflow tests and `git diff --check`.
Next Action: Select Increment 2 only after reading fresh Increment 1 evidence.

### Increment 2 - verify-and-close-remote-suitability-specification

State: Completed
Depends On: specify-remote-suitability-contract
Scope: Run the full Markdown-sensitive Java 17 regression on the exact final tree,
review the diff, synchronize task and handoff state, close the documentation-only task,
and create the final required ordinary local GREEN commit if closure changes remain.
Exit Criteria: Fresh full regression and diff checks are GREEN; owned documents are
current; the task is Completed; required local commits exist; Git is clean; and the
checkpoint is stable and clear.
Verification: Full README-owned test command, generated JUnit XML counts,
`git diff --check`, diff/status/log review, commits, and checkpoint reconciliation.
Next Action: Await separate authority for RFC-0030 implementation; do not add a
production fact source, provider or network path, route execution, persistence, or
push.

## Next

After this documentation-only task is completed, await separate user authority for a
RED-first pure RFC-0030 capability facts/suitability evaluator implementation.
Production fact acquisition, route-execution preparation, every provider, credential,
network, spend, persistence, fallback, MCP, release, and deployment path remain
separate later work.
