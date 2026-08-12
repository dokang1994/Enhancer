# Current Task

## Status

In Progress

## Task

Implement and intentionally deliver to `origin/main` a separately executable operator
launcher for installed cancellation-trust maintenance, with typed bounded failure
reporting and isolated temporary-installation integration tests, without invoking it on
a real installation or changing deployment permissions.

## Task ID

implement-cancellation-trust-maintenance-operator-launcher

## Context

The worktree contains the completed installed trust binding, supported runtime
`scheduler-apply-cancel` composition, maintenance contract, and Contract Verified
unexposed `com.enhancer.maintenance` INSTALL/ROTATE state machine. Its recorded next task
is a production operator launcher under separately explicit authority. The user's
2026-08-12 continuation authorizes that next repository-local implementation increment.
The user's repeated follow-up requests to commit, push, and merge to `main` authorize
delivery of this already verified work through the existing `main` branch.

The launcher remains a distinct main class and Gradle execution surface. It is not an
`EnhancerCli` command, scheduler command, runtime request, installed-distribution
mutation, installer, or deployment. Every invocation in this task targets only
JUnit-owned temporary installation trees.

## Justified By

- User continuation request on 2026-08-12 after maintenance state-machine completion
- 2026-08-12: Implement Cancellation Trust Maintenance With A Persistent Stateless Lock
- 2026-08-12: Separate Cancellation Trust Maintenance From Runtime Authority

## Approval

The user's continuation authorizes the minimum repository-local accepted decisions,
typed maintenance failures, dedicated Java operator main/execute surface, fixed Gradle
launcher task, tests, documentation synchronization, checkpoints, and fresh verification
required for the recorded production operator launcher increment. It authorizes launcher
execution only against test-owned temporary installation trees and one bounded read-only
development review. It also authorizes one commit containing the scoped synchronized
worktree and pushing the existing local `main` branch to `origin/main`; because the work
is already on `main`, no separate branch merge or merge commit is required.

It does not authorize invoking maintenance on a real installation; modifying any real
installed JAR, metadata, policy, lock, ACL, ownership, permission, package, launcher
installation, or deployment; adding the command to `EnhancerCli` or a scheduler/runtime
surface; installer/distribution mutation; cleanup/deletion; external anti-rollback;
credentials/private keys; tag, release, deployment, paid service, external message,
destructive cleanup, history rewriting, or unrelated work.

## Acceptance Criteria

- One dedicated `com.enhancer.maintenance` main/execute class accepts exactly `install`
  or `rotate`, required absolute normalized `--application-jar` and
  `--candidate-policy`, and ROTATE-only lowercase
  `--expected-current-metadata-sha256`; it rejects duplicates, unknowns, missing values,
  positionals, INSTALL CAS input, and all runtime/trust-pin/permission/cleanup fields.
- The launcher delegates only to `CancellationTrustMaintenance`, derives no pin or
  artifact path from environment, JVM properties, working directory, repository,
  request, proof, or ambient identity, and never reaches `EnhancerCli`, scheduler,
  runtime, audit, event, or AgentRuntime code.
- Maintenance failures are typed at their source so the launcher maps usage/input or
  invalid installed/candidate/current configuration to exit `2`, safe existing-binding/
  lock-contention/stale-CAS refusal to exit `20`, unexpected persistence/atomic/post-
  switch failure to exit `70`, and exact `INSTALLED`/`ROTATED`/`EXACT_REPLAY` success to
  exit `0` without parsing exception text.
- Success output is bounded deterministic key/value text containing only status,
  content-addressed public policy path, policy digest, and metadata digest. Failure
  output is bounded, single-channel, stack-free, and exposes no policy bytes, proof,
  credential, private material, environment, or unrelated filesystem content.
- A fixed Gradle `JavaExec` operator task selects only the dedicated main class without
  changing the application plugin's `EnhancerCli` main class or installing launcher
  scripts/distributions.
- Focused test-first coverage proves parsing, exact delegation, all exit classes,
  no-write replay, lock/stale refusal, malformed/private candidate denial, unexpected
  durable failure, output bounds, application-main preservation, and zero runtime CLI
  exposure using only isolated temporary installation trees.
- Fresh focused, full Java, architecture/governance, reference/search, diff, and status
  checks pass. Owning documents claim neither real-install operation nor permission,
  deployment, cleanup, or privileged anti-rollback maturity.
- The synchronized scoped change is committed on the existing `main` branch and pushed
  without force to `origin/main`; local `HEAD`, `main`, and `origin/main` agree afterward.

## Out Of Scope

Real installation invocation, installed-distribution or start-script packaging,
installer generation, `EnhancerCli`/scheduler/runtime command wiring, ACL/owner/
permission inspection or mutation, deployment, cleanup/deletion, metadata v2, automatic
rollback, backup, signed-release/package-manager/TPM/keystore integration, private keys,
credentials, proof generation, IdP/session integration, queue/process/Tool/effect
cancellation, `PAUSE`/`RESUME`, branch creation, history rewriting, force push, tag,
release, deployment, paid service, external message, destructive cleanup, or unrelated
work.

