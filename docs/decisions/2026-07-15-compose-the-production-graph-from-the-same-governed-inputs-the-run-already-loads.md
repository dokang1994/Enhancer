# 2026-07-15: Compose The Production Graph From The Same Governed Inputs The Run Already Loads

Status: Accepted Decision

Context:

- The decision projection and run-record observation are Contract Verified but have no production caller, and the CLI composes only the view.
- The run path already loads repository memory, collects the snapshot, and persists the RunRecord; the graph needs exactly those inputs plus prior run-record metadata.
- Reporting node identities or digests would leak repository structure into bounded diagnostics that existing rules keep content-free.

Decision:

- Construct the RunRecord store before snapshot collection so prior records are observed into the snapshot through `RunRecordMetadataCollector`.
- Extend the memory collector and the run-evidence producer with overloads for additional observations and additional evidence-backed nodes instead of adding parallel collection paths.
- After finalization, project accepted decisions from the same loaded memory, produce one graph keyed to the same snapshot, and answer the task impact query in process.
- Report only bounded counts (`graphNodes`, `graphEdges`, `graphDecisions`, `impactExecutions`); keep every existing output line, exit code, and replay behavior unchanged.
- Persist nothing new; graphs and impact answers remain derived, rebuildable outputs.

Rationale:

Composing from inputs the run already trusts keeps the graph an account of the governed run rather than a second collection path, and count-only reporting makes production composition observable without widening the diagnostic surface. The overloads preserve single enforcement points for snapshot and graph invariants.

Consequences:

- Snapshot identity now reflects prior run-record observations, so runs over the same tree with different run histories produce different snapshot identities by design.
- Graph production cost grows with stored record count; retention remains governed elsewhere.
- Decisions remain unlinked in impact answers until a task-to-decision reference grammar exists.
