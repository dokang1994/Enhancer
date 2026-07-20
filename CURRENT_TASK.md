# Current Task

## Status

Completed

## Task

Make the recorded `-Xlint:all -Werror` guarantee executable by enforcing it in the Gradle build instead of an ad-hoc javac invocation the build never ran.

## Task ID

enforce-strict-lint-in-build

## Context

Every increment since Gate 1 recorded "Java 17 strict lint passed" in
`docs/verification-log.md`, but neither `-Xlint:all` nor `-Werror` appeared in
`build.gradle`, `scripts/`, or any CI configuration. The flags lived only in a manual
javac invocation each session was trusted to remember. A lint regression therefore
could not fail the build, and `./gradlew build` reported green for a tree the recorded
standard would have rejected. This is the same class of gap the Markdown `inputs.files`
declaration already closes for the document guards: a check that does not run is not a
check.

## Acceptance Criteria

- `-Xlint:all -Werror` is applied by the build itself, so `./gradlew build` enforces
  what the verification log has been claiming.
- The guard is proven to fire rather than assumed: a deliberate warning fails the
  build, and the failure is observed on both production and test sources.
- Enforcement covers test sources as well, which already compile clean under the same
  flags and can regress just as easily.
- The change is behaviour-preserving: the regression count and outcome are unchanged.

## Out Of Scope

- CI configuration; this repository has none, and adding it is Gate 16 work.
- Any static-analysis tool beyond javac's own lint.
- Suppressing or reformatting existing code; nothing required a fix, which is the
  evidence that the manual practice had in fact been followed.

## Approval

Approved by the user's 2026-07-20 request to remedy the enforcement gap identified in
the completeness review, then commit, push, and merge the result.

## Verification

Recorded in `docs/verification-log.md` under Strict Lint Build Enforcement Verification.

## Next

Wire `ProcessIsolatedAgentRunExecution` into `DurableAgentRunWorker` and decide spool
retention, since an invocation root currently persists for every cycle with nothing to
remove it. This increment did not touch that work and it remains the next connection.
