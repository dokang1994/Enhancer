# Current Task

## Status

Completed

## Task

Specify RFC-0028 as the provider-neutral default-deny outbound model route-admission
contract that must precede any second candidate, Router, provider adapter, credential,
network transmission, paid invocation, or MCP work.

## Task ID

specify-provider-neutral-outbound-model-route-admission

## Context

The completed Gate 9 baseline audit found that the deterministic-fake path is bounded
and supported, but no provider-neutral route decision or outbound data-policy boundary
exists. The accepted sequencing decision selects RFC-0028 specification as the next
task before any Router or real-provider integration. The user requested continuation
on 2026-09-17.

## Justified By

- Select provider-neutral outbound model route admission before router or provider integration
- User continuation request on 2026-09-17 into provider-neutral outbound model route-admission specification

## Approval

The user's 2026-09-17 continuation request authorizes a documentation-only RFC-0028
specification task. It permits the RFC, one matching accepted continuation decision and
Decision Log entry, Architecture and `.ai/architecture.md` contract synchronization,
Roadmap RFC-index synchronization, task/state-consistency documentation, verification
evidence, Changelog, and any necessary session handoff update. It also permits focused
and full README-owned verification and the ordinary local commit required for the
verified GREEN increment.

This approval does not authorize Java or test-source implementation, a second model
candidate, Router or registry construction, provider adapter construction or
invocation, credential access, secret persistence, network transmission, spend, MCP,
durable schema change, capability-maturity promotion, push, merge, release, deployment,
permission change, destructive cleanup, or another external effect.

## Acceptance Criteria

- One accepted decision records the user's bounded continuation authority, with an
  exact matching `DECISION_LOG.md` entry.
- RFC-0028 defines one provider-neutral fresh default-deny decision over the exact
  active task, execution policy, profiled request, independently governed candidate and
  destination facts, purpose, retention, classification, content-inspection/redaction,
  and pricing facts.
- The RFC preserves profile classification as untrusted requirement data, treats cost
  ceilings as limits rather than spend authority, and makes sensitive-content
  inspection plus required redaction prerequisites for any remote eligibility.
- The RFC defines closed typed admission/refusal outcomes, deterministic evaluation
  order, local and remote behavior, identity/provenance retention, and the exact future
  Router consumer without granting credentials, network, spend, persistence, or
  reusable authority.
- Existing deterministic-fake local execution, RFC-0016 admission, provider-neutral
  model contracts, Tool/evidence/verification/RunRecord behavior, and durable schemas
  remain unchanged.
- Architecture, its `.ai/` mirror, Roadmap RFC tracking, current-state limitations,
  task, verification evidence, Changelog, and session handoff are synchronized only
  where their owning facts change; Gate 9 maturity does not change.
- Fresh focused document/architecture verification and full Markdown-sensitive Java 17
  regression pass, `git diff --check` passes, and the verified increment is committed
  locally with no push.

## Out Of Scope

Java or test-source implementation; candidate, Router, registry, provider, adapter,
endpoint, protocol, network, credential, secret-store, pricing-source, billing, spend,
MCP, cache, fallback, runtime-event, durable schema or migration work; capability
promotion; external verification; push, merge, release, deployment, permission change,
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

Review RFC-0028 against RFC-0013 through RFC-0027, the accepted outbound sequencing
decision, Architecture, Roadmap Gate 9, and current model types. Run the focused
decision/index, document ownership, Architecture mirror/source, RFC/source-locality,
Planner/Assisted Loop, and dynamic-workflow governance tests that apply, then run the
README-owned full `.\scripts\gradle.ps1 test` command and `git diff --check`.
Subagent reports are recommendations and are not verification evidence.

## Dynamic Workflow

Workflow ID: specify-provider-neutral-outbound-model-route-admission
Mode: Sequential
Increment Limit: 2
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on failed verification, task or checkpoint drift, an RFC-0016
compatibility conflict, missing content/pricing authority semantics, new implementation
or external-effect authority, or unsafe recovery.

### Increment 1 - specify-outbound-route-contract

State: Completed
Depends On: none
Scope: Record the accepted continuation decision and RFC-0028, then synchronize the
Architecture mirror, Roadmap RFC track, Changelog, and active task without changing
implementation or capability maturity.
Exit Criteria: The exact decision/index pair and accepted RFC exist; owned documents
describe one compatible default-deny contract; focused document governance and
`git diff --check` are GREEN.
Verification: Decision/index, ownership, Architecture, task/justification, Planner,
Assisted Loop, RFC-0016/locality, dynamic-workflow tests, and `git diff --check`.
Next Action: Select Increment 2 and close the task with fresh full regression evidence.

### Increment 2 - verify-and-close-specification

State: Completed
Depends On: specify-outbound-route-contract
Scope: Append verification evidence once, synchronize completion and next-task state,
run the full Markdown-sensitive Java 17 regression on the exact final tree, review the
diff, and create the required ordinary local GREEN commit.
Exit Criteria: Fresh full regression and diff checks are GREEN; owned documents are
current; the task is Completed; the local commit exists; Git is clean; and the
checkpoint is stable and clear.
Verification: Full `.\scripts\gradle.ps1 test`, generated JUnit XML counts,
`git diff --check`, diff/status/log review, commit, and checkpoint reconciliation.
Next Action: Await separate user authority for pure RFC-0028 implementation or another
task; do not push.

## Next

Await separate user authority to implement the pure RFC-0028 outbound route-admission
contract RED-first or to select another task. No Router, provider, network, credential,
spend, MCP, push, release, or deployment work is active.
