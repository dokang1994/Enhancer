# Current Task

## Status

In Progress

## Task

Deliver the five completed and verified coordinated durable ModelWork migration commits
from local `main` to `origin/main` with a non-force fast-forward push, observe external
verification, and record truthful delivery evidence.

## Task ID

deliver-coordinated-durable-model-work-migration-to-main

## Context

The clean local `main` currently ends at `628d24e` and contains five commits after the
tracked `origin/main` at `9424062`. Those commits implement and verify current-schema
dual-payload retention, coordinated preflight and cutover, crash re-entry and drift
hardening, and the bounded migration operator. They already lie directly on `main`, so
a non-force fast-forward push is the requested merge. A fresh fetch and ancestry check
remain mandatory before external delivery.

## Justified By

- User request on 2026-08-31 to commit, push, and merge the coordinated durable ModelWork migration to main
- User continuation request on 2026-08-25 into the coordinated durable ModelWork migration implementation

## Approval

The user's explicit 2026-08-31 request authorizes a bounded delivery-authority commit,
fresh local verification, non-force push of aligned local `main` to `origin/main`,
remote-ref and GitHub Actions observation, one truthful delivery-evidence follow-up
commit, and one final non-force push. It authorizes no force operation, history rewrite,
temporary branch, synthetic merge commit, tag, release, deployment, branch deletion,
permission or credential change, destructive cleanup, or additional implementation.

## Acceptance Criteria

- Fresh fetch and ref inspection prove `origin/main` is an ancestor of local `main`
  immediately before each push; any divergence stops delivery.
- The delivery authority and cursor are committed locally only after focused governance
  and fresh full README-owned Java 17 Markdown-sensitive verification pass.
- Pushes use the explicit `main:main` refspec without force and preserve linear history;
  no temporary branch or synthetic merge commit is created.
- The first push delivers all five completed commits plus the delivery-authority commit,
  and the advertised remote `refs/heads/main` matches the pushed local HEAD.
- The push-triggered GitHub Actions verification is observed to a successful terminal
  conclusion before delivery evidence is recorded.
- One append-only delivery observation records the exact pushed range, ref identities,
  fast-forward/merge meaning, and external verification result; the task cursor is then
  completed in one bounded follow-up commit.
- The follow-up commit is pushed without force after a fresh ancestry check; final local
  HEAD, `origin/main`, and advertised remote main match, the worktree is clean, and the
  final push-triggered verification is successful.
- No product implementation, RFC contract, maturity, architecture, Roadmap, runtime,
  schema, permission, tag, release, or deployment change occurs.

## Out Of Scope

Force push, rebase, reset, amend, squash, cherry-pick, temporary branch, synthetic merge
commit, tag, release, deployment, branch deletion, permission or credential changes,
destructive cleanup, additional implementation, RFC changes, and maturity promotion.

## Allowed Tools

- read-file
- write-docs
- build-output
- verify
- checkpoint
- git-inspect
- git-stage
- git-commit
- git-fetch
- git-push
- gh-inspect

## Verification

- Increment 1: focused governance passed 21 tests across 6 suites, the fresh full Java
  17 regression passed 990 tests across 180 suites with zero failures/errors and 10
  skips, and `99fa18f` was committed after clean diff checks. A fresh fetch proved
  remote `9424062` was the local merge base; non-force `main:main` push fast-forwarded
  `9424062..99fa18f`, fetched and advertised refs matched, and GitHub Actions run
  `33347793672` job `99355175758` succeeded with two dependency-deprecation warnings.
- Increment 2 evidence is pending.

## Dynamic Workflow

Workflow ID: deliver-coordinated-durable-model-work-migration-to-main
Mode: Sequential
Increment Limit: 2
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on failed local or external verification, remote divergence,
non-fast-forward refusal, unexpected path or ref, checkpoint drift, task drift, new
external authority, exhausted bounds, or unsafe recovery.

### Increment 1 - verify-and-push-coordinated-migration

State: Completed
Depends On: none
Scope: Commit the delivery authority/cursor, rerun local governance and full
verification, recheck ancestry, push aligned local main without force, verify the
remote ref, and observe the triggered GitHub Actions verification.
Exit Criteria: Local checks pass, the delivery-authority commit and all five completed
increments are present on remote main through a fast-forward push, the advertised ref
matches, and the triggered GitHub Actions run succeeds.
Verification: Focused governance tests, README-owned full Gradle test task, diff checks,
fetch/merge-base/ref inspection, non-force push output, advertised remote ref, and
GitHub Actions conclusion.
Next Action: Append delivery evidence once, close the cursor, commit, and push the
bounded follow-up.

### Increment 2 - record-and-push-delivery-evidence

State: In Progress
Depends On: verify-and-push-coordinated-migration
Scope: Append the exact delivery observation, complete the task cursor, verify the
Markdown-sensitive repository, commit the bounded evidence update, recheck ancestry,
push without force, and verify final refs and external verification.
Exit Criteria: Delivery evidence and the completed cursor are on remote main, final
local/fetched/advertised refs match, final external verification succeeds, and the
worktree and checkpoint are clean and stable.
Verification: Focused governance tests, git diff checks, commit inspection, fresh
fetch/merge-base/ref checks, non-force push output, GitHub Actions conclusion, and final
clean-tree inspection.
Next Action: Await separate authority for Model RunRecord v2 and Scheduler model-work
admission integration.

## Next

Complete Increment 2 by committing and pushing the exact delivery observation, then
verify final refs and external verification.
