# Current Task

## Status

Completed

## Task

Specify the first pure provider-neutral `ModelRouter` selection contract after
RFC-0028, without implementing the Router or adding governed fact sources, a provider,
adapter, credential, network, spend, persistence, runtime, or MCP path.

## Task ID

specify-pure-provider-neutral-model-router

## Context

RFC-0028 is Contract Verified and names a separately authorized pure `ModelRouter` as
its first consumer. The user requested continuation on 2026-10-07. Read-only sequence
and source-surface reviews found that the current deterministic-fake suitability value
is local-only and cannot bind remote eligibility, while no remote suitability proof,
bounded selection set, deterministic selection algebra, or identity-preserving selected
route exists. A specification is required before implementation or governed fact
acquisition can safely proceed.

## Justified By

- Select provider-neutral outbound model route admission before router or provider integration
- User continuation request on 2026-10-07 into pure provider-neutral ModelRouter specification

## Approval

The user's 2026-10-07 continuation request authorizes a documentation-only RFC for the
pure provider-neutral Router boundary, one matching accepted continuation decision and
Decision Log entry, Architecture and mirror synchronization, Roadmap RFC tracking,
task/evidence/Changelog synchronization, fresh README-owned verification, and one
ordinary local commit at the verified GREEN increment boundary.

This approval does not authorize Java or test-source implementation, changes to the
existing RFC-0013 through RFC-0028 behavior, production fact sources, scanners,
redactors, pricing acquisition, candidate or destination discovery, a registry, a
second executable candidate, provider or adapter construction, endpoint or protocol,
credential or secret access, network transmission, spend or paid use, Tool execution,
Scheduler/runtime/RunRecord/schema integration, persistence, MCP, push, merge, release,
deployment, permission change, destructive cleanup, or another external effect.

## Acceptance Criteria

- One accepted decision records the bounded continuation authority, with an exact
  matching `DECISION_LOG.md` entry.
- RFC-0029 defines the pure Router's exact relationship to RFC-0016, deterministic-fake
  local suitability, and RFC-0028 outbound eligibility without changing them.
- The contract defines a provider-neutral remote suitability proof bound to the exact
  request/profile and candidate facts, a bounded duplicate-free route-option set, and a
  closed deterministic selection/refusal algebra.
- Selection preserves the exact eligibility and suitability instances, candidate and
  destination identities, and caller-declared order while granting no execution,
  transmission, credential, spend, persistence, or fallback authority.
- Empty input, cross-bound request/profile/candidate/destination facts, duplicate route
  identity, and stale or non-eligible substitutions fail closed; every fallback option
  requires its own fresh eligibility and suitability.
- The first future implementation consumer, verification contract, safe rendering,
  source/locality/dependency guards, and explicit later integration boundaries are named.
- Fresh focused document-governance and full Java 17 verification passes, owned
  documents are synchronized, the task is completed, one local GREEN commit exists,
  and no push occurs.

## Out Of Scope

Java or test-source implementation; changes to existing model admission, suitability,
execution, submission, transport, or recovery behavior; production fact sources,
candidate/destination registries, discovery, scanners/redactors, pricing acquisition,
provider/adapter/endpoint/protocol/network/credential/secret/spend paths; Tool,
Scheduler, runtime, RunRecord, durable schema, migration, cache, fallback execution,
MCP, capability promotion, push, merge, release, deployment, permission change,
destructive cleanup, and unrelated external effects.

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
`.\scripts\gradle.ps1 test` command and `git diff --check`. Read fresh output before
making lifecycle claims. Subagent reports are recommendations and not verification
evidence.

## Dynamic Workflow

Workflow ID: specify-pure-provider-neutral-model-router
Mode: Sequential
Increment Limit: 2
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on failed verification, task or checkpoint drift, an RFC-0016 or
RFC-0028 compatibility conflict, incomplete suitability or selection binding, newly
required production/external authority, or unsafe recovery.

### Increment 1 - specify-pure-router-contract

State: Completed
Depends On: none
Scope: Record the accepted continuation decision and RFC-0029 pure Router contract,
then synchronize Architecture, its mirror, Roadmap RFC tracking, and delivery history.
Exit Criteria: The decision/index pair and RFC exist; exact suitability, bounded option,
selection, refusal, identity, freshness, fallback, and non-authority semantics are
specified; focused document-governance verification is GREEN; evidence is appended
once; and the increment is committed locally.
Verification: Focused RFC/index/decision/document ownership/Architecture mirror/
Roadmap/Planner/Assisted Loop/dynamic-workflow tests and `git diff --check`.
Next Action: Select Increment 2 only after reading fresh Increment 1 evidence.

### Increment 2 - verify-and-close-router-specification

State: Completed
Depends On: specify-pure-router-contract
Scope: Run the full Markdown-sensitive Java 17 regression on the exact final tree,
review the diff, synchronize task and handoff state, close the documentation-only task,
and create the final required ordinary local GREEN commit if closure changes remain.
Exit Criteria: Fresh full regression and diff checks are GREEN; owned documents are
current; the task is Completed; required local commits exist; Git is clean; and the
checkpoint is stable and clear.
Verification: Full `.\scripts\gradle.ps1 test`, generated JUnit XML counts,
`git diff --check`, diff/status/log review, commits, and checkpoint reconciliation.
Next Action: Await separate authority for RFC-0029 implementation; do not add fact
sources, a provider or network path, or push.

## Next

After this documentation-only task is completed, await separate user authority for a
RED-first pure RFC-0029 Router implementation. Governed RFC-0028 fact sources and every
provider, credential, network, spend, persistence, MCP, release, and deployment path
remain separate later work.
