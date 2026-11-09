# Current Task

## Status

In Progress

## Task

Deliver the already verified RFC-0034 specification commits from the current local
`main` branch to `origin/main`, performing a normal merge only if a fresh remote query
shows that one is actually required.

## Task ID

deliver-rfc0034-specification-to-main

## Context

The RFC-0034 specification task is Completed and committed locally as `5fdc694` and
`5a8a4cf`. The working tree was clean, `HEAD` was already on `main`, and the last local
tracking state showed `main` ahead of `origin/main`. Remote state must be fetched before
delivery, so a separate branch-to-main merge may be unnecessary.

## Justified By

- User request on 2026-11-09 to commit, push, and complete the main-branch merge

## Approval

The user's request authorizes the repository-local task document and verification-log
updates needed to govern delivery, ordinary local commits for those verified document
increments, a fresh fetch of `origin`, a normal non-force merge into local `main` only
if fresh remote state requires it, and pushes of the verified local `main` commits to
`origin/main`.

It does not authorize force push, history rewrite, deletion, reset, rebase, tag,
release, deployment, branch deletion, pull-request mutation, unrelated remote changes,
provider or network runtime integration, paid service use, or RFC-0034 Java
implementation.

## Acceptance Criteria

- A fresh fetch establishes the authoritative `origin/main` relationship before any
  merge or push.
- Delivery begins from a clean local `main` containing the verified RFC-0034 commits.
- If `origin/main` is already an ancestor of local `main`, no unnecessary merge commit
  is created; otherwise only a normal non-force merge authorized by this task is used.
- The README-owned Java 17 regression is fresh after this Markdown task activation and
  before external delivery.
- The final push is non-force, a fresh remote query proves `origin/main` resolves to the
  delivered local `HEAD`, Git is clean, and delivery evidence is recorded once.
- The checkpoint remains active through every external delivery step and is cleared
  only after the repository is synchronized, verified, committed, and remotely
  confirmed.

## Out Of Scope

Java production or test implementation, RFC contract change, Architecture, Project
State, Roadmap or capability-maturity change, force push, history rewrite, reset,
rebase, destructive cleanup, tag, release, deployment, branch deletion, pull-request
mutation, unrelated remote change, provider, credential, runtime network integration,
spend, execution, persistence, runtime, MCP, or another external effect.

## Allowed Tools

- read-file
- write-docs
- build-output
- verify
- checkpoint
- git-inspect
- git-fetch
- git-merge
- git-stage
- git-commit
- git-push

## Verification

Run focused current-task/document-ownership/planner governance tests and
`git diff --check`, then the complete README-owned Java 17 test task. Inspect generated
JUnit evidence, fetch `origin`, inspect the exact merge-base and ahead/behind counts,
perform no merge when local `main` already contains `origin/main`, push without force,
and verify the remote `refs/heads/main` object ID equals local `HEAD`.

## Dynamic Workflow

Workflow ID: deliver-rfc0034-specification-to-main
Mode: Sequential
Increment Limit: 2
Selection Rule: Select the first dependency-ready Pending increment in numeric order.
Stop Conditions: Stop on failed verification, authority conflict, task/checkpoint drift,
remote divergence requiring conflict resolution, force-push requirement, unexpected
remote identity, scope expansion, exhausted workflow bounds, or unsafe recovery.

### Increment 1 - prepare-rfc0034-main-delivery

State: Completed
Depends On: none
Scope: Activate and verify the bounded delivery task, run focused and full regression,
inspect generated evidence, and commit the verified delivery contract locally.
Exit Criteria: The task document satisfies governance and ownership checks, the full
Markdown-sensitive regression is GREEN, and the delivery contract is committed on a
clean local `main`.
Verification: Focused governance tests, full README-owned Java 17 regression, generated
JUnit evidence inspection, `git diff --check`, and local Git status/log review.
Next Action: Fetch, reconcile and deliver local `main` to `origin/main`.

### Increment 2 - deliver-rfc0034-to-origin-main

State: In Progress
Depends On: prepare-rfc0034-main-delivery
Scope: Fetch and reconcile `origin/main`, preserve a clean non-force history, push
local `main`, confirm remote identity, record evidence, and close the delivery
checkpoint.
Exit Criteria: All delivery commits are on `origin/main`, remote and local `main`
resolve to the same object ID, Git is clean, and the stable checkpoint is cleared.
Verification: Merge-base/ahead-behind inspection, non-force push output, remote-ref
confirmation, and final Git status/log review.
Next Action: Await separate authority for RED-first RFC-0034 implementation.

## Next

After delivery is remotely verified, mark this task Completed, record delivery evidence
without duplicating it into architecture or maturity documents, commit and push the
closure, then await separate authority for RED-first RFC-0034 implementation.
