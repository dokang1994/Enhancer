# 2026-07-14: Complete Agent Runs Only Through External Evidence Verification And Durable Records

Status: Accepted Decision

Context:

- Gate 3 intentionally stops successful Tool output at `AWAITING_VERIFICATION` and does not preserve an externally inspectable completion record.
- A worker summary, exit status, or self-reported success cannot independently prove that the expected result was produced.
- Truncated evidence must be resolved and integrity-checked before completion, while missing evidence must remain distinguishable from proven mismatch or corruption.
- Failed, stagnated, and iteration-limited runs also need durable diagnostic history even though they never enter verification.

Decision:

- Introduce typed Verification statuses: Verified, Rejected, Unverified, and Not Performed, each constrained by a structured reason code.
- Bind every `VerificationRequest` to the approved task, executed Tool request, Tool result, and an expected SHA-256 content digest supplied outside the worker.
- Implement the first `IndependentVerifier` as a deterministic read-file verifier that recomputes complete-content identity and resolves truncated evidence through the existing `EvidenceStore`.
- Preserve the executed request in terminal Agent state instead of reconstructing it from progress hashes or diagnostic prose.
- Permit only the sequential finalization boundary to create `COMPLETED`, and only after a Verified decision.
- Persist a typed RunRecord before returning completed finalization. The record includes inputs, policy snapshot and decision, Tool result and evidence, verification, iterations, and worker/final stop reasons.
- Store RunRecords as versioned binary payloads in atomically published SHA-256 envelopes and support restart-safe replay.
- Record worker failure, stagnation, and iteration exhaustion with verification Not Performed rather than fabricating a verification attempt.

Rationale:

This is the smallest provider-neutral boundary that turns worker output into independently checked, replayable execution history. Digest comparison avoids trusting prose, reuse of `EvidenceStore` keeps complete-output integrity in one place, and persist-before-return prevents an in-memory completion from being reported without durable audit evidence.

Consequences:

- Missing evidence is Unverified; corrupted or mismatched evidence is Rejected; neither can complete a run.
- A RunRecord persistence failure prevents the finalizer from returning completion.
- The initial RunRecord format is local, bounded, versioned, and integrity-checked but not encrypted, signed, remotely replicated, or automatically deleted.
- Gate 5 can consume the finalizer and RunRecord reference through a supported CLI without changing verification authority.
- LLM verification, human review adapters, parallel reviewers, Git mutation, and distributed storage remain future work.
