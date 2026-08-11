# User request on 2026-08-11 to commit, push, and merge the authenticated cancellation work to main

Status: Accepted Decision

## Context

The working tree holds the completed and freshly verified authenticated-cancellation
application surface, detached signed exact-request grant architecture, reusable public-
trust verifier, deterministic authorization audit store, and audit-backed authorizer.
Local `main` and the cached `origin/main` currently resolve to the same baseline. The
user has now explicitly requested commit, push, and merge to `main`.

## Decision

Deliver the complete reviewed working-tree candidate through an ordinary non-amending
commit and a non-force push to `origin/main`. Because the work is already based directly
on aligned `main`, a successful linear push is the merge result; no synthetic branch or
merge commit is created. Append delivery evidence once afterwards and publish it in a
small follow-up evidence commit.

## Rationale

The application, authentication core, tests, accepted decisions, and owning documents
form one coherent cancellation boundary. The repository's established direct-main
delivery pattern preserves the exact reviewed history and avoids an empty or synthetic
merge while retaining separately reviewable delivery evidence.

## Consequences

This authorization covers delivery-task and decision synchronization, correction of a
stale compact architecture mirror statement inside the same candidate, fresh fetch and
verification, exact-path staging, ordinary non-amending commits on `main`, non-force
pushes to `origin/main`, direct-main integration verification, and the follow-up
delivery-evidence commit. It does not authorize force push, amend, rebase, reset,
history rewrite, tag, release, deployment, pull-request or issue mutation, branch
deletion, destructive cleanup, credential or permission change, paid service, external
message, or unrelated implementation.
