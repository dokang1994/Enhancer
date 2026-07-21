# Session Close Prompt

Read and execute this prompt before ending a Codex work session.

## Required Steps

1. Check changed files.
2. Run relevant tests.
3. Append this increment's verification evidence to `docs/verification-log.md`. Write it once; never revise an earlier entry.
4. Update `PROJECT_STATE.md` only where current state, maturity, or a known limitation actually changed. Verification records do not go here.
5. Update `CURRENT_TASK.md`, including the next task.
6. Update `ARCHITECTURE.md` only if a boundary, component, or contract changed. Maturity statements do not go here.
7. Record a new accepted decision as a file under `docs/decisions/` opening with its exact heading as a level-1 title, and add the matching `### <heading>` entry with its `Status: Accepted Decision` line to the `DECISION_LOG.md` index. Both sides are required; the heading text is the decision's identity and must never change after acceptance.
8. Distill reusable knowledge:
   - Promote project-independent repeatable procedures to a validated Skill and synchronize `skills/INDEX.md`.
   - Promote repository-specific rationale or pitfalls to `DECISION_LOG.md` or an ADR.
   - Do not duplicate promoted knowledge in `SESSION_HANDOFF.md`.
9. Reduce `SESSION_HANDOFF.md` to what is true now and would otherwise be lost. Delete anything another document owns.
10. Update `CHANGELOG.md` when notable changes occurred.
11. Review the final diff.
12. Record a `STABLE` development-session checkpoint containing every currently changed
    artifact and the applicable evidence references. Do not clear it yet.
13. Commit, push, merge, release, or perform another external delivery action only when
    that action is explicitly authorized, recording checkpoint intent and outcome around
    each action.
14. Confirm the recorded lifecycle state is supported by fresh evidence and does not imply release.
15. After the intended final Git and external state is freshly verified, clear the stable
    checkpoint. If clearing fails or drift is reported, leave it intact and report why.

If a step has nothing to change, say so and move on. Restating an unchanged fact to make a step feel done is the failure these steps are written against.

## Final Report

Report:

- Changed files
- Verification commands and results
- Lifecycle state and checks not run
- Commit hash, if committed
- Remaining risks or next task
- Final checkpoint state, including why it was retained if it could not be cleared

## Constraint

Do not push unless the user explicitly asks.

## Handoff Requirement

The repository must be complete enough for a future AI session to recover without chat
history. That is a property of the documents as a set, not of any single file.

`SESSION_HANDOFF.md` carries only what no other document owns:

- the state of the working tree and branches right now
- work in flight and where it stopped
- instructions for the next agent that are not already repository rules

It MUST NOT restate completed work, current state, capability maturity, the next task,
accepted decisions, or verification commands. Those belong to `CHANGELOG.md` and git,
`PROJECT_STATE.md`, `CURRENT_TASK.md`, `DECISION_LOG.md`, and
`docs/verification-log.md` respectively. If the next agent could learn a fact by
reading its owning document, it does not belong in the handoff.
