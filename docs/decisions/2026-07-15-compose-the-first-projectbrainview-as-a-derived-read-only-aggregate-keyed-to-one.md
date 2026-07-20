# 2026-07-15: Compose The First ProjectBrainView As A Derived Read-Only Aggregate Keyed To One Snapshot

Status: Accepted Decision

Context:

- The Gate 6 `WorkspaceSnapshot` contract is Contract Verified but has no consumer, so Gate 6 cannot claim Integrated maturity.
- Architecture defines Project Brain as the reasoning-facing aggregate of repository memory, Workspace snapshots, decisions, and RunRecords that preserves source identity and freshness.
- The Context Reader already produces `ProjectContext` repository memory with document content, and the RunRecord Store already produces verified `RunRecord` run history.
- Carrying document content, Tool payloads, or evidence bodies into the aggregate would widen the sensitive-data boundary that the snapshot contract deliberately closed.

Decision:

- Add the first Project Brain aggregate as `ProjectBrainView` under a new provider-neutral `com.enhancer.brain` package that depends on `com.enhancer.workspace`, `com.enhancer.context`, and `com.enhancer.run`.
- Compose the view from exactly one `WorkspaceSnapshot`, one `ProjectContext`, and one `RunRecord`; the view derives its content and never collects sources itself.
- Key the view to the snapshot's canonical identity rather than recomputing a second identity.
- Project repository memory to document path, read order, and a computed lowercase SHA-256 of the document content; retain no document content in the aggregate.
- Derive explicit repository-memory freshness by comparing each document digest against the snapshot's `REPOSITORY_DOCUMENT` observation with the same source identity: equal digests are `SNAPSHOT_MATCHED`, differing digests are `SNAPSHOT_DIVERGED`, and an unobserved document is `NOT_OBSERVED`.
- Require the RunRecord's approved task identity and source document to equal the snapshot's `ApprovedTaskRevision`; reject a mismatched or unrelated run instead of silently aggregating it.
- Project the RunRecord to metadata-only provenance of logical run identity, record time, approved task identity, and verification status; exclude Tool requests, results, evidence bodies, and chat history.
- Expose the snapshot's Available, Stale, and Unavailable observations unchanged rather than collapsing or defaulting them.
- Add no persistence, graph projection, source adapter, or Tool authority in this increment.

Rationale:

Deriving the aggregate from existing verified inputs proves the snapshot contract is consumable without inventing a second collection path or a competing identity. Comparing loaded repository memory against the snapshot's observed digests turns divergence into an explicit, inspectable state rather than an unnoticed inconsistency, which is what the Gate 6 exit criterion about explaining a run actually requires. Requiring the run and snapshot to name the same approved task keeps provenance honest, because an aggregate that mixes unrelated work would misattribute the evidence it presents.

Consequences:

- The view is only as complete as the snapshot it is given; a snapshot without observations yields `NOT_OBSERVED` memory freshness rather than an error.
- Repository-memory digests are computed from already-loaded `ProjectContext` content, so this increment adds no new filesystem access or Tool authority.
- Freshness derivation is metadata comparison only; it does not prove that either revision is correct, authorized, or safe.
- Later graph projections and source adapters must extend this aggregate through separate approved tasks.
- Gate 6 may claim Integrated maturity only if fresh evidence shows the real snapshot, Context, and RunRecord contracts connected through this view.
