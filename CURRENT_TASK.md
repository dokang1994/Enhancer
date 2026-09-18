# Current Task

## Status

In Progress

## Task

Implement the pure RFC-0028 provider-neutral outbound model route-admission contract
RED-first, without adding a production caller, Router, provider adapter, credential,
network, or spend path.

## Task ID

implement-provider-neutral-outbound-model-route-admission

## Context

RFC-0028 and its accepted sequencing decision now define the required default-deny
boundary after RFC-0016 admission and before any remote model route can become
eligible. The user requested continuation on 2026-09-18. This task implements only
that pure immutable contract and evaluator; runtime fact acquisition and consumption
remain separately governed future work.

## Justified By

- User continuation request on 2026-09-17 into provider-neutral outbound model route-admission specification
- User continuation request on 2026-09-18 into RFC-0028 pure outbound route-admission implementation

## Approval

The user's 2026-09-18 continuation request authorizes production and test source
changes only for the pure RFC-0028 contract under `com.enhancer.model`, one matching
accepted continuation decision and Decision Log entry, focused and full README-owned
verification, owned-document synchronization, and ordinary local commits at verified
GREEN increment boundaries.

This approval does not authorize production fact sources or callers, a Router or
registry, a second executable candidate, provider or adapter construction, endpoint or
network access, credentials or secrets, spend or paid invocation, pricing acquisition,
content scanner or redactor implementation, runtime or Scheduler wiring, durable
schema or persistence changes, MCP, push, merge, release, deployment, permission
change, destructive cleanup, or another external effect.

## Acceptance Criteria

- One accepted decision records the user's bounded continuation authority, with an
  exact matching `DECISION_LOG.md` entry.
- Provider-neutral immutable proposal and fact values implement RFC-0028's exact
  versions, temporal bounds, identities, provenance, and cross-bindings.
- A sealed route-admission decision exposes eligible and refused outcomes, and one
  closed rejection enum contains the 24 RFC-0028 reasons in normative order.
- A field-free evaluator delegates to existing RFC-0016 admission and continues only
  from its exact `OUTBOUND_POLICY_REQUIRED` refusal; every earlier RFC-0016 refusal
  and every local admission remains terminal for outbound routing.
- Evaluation is deterministic and default-deny across strict freshness, exact route,
  purpose, retention, content, inspection, redaction, effective-classification, and
  pricing bindings; only a verified zero-charge route can become eligible.
- Eligible output retains the exact proposal identity and effective classification,
  safe rendering does not disclose outbound content, and production source and
  dependency guards prevent forbidden callers or infrastructure coupling.
- The existing deterministic-fake execution path and RFC-0016 behavior remain
  unchanged.
- Fresh focused and full Java 17 verification passes, owned documents are synchronized,
  each verified GREEN increment is committed locally, and no push occurs.

## Out Of Scope

Production fact sources or callers; Router, registry, second executable candidate,
provider, adapter, endpoint, protocol, network, credential, secret-store,
pricing-source, billing, spend, scanner, redactor, MCP, cache, fallback, runtime-event,
Scheduler wiring, durable schema or migration work; capability promotion; external
verification; push, merge, release, deployment, permission change, destructive cleanup,
and unrelated external effects.

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
- development-subagents-read-only

## Verification

Use test-first development for observable evaluator behavior. Run focused model
contract, evaluator, reflection, source-locality, dependency, and architecture tests,
including applicable RFC-0014 through RFC-0016 and deterministic-fake regressions.
Then run the README-owned full `.\scripts\gradle.ps1 test` command and
`git diff --check`. Read the fresh output before making lifecycle claims. Subagent
reports are recommendations and are not verification evidence.

## Dynamic Workflow

Workflow ID: implement-provider-neutral-outbound-model-route-admission
Mode: Sequential
Increment Limit: 4
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on failed verification, task or checkpoint drift, an RFC-0016
compatibility conflict, incomplete closed-world semantics, new caller or external-
effect authority, or unsafe recovery.

### Increment 1 - define-route-proposal-values

State: Completed
Depends On: none
Scope: Record the accepted continuation decision, add RED tests for RFC-0028's
immutable provider-neutral proposal/fact values and closed outcomes, then implement
the minimum pure values required to make those tests GREEN.
Exit Criteria: Exact decision/index pair exists; value construction, validation,
identity, provenance, safe rendering, and the 24-reason closed vocabulary are covered
by fresh focused GREEN tests; verification evidence is appended once; and the increment
is committed locally.
Verification: Focused RFC-0028 value, reflection, dependency, decision/index, document
governance, and `git diff --check` tests.
Next Action: Select Increment 2 after reading fresh Increment 1 evidence.

### Increment 2 - implement-default-deny-evaluator

State: In Progress
Depends On: define-route-proposal-values
Scope: Add RED decision-table tests, then implement the minimum field-free evaluator
that delegates to RFC-0016 and enforces RFC-0028's ordered default-deny checks.
Exit Criteria: Every RFC-0016 gate and all RFC-0028 refusal branches are covered;
eligible output retains exact identity; focused evaluator/regression tests are GREEN;
evidence is appended once; and the increment is committed locally.
Verification: Focused evaluator decision table, RFC-0014 through RFC-0016,
deterministic-fake regression, architecture, and `git diff --check` tests.
Next Action: Select Increment 3 after reading fresh Increment 2 evidence.

### Increment 3 - enforce-source-and-locality-guards

State: Pending
Depends On: implement-default-deny-evaluator
Scope: Add and satisfy source, locality, reflection, and dependency guards proving
that the pure contract has no production caller or forbidden infrastructure coupling.
Exit Criteria: Guard tests are GREEN; no runtime or infrastructure caller exists;
evidence is appended once; and the increment is committed locally.
Verification: RFC-0028 source/locality, dependency, reflection, architecture, task,
and `git diff --check` tests.
Next Action: Select Increment 4 after reading fresh Increment 3 evidence.

### Increment 4 - verify-and-close-pure-contract

State: Pending
Depends On: enforce-source-and-locality-guards
Scope: Synchronize owned documents, run full Markdown-sensitive Java 17 regression on
the exact final tree, review the diff, close the task, and create the final required
ordinary local GREEN commit.
Exit Criteria: Fresh full regression and diff checks are GREEN; owned documents are
current; the task is Completed; local commits exist; Git is clean; and the checkpoint
is stable and clear.
Verification: Full `.\scripts\gradle.ps1 test`, generated JUnit XML counts,
`git diff --check`, diff/status/log review, commits, and checkpoint reconciliation.
Next Action: Await separate authority for a future Router or governed fact sources;
do not push.

## Next

Complete Increment 2 by defining and verifying the field-free default-deny evaluator
against every RFC-0016 and RFC-0028 refusal branch. Do not add a production caller,
Router, provider, network, credential, spend, MCP, push, release, or deployment path.
