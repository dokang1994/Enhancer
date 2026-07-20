# 2026-07-15: Start Gate 6 With A Metadata-Only Content-Addressed Workspace Snapshot

Status: Accepted Decision

Context:

- Gate 6 owns the immutable common input snapshot and approved task revision that later Project Brain, messaging, and worker handoffs require.
- Repository files, Git state, diagnostics, selection, terminal-session metadata, and RunRecords have different adapters and permissions that are not yet implemented.
- Capturing source content in the foundational contract would expand sensitive-data and memory boundaries before an actual consumer or adapter exists.

Decision:

- Begin Gate 6 with provider-neutral immutable metadata contracts under `com.enhancer.workspace`.
- Represent approved work with task identity, source-document identity, and a lowercase SHA-256 source revision; this is provenance and not Tool authority.
- Represent Workspace inputs as typed observations with source kind, source identity, adapter provenance, observation time, optional source-update time, explicit Available/Stale/Unavailable state, optional SHA-256 content identity, and bounded diagnostic reason.
- Store no source payload in the first snapshot contract.
- Compute a canonical SHA-256 snapshot identity in production from the normalized project root, capture time, task revision, and deterministically ordered observation metadata.
- Reject duplicate sources, inconsistent availability/digest combinations, invalid temporal relationships, excessive item counts, and unbounded strings.
- Name the next Gate 6 Project Brain aggregate as the immediate integration consumer and Gate 7 message envelopes as the next-gate identity consumer.
- Keep Gate 6 `Specified - Next` while this sub-capability advances only to Contract Verified.

Rationale:

A content-addressed metadata boundary gives every later adapter and worker one stable snapshot identity without prematurely retaining source payloads or creating command authority. Explicit unavailable and stale states prevent absence from being confused with freshness, while deterministic ordering keeps identity independent of caller collection order.

Consequences:

- Source adapters must later provide bounded metadata and digests through separate approved tasks.
- Snapshot equality and identity do not prove that source content is safe, trusted, or authorized; provenance and repository rules remain authoritative.
- A future ProjectBrainView must consume this contract before the Workspace capability can be called Integrated.
