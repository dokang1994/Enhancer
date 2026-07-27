# Current Task

## Status

Completed

## Task

Deliver the completed submission manifest schema-v2 priority persistence and migration
increment to `origin/main`.

## Task ID

deliver-submission-manifest-schema-v2-priority-migration

## Context

The implementation task is completed and freshly verified. The user now explicitly
authorized commit, push, and merge. The completed changes already reside in the local
`main` working tree, so delivery must preserve that topology without manufacturing an
empty merge commit.

## Justified By

- 2026-07-27: Deliver Submission Manifest Schema V2 Priority Migration Directly To Main
- 2026-07-27: Persist Requested Scheduler Priority In Submission Manifest Before Exposing Admission Input

## Acceptance Criteria

- Fetch `origin/main` and confirm the local and remote branch share the expected base
  without absorbing unrelated remote changes.
- Run a fresh strict full build and read its complete test evidence before committing.
- Review the complete staged path set, whitespace, and bounded credential-pattern scan.
- Commit the completed implementation and synchronized project documents on local
  `main` with an ordinary commit.
- Push local `main` to `origin/main` without force; do not create a content-free merge
  commit when no topic branch exists.
- Record delivery history in its owning documents, commit that record separately, and
  push it without force.
- Freshly verify that local `HEAD`, `origin/main`, and the remote-advertised
  `refs/heads/main` are identical and that the working tree is clean.

## Out Of Scope

- Further feature behavior; public priority input; generated-request priority; release,
  tag, deployment, pull-request mutation, branch deletion, force push, history rewrite,
  destructive operation, permission expansion, or unrelated cleanup.

## Approval

The user explicitly requested commit, push, and merge for the completed work on
2026-07-27.

## Allowed Tools

- read-file
- write-docs
- verify
- git
- network

## Verification

Fresh `git fetch origin main` established local `main`, `origin/main`, and their
merge-base at `406ea06468b690262f3ae08d2f759f82be4e4f1f`, with divergence `0 0`.

Fresh strict `clean build --no-build-cache --warning-mode all --quiet` passed 589 tests
across 117 suites: 586 passed, three existing Windows symbolic-link
privilege-dependent setup cases skipped, and zero failures or errors occurred.

The complete staged diff covered 26 paths with 1,151 insertions and 135 deletions.
`git diff --cached --check` passed and the bounded credential-pattern scan found zero
matches. Commit `b6f75051b4fb1f2bc8bb1a574e5526afec4acc88` was pushed from local
`main` to `origin/main` without force. A fresh remote query returned the same commit for
`refs/heads/main`. The final delivery-record commit and remote identity check follow
this record.

## Next

After delivery, connect optional `NORMAL`/`EXPEDITED` input and effective-priority
output to the explicit `scheduler-submit` command in a separate task.
