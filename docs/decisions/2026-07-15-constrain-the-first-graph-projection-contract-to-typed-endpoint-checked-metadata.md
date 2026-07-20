# 2026-07-15: Constrain The First Graph Projection Contract To Typed Endpoint-Checked Metadata Keyed To One Snapshot

Status: Accepted Decision

Context:

- The Gate 6 scope requires graph projection contracts for Decision, Architecture, Dependency, Task, and Execution relationships, and its exit criteria require nodes and edges to retain source, freshness, version, and rebuild status.
- The Architecture defines Project Brain graphs as rebuildable projections that must never silently overwrite their authoritative sources.
- No projection producer, query, or persistence exists yet; the first consumer is the task-to-decision-to-code-to-test impact query in a later increment.
- An unconstrained stringly-typed graph would accept relationships that the impact query could not interpret and that no source could justify.

Decision:

- Add the projection contract under `com.enhancer.brain` with five node kinds (task, decision, component, artifact, execution) and six endpoint-checked edge kinds: `JUSTIFIED_BY` task-to-decision, `SUPERSEDES` decision-to-decision, `DEPENDS_ON` between components and artifacts, `MODIFIES` task-to-artifact, `VERIFIED_BY` artifact-to-artifact, and `RECORDED_AS` task-to-execution.
- Give every node and edge an immutable provenance of bounded source reference, optional lowercase SHA-256 source revision, and an explicit freshness state (`CURRENT`, `STALE`, `SOURCE_MISSING`) whose rebuild-required status is derived, not stored separately.
- Require a source revision for Current and Stale provenance and prohibit it for Source-Missing provenance, mirroring the Workspace observation digest rules.
- Key each `ProjectBrainGraph` to one valid source snapshot identity with an explicit projection time and a versioned projection identifier, so projections are traceable and rebuildable rather than free-floating.
- Enforce deterministic ordering, duplicate and self-loop rejection, unknown-endpoint rejection, and 4096-entry bounds in the contract itself.
- Defer projection producers, the impact query, traversal, persistence, rebuild execution, and confidence metadata to separate approved tasks.

Rationale:

Endpoint-kind checking makes the six relationships carry their meaning in the type system, so a later impact query can traverse task-to-decision-to-code-to-test chains without interpreting conventions. Deriving rebuild status from freshness avoids two fields that could contradict each other. Keying to the snapshot identity reuses the already-verified content-addressed boundary instead of inventing a second projection identity source.

Consequences:

- Producers must later justify every node and edge from repository documents, RunRecords, or snapshot observations through separate approved tasks.
- The five graph domains share one contract; a domain needing a new relationship must extend the edge taxonomy through a recorded decision rather than reusing a loosely-fitting kind.
- Graph identity intentionally does not exist yet; equality is structural, and a persisted graph identity would arrive with persistence work.
