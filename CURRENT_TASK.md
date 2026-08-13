# Current Task

## Status

In Progress

## Task

Commit the completed cancellation-trust operator distribution, platform-neutral
installation permission contracts, and fake-gateway-verified Windows adapter boundary;
merge them into `main`; and push the synchronized result to `origin/main` without
rewriting history.

## Task ID

deliver-windows-installation-permission-boundary-to-main

## Context

The repository-local implementation task is Completed and its final Java 17 regression,
Markdown governance, architecture separation, structural dependency inspection,
`git diff --check`, and checkpoint close all passed. The user explicitly requested on
2026-08-13 that the completed work be committed, pushed, and merged into `main`.

The worktree is currently on `main`, and `HEAD`, `main`, and `origin/main` initially
resolve to `3e192436c053f40d6d78dd8cfd60dc6adff35d6e` with divergence `0 0`. To make the
requested merge explicit without creating a synthetic merge commit, delivery branches
from that commit, commits the existing verified work, and fast-forwards `main` with
`--ff-only`. A non-force push then advances `origin/main`.

## Justified By

- User request on 2026-08-13 to commit, push, and merge the Windows installation permission boundary

## Approval

The user's explicit request authorizes repository-local delivery documentation,
checkpoint operations, fresh governance and Git verification, creation of one local
delivery branch, ordinary commits of the currently verified scoped work, a fast-forward
merge into `main`, and non-force pushes needed to synchronize `origin/main`.

It does not authorize force push, history rewriting, destructive reset, deletion,
release, tag, deployment, real installation, native Windows gateway work, permission or
owner mutation, credentials/private-key handling, paid service, external message beyond
the requested Git remote update, or unrelated changes.

## Acceptance Criteria

- A decision records the user's explicit commit/push/merge authority and is indexed.
- Fresh Markdown-sensitive governance and `git diff --check` pass before the first
  commit; the final implementation regression evidence remains the latest evidence after
  production/test code changes.
- One local delivery branch contains the verified implementation commit, and `main`
  accepts it only through `git merge --ff-only`.
- `origin/main` advances only through non-force push, and fresh local/remote ref queries
  prove `HEAD`, `main`, and `origin/main` are equal with divergence `0 0`.
- Delivery history is recorded in `CHANGELOG.md` and Git, verification evidence is
  appended once, `CURRENT_TASK.md` is Completed, and a final closure commit is pushed.
- The checkpoint remains active through branch, commit, merge, and push operations, then
  becomes `STABLE` and is cleared only after zero artifact mismatch and intended Git
  state are verified.

## Out Of Scope

Force push, rebase, amend, squash, reset, history rewriting, synthetic merge commit,
release, tag, deployment, real installation, Windows/native gateway implementation,
permission/ACL/owner mutation, cleanup/uninstall, deletion, rollback, paid service,
external message beyond the requested Git push, or unrelated work.

## Allowed Tools

- read-file
- write-docs
- build-output
- verify
- checkpoint
- git-inspect
- git-branch
- git-commit
- git-merge-fast-forward
- git-push-non-force

## Dynamic Workflow

Workflow ID: deliver-windows-installation-permission-boundary

Mode: Sequential

Increment Limit: 3

Selection Rule: Select the first dependency-ready Pending increment in document order.

Stop Conditions: Stop on failed governance, dirty-path drift outside the verified
manifest, non-fast-forward merge, remote divergence, push rejection, checkpoint drift,
insufficient authority, or any requirement for force/history rewriting.

### Increment 1 - prepare-delivery-commit

State: In Progress

Depends On: none

Scope: Record delivery authority, start the task checkpoint, rerun governance and diff
checks, create the delivery branch, and commit the currently verified work.

Exit Criteria: Governance and diff checks pass and the delivery branch has one ordinary
commit containing the complete intended manifest.

Verification: Fresh governance tests, `git diff --check`, commit show/status inspection,
and checkpoint reconciliation.

Next Action: Fast-forward `main` to the delivery commit.

### Increment 2 - merge-and-push-main

State: Pending

Depends On: prepare-delivery-commit

Scope: Check out `main`, fast-forward merge the delivery branch, push `main` without
force, and verify local/remote refs.

Exit Criteria: `HEAD`, `main`, and `origin/main` equal the delivery commit with divergence
`0 0` and the worktree contains no undisclosed change.

Verification: Merge output, non-force push output, `git fetch origin main`, ref equality,
divergence, log, and status.

Next Action: Record delivery history and close the task.

### Increment 3 - close-delivery-record

State: Pending

Depends On: merge-and-push-main

Scope: Append delivery evidence, update the owning task/changelog documents, run final
governance, commit and push the closure record, verify refs, and clear the checkpoint.

Exit Criteria: Delivery records are current, final governance passes, the closure commit
is present on `origin/main`, refs are equal, and the stable checkpoint is cleared.

Verification: Fresh governance tests, `git diff --check`, commit/push/ref/status checks,
and checkpoint show/clear.

Next Action: Await separate authority for a real/default/native Windows gateway or real
installation enforcement.

## Verification

- Initial delivery inspection found the checkpoint empty, current branch `main`, clean
  diff formatting, and `HEAD`, `main`, and `origin/main` equal at
  `3e192436c053f40d6d78dd8cfd60dc6adff35d6e` with divergence `0 0`.
- The preceding implementation task's final full Java 17 regression ran 822 tests across
  158 suites: 812 passed, 10 environment-dependent cases skipped, and zero failed or
  errored. Its final governance, structural, Git, and checkpoint verification also
  passed after the last production/test change.

## Next

Record the delivery decision, run fresh governance, and create the scoped delivery
commit on a local branch before the fast-forward merge into `main`.
