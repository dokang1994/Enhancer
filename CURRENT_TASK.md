# Current Task

## Status

Completed

## Task

Verify filesystem escape rejection with Windows junctions and align the Evidence policy API with its actually enforced storage behavior.

## Task ID

security-junction-and-evidence-storage-contract

## Justified By

- 2026-07-16: Verify Real-Path Boundaries With Junctions And Remove Fictional Evidence Retention

## Context

The two `toRealPath()` escape tests are skipped on this Windows host because they require symbolic-link privilege, and the Evidence policy exposes a 30-day retention period that production never reads or enforces.

## Acceptance Criteria

- Add Windows directory-junction escape tests for `ReadFileTool` and `ProjectContextReader` that must execute on Windows.
- Preserve the production real-path containment guards and prove both reject an outside junction target.
- Replace the unused retention-period policy with a truthful max-content storage policy across production and tests.
- Remove the CLI's fictional 30-day retention claim.
- Add no evidence deletion, expiry, migration, replay change, or new cleanup authority.

## Out Of Scope

- Evidence deletion/expiry, aggregate quotas, cleanup scheduling, or migration
- RunRecord retention, graph persistence, or filesystem permission redesign
- Commit, push, PR, merge, release, or deployment

## Approval

Approved by the user's 2026-07-16 request for Windows junction verification and retention-contract cleanup.

## Verification Plan

- Add focused junction tests and a storage-policy contract test with no retention-period accessor.
- Confirm RED against the absent junction coverage and old policy API.
- Rename and narrow the policy without changing stored evidence behavior.
- Run focused context/tool/evidence tests and inspect fresh XML.
- Run the complete Gradle suite with `--warning-mode all` and inspect fresh XML.
- Run Java 17 production compilation with `-Xlint:all -Werror`.
- Synchronize architecture, project state, changelog, and handoff, then review the complete diff and run `git diff --check`.

## Implementation

- Added Windows directory-junction escape tests for governed file reads and required project documents; both execute rather than skip on Windows.
- Preserved and verified both production `toRealPath()` containment guards.
- Replaced `EvidenceRetentionPolicy` with `EvidenceStoragePolicy(maxContentBytes)` and renamed the store accessor accordingly.
- Removed the CLI's unused 30-day duration and changed no stored envelope, replay, expiry, or deletion behavior.

## Verification

- Focused RED produced five expected missing `EvidenceStoragePolicy` errors.
- Focused GREEN passed the storage policy/store and both boundary suites; XML confirms both junction tests executed and the two existing privilege-dependent symbolic-link cases alone were skipped.
- Expanded Tool, Context, Verification, and Loop regression passed 78 tests with 2 existing symbolic-link skips, 0 failures, and 0 errors after correcting a fixed-name junction fixture collision exposed by the first expanded run.
- Fresh whole-project regression passed 44 suites and 200 tests: 198 passed, 2 existing symbolic-link skips, 0 failures, and 0 errors; both Windows junction cases executed.
- Java 17 production compilation passed `-Xlint:all -Werror` across all 115 sources.
- Forbidden legacy-policy/unsafe-current-command searches and `git diff --check` passed before delivery.

## Next

After this authorized delivery is published, activate the Gate 7 transport-neutral IPC interface only as a separate task.
