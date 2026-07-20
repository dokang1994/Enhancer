# 2026-07-16: Bound Work Payload Tool Scope Cardinality

Status: Accepted Decision

Context:

- The Gate 7 maturity assessment found that `WorkPayload` bounds every allowed-tool name to 256 characters but accepts an arbitrarily large set, so the aggregate message payload has no contract ceiling.
- The Roadmap requires payloads to be bounded or replaced by evidence references before Gate 7 can exit.
- A concrete IPC adapter would not correct this in-memory contract defect and remains outside the current foundation scope.

Decision:

- Add `WorkPayload.MAX_ALLOWED_TOOLS` with a maximum of 256 unique allowed-tool names.
- Accept scopes from 1 through 256 entries and reject larger scopes before copying them into the immutable payload.
- Keep the existing 256-character per-name bound, snapshot/task provenance, immutable set semantics, and authority model unchanged.
- Change no Message Bus delivery, transport, serialization, persistence, scheduling, or production-wiring behavior.

Rationale:

A cardinality ceiling combined with the existing per-name ceiling gives the only collection-bearing payload a finite aggregate bound of at most 65,536 tool-name characters. A limit of 256 follows the existing bus identity bound, remains far above realistic Tool scopes, and closes the recorded exit criterion without inventing a wire format or adapter.

Consequences:

- Existing normal scopes remain source- and behavior-compatible; only previously unbounded scopes above 256 entries are rejected.
- Gate 7 still requires fresh reassessment before any maturity promotion or Gate 8 activation.
- Concrete local-process or remote adapters remain deferred to later integration work.
