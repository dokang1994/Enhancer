# 2026-07-22: Reuse The Immutable Submission Manifest As The Sole Generated-Input Recovery Record

Status: Accepted Decision

Context:

- The explicit `scheduler-submit` workflow is replay-safe only when an operator retains
  every supplied identity and occurrence time. Generating those values and then crashing
  before the immutable submission manifest is visible could make a retry create different
  work, while crashing after persistence but before output could hide a successful submit.
- The existing submission manifest already persists the exact queue identity/capacity,
  capability, occurrence time, task revision, Workspace snapshot, authorization scope,
  execution input, and message provenance before queue creation and admission.
- A second invocation manifest containing those facts would require another identity and
  persistence order while duplicating the existing manifest's authority.

Decision:

- Do not add a separate durable Scheduler invocation manifest for generated submission
  inputs.
- Use one caller-retained canonical submission UUID as the stable replay key and message
  identity. A future generated-input submission boundary may derive its queue,
  correlation, and logical-run identities through explicit versioned domain transforms,
  so the same key always names the same generated identities.
- On first use only, capture the occurrence time and governed repository snapshot, build
  the exact existing `DurableSubmissionManifest`, and persist it before queue creation or
  admission through the existing service.
- On replay, resolve the existing submission manifest by the stable key before consulting
  a clock or recapturing repository context. Reuse its exact occurrence time and envelope,
  and fail closed if caller-owned task, capacity, capability, producer, target, or digest
  intent conflicts with the stored value.
- Keep the existing manifest as the sole authority for generated inputs and the existing
  queue history as the sole admission/disposition authority. Generated-input submission
  must remain separate from `scheduler-cycle`, polling, or automatic execution.

Alternatives considered:

- Keep every identity and time explicit: this is the current safe operational workflow
  and needs no new state, but it does not offer generated-input ergonomics and leaves the
  operator responsible for preserving the entire replay tuple.
- Add a separate durable invocation manifest: this can close the pre-submission crash
  window, but it duplicates the exact envelope and occurrence time already owned by the
  submission manifest and creates another identity, store, and recovery prefix.
- Reuse the existing submission manifest behind a stable replay key: selected because it
  closes the generated-value window, preserves one owner per fact, and feeds the existing
  queue-creation/admission consumer without coupling execution.

Rationale:

The stable key plus deterministic domain separation makes every generated identity
recoverable, while the existing persist-first manifest closes the occurrence-time crash
window without a second copy of the same facts. Resolving before generating makes an
uncertain prior result idempotent and preserves the current manifest -> queue -> admission
prefix.

Consequences:

- One stable caller-supplied UUID remains necessary; a command cannot safely generate an
  unreported recovery handle and also promise replay after an arbitrary stop.
- The future generated-input path can reuse the current manifest schema and submission
  service rather than introducing a new store or coupling execution.
- The explicit-input `scheduler-submit` command remains valid and unchanged until a
  separate bounded implementation task adds and verifies the generated-input boundary.
- A distinct invocation-level record may be reconsidered only if one invocation must own
  multiple submissions or lifecycle facts not already owned by the submission manifest
  and queue.
