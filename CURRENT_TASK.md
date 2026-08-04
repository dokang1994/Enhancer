# Current Task

## Status

Completed

## Task

Establish a repository-wide adaptive development-subagent policy so the primary Agent
selects bounded subagents when independent analysis, comparison, risk review, or test
surface review materially improves a task, while keeping a single-agent topology for
small, sequential, or tightly coupled work.

## Task ID

govern-adaptive-development-subagent-delegation

## Context

The repository currently permits subagents only through one-off task authority even
though Constitution Section 7 already permits read-only inspection and normal local
implementation steps inside the user request and Active Task. This makes execution
topology a repeated chat decision rather than a bounded primary-Agent responsibility.

This task governs Codex development sessions over this repository. It does not
implement or promote the Gate 13 product runtime, background execution, typed Handoff
delivery, or multi-agent capability maturity. The user supplies the work authority;
the primary Agent selects the smallest useful execution topology inside that envelope.

## Justified By

- 2026-08-04: Let Primary Agents Select Bounded Development Subagents Adaptively

## Approval

The user requested a repository configuration in which the primary Agent automatically
judges whether a task is large enough or otherwise benefits from subagents, and directed
that the policy contain no per-use explicit-user-approval condition for subagent
selection.

This task authorizes one accepted development-session delegation decision, a structural
RED/GREEN document contract, synchronization of Agent instructions, compact workflow,
Architecture/mirror, human README, affected prompts, governance state, evidence, and
history, plus fresh verification. Existing authority rules still govern the underlying
work and every privileged action; delegation cannot create new scope, Tools,
permissions, budget, lifecycle state, or external/destructive authority.

After local completion, the user explicitly authorized committing the complete verified
working tree, pushing it, and merging it into `main`. Because the working branch is
already `main`, the authorized delivery is one commit directly on `main`, followed by a
push to `origin/main` and an exact local/remote synchronization check; no synthetic
merge commit or temporary branch is required.

## Acceptance Criteria

- The primary Agent evaluates adaptive delegation for non-trivial tasks and selects the
  smallest topology whose expected quality, risk, or latency benefit exceeds
  coordination cost.
- The policy names positive selection signals and explicit single-agent conditions,
  requires concrete independent scopes and join criteria, and works with or without a
  Dynamic Workflow.
- Development subagents are read-only by default and bounded to at most three concurrent
  children, one delegation level, three dispatches per increment, and six per Active
  Task. Nested delegation, background continuation, and shared-worktree parallel
  mutation remain prohibited.
- The primary Agent alone reconciles repository authority, mutates files/checkpoints/Git,
  classifies RED/GREEN, reads raw verification, synthesizes reports, and makes lifecycle
  claims. Subagent reports remain recommendations, never authority or verification.
- Dynamic Workflow increment selection remains sequential; adaptive delegation may
  parallelize only independent bounded read-only work inside the selected increment and
  does not create a second task or runtime capability.
- Fresh structural RED/GREEN, governance regression, full Java 17 build, and final diff
  checks pass, with the decision, instructions, architecture, state, task, evidence,
  changelog, README, prompts, and handoff reviewed and synchronized where affected.
- The complete verified 27-path working tree is staged without generated output,
  deletions, or credentials, committed with a subagent-governance documentation message,
  and delivered so `main`, `origin/main`, and `HEAD` resolve to the same commit.

## Out Of Scope

Constitution amendment; Gate 13 product/runtime implementation or maturity promotion;
typed Handoff or MessageEnvelope changes; Scheduler, background, supervisor, or worker
runtime behavior; subagent mutation; shared-worktree parallel writes; nested delegation;
more than three concurrent subagents; automatic approval; scope, Tool, permission,
budget, or lifecycle expansion; release, deployment, destructive action,
credential/security change, paid service, external message, or unrelated work.

## Allowed Tools

- read-file
- subagent-read-only
- write-tests
- write-docs
- verify
- checkpoint
- git-inspect
- git-stage
- git-commit
- git-push

## Subagent Coordination

Coordinator: Primary Agent
Selection Basis: Governance/self-hosting rule changes benefit from independent bounded
policy, conflict, and test-surface review.
Maximum Concurrent Subagents: 3
Delegation Depth: 1
Mutation Owner: Primary Agent only
Evidence Rule: Reports are recommendations and never verification evidence.
Join Rule: All three declared reports must be joined and reconciled before the policy
contract is accepted.

- Delegation Policy Analyst: recommend automatic selection and single-agent criteria,
  bounds, ownership, synthesis, and stop conditions.
- Governance Conflict Analyst: verify constitutional compatibility and distinguish
  development delegation from Gate 13 runtime authority.
- Governance Test Analyst: identify affected instruction surfaces, structural RED/GREEN
  assertions, and regression scope.

## Dynamic Workflow

Workflow ID: adaptive-development-subagent-governance
Mode: Sequential
Increment Limit: 5
Selection Rule: Select the first dependency-ready Pending increment after reading the
required evidence for every dependency.
Stop Conditions: Stop on missing subagent join, unresolved repository-authority
conflict, failed verification, task drift, overlapping mutation, scope or authority
expansion, exhausted bounds, stagnation, or unsafe recovery.