## Allowed Tools

- read-file
- write-code
- write-tests
- write-docs
- verify
- checkpoint
- git-inspect
- git-commit
- git-push-main
- bounded-read-only-subagent-review

## Dynamic Workflow

Workflow ID: implement-cancellation-trust-maintenance-operator-launcher
Mode: Sequential
Increment Limit: 4
Selection Rule: Select the first dependency-ready Pending increment after reading fresh
evidence for every dependency.
Stop Conditions: Stop on real-install or permission mutation, runtime authority leakage,
untyped failure ambiguity, deployment/distribution expansion, failed verification,
checkpoint mismatch, or an unjoined reviewer.

### Increment 1 - type-failures-and-fix-launcher-contract

State: Completed
Depends On: none
Scope: Record the explicit launcher decision, type maintenance failure categories at
their source, and add focused RED parsing/delegation/exit/output/build-separation tests.
Exit Criteria: The launcher contract is exact, public failure classification requires no
message parsing, and focused tests fail only for absent launcher/failure types or missing
declared behavior.
Verification: Joined bounded read-only security review and focused RED output.
Next Action: Implement the dedicated main/execute surface and fixed Gradle JavaExec task.

### Increment 2 - implement-and-integrate-operator-launcher

State: Completed
Depends On: type-failures-and-fix-launcher-contract
Scope: Implement strict argument parsing, typed exit mapping, bounded output, state-
machine delegation, and one non-distribution Gradle JavaExec operator task.
Exit Criteria: Focused launcher, maintenance, loader, and separation tests pass using
temporary installation trees only.
Verification: Focused launcher/state-machine/runtime-separation tests and task inspection.
Next Action: Run full regression, synchronize owning documents, and close.

### Increment 3 - verify-synchronize-and-close

State: Completed
Depends On: implement-and-integrate-operator-launcher
Scope: Run full Java/architecture regression, append verification once, synchronize only
owning documents, review final diff/status, and stabilize then clear the checkpoint.
Exit Criteria: All acceptance criteria have fresh evidence, task/workflow are Completed,
and the intended uncommitted worktree is preserved without real-install or external
effects.
Verification: Full suite, structural searches, diff/status, stable/clear/show.
Next Action: Await separate authority for real-install invocation, installed launcher
packaging, permission controls, deployment, cleanup, or external anti-rollback.

### Increment 4 - commit-and-push-main

State: In Progress
Depends On: verify-synchronize-and-close
Scope: Commit the verified synchronized worktree on the existing `main` branch, push it
without force to `origin/main`, verify reference equality and a clean worktree, then
stabilize and clear the delivery checkpoint.
Exit Criteria: One scoped commit contains the intended worktree, local `HEAD`, `main`,
and `origin/main` agree, the worktree is clean, and the stable checkpoint is cleared.
Verification: Commit inspection, post-push reference/divergence/status checks, and
checkpoint stable/clear/show.
Next Action: Await separate authority for any real-install, packaging, permission,
deployment, cleanup, anti-rollback, tag, or release work.

## Verification

- Required repository authority was reread in order. `checkpoint-show` returned `EMPTY`.
  Local `HEAD`, `main`, and `origin/main` remain equal at
  `3367c987fc1d3aaab5ead506e5c243dfad3bbd25` with divergence `0 0`; the existing 37
  changed paths are preserved prior completed work.
- One bounded read-only security review joined and confirmed the new Active Task resolves
  launcher authority while real installation, permissions, deployment, cleanup, and
  installed packaging remain excluded. Its exact parsing, typed error, output, direct-
  JVM exit, and build-separation recommendations are implemented.
- Focused RED initially failed on seven absent launcher/failure types and then narrowed
  to the single absent operator class after finite reason-owned categories compiled.
- Focused final verification ran 22 operator/state-machine/separation tests with zero
  failures, including direct JVM exits `2`, `20`, and `70`, exact replay no-write,
  contention/stale refusal, invalid/private input, injected durability, output bounds,
  and Gradle/application-main separation in isolated temporary installation trees.
- Fresh full Java 17 regression ran 796 tests across 153 suites: 786 passed, 10 skipped,
  and zero failed/errored. Fresh five-suite governance/separation verification passed,
  `git diff --check` was clean, and runtime CLI maintenance searches returned zero hits.
- A Windows sandbox helper cancellation blocked document synchronization four times.
  Checkpoint revision 10 preserved the failed step with zero artifact mismatch; the next
  continuation recovered it and completed the append-only close without implementation
  change.

## Next

Commit and push this verified change through the existing `main` branch under the
user's explicit delivery authority. After delivery, await separate authority for
real-install invocation, installed operator packaging, permission controls, deployment,
cleanup, or external anti-rollback integration.
