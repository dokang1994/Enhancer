# 2026-07-20: Give Every Project Fact One Owning Document And Separate Verification Evidence From Current State

Status: Accepted Decision

Context:

- Six root documents were rewritten in almost every commit: `SESSION_HANDOFF.md` and `PROJECT_STATE.md` in 17 of the last 20, `CURRENT_TASK.md`, `CHANGELOG.md`, and `ARCHITECTURE.md` in 13, and `DECISION_LOG.md` in 12. Parallel feature branches therefore conflicted in all six on every increment.
- The mechanical cause was triple-writing, not file size. `Repository State` and `Current Maturity` existed in both `PROJECT_STATE.md` and `SESSION_HANDOFF.md`; per-increment verification records existed in both `PROJECT_STATE.md` and the `### Gate N` subsections of `ARCHITECTURE.md`; the next task was stated in `PROJECT_STATE.md`, `SESSION_HANDOFF.md`, and `CURRENT_TASK.md` at once.
- The duplicates had already diverged. The three next-task statements disagreed; `SESSION_HANDOFF.md` instructed the next agent to confirm a task ID that no longer existed and named a superseded branch; its "not yet committed" claims were false; and the recorded test-source count was 66 against an actual 65.
- `prompts/SESSION_CLOSE.md` required `SESSION_HANDOFF.md` to contain completed work, current state, next task, decisions, and verification commands — it instructed the duplication directly, so no amount of manual cleanup would have held.
- Verification records were 679 of the 861 lines of `PROJECT_STATE.md` (79%). They are immutable evidence written once per increment, not state, and they crowded out the hand-authored maturity judgment that is the document's actual value.
- `DECISION_LOG.md` cannot be split into per-decision files without a code change: `AcceptedDecisionProjector` requires it as one document in repository memory, `RequiredProjectDocument` hard-codes its path, and `TaskJustificationProjector` matches `## Justified By` bullets against its `###` headings by exact string.

Decision:

- Establish in `CONSTITUTION.md` Section 4 that every fact has exactly one owning document, that a document references rather than restates a fact it does not own, and that a discovered duplicate is deleted rather than synchronized.
- Bind three ownership rules explicitly, each because it has already produced a contradiction: the next task belongs to `CURRENT_TASK.md` alone; capability maturity belongs to `PROJECT_STATE.md` alone; delivery history belongs to git and `CHANGELOG.md` alone.
- Add `docs/verification-log.md` as the append-only home for per-increment verification evidence, and move the 58 historical verification and assessment sections there from `PROJECT_STATE.md`, preserving append order and content byte for byte.
- Reduce `PROJECT_STATE.md` to current state, the maturity judgment behind it, and a `Known Limitations` register; reduce `SESSION_HANDOFF.md` to working-tree facts and next-agent instructions; strip per-gate maturity trailers and ordinal increment narration from `ARCHITECTURE.md`.
- Rescue the completion-conflict root-cause analysis and the Option A/B/C queue-capacity alternatives from `SESSION_HANDOFF.md` into `ARCHITECTURE.md`, retaining the Option letters so the existing decision cross-reference stays resolvable.
- Rewrite the duplication-causing instructions in `prompts/SESSION_CLOSE.md`, `AGENTS.md`, and `.ai/workflow.md`, and state that `.ai/workflow.md` is the operational expansion of Constitution Section 6 rather than a competing sequence.
- Keep `DECISION_LOG.md` as a single document. Splitting it is a Project Brain code change, not a documentation change, and requires its own bounded task.

Rationale:

The churn was caused by an instruction to duplicate, so the fix had to be to the instruction, not to the documents. Ownership is enforceable at review time by a single question — does this document own this fact — whereas "keep the documents synchronized" has already been shown to fail silently and produce contradictory copies. Separating immutable evidence from mutable state also restores the signal in `PROJECT_STATE.md`: what remains is exactly the judgment that no test, decision entry, or run record can reproduce.

Consequences:

- `PROJECT_STATE.md` drops from 861 to 172 lines and `SESSION_HANDOFF.md` from 414 to 41; the removed content is preserved in `docs/verification-log.md` or in git.
- An increment now appends evidence once and edits current-state documents only where a fact actually changed, so parallel branches no longer conflict in six files by construction.
- `docs/verification-log.md` grows without bound by design. It is never read for current state and is not one of the required startup documents, so its size does not affect context loading.
- No production or test code changes; `RequiredProjectDocument`'s 15 paths, the `DECISION_LOG.md` heading grammar, and the `## Justified By` exact-match coupling are all preserved.
- Out of scope: splitting `DECISION_LOG.md`, generating `PROJECT_STATE.md` from its sources, any SQLite or database projection, and the residual maturity language still present in `.ai/architecture.md`.
