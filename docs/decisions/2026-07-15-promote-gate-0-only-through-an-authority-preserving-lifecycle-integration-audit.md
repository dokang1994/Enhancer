# 2026-07-15: Promote Gate 0 Only Through An Authority-Preserving Lifecycle Integration Audit

Status: Accepted Decision

Context:

- Gate 0 remains Contract Verified even though later Gates consume its Context, planning, loop, result, evidence, and governance contracts.
- The original Gate 0 limitation says the pieces do not execute one connected Agent run, while Gate 5 now provides an Operational read-only run over most of those boundaries.
- Planner proposals deliberately cannot approve themselves, so a valid integration test must not create an automatic Proposal-to-execution authority path.

Decision:

- Prepare one bounded Gate 0 promotion task that inventories each contract's real consumer and adds an end-to-end lifecycle integration test over a governed temporary repository.
- Split the test into a read-only planning phase and an execution phase separated by an explicit external test-fixture activation of `CURRENT_TASK.md`.
- Prove that planning leaves repository authority unchanged, execution before activation is rejected, and the activated read-only task reaches verified completion and restart-safe RunRecord replay through existing production boundaries.
- Reuse the Gate 5 CLI composition rather than adding a second production orchestrator.
- Allow the new characterization test to be initially GREEN if existing behavior already satisfies the accepted integration contract; create a RED cycle only for a genuine aligned behavior gap.
- Promote Gate 0 from Contract Verified to Integrated only after focused, consumer, full-regression, lint, structural, and documentation evidence passes.
- Keep Gate 6 as the sole `Specified - Next` product gate throughout this maturity-reconciliation task.

Rationale:

Integration maturity requires evidence that real collaborators are connected, not artificial new code or an authority shortcut. The explicit fixture transition models the human or governed approval boundary while allowing the full planning-to-verified-run lifecycle to be tested without mutating the actual repository.

Consequences:

- Gate 0 may be promoted without a new user interface because Operational entry is already owned by Gate 5.
- A passing characterization test can justify a documentation-only maturity promotion when it proves that existing downstream integrations satisfy every Gate 0 contract.
- Any uncovered defect remains subject to the active task, RED classification, and minimum-change rules.
