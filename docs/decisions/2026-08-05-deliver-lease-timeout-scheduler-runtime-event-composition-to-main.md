# User request on 2026-08-05 to commit, push, and merge the completed lease-timeout Scheduler composition to main

Status: Accepted Decision

## Context

The lease-timeout Scheduler runtime-event composition is completed, freshly verified,
and synchronized as one intentional working-tree candidate. The user explicitly
requested commit, push, and merge to `main`.

The candidate currently resides directly on local `main`, whose `HEAD`, local branch,
and last fetched `origin/main` were aligned before delivery-task activation. A temporary
feature branch and synthetic merge commit would add history without creating a distinct
integration boundary. Current remote state must still be fetched and compared before
staging.

## Decision

- Authorize ordinary non-amending commits on the current `main` branch and non-force
  pushes to `origin/main` for the exact reviewed lease-timeout composition and its
  delivery-evidence synchronization.
- Treat a direct commit and successful fast-forward push from aligned local `main` as
  the requested merge result. Create no synthetic merge commit when no distinct branch
  exists.
- Fetch and compare local and remote references before staging. Remote divergence,
  non-fast-forward refusal, unexpected paths, failed verification, or artifact drift
  stops delivery for reconciliation instead of widening authority.
- Keep the repository checkpoint active through review, verification, staging, commit,
  push, evidence synchronization, final remote verification, and clean-tree
  confirmation.

## Consequences

The completed lease-timeout composition may be delivered with one truthful
implementation commit and one small follow-up commit carrying external delivery
evidence. This authority does not permit force push, amend, rebase, reset, history
rewrite, tag, release, deployment, pull-request or issue mutation, branch deletion,
credential or permission changes, destructive cleanup, or unrelated implementation.
