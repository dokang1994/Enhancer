# Commit, push, and merge the pinned cancellation trust-policy loader to main

Status: Accepted Decision

## Context

The pinned cancellation trust-policy loader task is completed locally with 15 changed
paths, fresh focused and full verification, a clean diff check, and an empty recovery
checkpoint. `HEAD`, local `main`, and observed `origin/main` are equal at
`59d644ddc46fdb49f505eaf932464b3e44e9a915`. The user explicitly requested commit,
push, and merge to `main`.

## Decision

Treat the user's request as authority to review and verify the exact completed loader
candidate plus this delivery authorization, create an ordinary non-amending commit on
the current `main`, and advance `origin/main` through a non-force `main:main` push only
when a fresh fetch proves the remote remains the candidate parent. Because the work is
already based directly on `main`, that linear update is the requested merge result; do
not manufacture a merge commit.

After the implementation commit is remotely confirmed, synchronize only the owning
delivery task, changelog, and append-only verification evidence, create one ordinary
follow-up evidence commit, and advance `origin/main` again through the same fresh-fetch,
non-force, ancestor-preserving procedure. Keep the checkpoint active through both
external delivery steps and clear it only after local/remote/fetch/merge-base identity,
zero divergence, clean worktree, and final verification are proven.

## Rationale

An exact staged-path review, fresh tests, and explicit ancestry checks preserve the
verified candidate and avoid overwriting concurrent remote work. A small follow-up
evidence commit records facts that cannot truthfully exist until after the first push.

## Consequences

- The delivery may stage and commit only the reviewed loader implementation, tests,
  accepted decisions, and owning documents, followed by the three owning delivery-
  evidence documents.
- Network fetch and non-force push to `origin/main` are authorized. Force push, amend,
  rebase, reset, history rewrite, tag, release, deployment, pull request, issue, branch
  deletion, destructive cleanup, permission/credential change, paid service, external
  message, and unrelated changes remain unauthorized.
- Any remote drift, failed verification, unexpected staged path, reviewer blocker, or
  checkpoint mismatch stops delivery before the next external effect.
