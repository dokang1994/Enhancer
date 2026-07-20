# 2026-07-15: Collect The First Real Workspace Snapshot From Already-Loaded Repository Memory

Status: Accepted Decision

Context:

- The `WorkspaceSnapshot` and `ProjectBrainView` contracts are Contract Verified, but every snapshot so far was hand-written by tests, so no real source informs the aggregate.
- The two named paths toward Gate 6 integration are an explicit source adapter or a production composition path; the adapter is the smaller increment.
- The Context Reader already loads bounded, containment-checked, UTF-8-validated repository documents, and the Gate 0 lifecycle test already produces a real persisted RunRecord in a governed temporary project.
- Giving the first collector its own filesystem access would duplicate the Context Reader's containment and bounds enforcement and widen the authority surface without need.

Decision:

- Add a read-only `RepositoryMemorySnapshotCollector` under `com.enhancer.workspace` that derives a snapshot from a project root, an explicit caller-supplied capture time, an `ApprovedTask`, and an already-loaded `ProjectContext`.
- Derive the `ApprovedTaskRevision` inside the collector by digesting the approved task's source document content out of the same loaded memory, so the revision provably describes the memory that was actually read; reject a memory without that document.
- Emit one `AVAILABLE` `REPOSITORY_DOCUMENT` observation per loaded document with the document path as source identity, `context-reader` provenance, and a computed lowercase SHA-256 content digest; retain no content.
- Reuse `WorkspaceSnapshot.capture` for ordering, duplicates, bounds, and canonical identity instead of adding a second identity computation.
- Take the capture time as an explicit parameter rather than reading a clock inside the collector, keeping collection deterministic and testable.
- Prove the end-to-end path in an integration test that combines a real governed CLI run, the real Context Reader, the collector, and the view, including a divergence check after the source document changes.
- Defer Git, diagnostics, selection, terminal, and RunRecord-source adapters, and any production composition path, to separate approved tasks.

Rationale:

Deriving observations from memory the Context Reader already loaded keeps the collector free of filesystem authority and reuses the hardened containment path instead of duplicating it. Deriving the task revision from the same memory closes the gap where a caller could claim a revision digest unrelated to what was actually read. An explicit capture time avoids a hidden clock dependency, which keeps snapshots reproducible in tests and later replayable.

Consequences:

- The collector observes only what the Context Reader loads; unfetched sources are absent from the snapshot rather than marked `STALE` or `UNAVAILABLE`, and those states first appear with real per-source adapters.
- Observation time equals capture time for every document because loading time is not tracked per document in `ProjectContext`.
- The end-to-end evidence integrates the repository-memory path only; Gate 6 stays `Specified - Next` until its remaining scope exists.
