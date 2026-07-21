# 2026-07-21: Persist Development Session Checkpoints Outside Canonical Project Documents

Status: Accepted Decision

Context:

- `SESSION_HANDOFF.md` is written during orderly session close and therefore cannot be
  the correctness boundary for forced termination, token exhaustion, or process loss.
- `CURRENT_TASK.md` owns approved work and lifecycle state, Git owns the actual diff,
  and `docs/verification-log.md` owns promoted verification evidence. Repeating those
  facts in a handoff or checkpoint would violate the one-owner document rule.
- A resumed Agent can reconstruct partial work from canonical documents and the working
  tree, but without a durable execution position it must infer the last successful
  atomic step and whether the next action is a retry.
- `.enhancer/` is already excluded from Git and is the repository-local runtime-artifact
  boundary. It can retain resumable execution state without becoming canonical project
  documentation or delivery history.

Decision:

- Add one machine-written development-session checkpoint below
  `.enhancer/session-checkpoint/`. It is a runtime artifact, not a canonical document,
  and is never loaded by `ProjectContextReader`.
- Bind every checkpoint to one generated run identity, the active `CURRENT_TASK.md`
  task identity, and a SHA-256 revision of the task contract sections. Status,
  verification, and next-task sections are excluded so ordinary lifecycle promotion
  does not rewrite the approved scope identity.
- Persist a monotonic checkpoint revision, typed execution state, current step, last
  successful step, next executable action, bounded evidence references, and a bounded
  manifest of project-relative artifact paths with present/missing state and content
  digests. The checkpoint references canonical facts; it does not copy task prose,
  maturity, decisions, or verification results.
- Publish each replacement as a bounded, strict-UTF-8, integrity-checked atomic
  filesystem artifact before reporting success. A corrupt, oversized, stale-revision,
  different-run, different-task, or changed-task-contract update fails closed.
- Expose start, record, show, and clear commands through the existing local CLI. Start
  refuses to overwrite an existing checkpoint. Record uses the expected revision as a
  single-writer fence. Show reports the durable execution position. Clear is allowed
  only from a stable checkpoint whose recorded artifact manifest still matches the
  working tree.
- Update session start, implementation, resume, and close instructions so intent is
  checkpointed before a mutating or verification step, outcome is checkpointed after
  it, and orderly close clears the checkpoint only after verification and document
  synchronization. `SESSION_HANDOFF.md` remains limited to facts that no other source
  owns.

Rationale:

Correctness cannot depend on code that runs only when a session ends normally. An
atomic current checkpoint makes forced termination equivalent to losing at most the
in-flight step, while the expected revision prevents two sessions from silently
overwriting each other. Task-contract and artifact digests make drift visible without
turning runtime state into another project document.

Consequences:

- A new session can identify the last successful step and the intended retry before it
  edits the repository.
- The working tree remains the source of actual changes and fresh tests remain required;
  a checkpoint is recovery metadata, not completion or verification evidence.
- The first increment supports one active development session per repository. It does
  not implement background timers, automatic platform shutdown hooks, multi-session
  merging, WIP commits, remote replication, or external-effect deduplication.
- Gate 8 product-runtime checkpoints remain separate. This boundary governs development
  of the Enhancer repository itself and does not promote the Agent Runtime maturity.
