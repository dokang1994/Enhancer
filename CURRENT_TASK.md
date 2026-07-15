# Current Task

## Status

Completed

## Task

Harden the integrated Gradle build and Delivery Gate 1 through 4 implementation before beginning the first Operational CLI.

## Task ID

gradle9-foundation-hardening

## Context

The user requested correction of every issue found during the Gradle 9 compatibility and implementation-source review. The review reproduced one Gradle 9 test-runtime blocker and multiple boundary defects in timeout isolation, duration representation, persisted-envelope integrity, UTF-8 preservation, startup-context containment, evidence-failure classification, test temporary-directory portability, and Java serialization lint hygiene.

This is a bounded hardening task over capabilities already Integrated. Delivery Gate 5 remains `Specified - Next`; this task does not add a CLI or promote capability maturity.

## Acceptance Criteria

- Declare the JUnit Platform launcher explicitly so Gradle does not rely on the test-framework implementation dependency that Gradle 9 removes.
- Make the standard Gradle test task use a workspace-local temporary directory and remain overrideable by an explicit caller setting.
- Prevent a timed-out interruption-ignoring Tool invocation from starving later Tool invocations.
- Reject timeout values that cannot be represented consistently by execution and persisted policy records.
- Cover envelope version, timestamp, declared length, and payload/content with the stored evidence and RunRecord integrity digest.
- Reject malformed Unicode during RunRecord persistence rather than silently replacing characters.
- Keep required startup documents within the real project root, reject symbolic-link escape where the host permits exercising it, enforce a bounded document size, and decode UTF-8 strictly.
- Classify oversized output from the non-persisting `ReadFileTool` as an evidence-capability execution failure rather than an invalid request.
- Remove production `javac -Xlint:all` serialization warnings without weakening checked failure types.
- Add focused regression tests first, observe expected RED failures, then pass focused and full regression verification.
- Run Gradle with `--warning-mode all`, inspect fresh test XML, run production `javac -Xlint:all -Werror`, and review the final diff.
- Synchronize Architecture, Decision Log, Project State, Current Task, Session Handoff, and Changelog without changing roadmap maturity or Gate 5 ordering.

## Out Of Scope

- Delivery Gate 5 CLI implementation
- Gradle Wrapper major-version upgrade
- New Tool authority, shell or Git mutation, network access, LLM calls, or multi-agent execution
- Signed, encrypted, or externally tamper-proof evidence storage
- Backward migration of unpublished/local pre-hardening binary evidence or RunRecord artifacts
- Commit, push, merge, release, or deployment

## Approval

Approved by the user on 2026-07-15 through the explicit request to fix every issue reported by the Gradle 9 and implementation-source review.

## Allowed Tools

- read-file

## Verification Plan

- Run focused tests for Tool execution, policy decisions, evidence persistence, RunRecord persistence, and project-context loading.
- Confirm new regression tests fail against the pre-fix implementation for the expected reasons.
- Run the complete Gradle regression suite with `--warning-mode all` and inspect fresh XML counts.
- Run production compilation with `javac -Xlint:all -Werror`.
- Confirm Gradle emits no deprecation warning for automatic test-framework implementation dependency loading.
- Run `git diff --check`, inspect changed files, and review the final diff.

## Implementation Result

- Added explicit `junit-platform-launcher` runtime resolution through the existing JUnit BOM and a workspace-local default Gradle test temp directory with `testTmpDir` override support.
- Replaced the shared single Tool worker with invocation-isolated tracked daemon workers; timed-out interruption-ignoring work is retired without blocking a later invocation.
- Rejected sub-millisecond and nanosecond-overflow timeout values during `ExecutionPolicy` construction.
- Extended Evidence and RunRecord integrity digests across magic/version, timestamp, declared length, and complete content/payload.
- Replaced lossy RunRecord string encoding with a strict UTF-8 encoder.
- Added 1 MiB startup-document limits, strict UTF-8 decoding, and real-project-root containment to `ProjectContextReader`.
- Classified a no-persistence truncated read as `EXECUTION_FAILED` with an evidence-persistence diagnostic rather than `INVALID_REQUEST`.
- Added `serialVersionUID` to all seven production exception classes reported by `javac -Xlint:all`.
- Synchronized Architecture, Tool and environment guides, RFC-0006, compact AI architecture, Decision Log, Project State, Changelog, and Session Handoff without changing Roadmap maturity or Gate 5 ordering.

## Verification

- Focused RED command covered Context Reader, Tool Executor, Execution Policy, ReadFileTool, Evidence Store, and RunRecord Store tests.
- Focused RED result: 28 tests, 7 expected failures, 2 Windows symbolic-link setup skips; the failures matched every newly asserted defect.
- An earlier launcher attempt with a non-existent manually selected temp directory failed before Gradle could start and was discarded as product evidence.
- Focused GREEN result: 6 suites, 28 tests, 26 passed, 2 symbolic-link setup skips, 0 failures, and 0 errors.
- Final command: `.\scripts\gradle.ps1 --no-daemon cleanTest test --warning-mode all`.
- Final result: 21 suites, 90 tests, 88 passed, 2 Windows symbolic-link setup skips, 0 failures, and 0 errors.
- The final Gradle output contained no deprecated automatic test-framework implementation dependency warning.
- Dependency verification resolved `org.junit.platform:junit-platform-launcher:1.10.2` explicitly through the JUnit 5.10.2 BOM.
- The standard suite passed without an external temp override; `.\scripts\gradle.ps1 --no-daemon test -PtestTmpDir=build/tmp/junit-override --tests 'com.enhancer.tool.ToolRequestTest' --rerun-tasks` also passed.
- Java 17 compiled all 64 production source files with `javac -Xlint:all -Werror` and no warnings or errors.
- `git diff --check` passed.

## Next Task

After this hardening task is Completed, implement Delivery Gate 5 First Operational CLI.
