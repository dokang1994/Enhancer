# 2026-07-15: Promote Gate 6 Sub-Capabilities Only Against Named Fresh Integration Evidence

Status: Accepted Decision

Context:

- Six Gate 6 sub-capabilities are recorded as Contract Verified, but later increments connected each of them to real upstream and downstream components through integration tests and actual-repository runs.
- The Roadmap state model requires integration tests as the evidence class for Integrated, and the Constitution forbids promotion from past results or documentation claims alone.
- The Gate 0 precedent promoted through an audit with fresh evidence and no manufactured production change.

Decision:

- Promote a sub-capability to Integrated only when a named integration test, re-run fresh in this task, connects it to real upstream and downstream components; leave anything else Contract Verified and report the gap.
- Map evidence explicitly: `WorkspaceSnapshot`, `ProjectBrainView`, the graph projection contract, and `TaskImpactQuery` through `WorkspaceCollectionIntegrationTest` and the CLI composition suites; `AcceptedDecisionProjector` through the CLI graph-composition suite over a real decision log; `RunRecordMetadataCollector` through the real filesystem store suite and the CLI second-run prior-record observation.
- Change no production or test code for the promotion itself; an audit-exposed defect would instead stop the promotion and be reported.
- Keep Delivery Gate 6 `Specified - Next` and keep the existing Operational records for the production view and graph composition unchanged.

Rationale:

Maturity promotion is a claim about evidence, not about code, so the only honest procedure is to name the connecting evidence per capability and re-run it fresh. Requiring the evidence to predate the promotion task prevents the audit from writing tests whose purpose is to justify the promotion they measure.

Consequences:

- Promoted records cite the same suites future regressions will keep running, so integration claims stay continuously re-verified.
- Sub-capabilities promoted to Integrated may still lack Operational status individually; the production-path Operational records remain separate.
- Gate 6 gate-level promotion still requires the remaining scope: reference grammar, remaining adapters, and boundary enforcement evidence.
