# 2026-07-14: Harden Agent Runs With Repository Approval And Semantic Progress

Status: Accepted Decision

Context:

- Gate 3 integrates real Tool results, but `approvedTask` is only a non-blank string and the integration test does not derive it from repository context.
- Progress currently includes an opaque evidence reference, so identical content stored under a new UUID can appear to be progress and evade stagnation detection.
- Tool failures expose only success or failure; a real retry policy cannot distinguish timeout, cancellation, denial, invalid requests, or temporary failures without parsing prose.
- The public `AgentRunState` record constructor accepts caller-supplied progress keys and structurally valid states outside the governed transition path.

Decision:

- Introduce `ApprovedTask` and `ApprovedTaskReader`. The reader consumes `CURRENT_TASK.md` from `ProjectContext` and requires explicit `Task ID`, `Status: In Progress`, `Task`, `Approval`, and `Allowed Tools` sections.
- Bind every initial `ToolRequest` to the approved task's Tool-name scope before execution policy is applied. Repository approval evidence is explicit provenance, not a cryptographic signature or permission escalation.
- Add structured `ToolFailureCode` to every failed `ToolResult`; successful results carry no failure code. The executor assigns codes at its policy and execution boundaries.
- Provide a standard failure classifier that retries only explicitly temporary failures and timeouts; all other codes are terminal by default.
- Add an optional SHA-256 content digest to `VerificationEvidence.capture` and use it for semantic progress. Opaque storage references and human-readable summaries do not define progress identity.
- Replace the public Agent run record constructor with a final immutable class whose constructor is private. Only the initial approval factory and package-owned controller transitions can construct states.

Rationale:

These changes turn the RED scenarios into production invariants instead of test conventions. Approval, failure semantics, and progress equality become structured and deterministic, while state transitions remain controlled by the orchestration boundary.

Consequences:

- A repository task without an active status, explicit approval evidence, or Tool scope cannot start an Agent run.
- Execution policy remains an additional deny-over-allow boundary; repository approval cannot broaden it.
- Identical evidence content remains identical progress even when persisted at different references.
- Retry behavior no longer depends on diagnostic message text.
- Gate 4 can consume structured approval, failure, and evidence identities in its verifier and RunRecord.
- Signature-backed approval, argument-level authorization, identity federation, mutation Tools, independent verification, and RunRecord persistence remain deferred.
