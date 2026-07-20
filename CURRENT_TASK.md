# Current Task

## Status

Completed

## Task

Make the accepted single-document ownership rule executable with a `DocumentOwnershipTest` structural test, remove the six stale gate-maturity claims it exposes across `README.md`, `docs/08-Multi-Agent.md`, `docs/10-Roadmap.md`, `docs/11-Architecture.md`, and `docs/rfcs/RFC-0009-Multi-Agent.md`, and state the store write-root contract exactly in `ARCHITECTURE.md` and `README.md`.

## Task ID

docs-ownership-enforcement

## Justified By

- 2026-07-20: Enforce Document Ownership With A Structural Test And State The Store Write-Root Contract Exactly

## Context

The ownership rule was accepted in prose and drifted the same day: six documents still asserted gate maturity, and all five that named Gate 7 called it `Specified - Next` after Gate 8 had taken the marker. A separate audit finding alleged an unconfined write surface behind `--evidence-root` and `--run-record-root`; re-reading the accepted Gate 5 decision showed the roots are explicit caller inputs by design and the stores already refuse a symbolic-link root, so the real defect was imprecise prose rather than behaviour.

## Acceptance Criteria

- `DocumentOwnershipTest` fails before the document fixes and passes after, asserting that gate maturity appears only in `PROJECT_STATE.md` and `## Next Task` only in `CURRENT_TASK.md`.
- Exemptions cover `PROJECT_STATE.md`, `ROADMAP.md`, `DECISION_LOG.md`, `CHANGELOG.md`, `docs/verification-log.md`, and `docs/superpowers/**`, and nothing else.
- The six stale claims are replaced by references to `PROJECT_STATE.md` rather than updated in place.
- `ARCHITECTURE.md` and `README.md` state that the store roots are explicit caller inputs, not confined to the project root, with `.enhancer/` an example layout; and that each store refuses a symbolic-link root and only creates new UUID-named entries.
- No store or CLI behaviour changes; the only production-side change is documentation.
- Full regression passes at 66 suites and 301 tests with 0 failures and 0 errors.

## Out Of Scope

- Adding project-root confinement to the stores; it contradicts the accepted Gate 5 explicit-input model and would break the existing `CliArgumentsTest` sibling-root case.
- A test for the symbolic-link root refusal; it needs symbolic-link privilege this Windows host denies, and would only add a third permanently skipped test here.
- The `GitWorkspaceCollector` output-cap and watchdog-timeout tests, and the undocumented public types found in the same audit.
- Splitting `DECISION_LOG.md`, and any database projection of the documents.

## Approval

Approved by the user's 2026-07-20 request to fix both audit findings and re-verify the workflow against recurrence, followed by their request to check the write-root design for improvements rather than assume a defect.

## Verification

Recorded in `docs/verification-log.md` under Document Ownership Enforcement Verification.

## Next

Gate 8 connection sub-increment 3b (worker process isolation) or 3c (the concrete `MessageTransport` local IPC adapter), which the user is taking up in a separate session.
