# Current Task

## Status

In Progress

## Task

Deliver the completed runtime-event owner connections through a reviewed commit,
working-branch push, fast-forward merge to `main`, and final `main` push.

## Task ID

deliver-runtime-event-owner-connections-to-main

## Context

Three bounded runtime-event owner increments are implemented, freshly verified, and
documented in the working tree: cancellation request admission,
`VERIFICATION_RECORDED`, and `WORK_ITEM_TERMINATED`. The user explicitly requested
commit, push, and merge to `main` on 2026-08-03.

## Justified By

- User request on 2026-08-03 to commit, push, and merge the completed runtime-event owner connections to main

## Approval

The user explicitly authorized the local commits, working-branch push, merge to
`main`, and `main` push needed for this delivery. Force push, history rewriting,
branch deletion, release, tag, deployment, and unrelated external effects remain
unauthorized.

## Acceptance Criteria

- Reconcile the working tree against the fetched remote `main` without discarding or
  rewriting work.
- Review and commit only the completed runtime-event owner implementation, tests,
  accepted decisions, and synchronized project documents.
- Push a dedicated working branch, merge it into `main` with `--ff-only`, and push
  `main` without force.
- Record fresh delivery and governance verification evidence once in
  `docs/verification-log.md`, then synchronize the closing task state.
- End with local `main`, `origin/main`, and the remote `main` ref at the same commit,
  zero branch divergence, a clean working tree, and no active checkpoint.

## Out Of Scope

Force push; rebase or history rewriting; branch deletion; pull request creation;
release, tag, or deployment; implementation of another runtime-event owner; unrelated
changes.

## Allowed Tools

- read-file
- write-docs
- verify
- git-commit
- git-push
- git-merge
- checkpoint

## Verification

Pending delivery verification.

## Next

Connect `DurableAgentRunRetryController` to one persist-after-decision
`RETRY_DECISION_RECORDED` event under a separate bounded task, keeping
`RETRY_STARTED` separate until the replacement AgentRun is durable.
