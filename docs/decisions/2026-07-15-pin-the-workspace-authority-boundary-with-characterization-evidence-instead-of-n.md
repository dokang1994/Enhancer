# 2026-07-15: Pin The Workspace Authority Boundary With Characterization Evidence Instead Of New Mechanism

Status: Accepted Decision

Context:

- The Gate 6 exit criteria require that Workspace observations cannot override repository authority or grant Tool permission, but no test stated those claims explicitly.
- Tool scope enters execution only through the task document via `ApprovedTaskReader`, and observations carry digests and bounded metadata by construction, so the boundary should already hold.
- The Gate 0 precedent promotes claims through characterization tests that pass on first run, without manufacturing production changes.

Decision:

- Characterize the boundary in `WorkspaceAuthorityBoundaryIntegrationTest`: adversarial tool-grant text inside every observed non-task document must not widen the persisted approved task or policy scope beyond the task document's declared tools, must not appear in bounded output, and must not survive into any repository document mutation.
- Assert the converse: a task document that does not allow `read-file` is rejected as a configuration error regardless of grant text elsewhere.
- Treat a first-run failure as a defect that stops the task; add no production mechanism for this evidence.

Rationale:

The boundary's strength comes from the existing single-entry design (task document to `ApprovedTaskReader` to policy), so the honest evidence is a test that tries to break it from the observation side and fails. Adding new enforcement code for an already-enforced property would obscure where authority actually enters.

Consequences:

- The exit criterion "Workspace observations cannot override repository authority or grant Tool permission" is now pinned by regression evidence.
- Future adapters that introduce new observation kinds inherit the same test pattern for their sources.
