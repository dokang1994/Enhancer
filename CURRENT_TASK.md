# Current Task

## Status

In Progress

## Task

Deliver deterministic RunRecord and bounded lost-acknowledgement recovery increments
through a reviewed commit, remote feature branch, and main.

## Task ID

deliver-gate-8-deterministic-runrecord-and-recovery-fixtures

## Context

The working tree contains the deterministic Goal/AgentRun-bound RunRecord recovery
implementation, its accepted decision and architecture/state synchronization, plus
worker-level lease-expiry and disposition-before-checkpoint-clear recovery fixtures.
Fresh strict verification passed before delivery. The repository is currently on local
`main`, which matched the last observed `origin/main`.

## Justified By

- 2026-07-27: Bind Process-Isolated RunRecords To Deterministic AgentRun Identities Before Result Publication
- 2026-07-24: Assess Gate 8 Maturity Against Every Exit Criterion
- 2026-07-21: Select The Process-Isolated Durable Worker And Retire Spools After Checkpoint

## Acceptance Criteria

- Fetch and verify the current remote base before changing Git history.
- Review exactly the intended source, tests, accepted decision, and owning documents;
  exclude generated output, secrets, and unrelated changes.
- Run a fresh strict build and read its result before committing.
- Create a dedicated delivery branch, commit the intended increment, and push it
  without force.
- Integrate the delivery commit into local `main` without rebasing or rewriting
  history, push `main` without force, and verify fetched remote references.
- Append delivery evidence and synchronize the final task/handoff state in a closeout
  commit on `main`, then verify local and remote `main` match.
- Keep the development-session checkpoint active through every external delivery step
  and clear it only after the final intended Git state is verified.

## Out Of Scope

- Rebase, force push, hard reset, squash, tag, release, deployment, pull-request
  mutation, branch deletion, or unrelated upstream changes.
- Additional production behavior, persistence schema, CLI, authority, or maturity
  changes.

## Approval

The user explicitly requested commit, push, and merge to the main branch. This grants
those external delivery authorities for this exact working-tree increment.

## Allowed Tools

- read-file
- verify
- git

## Verification

Pending fresh delivery verification.

## Next

After delivery, select the next bounded task from the broader Gate 8 and later-gate
gaps; do not extend this delivery increment.
