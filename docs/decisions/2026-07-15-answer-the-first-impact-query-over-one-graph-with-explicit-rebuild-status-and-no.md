# 2026-07-15: Answer The First Impact Query Over One Graph With Explicit Rebuild Status And No Transitive Closure

Status: Accepted Decision

Context:

- The graph projection contract is Contract Verified and the roadmap names the task-to-decision-to-code-to-test impact query as its first consumer.
- No producer projects real graphs yet, so the query can only be proven against contract-constructed projections.
- Transitive dependency propagation over `DEPENDS_ON` would answer a broader "what else is affected" question, but its semantics (direction, depth, cycles) deserve their own decision once real dependency projections exist.
- A query answer detached from its graph's snapshot identity could not say when it must be recomputed.

Decision:

- Add `TaskImpactQuery` and the immutable `TaskImpact` result under `com.enhancer.brain`, answering over exactly one `ProjectBrainGraph`.
- Traverse only the named chain from the queried task: `JUSTIFIED_BY` to decisions, `MODIFIES` to artifacts, `VERIFIED_BY` from those modified artifacts to verifying artifacts, and `RECORDED_AS` to executions.
- Carry the graph's source snapshot identity in the result and derive one rebuild-required status that is true exactly when the task node, any traversed edge, or any returned node requires rebuild.
- Keep result ordering derived from the graph's canonical ordering, deduplicate shared verifying artifacts, return empty collections for an edgeless task, and reject unknown or non-task identities.
- Defer transitive `DEPENDS_ON` closure, multi-graph answering, producers, persistence, caching, and indexing to separate approved tasks.

Rationale:

The named chain is the exact question the roadmap commits to answering first, and answering it over one snapshot-keyed graph keeps the result reproducible: the same graph always yields the same answer, and the snapshot identity plus rebuild status say precisely when that answer stops being trustworthy. Deriving one aggregate rebuild flag from element provenance keeps staleness visible without inventing a second freshness model.

Consequences:

- The query is only as complete as the projected graph; missing projections yield empty results rather than inferred impact.
- Impact through dependency chains is not yet visible; that closure arrives with real dependency projections and its own decision.
- Producers must later construct graphs from repository evidence before the query answers anything about the actual project.
