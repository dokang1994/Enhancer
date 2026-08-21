# Current Task

## Status

In Progress

## Task

Implement and verify the accepted RFC-0016 pure model invocation-admission contract
under `com.enhancer.model` without changing existing production source or runtime
wiring.

## Task ID

implement-model-invocation-admission

## Context

RFC-0016 is Accepted and names one stateless admission evaluator, one sealed decision
with exactly two nested record results, and five closed rejection reasons. RFC-0013
through RFC-0015 are already Contract Verified and delivered on `main`; no current
runtime caller has both a complete profile and the required independent authority
sources.

## Justified By

- User continuation request on 2026-08-21 into the RFC-0016 model invocation admission implementation
- User continuation request on 2026-08-20 into the model invocation admission specification

## Approval

The 2026-08-21 continuation authorizes RED-first focused tests, three new pure
production types under `com.enhancer.model`, fresh focused and full Java 17 verification,
owning lifecycle-document synchronization, development-session checkpoints, and
ordinary local commits at verified GREEN increment boundaries. It authorizes no change
to existing production source or runtime wiring, push, merge, release, deployment,
external effect, or destructive cleanup.

## Acceptance Criteria

- Missing-symbol RED fails only because the RFC-0016 evaluator, sealed decision, and
  rejection reason types do not yet exist.
- The evaluator is final, stateless, field-free, accepts the exact RFC-0016 inputs, and
  returns the first matching rejection in the accepted task, policy, capability,
  timeout, locality order.
- The sealed decision exposes exactly nested `Admitted(ProfiledModelRequest)` and
  `Rejected(ModelInvocationRejectionReason)` records, and the enum contains exactly the
  five accepted reasons in order.
- Null inputs fail as caller programming errors; strict timeout and exact-value
  retention boundaries hold; response-character/token limits, classification, and cost
  do not create unrelated policy behavior.
- No existing production source or runtime wiring changes, and no gateway, provider,
  persistence, routing, credential, network, transmission, or spend authority is added.
- Focused model and architecture checks plus the full README-owned Java 17 regression
  pass; owning lifecycle documents and append-only verification evidence are current.

## Out Of Scope

Changes to existing production source; runtime, CLI, Scheduler, Tool, gateway, fake,
adapter, schema, or persistence integration; profile sourcing or parsing; model
suitability; routing; providers; endpoint or destination selection; network or remote
transmission; credentials; paid services; push, merge, release, deployment, history
rewrite, permission changes, and destructive cleanup.

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

- Increment 1: focused RED failed only on the three missing RFC-0016 types. Focused
  GREEN passed 11 tests, and the combined model-package and architecture-governance
  selection passed 72 tests across 13 suites with zero failures, errors, or skips.
  The implementation added exactly three new production files and one focused test;
  existing production source remained unchanged.

## Dynamic Workflow

Workflow ID: implement-model-invocation-admission
Mode: Sequential
Increment Limit: 2
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on contract-conflicting RED, failed verification, task drift,
unexpected existing-production change, checkpoint drift, new authority, exhausted
bounds, or unsafe recovery.

### Increment 1 - implement-pure-invocation-admission

State: Completed
Depends On: none
Scope: Add missing-symbol focused tests, then the minimum three RFC-0016 production
types, and run focused model and architecture verification.
Exit Criteria: RED is classified, the exact pure contract is GREEN, regressions pass,
the diff is scoped and clean, verification evidence is appended once, and the verified
increment is committed locally.
Verification: Focused `ModelInvocationAdmissionTest`, model-package regression,
architecture governance, and `git diff --check`.
Next Action: Select Increment 2 after the verified implementation commit.

### Increment 2 - verify-and-close-invocation-admission

State: In Progress
Depends On: implement-pure-invocation-admission
Scope: Run the full README-owned Java 17 regression, synchronize only changed lifecycle
owners, rerun Markdown-sensitive governance, and close the task.
Exit Criteria: Full verification and final governance pass, project state and evidence
are truthful, the completed cursor is committed locally, and the worktree/checkpoint
reach the intended clean stable state.
Verification: Full `test`, focused architecture/decision/workflow governance,
JUnit XML aggregation, `git diff --check`, and final Git/checkpoint inspection.
Next Action: Complete and locally commit the implementation increment.

## Next

Run Increment 2 full regression and synchronize the final lifecycle owners.
