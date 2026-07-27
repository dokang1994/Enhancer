# 2026-07-27: Deliver Submission Manifest Schema V2 Priority Migration Directly To Main

Status: Accepted Decision

## Context

The submission manifest schema-v2 priority persistence, exact durable admission
propagation, explicit schema-v1 migration, bounded migration CLI, tests, and owning
documents are completed and freshly verified in the working tree. The user explicitly
authorized commit, push, and merge for this completed work.

The working tree is already on local `main`, which started this delivery from the same
commit as `origin/main`. There is no separate topic branch whose history needs a merge
commit.

## Decision

After a fresh remote-base check, strict build, and staged-diff review, deliver the
completed increment directly from local `main` to `origin/main` using ordinary commits
and non-forced pushes. Do not manufacture an empty or content-free merge commit.

Keep the repository checkpoint through every authorized external delivery step.
Delivery is complete only after local `main`, the `origin/main` tracking reference, and
the remote-advertised `refs/heads/main` identity match and the working tree is clean.

## Rationale

Directly advancing `main` preserves the actual branch topology. Creating an artificial
merge commit when all completed changes already reside in `main` would add no
integration evidence and would misrepresent how the work was developed.

## Consequences

- The completed manifest schema-v2 priority and migration increment may be committed
  and pushed to `origin/main`.
- A second ordinary commit may record the verified delivery result in the owning
  delivery-history documents.
- Force push, history rewrite, branch deletion, release packaging, deployment, and
  unrelated changes remain unauthorized.
- Remote delivery claims require a fresh remote reference query.
