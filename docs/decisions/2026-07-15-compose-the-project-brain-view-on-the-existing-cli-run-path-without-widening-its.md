# 2026-07-15: Compose The Project Brain View On The Existing CLI Run Path Without Widening Its Surface

Status: Accepted Decision

Context:

- The repository-memory path is Integrated in a temporary-project test, but no production caller composes the `ProjectBrainView` during an actual governed run.
- The `EnhancerCli` `run` command already loads the full `ProjectContext` to derive and validate the approved task, then discards it.
- Reloading memory after the run would create a second read of the same sources and could disagree with the memory that actually informed approval.
- Persisting the snapshot identity would change the RunRecord schema, which carries its own compatibility and replay obligations.

Decision:

- On the `run` path, keep the already-loaded `ProjectContext`, collect the `WorkspaceSnapshot` through `RepositoryMemorySnapshotCollector` with a capture time taken before worker execution, and compose the `ProjectBrainView` after finalization with the persisted `RunRecord`.
- Compose for every run outcome that produces a record, since the view explains what informed the run regardless of how the run stopped.
- Report only bounded metadata in the existing output: the snapshot identity, the observation count, and a matched/diverged/notObserved freshness summary; never document content, digest lists, or evidence.
- Keep exit codes, existing output lines, persist-before-report ordering, replay behavior, and the output bound unchanged, and add no command, argument, or authority.
- Defer persisting the snapshot identity in the RunRecord to the Gate 7 envelope work that already owns cross-handoff identity.

Rationale:

Composing from the memory that produced the approved task makes the reported snapshot describe exactly the inputs the run was approved against, at no additional authority or read cost. Reporting identity and freshness counts keeps the bounded-diagnostics contract intact while making the composition externally observable, which is what an Operational claim needs.

Consequences:

- The reported freshness is trivially all-matched unless repository documents change between context load and composition, because the same in-memory context is both the snapshot source and the comparison input; real divergence reporting arrives when snapshots persist or sources are re-observed.
- Replay does not reproduce the snapshot identity because the RunRecord does not store it; that linkage is explicitly deferred.
- A composition failure after the record is persisted surfaces as an internal error while the durable record remains replayable.
