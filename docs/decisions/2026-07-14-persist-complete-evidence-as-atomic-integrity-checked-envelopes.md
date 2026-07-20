# 2026-07-14: Persist Complete Evidence As Atomic Integrity-Checked Envelopes

Status: Accepted Decision

Context:

- `VerificationEvidence` requires a complete-output reference when its 4096-character tail is truncated, but no current component makes that reference real.
- Gate 2 must provide durable resolution and corruption detection without broadening Tool authority or fabricating references.
- Separate payload and metadata files would make atomic publication across both files difficult in the first filesystem implementation.
- Automatic retention cleanup would delete data and is not required to prove the Gate 2 exit criteria.

Decision:

- Add an `EvidenceStore` boundary with a filesystem implementation that generates UUID run and evidence identities.
- Store metadata and UTF-8 payload together in one versioned binary envelope.
- Publish evidence by atomic move from a temporary file in the final run directory; do not fall back to a non-atomic move.
- Record creation time, UTF-8 byte length, and SHA-256 digest and validate all of them during resolution.
- Use opaque `evidence/<run-id>/<evidence-id>` references and reject malformed, missing, oversized, or corrupted artifacts.
- Add an explicit maximum-content and retention-duration policy, but perform no automatic cleanup in Gate 2.
- Connect a persistence-enabled `ReadFileTool` through `EvidenceRecorder`; the request correlation identity is a run identity previously created by the store.

Rationale:

A single atomic envelope is the smallest durable format that keeps reference metadata and content consistent. Digest, length, strict decoding, bounded reads, and reference containment make evidence failures observable before later verification consumes them.

Consequences:

- Truncated Tool output can carry a real reference that resolves after the Tool call.
- Short output remains in memory and does not create unnecessary evidence artifacts.
- Gate 2 detects accidental or unauthorized artifact modification but does not provide encryption, signatures, or external tamper-proof storage.
- Evidence deletion, compaction, migration, distributed storage, Agent Loop integration, independent verification, and RunRecord remain deferred.
