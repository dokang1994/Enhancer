# Current Task

## Status

In Progress

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

Pending fresh delivery verification.

## Next

After delivery, connect optional `NORMAL`/`EXPEDITED` input and effective-priority
output to the explicit `scheduler-submit` command in a separate task.
