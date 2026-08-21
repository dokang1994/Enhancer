# Current Task

## Status

In Progress

## Task

Deliver the three completed and verified typed ModelWork golden-wire commits from local
`main` to `origin/main` with non-force fast-forward pushes, observe external
verification, and record truthful delivery evidence.

## Task ID

deliver-typed-model-work-golden-wire-to-main

## Context

The clean local `main` ends at `4b098b8` and contains three commits after tracked
`origin/main` at `f654ed9`: the typed payload, ModelWork-only golden wire v2, and final
lifecycle synchronization. They already lie directly on `main`, so a non-force
fast-forward push is the requested merge. A fresh fetch and ancestry check are still
mandatory immediately before each push.

## Justified By

- User request on 2026-08-21 to commit, push, and merge the typed ModelWork golden-wire work
- User continuation request on 2026-08-21 into the typed model-work golden-wire implementation

## Approval

The user's explicit 2026-08-21 request authorizes a bounded delivery-authority commit,
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
- The first push delivers the three completed implementation commits plus the delivery-
  authority commit, and fetched and advertised remote refs match the pushed local HEAD.
- The push-triggered GitHub Actions verification reaches a successful terminal state
  before delivery evidence is recorded.
- One append-only observation records the exact pushed range, ref identities, fast-
  forward/merge meaning, and external verification result; the task cursor is completed
  in one bounded follow-up commit.
- The follow-up commit is pushed without force after a fresh ancestry check; final local
  HEAD, `origin/main`, and advertised remote main match, the worktree is clean, and the
  final push-triggered verification succeeds.
- No product implementation, RFC contract, capability maturity, architecture, Roadmap,
  runtime, schema, permission, tag, release, or deployment change occurs.

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

Evidence is appended once per completed increment to `docs/verification-log.md` after
the declared checks complete.

## Dynamic Workflow

Workflow ID: deliver-typed-model-work-golden-wire-to-main
Mode: Sequential
Increment Limit: 2
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on failed local or external verification, remote divergence,
non-fast-forward refusal, unexpected path or ref, checkpoint drift, task drift, new
external authority, exhausted bounds, or unsafe recovery.

### Increment 1 - verify-and-push-model-work-golden-wire

State: In Progress
Depends On: none
Scope: Commit the delivery authority/cursor, rerun local governance and full
verification, recheck ancestry, push aligned local main without force, verify remote
refs, and observe the triggered GitHub Actions verification.
Exit Criteria: Local checks pass, the authority commit and all three completed
implementation commits are present on remote main through a fast-forward push, refs
match, and the triggered GitHub Actions run succeeds.
Verification: Focused governance tests, README-owned full Gradle test task, diff checks,
fetch/merge-base/ref inspection, non-force push output, advertised remote ref, and
GitHub Actions conclusion.
Next Action: Append delivery evidence once, close the cursor, commit, and push the
bounded follow-up.

### Increment 2 - record-and-push-model-work-delivery-evidence

State: Pending
Depends On: verify-and-push-model-work-golden-wire
Scope: Append the exact delivery observation, complete the task cursor, verify the
Markdown-sensitive repository, commit the bounded evidence update, recheck ancestry,
push without force, and verify final refs and external verification.
Exit Criteria: Delivery evidence and the completed cursor are on remote main, final
local/fetched/advertised refs match, final external verification succeeds, and the
worktree and checkpoint are clean and stable.
Verification: Focused governance tests, `git diff --check`, commit inspection, fresh
fetch/merge-base/ref checks, non-force push output, GitHub Actions conclusion, and final
clean-tree inspection.
Next Action: Await separate authority for the coordinated durable ModelWork migration.

## Next

Complete Increment 1, then select Increment 2 only after reading its fresh dependency
evidence.
