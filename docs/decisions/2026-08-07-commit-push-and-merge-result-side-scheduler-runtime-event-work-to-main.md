# User request on 2026-08-07 to commit, push, and merge the completed Result-side Scheduler runtime-event work to main

Status: Accepted Decision

## Context

The working tree holds one completed and freshly verified Gate 8 increment that composes
Result-side runtime-event publication across `scheduler-cycle`, `scheduler-drain`, and
`scheduler-service`. Local `main` and the cached `origin/main` currently resolve to the
same baseline. The user has now explicitly requested commit, push, and merge to `main`.

## Decision

Deliver the complete reviewed working-tree candidate through an ordinary non-amending
commit and a non-force push to `origin/main`. Because the work is already based directly
on aligned `main`, a successful linear push is the merge result; no synthetic branch or
merge commit is created. Append delivery evidence once afterwards and publish it in a
small follow-up evidence commit.

## Rationale

The increment forms one coherent Result-side publication and recovery candidate, its
owning documents and accepted decisions are synchronized, and its implementation is
covered by focused and full Java 17 verification. The repository's established
direct-main delivery pattern preserves the exact reviewed history without an empty or
synthetic merge.

## Consequences

This authorization covers delivery-task and decision synchronization, fresh fetch and
verification, exact-path staging, ordinary non-amending commits on `main`, non-force
pushes to `origin/main`, direct-main integration verification, and the follow-up
delivery-evidence commit. It does not authorize force push, amend, rebase, reset,
history rewrite, tag, release, deployment, pull-request or issue mutation, branch
deletion, destructive cleanup, credential or permission change, paid service, external
message, or unrelated implementation.
