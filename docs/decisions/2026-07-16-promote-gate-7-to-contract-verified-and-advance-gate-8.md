# 2026-07-16: Promote Gate 7 To Contract Verified And Advance Gate 8

Status: Accepted Decision

Context:

- The Gate 7 maturity model requires core types, invariants, and focused contract tests for Contract Verified status; it does not require a production caller or real process boundary until Integrated or Operational maturity.
- The prior assessment mapped all six scope items and all four exit criteria, finding only an unbounded `WorkPayload.allowedTools` collection. The completed test-first correction now bounds that collection to 256 unique names.
- Gate 8 is the immediate integration consumer for the message, control, handoff, replay, cancellation, ordering, and backpressure contracts.

Decision:

- Re-run the complete Gate 7 bus contract suite, full regression, strict production lint, and document self-hosting checks before changing maturity state.
- If the fresh evidence passes, promote Delivery Gate 7 from `Specified - Next` to `Contract Verified` and advance the Roadmap's sole `Specified - Next` marker to Delivery Gate 8.
- Treat the provider-neutral `MessageTransport` interface as sufficient for Gate 7 Contract Verified scope. Concrete local-process or remote adapters, wire formats, authentication, persistence, production wiring, and real process hops remain later integration work.
- Make no production-code change. Update only the two actual-Roadmap self-hosting assertions whose contract is the current `Specified - Next` gate, and claim no Gate 7 Integrated, Operational, or Released maturity from this assessment.

Rationale:

Contract Verified maturity is intentionally the boundary between a tested foundation and a connected runtime. Holding Gate 7 open for a concrete adapter would confuse integration evidence with contract evidence and block its named Gate 8 consumer. Advancing the next marker after fresh verification preserves the dependency sequence without overstating what exists.

Consequences:

- Gate 8 becomes eligible for separately authorized bounded contract work; it is not implemented merely because it becomes `Specified - Next`.
- Gate 7 still lacks production publishers/consumers, a concrete adapter, durability, threading, and a supported messaging entry point, so Integrated and Operational claims remain prohibited.
- Any failed or incomplete fresh verification stops the promotion and leaves Gate 7 `Specified - Next`.
