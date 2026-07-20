# 2026-07-20: Record Caller-Supplied WorkPayload Execution Input Enabling Arbitrary-Target Worker Execution

Status: Accepted Decision

Context:

- The AgentLoop-backed execution port derives its target and expected digest from the approved task revision behind one private seam, bounding executed work to re-reading the task document; the same-day decision deferred the payload extension and named its open question: which producer supplies the target and digest at publish time.
- The CLI's authority model already answers that shape: the caller supplies `target-path` and `expected-sha256` as explicit governed inputs, distinct from the approved task document that binds the work.
- Deriving the pair from Workspace snapshot observations would treat observation-time digests as approval authority and couple publication to observation states; both are wrong — observations are evidence, not authority.
- Both filesystem stores already serialize an optional string with a presence flag (`causationId`), and the terminal-disposition increment established the precedent of revising schema v1 in place for the unreleased artifact with pre-existing snapshots failing closed.

Decision:

- Extend `WorkPayload` with one optional nested `ExecutionInput(targetPath, expectedContentSha256)` component — `targetPath` bounded non-blank (max 1024 characters), the digest 64 lowercase hex — plus a three-argument convenience constructor delegating to empty so every existing call site stays valid.
- Expose the `WorkItem.executionInput()` projection; append a presence flag plus the two strings after `allowedTools` in both filesystem serializers, revising schema v1 in place with no version bump.
- Give `WorkMessagePublisher.publish` an overload carrying `Optional<WorkPayload.ExecutionInput>`; the caller supplies the execution input explicitly and the publisher adds no validation beyond the payload contract.
- Make the port's derivation seam prefer the payload-declared input and fall back to `(sourceDocument, sourceSha256)` when absent; the `ApprovedTask` construction and Goal binding are unchanged.

Rationale:

Caller-supplied execution input matches the only Operational authority model the product has and keeps the envelope the single source of what the work is; the seam localizes the change to one derivation, and the in-place schema revision reuses an accepted precedent instead of inventing migration machinery for an unreleased artifact.

Consequences:

- A WorkItem can now declare an arbitrary governed target with its expected digest, and the durable worker executes it through the same contained read-file, evidence, verification, and RunRecord pipeline; absent input preserves the source-document behaviour exactly.
- Pre-existing local queue/runtime snapshots without the new field fail closed on read (accepted for the unreleased artifact).
- Out of scope: write/mutation Tools, multiple execution inputs, payload-carried plans or scripts, 3b/3c, retry, controls, and schema version bumps.
