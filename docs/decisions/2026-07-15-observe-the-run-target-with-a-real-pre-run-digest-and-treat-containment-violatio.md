# 2026-07-15: Observe The Run Target With A Real Pre-Run Digest And Treat Containment Violations As Errors

Status: Accepted Decision

Context:

- Snapshots observed only canonical documents and prior run records, so they could not explain which file the run was about.
- The externally supplied expected digest is a claim, not an observation, and must not masquerade as observed provenance.
- The Context Reader precedent already accepts bounded, containment-checked direct reads as normal governed infrastructure.

Decision:

- Add `TargetFileMetadataCollector`: one pre-run streamed SHA-256 of the relative target under real-path containment, emitted as a `REPOSITORY_FILE` observation with `target-file-reader` provenance; content is never retained.
- Observe a missing or over-64-MiB target as explicit `UNAVAILABLE` with a bounded reason, preserving the run's own failure semantics.
- Reject absolute, traversal, escaping, and non-regular targets as configuration errors surfaced by the CLI as usage errors before execution; violations are never observations.
- Include the observation in the CLI-collected snapshot so the target appears as an `ARTIFACT` node through the existing producer.

Rationale:

A snapshot that explains a run must observe the run's subject with evidence of its own, and a real digest is the only observation that can later expose divergence from the externally claimed expectation. Keeping containment violations as errors preserves the distinction between an unhealthy source and an illegal request.

Consequences:

- The target is read twice per run (observation and Tool execution); both reads are bounded and read-only.
- Snapshot identity now reflects the target's pre-run content revision.
- Divergence between the observed digest and the verified expectation is visible across the snapshot and the RunRecord.