### Increment 1 - analyze-adaptive-delegation-policy

State: Completed
Depends On: none
Scope: Join three bounded read-only reviews of delegation policy, governance conflicts,
and the structural test surface, then reconcile them against repository authority.
Exit Criteria: All reports are joined without mutation or nested delegation, and the
primary review identifies one Constitution-compatible development-session policy.
Verification: Primary structural review of Constitution Sections 7 and 9, AGENTS,
Architecture, RFC-0009, existing decisions, prompts, tests, checkpoint, and diff.
Next Action: Record the accepted policy and focused structural RED contract.

### Increment 2 - specify-adaptive-delegation-contract

State: Completed
Depends On: analyze-adaptive-delegation-policy
Scope: Record one indexed Accepted Decision and a focused failing structural test for
the adaptive delegation rules and required instruction surfaces.
Exit Criteria: The decision and RED remain inside this task, distinguish development
delegation from Gate 13 runtime, and fail only on the missing policy connections.
Verification: Decision/index/task structural review and focused Java 17 RED output.
Next Action: Synchronize the minimum governed instruction surfaces.

### Increment 3 - implement-adaptive-delegation-docs

State: Completed
Depends On: specify-adaptive-delegation-contract
Scope: Add the bounded automatic selection algorithm, ownership and stop rules, Dynamic
Workflow interaction, and product-runtime distinction to the owning documents.
Exit Criteria: Focused structural GREEN passes and no policy grants new underlying
work, Tool, external, destructive, delivery, or lifecycle authority.
Verification: Focused Java 17 delegation and document-governance suites.
Next Action: Run full verification and synchronize final evidence/state/history.

### Increment 4 - verify-adaptive-delegation-governance

State: Completed
Depends On: implement-adaptive-delegation-docs
Scope: Run full and post-synchronization verification, review the final diff, and close
a stable artifact-matched checkpoint.
Exit Criteria: All declared checks pass, every increment is Completed, all reports are
joined, owned documents are current, and the diff stays inside the approved policy
boundary.
Verification: Fresh Java 17 full build, focused governance tests, and final diff review.
Next Action: Stage and deliver the user-authorized verified working tree.

### Increment 5 - deliver-adaptive-delegation-and-accumulated-verified-work

State: Completed
Depends On: verify-adaptive-delegation-governance
Scope: Stage the complete verified 27-path working tree, run cached boundary checks,
commit it directly on the already-current `main` branch with the requested
subagent-documentation subject, push to `origin/main`, and verify exact synchronization.
Exit Criteria: Cached checks pass, one commit contains the complete intended change set,
the push succeeds, and `HEAD`, `main`, and `origin/main` name the same commit with a clean
working tree.
Verification: Cached whitespace/status/stat review, commit inspection, push output,
post-push fetch-independent reference comparison, and clean final status.
Next Action: Record delivery evidence, stabilize the checkpoint, and clear it.

## Verification

- Three bounded read-only subagents joined without editing, testing, checkpoint
  mutation, or nested delegation. Their reports were treated as recommendations only.
- Primary review found no Constitution amendment is needed: Section 7 permits bounded
  in-scope local inspection, while Section 9 is satisfied by this user-requested policy
  task and its accepted decision. Gate 13 runtime contracts remain unchanged.
- The indexed decision and focused structural test compile under Java 17. Focused RED
  ran all three `DynamicWorkflowDocumentTest` cases: the two existing workflow cases
  passed and the new adaptive-delegation case failed only on the eight missing governed
  policy connections.
- Focused GREEN recompiled production and tests and ran 23 tests across six delegation,
  decision-index, ownership, package-boundary, Planner, and Context suites: 22 passed,
  one Windows environment-dependent symbolic-link case skipped, and zero failed or
  errored. Fresh `git diff --check` was clean.
- Fresh Java 17 `clean build --no-daemon --console=plain` completed in 6 minutes 45
  seconds with all eight tasks executed. It assembled distributions and ran 710 tests
  across 134 suites: 703 passed, seven environment-dependent cases skipped, and zero
  failed or errored. Production and test source counts are 305 and 135; fresh
  `git diff --check` was clean.
- Post-synchronization Java 17 verification recompiled production and tests and reran
  the six focused governance suites in 1 minute 57 seconds: 22 of 23 tests passed, the
  one Windows environment-dependent symbolic-link case skipped, and zero failed or
  errored. Fresh `git diff --check` was clean.
- Cached delivery review covered exactly 27 paths with 1,736 insertions and 148
  deletions; `git diff --cached --check` was clean, with no deleted file or generated
  build output. Commit `3d4e4e5949eaf544f9035b96945a724e2b8ba44b` used the requested
  `docs: govern adaptive project subagent usage` subject and retained the accumulated
  verified runtime-event work in its body.
- `git push origin main` advanced the remote from `ae7d733` to `3d4e4e5`. Fresh local
  reference comparison found `HEAD`, `main`, and `origin/main` at the exact same full
  commit identity with a clean working tree.

## Next

After authorized delivery is verified, resume the recorded runtime-event path by
selecting a bounded consumer contract or another explicit owner composition.
