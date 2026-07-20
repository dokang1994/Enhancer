# 2026-07-16: Bound Workspace RunRecord Observation To A Recent Window

Status: Accepted Decision

Context:

- `RunRecordMetadataCollector` resolves every durable record into every new Workspace snapshot. Since `WorkspaceSnapshot` is capped at 4096 observations, the 4,080th run with the current fixed sources cannot even begin.
- Resolving all prior record payloads on every run also creates cumulative quadratic payload I/O, while deletion is neither authorized nor required for observation.
- Full reference listing and point replay remain useful storage capabilities and need not be constrained by the Workspace projection window.

Decision:

- Add `RunRecordStore.recentReferences(limit)` as a bounded newest-first metadata query and retain `references()` for complete diagnostic/replay enumeration.
- Limit `RunRecordMetadataCollector` to the 256 most recent record references. Resolve only those payloads, preserving explicit unavailable observations for selected corrupted or missing entries.
- Implement filesystem recency selection with one directory scan and a bounded priority queue over no-follow file modification metadata; do not load every RunRecord payload or delete any artifact.
- Validate requested reference-window bounds from 1 through the store collection limit and keep output deterministic with the reference as the tie-breaker.

Rationale:

A fixed observation window keeps snapshot cardinality and per-run payload resolution bounded without inventing destructive retention. The complete store remains available for direct replay and future pagination/index work, while Workspace answers honestly describe a recent execution horizon.

Consequences:

- New runs no longer hit the snapshot's 4096-observation wall because of accumulated records, and record payload reads are capped at 256 per collection.
- Filesystem directory enumeration is still linear in artifact count; a durable summary index or pagination may replace it when scale justifies the additional consistency protocol.
- File modification time determines filesystem recency selection; envelope integrity and stored time are still validated when a selected reference is resolved.
