# Current Task

## Status

Completed

## Task

Execute the authority-preserving integration audit and promote Delivery Gate 0 Foundation Safety Contracts from Contract Verified to Integrated only after fresh evidence passes.

## Task ID

gate-0-integration-promotion

## Context

Gate 0 groups the Repository Context Reader, deterministic Task Planner, Assisted Development Loop, repeated Agent Loop termination, bounded ToolResult and VerificationEvidence, and Constitution governance. Later Gates consume these contracts, but Gate 0 still carries its original Contract Verified label because its integration evidence is distributed across later tasks.

The promotion must prove the lifecycle through real consumers without creating an automatic Proposal-to-approval path. A Planner proposal remains non-authoritative; an explicit external test-fixture transition to an active approved task separates planning from execution.

This maturity-reconciliation task does not displace Delivery Gate 6 as the sole `Specified - Next` product gate.

## Acceptance Criteria

- Inventory every Gate 0 delivered contract and identify its real upstream and downstream integration consumer.
- Add `FoundationLifecycleIntegrationTest` or an equivalently named test over a governed temporary repository before any production edit.
- In the planning phase, load the complete governed context, produce the current Gate 6 proposal from a Completed task state, and prove that planning does not mutate repository documents or create approval.
- Prove execution before explicit task activation is rejected and cannot report completion or fabricate a RunRecord.
- Represent approval only as an explicit external test-fixture transition to an `In Progress` task containing task identity, approval evidence, and `read-file` Tool scope; production code must not perform that transition.
- In the execution phase, derive `ApprovedTask` from the active repository context and connect it through the existing read-only Tool, evidence, Agent Loop, independent verification, finalization, durable RunRecord, and restart-safe replay boundaries.
- Prove that verified execution completes only after RunRecord persistence and that replay does not re-execute the Tool.
- Reuse existing production APIs and the Gate 5 CLI composition; add no second orchestration path unless an aligned RED test demonstrates a real missing boundary.
- Treat an initially GREEN characterization test as valid integration evidence when no production behavior change is required; do not manufacture a failing test solely to satisfy RED terminology.
- If a genuine aligned behavior gap is found, follow the repository RED classification workflow and implement only the minimum scoped correction.
- Run the Gate 0 focused suites, the lifecycle integration test, the Gate 5 CLI suite, and the complete Gradle regression with `--warning-mode all`.
- Inspect fresh XML results, run Java 17 production lint with `-Xlint:all -Werror`, and report every skip and limitation.
- Promote Gate 0 to Integrated only after fresh evidence passes, remove the stale remaining limitation, and synchronize all maturity documents.
- Preserve Delivery Gate 6 Workspace and Project Brain Foundation as the sole `Specified - Next` gate.

## Out Of Scope

- Automatic conversion of a Planner Proposal into an Accepted Decision, Active Task, or Tool authority
- A new `plan` CLI command, interactive approval UI, or mutation of the actual repository during tests
- New Context, Planner, Agent Loop, Tool, Evidence, verifier, RunRecord, or orchestration abstractions without a demonstrated integration gap
- Delivery Gate 6 implementation
- Shell, terminal, Git mutation by product code, network, LLM, MCP, plugin, Skill, scheduler, event bus, or multi-agent behavior
- PR, merge, release, deployment, or publication beyond the separately user-authorized commit and push

## Approval

Approved by the user on 2026-07-15 through the explicit request to continue the prepared Gate 0 promotion work.

## Allowed Tools

- read-file

## Verification Plan

- Add the lifecycle integration test before any production edit.
- Run it once against the existing implementation and classify the result: initial GREEN proves existing integration, aligned RED identifies the minimum missing boundary, and unrelated RED is separated under the repository workflow.
- Run focused Gate 0 Context, Planner, Assisted Loop, Agent Loop, ToolResult, and VerificationEvidence tests.
- Run the Gate 5 CLI suite as the supported real consumer.
- Run the complete Gradle suite with `--warning-mode all` and inspect fresh XML counts.
- Run Java 17 production compilation with `-Xlint:all -Werror`.
- Confirm exactly one `Specified - Next` marker remains at Gate 6 and run `git diff --check`.
- Review the final diff before promotion, commit, and push.

## Implementation Result

- Added `FoundationLifecycleIntegrationTest` over a governed temporary repository without changing production code.
- Proved read-only planning produces the current Gate 6 Proposal while preserving every required document byte-for-byte.
- Proved execution is rejected before explicit activation and creates no evidence or RunRecord storage.
- Used only an external test-fixture transition to create the active task and derive `ApprovedTask` authority.
- Reused the Gate 5 CLI and Gate 1 through 4 boundaries through complete evidence, independent verification, durable completion, target deletion, and restart-safe replay.
- The characterization test passed on its first run, so no production correction or second orchestrator was introduced.
- Promoted Gate 0 from Contract Verified to Integrated and synchronized maturity documentation while retaining Gate 6 as the sole next product gate.

## Verification

- Initial lifecycle characterization: 1 suite, 1 test, passed on the first run with no production edit.
- Combined Gate 0 lifecycle and Gate 5 consumer verification: 10 suites, 43 tests, 42 passed, 1 Windows symbolic-link setup skip, 0 failures, and 0 errors.
- Full command: `.\scripts\gradle.ps1 --no-daemon clean test --warning-mode all`.
- Full result: 25 suites, 98 tests, 96 passed, 2 Windows symbolic-link setup skips, 0 failures, and 0 errors.
- Java 17 production compilation passed with `-Xlint:all -Werror` and no warning or error.
- Roadmap retains exactly one `Specified - Next` marker at Delivery Gate 6.
- `git diff --check` passed.

## Next Task

After verified Gate 0 promotion and publication, Delivery Gate 6 Workspace and Project Brain Foundation remains the next product implementation gate.
