# 2026-07-15: Start Gate 7 With Reference-Only Versioned Envelopes And Exactly Four Payload Kinds

Status: Accepted Decision

Context:

- Gate 7 owns the typed message foundation, and the adopted orchestration invariants require every handoff to preserve the approved task revision, snapshot identity, authorization, correlation, and causation.
- The Gate 6 snapshot identity is deliberately absent from the RunRecord because envelopes own cross-handoff identity.
- Carrying payload content, evidence bodies, or Tool authority inside messages would widen the sensitive-data and authority boundaries that the evidence and policy layers already govern.

Decision:

- Add the envelope contract under `com.enhancer.bus`: an immutable versioned `MessageEnvelope` with canonical-UUID message identity, bounded correlation identity, optional canonical-UUID causation identity distinct from the message identity, bounded logical run and producer identities, an occurrence time, and one typed payload.
- Seal the payload hierarchy to exactly four kinds — work, result, control, handoff — so consumers exhaust them by type instead of interpreting conventions.
- Carry authorization and provenance as data only: the work and handoff payloads carry the approved task revision, a valid snapshot identity, and (for work) a bounded non-empty allowed-tool scope copied immutably; the result payload carries the run-record reference and verification status; the control payload carries a typed cancel/pause/resume signal with a bounded reason.
- Store references and identities only; delivery, topics, queues, retry, idempotency, replay, ordering, backpressure, and IPC transport arrive in later increments over this contract.

Rationale:

A message that carries the task revision, snapshot identity, and authorization scope as bounded data lets every later consumer verify what it received against repository authority instead of trusting the sender, which is the invariant the orchestration model demands. Sealing the payload kinds makes exhaustive handling a compiler guarantee before any bus exists.

Consequences:

- Possessing an envelope grants nothing: authority still enters only through the task document and execution policy, and later delivery code must re-validate rather than trust.
- New payload kinds require extending the sealed hierarchy through a recorded decision.
- Envelope schema evolution beyond the version identifier is deliberately unspecified until persistence or IPC needs it.
