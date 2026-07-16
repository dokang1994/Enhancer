# Current Task

## Status

Completed

## Task

Remove the loop/run/verification package cycle through the smallest behavior-preserving neutral lifecycle and application-orchestration extraction.

## Task ID

runtime-package-cycle-extraction

## Justified By

- 2026-07-16: Make Runtime Persistence And Verification Dependencies Acyclic

## Context

`loop`, `run`, and `verification` compile inside one module but form a strongly connected package component that blocks future Kernel/Runtime/Verification/Persistence module separation. The smallest correction moves verification decision values to a neutral Kernel package and finalization orchestration to an application package while leaving Agent state, durable formats, and behavior unchanged.

## Acceptance Criteria

- Move VerificationDecision, VerificationStatus, and VerificationCode unchanged to `com.enhancer.kernel`.
- Move AgentRunFinalizer orchestration to `com.enhancer.application`.
- Ensure `loop` imports neither `run` nor `verification`.
- Ensure `run` does not import `verification`.
- Ensure `kernel` imports none of application, loop, run, or verification.
- Preserve verification behavior, CLI behavior, RunRecord validation, enum constant names, and binary replay compatibility.
- Add a source-structure regression test for the forbidden dependency directions.
- Update all production and test imports without compatibility wrappers for unreleased old package names.
- Run focused finalizer/verifier/RunRecord/CLI tests, full regression, Java 17 strict lint, self-hosting, structural, and whitespace checks.
- Add no Gradle modules, Agent lifecycle redesign, persistence format version change, process isolation, or capability maturity promotion.

## Out Of Scope

- Moving ApprovedTask, AgentRunState, stop reasons, Tool contracts, or RunRecord types
- Gradle multi-module extraction or JPMS
- Compatibility adapters for unreleased Java package names
- Persistence SPI redesign or schema/version changes
- Commit, push, PR, merge, release, or deployment

## Approval

Approved by the user's 2026-07-16 request to improve the reported package dependency cycle.

## Verification Plan

- Add a structural test first and confirm RED reports the current forbidden import directions.
- Move the three neutral verification values and AgentRunFinalizer with import-only consumer changes.
- Run structural, verifier, finalizer, RunRecord persistence, CLI, graph/view, and integration suites.
- Run the complete regression with fresh XML inspection and `--warning-mode all`.
- Compile all production sources with Java 17 `-Xlint:all -Werror`.
- Synchronize Architecture, compact architecture, Project State, Changelog, Current Task, Decision Log, and Session Handoff.
- Run actual-document Context Reader, Planner, and Assisted Loop tests plus diff and whitespace checks.

## Implementation

- Moved VerificationDecision, VerificationStatus, and VerificationCode unchanged to neutral `com.enhancer.kernel`.
- Moved AgentRunFinalizer to `com.enhancer.application`, where it composes loop state, verification, and RunRecord persistence.
- Added `VerifiedAgentRunTransition` as the explicit application-facing transition port while keeping AgentRunState's actual completion method package-private.
- Updated production and test imports with no compatibility wrappers for unreleased package names.
- Added `RuntimePackageBoundaryTest`, forbidding loop-to-run, loop-to-verification, run-to-verification, and inward kernel imports.
- Preserved enum constant names, RunRecord binary schema/version, finalization behavior, verified-only completion, CLI behavior, and replay compatibility.
- Added no Gradle modules, ApprovedTask relocation, persistence SPI redesign, or capability maturity promotion.

## Verification

- Structural RED compiled successfully and failed exactly one test listing six forbidden directions: loop-to-run 1, loop-to-verification 2, and run-to-verification 3.
- Focused GREEN passed 27 of 27 tests across the package boundary, verifier, finalizer, RunRecord, filesystem replay, and CLI suites.
- Source searches found zero forbidden runtime imports and zero previous verification-value or loop-finalizer package references.
- Fresh full regression with `--warning-mode all` passed 53 suites and 228 tests: 226 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors.
- Java 17 production compilation passed `-Xlint:all -Werror` across all 135 production sources.
- Post-document Context Reader, Planner, Assisted Loop, and package-boundary verification passed 16 of 17 tests with 1 existing Windows symbolic-link setup skip and no failure or error.

## Next

Return to Gate 8 durable Goal/AgentRun lifecycle work. Process isolation and parent-directory power-loss durability remain explicit future boundaries.
