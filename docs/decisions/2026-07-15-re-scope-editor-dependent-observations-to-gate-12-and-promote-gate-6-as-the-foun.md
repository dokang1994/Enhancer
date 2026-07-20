# 2026-07-15: Re-Scope Editor-Dependent Observations To Gate 12 And Promote Gate 6 As The Foundation

Status: Accepted Decision

Context:

- The recorded Gate 6 maturity assessment evidenced every scope item and exit criterion except diagnostics, terminal-session, and active/selected-file observation.
- Those three observations require diagnostic providers, terminal integration, and editor state that first exist with the Gate 12 interface work; Gate 6 cannot produce honest evidence for them, and stub adapters were rejected.
- The Roadmap keeps exactly one `Specified - Next` product gate, and leaving Gate 6 open across several gates would block that flow on capabilities it does not own.
- The user approved proceeding on the assessment's Option B recommendation on 2026-07-15.

Decision:

- Move the diagnostics, terminal-session metadata, and active/selected-file observation items from Gate 6 to Gate 12 as Workspace observation integrations of the interfaces that own those capabilities; their source kinds stay typed in the Workspace contract now.
- Promote Delivery Gate 6 to Integrated as the Workspace and Project Brain foundation on the evidence recorded in the maturity assessment; the production view and graph composition remain Operational sub-capabilities.
- Advance the sole `Specified - Next` marker to Delivery Gate 7 Event Bus and IPC Foundation.
- Update the two actual-roadmap test expectations to Gate 7 in the same change, because their contract is "the current next gate", not "Gate 6".

Rationale:

A gate should exit when everything it can honestly evidence is evidenced and the remainder belongs to capabilities other gates own; keeping typed source kinds now and integrating their adapters where the sources first exist preserves both the contract's completeness and the evidence discipline. The dependency order stays truthful: Gate 7 depends on snapshots and RunRecords, which are Integrated and Operational.

Consequences:

- Gate 12 gains three explicit Workspace observation integration items and Gate 6's exit no longer waits on them.
- Gate 6 is Integrated, not Operational or Released: its Operational surface remains the governed read-only CLI scenario, and per-file Git metadata, payload capture, modifies/verified-by producers, and graph persistence remain future work in their owning gates.
- The next product work is the Gate 7 event and message envelope foundation, which carries the snapshot identity across handoffs by design.
