# Current Task

## Status

Completed

## Task

Add the first Delivery Gate 7 contract: immutable versioned message envelopes with event, correlation, causation, run, and producer identities, and typed work, result, control, and handoff payloads that preserve authorization and snapshot references, without any delivery, topic, queue, bus, or transport implementation.

## Task ID

gate-7-message-envelope-contract

## Justified By

- 2026-07-15: Re-Scope Editor-Dependent Observations To Gate 12 And Promote Gate 6 As The Foundation

## Context

Delivery Gate 7 Event Bus and IPC Foundation is the sole `Specified - Next` product gate, and the deterministic Planner proposes it from the actual Roadmap. The orchestration invariants adopted for Gates 6 through 15 require every worker handoff to preserve the approved task revision, the common immutable Workspace snapshot identity, authorization, correlation, and causation; the Gate 6 snapshot identity intentionally waits for these envelopes to carry it across handoffs.

This increment establishes only the provider-neutral envelope contract under a new `com.enhancer.bus` package. Payloads carry bounded identities and references — task revisions, snapshot identities, run-record references, verification status, control signals — never file content, evidence bodies, or Tool authority. The named consumer is the next Gate 7 increment: deterministic in-process topic and queue delivery over these envelopes.

## Acceptance Criteria

- Add an immutable `MessageEnvelope` with a versioned envelope identifier, a canonical-UUID message identity, a bounded correlation identity, an optional canonical-UUID causation identity that must differ from the message identity, a bounded logical run identity, a bounded producer identity, an occurrence time, and one typed payload.
- Add a sealed `MessagePayload` hierarchy with exactly four kinds: work, result, control, and handoff.
- Make the work payload carry the approved task revision, a valid snapshot identity, and a bounded non-empty allowed-tool scope; authorization data is carried, never created.
- Make the result payload carry the task identity, a bounded run-record reference, and the verification status.
- Make the control payload carry a typed cancel/pause/resume signal and a bounded reason.
- Make the handoff payload carry the approved task revision, a valid snapshot identity, and a bounded run-record reference.
- Validate all null, blank, length, identity-format, and consistency invariants; keep every published collection immutable.
- Store identities and references only: no payload content, no delivery semantics, no topic, queue, retry, idempotency, or transport types.
- Add focused RED tests before production types exist, classify the failure, then implement the minimum Java 17 change.
- Run focused bus tests, full Gradle regression with `--warning-mode all`, fresh XML inspection, Java 17 `-Xlint:all -Werror`, and `git diff --check`.
- Record the contract as Contract Verified only after fresh evidence passes and keep Gate 7 `Specified - Next`.

## Out Of Scope

- Topic, queue, delivery, subscription, retry, idempotency, dead-letter, replay, ordering, backpressure, or IPC transport types
- Bus wiring into the CLI, Agent Loop, or any production path
- Payload content, evidence bodies, chat history, or Tool authority
- Serialization, persistence, or schema evolution beyond the version identifier
- Commit, push, PR, merge, release, deployment, or publication

## Approval

Approved by the user on 2026-07-15 through the request to proceed with the proposed first Gate 7 increment.

## Allowed Tools

- read-file

## Verification Plan

- Write focused envelope and payload contract tests before production types exist.
- Confirm focused compilation fails only because the selected bus types are missing, and classify the failure.
- Implement the minimum immutable contract types.
- Run focused bus tests and inspect fresh XML output.
- Run the complete Gradle suite with `--warning-mode all` and inspect fresh XML counts.
- Run Java 17 production compilation with `-Xlint:all -Werror`.
- Confirm the Roadmap retains exactly one `Specified - Next` gate status marker at Gate 7 and run `git diff --check`.
- Review the final diff and synchronize all affected project documents.

## Implementation Result

- Added the provider-neutral `com.enhancer.bus` package: immutable versioned `MessageEnvelope` and the sealed `MessagePayload` hierarchy with exactly four kinds (`WorkPayload`, `ResultPayload`, `ControlPayload` with `ControlSignal`, `HandoffPayload`).
- Enforced canonical-UUID message identity, bounded correlation/run/producer identities, optional canonical-UUID causation distinct from the message identity, and an occurrence time.
- Carried authorization and provenance as bounded data only: task revisions, valid snapshot identities, immutable allowed-tool scopes, run-record references, verification status, and typed control signals; no content, delivery semantics, or Tool authority.
- Sealedness is pinned by a test asserting exactly four permitted payload subclasses.

## Verification

- RED: after replacing a Java 17 preview switch pattern with an instanceof/sealedness check, the focused compile failed with 38 expected missing-symbol errors naming only the seven intentionally absent bus types.
- Focused GREEN: the bus suite passed 4 tests with no skips, failures, or errors, confirmed against fresh XML output.
- Full regression: `.\scripts\gradle.ps1 --no-daemon clean test --warning-mode all` passed 43 suites and 156 tests: 154 passed, 2 existing Windows symbolic-link setup skips, 0 failures, and 0 errors.
- Gradle emitted no deprecation warning; Java 17 production compilation passed with `-Xlint:all -Werror`.
- Structural verification retained exactly one `Specified - Next` gate status marker at Gate 7; `git diff --check` passed.
- Not run: no delivery, topic, queue, or transport exists, so no envelope has crossed a real hop; the contract is proven by construction and invariants only.

## Next Task

Activate the next Gate 7 increment under separate explicit activation: deterministic in-process topic and queue delivery over these envelopes with idempotency and replay contracts.
