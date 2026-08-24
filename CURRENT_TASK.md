# Current Task

## Status

In Progress

## Task

Correct the platform-dependent legacy MessageEnvelope golden fixtures exposed by the
first typed ModelWork delivery CI run, then complete the bounded non-force delivery and
record truthful external verification evidence.

## Task ID

correct-cross-platform-model-work-golden-fixtures

## Context

The first delivery push placed `78633d1` on remote `main`. GitHub Actions run
`32472094472` used Temurin 17 on Linux and failed exactly one of 949 tests:
`MessageEnvelopeCodecTest.preservesEveryLegacyPayloadFrameByteForByte()`. Its uploaded
report shows that production emitted canonical UTF-8 for `agent-loop-🚀`, while the
Windows-authored expected legacy frames contained bytes for mojibake text. Local `main`,
tracked `origin/main`, and the clean worktree all remain at `78633d1` before correction.

## Justified By

- User authorization on 2026-08-24 to correct cross-platform ModelWork golden fixtures and complete delivery
- User request on 2026-08-21 to commit, push, and merge the typed ModelWork golden-wire work

## Approval

The user's explicit 2026-08-24 continuation authorizes a bounded test-fixture
correction, lifecycle-document synchronization, fresh local verification, ordinary
local commits at verified GREEN increment boundaries, non-force fast-forward pushes of
aligned local `main` to `origin/main`, remote-ref inspection, and observation of the
resulting GitHub Actions runs. It authorizes no production-code or schema change,
workflow rerun, force operation, history rewrite, temporary branch, synthetic merge,
tag, release, deployment, permission or credential change, destructive cleanup, or
unrelated implementation.

## Acceptance Criteria

- The Linux CI report and an explicit UTF-8 focused reproduction expose the expected
  legacy golden mismatch before correction.
- The minimum test-only correction replaces every affected legacy frame expectation
  with canonical UTF-8 bytes while production codec code, legacy v1 structure,
  ModelWork-only v2 structure, and cancellation signing bytes remain unchanged.
- Focused codec/golden/governance tests and the README-owned full Java 17 Gradle test
  task pass freshly; `git diff --check` and commit inspection show only authorized
  paths and behavior.
- Immediately before each push, a fresh fetch and ref inspection prove `origin/main`
  is an ancestor of local `main`; any divergence stops delivery.
- Pushes use explicit `main:main` without force and preserve linear history.
- The correction commit reaches remote `main`, fetched and advertised refs match, and
  its push-triggered GitHub Actions verification succeeds.
- One append-only observation records the failed baseline, corrected commit/range,
  ref identities, fast-forward meaning, and successful external verification.
- The bounded lifecycle/evidence follow-up commit reaches remote `main` without force;
  final local, fetched, and advertised refs match, its external verification succeeds,
  and the worktree and checkpoint are clean and stable.

## Out Of Scope

Production Java changes, message or durable schema changes, codec behavior changes,
RFC changes, capability maturity promotion, architecture or Roadmap contract changes,
workflow rerun, force push, rebase, reset, amend, squash, cherry-pick, temporary branch,
synthetic merge commit, tag, release, deployment, branch deletion, permission or
credential changes, destructive cleanup, and unrelated implementation.

## Allowed Tools

- read-file
- write-code
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

Workflow ID: correct-cross-platform-model-work-golden-fixtures
Mode: Sequential
Increment Limit: 2
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on failed or unrelated local/external verification, remote
divergence, non-fast-forward refusal, unexpected path or ref, checkpoint drift, task
drift, new external authority, exhausted bounds, or unsafe recovery.

### Increment 1 - correct-verify-and-push-golden-fixtures

State: In Progress
Depends On: none
Scope: Reproduce the UTF-8-sensitive legacy golden mismatch, correct only the affected
test fixtures, run focused and full verification, commit the GREEN correction, recheck
ancestry, push aligned local main without force, verify remote refs, and observe the
triggered GitHub Actions verification.
Exit Criteria: The scoped correction is committed and present on remote main through a
fast-forward push, all local checks pass, refs match, and the triggered GitHub Actions
run succeeds.
Verification: Failed CI artifact and explicit UTF-8 RED, focused codec/golden and
governance tests, README-owned full Gradle test task, diff/commit checks, fetch/
merge-base/ref inspection, non-force push output, advertised remote ref, and GitHub
Actions conclusion.
Next Action: Append correction and delivery evidence once, complete the task cursor,
commit the bounded lifecycle update, and push it after fresh verification.

### Increment 2 - record-and-push-corrected-delivery-evidence

State: Pending
Depends On: correct-verify-and-push-golden-fixtures
Scope: Append exact correction/delivery evidence, synchronize the task and handoff,
verify the Markdown-sensitive repository, commit the bounded documentation update,
recheck ancestry, push without force, and verify final refs and external verification.
Exit Criteria: Evidence and the completed cursor are on remote main, final local/
fetched/advertised refs match, final external verification succeeds, and the worktree
and checkpoint are clean and stable.
Verification: Focused governance tests, README-owned full Gradle test task,
`git diff --check`, commit inspection, fresh fetch/merge-base/ref checks, non-force push
output, GitHub Actions conclusion, and final clean-tree inspection.
Next Action: Await separate authority for the coordinated durable ModelWork migration.

## Next

Complete Increment 1, then select Increment 2 only after reading its fresh dependency
evidence.
