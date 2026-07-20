# Current Task

## Status

Completed

## Task

Give every project fact one owning document: move the historical verification and assessment sections out of `PROJECT_STATE.md` into a new append-only `docs/verification-log.md`, reduce `SESSION_HANDOFF.md` to session-scoped facts after rescuing its durable root-cause and rejected-alternative content into `ARCHITECTURE.md`, strip per-gate maturity trailers from `ARCHITECTURE.md`, and rewrite the `CONSTITUTION.md`, `AGENTS.md`, `.ai/workflow.md`, and `prompts/SESSION_CLOSE.md` instructions that required the duplication.

## Task ID

docs-single-owner-restructure

## Justified By

- 2026-07-20: Give Every Project Fact One Owning Document And Separate Verification Evidence From Current State

## Context

Six root documents were rewritten in almost every commit because the same fact had to be edited in three or four places per increment, and the duplicates had already diverged: three documents stated three different next tasks, the handoff instructed the next agent to confirm a task ID that no longer existed, and the recorded test-source count was one higher than the actual. `prompts/SESSION_CLOSE.md` required the duplication explicitly, so the instruction had to change before the documents could stay clean.

## Acceptance Criteria

- `docs/verification-log.md` exists and holds the 58 historical verification and assessment sections in append order, with content preserved exactly; line accounting reconciles against the original 861-line `PROJECT_STATE.md`.
- `PROJECT_STATE.md` retains only current state, capability maturity, accepted product direction, negative-space claims, known limitations, and the current delivery position, and it preserves every hand-authored judgment that no test, decision, or run record can reproduce.
- `SESSION_HANDOFF.md` states only working-tree facts and next-agent instructions; the completion-conflict root-cause analysis and the Option A/B/C queue-capacity alternatives survive in `ARCHITECTURE.md` with the Option letters intact so the existing decision cross-reference stays resolvable.
- `ARCHITECTURE.md` states no per-gate maturity verdict and no ordinal increment narration.
- `CONSTITUTION.md` Section 4 binds single-document ownership; `AGENTS.md`, `.ai/workflow.md`, and `prompts/SESSION_CLOSE.md` instruct writing each fact only to its owning document and no longer require the handoff to restate state, evidence, maturity, the next task, or decisions.
- No production or test source changes; the full regression matches the pre-change baseline of 65 suites and 299 tests with 0 failures and 0 errors.
- `CURRENT_TASK.md`'s `## Justified By` resolves against the new accepted decision through the real `TaskJustificationProjector` path, proving the document pipeline still works end to end.

## Out Of Scope

- Splitting `DECISION_LOG.md` into per-decision files; it requires changes to `AcceptedDecisionProjector`, `RequiredProjectDocument`, and `TaskJustificationProjector` and is its own bounded task.
- Generating `PROJECT_STATE.md` from its sources, and any SQLite or database projection of the documents.
- The residual maturity language in `.ai/architecture.md` beyond the ownership header and the `Specified - Next` bookkeeping removed here.
- Any production or test code change, schema change, or capability maturity promotion.

## Approval

Approved by the user's 2026-07-20 selection of the deduplication-only scope after the code-coupling investigation, with verification by a real increment and merge to `main` through a pull request.

## Verification

- Baseline before the change: 65 suites, 299 tests, 297 passed, 2 existing Windows symbolic-link setup skips, 0 failures, 0 errors under `--warning-mode all`.
- Line accounting on the split: the original 861-line `PROJECT_STATE.md` reconciles exactly as 657 historical lines plus 185 retained lines plus 19 lines of duplicated next-task and reading-order content deleted at source.
- Post-change regression and structural checks are recorded in `docs/verification-log.md`.

## Next

Choose the next bounded Gate 8 sub-increment: worker process isolation (3b) or the concrete `MessageTransport` local IPC adapter (3c).
