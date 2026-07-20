# Current Task

## Status

Completed

## Task

Close the four open documentation-audit items: widen `DocumentOwnershipTest` to the word orders it was missing and remove the claims that survived, make Gradle re-run the guards when only Markdown changes, document `SchedulerQueueStore` as the Gate 8 durability seam and the planner result types, and cover `GitWorkspaceCollector.resolveGitExecutable`'s rejection branches.

## Task ID

close-documentation-audit-gaps

## Justified By

- 2026-07-20: Close The Audit Gaps And Make The Ownership Guard Run On Documentation Changes

## Context

The ownership guard matched only the subject-first word order and let the verb-first form with an `at` connector through, along with two parenthetical verdicts in a table. Adversarial testing then exposed a worse defect: because the guards read Markdown that Gradle does not track as a task input, a documentation-only change left `test` up to date and the guards silently did not run. `SchedulerQueueStore` is the seam Gate 8 sub-increments 3b and 3c will replace and was documented only through its adapter, and the only class holding external command authority had its positive resolution path covered but none of its rejections.

## Acceptance Criteria

- `DocumentOwnershipTest` matches subject-first, verb-first with an `at`/`to`/`as` connector, and parenthetical table verdicts, and stays quiet on forward-looking conditions and commentary about removed claims.
- The Gradle `test` task declares the project's Markdown as an input, so a documentation-only edit invalidates it.
- `ARCHITECTURE.md` and `docs/11-Architecture.md` carry no surviving maturity claim.
- `ARCHITECTURE.md` and `.ai/architecture.md` describe `SchedulerQueueStore` as the durability seam, stating what stays above it; `ARCHITECTURE.md` describes `TaskProposal`, `ProposalState`, and `PlanningException`.
- `GitWorkspaceCollectorTest` covers candidates inside the observed project at any depth, relative and absent PATH entries, a directory named like the executable, an absent or blank PATH, case-insensitive PATH lookup, and pins the output cap and timeout constants.
- Full regression passes with 0 failures and 0 errors.

## Out Of Scope

- Inducing the 4 MiB output cap or the five-second watchdog with a purpose-built git; both are pinned as constants instead.
- The remaining undocumented public types the audit judged trivial, and a `SUPERSEDES` producer.
- Gate 8 sub-increments 3b and 3c.

## Approval

Approved by the user's 2026-07-20 direction to continue with the remaining audit items, test the modified parts, then run the full regression, then commit, push, and merge to main.

## Verification

Recorded in `docs/verification-log.md` under Documentation Audit Closure Verification.

## Next

Gate 8 connection sub-increment 3b (worker process isolation) or 3c (the concrete `MessageTransport` local IPC adapter).
