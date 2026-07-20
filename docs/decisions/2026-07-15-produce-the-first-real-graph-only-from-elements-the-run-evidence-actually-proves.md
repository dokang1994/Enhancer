# 2026-07-15: Produce The First Real Graph Only From Elements The Run Evidence Actually Proves

Status: Accepted Decision

Context:

- The graph contract and impact query are Contract Verified only against contract-constructed graphs; no producer projects real repository evidence.
- The available real evidence is one Workspace snapshot (approved task revision plus observed repository documents with digests and explicit states) and one stored run record (task, verification, envelope digest, durable reference).
- A read-only Gate 5 run modifies nothing, no decision parser exists, and no test-to-code linkage evidence exists, so `MODIFIES`, `VERIFIED_BY`, `JUSTIFIED_BY`, `SUPERSEDES`, and `DEPENDS_ON` edges cannot yet be justified.
- Projecting unjustified edges would make the impact query report relationships no evidence supports, which is exactly what the graph model prohibits.

Decision:

- Add `RunEvidenceGraphProducer` under `com.enhancer.brain`, producing from one snapshot, one task-matched `ResolvedRunRecord`, and an explicit projection time.
- Project only what the evidence proves: one task node from the approved task revision, one artifact node per repository document/file observation, one execution node from the stored record, and one `RECORDED_AS` edge from task to execution.
- Map observation states to element freshness one-to-one: Available to Current, Stale to Stale, Unavailable to Source-Missing, carrying the observation digest exactly when present.
- Use the stored record's envelope SHA-256 and durable reference as the execution node's provenance.
- Key the graph to the snapshot identity and delegate all structural enforcement to `ProjectBrainGraph.project`.
- Skip non-repository observation kinds and emit no other node or edge kinds until later producers can justify them from parsed decisions, write operations, or test evidence.

Rationale:

A projection is trustworthy only if every element can be traced to evidence, so the first producer's value is precisely its refusal to invent. The one-to-one state-to-freshness mapping preserves the snapshot's explicit staleness semantics inside the graph, and reusing the stored envelope digest gives executions the same content-addressed provenance discipline as documents.

Consequences:

- Impact answers over produced graphs currently return executions and observed artifacts only; decisions, modified artifacts, and verifying tests stay empty until their producers exist.
- Later producers extend the same graph rather than replacing it; each new edge kind requires its own evidence source and decision.
- Non-repository observations (Git status, diagnostics, terminal) remain unprojected until their adapters and node semantics are decided.
