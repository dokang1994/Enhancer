# 2026-07-16: Add Product Journeys Evaluation And Layered Security Across Delivery Gates

Status: Accepted Decision

Context:

- Delivery gates describe dependency-ordered capabilities, but a collection of mature components does not by itself prove that a user can complete a development job safely and understand the result.
- The Roadmap names interfaces, runtime reliability, multi-agent work, security controls, and release packaging in separate gates without one cross-cutting evaluation contract.
- Universal exactly-once execution cannot be guaranteed across arbitrary external Tools and side effects; truthful reliability comes from at-least-once delivery combined with stable idempotency, fenced ownership, replay-safe effects, and recovery evidence.
- The Constitution already establishes authority, untrusted-input, verification, and amendment rules. Detailed product security, journey, UX, and evaluation guidance belongs in supporting Architecture and Roadmap documents, so no constitutional amendment is needed for this clarification.

Decision:

- Add a cross-cutting Product Journey and Evaluation Track alongside, not in place of, Delivery Gates.
- Begin with four canonical journeys: governed bug repair, bounded feature delivery, evidence-backed codebase explanation, and interrupted-run recovery. Each journey must define its user-visible outcome, approvals, evidence, recovery behavior, supported surfaces, and versioned evaluation fixtures.
- Add a fifth product priority: a repeatable evaluation and release-quality harness that records task success, incorrect changes, recovery, cost, duration, user intervention, post-verification regression, and multi-agent delta using explicit denominators and immutable result provenance.
- Require thresholds to be selected and versioned before a release evaluation run. Agent confidence, reviewer self-report, or a single passing test cannot substitute for journey evidence.
- Define Gate 8 delivery as at-least-once with stable idempotency keys, fenced leases, checkpointed recovery, versioned state migration, orphan reclamation, and replay-safe external effects. Do not claim universal exactly-once execution.
- Require all user interfaces to consume one shared Run, approval, verification, evidence, and control API. Use the CLI as the reference surface, add VS Code next for in-context work, and add Desktop later as a supervisory and cross-run view.
- Require a common change-review projection showing plan, changed files and diff, tests/evidence, risks, approvals, recovery, and commit readiness rather than exposing internal Agent topology as the primary UX.
- Treat repository instructions, Tool output, model responses, MCP content, plugins, dependencies, and terminal output as untrusted data at the Architecture layer. Assign concrete enforcement to the owning gates for secret detection, outbound data control, permission manifests, isolation, audit, provenance, disablement, and rollback.
- Require Gate 13 multi-agent promotion to demonstrate improvement over a single-agent baseline on the same versioned task set and comparable budget envelope. Require Gate 16 release evidence to meet versioned journey thresholds in addition to packaging checks.

Rationale:

This keeps capability maturity technically precise while adding a second, user-centered proof dimension. The cross-cutting track prevents isolated features from being mistaken for a usable product, makes reliability claims implementable, and gives every interface and extension the same evidence, approval, and security model.

Consequences:

- Delivery Gate numbers, dependencies, and current maturity states do not change.
- A gate may pass its technical maturity criteria while a product journey remains incomplete; release claims require both applicable gate evidence and journey-quality evidence.
- Numeric thresholds remain unspecified until the evaluation fixtures and baseline measurement task is activated.
- The Constitution remains unchanged. Any future change to its authority or safety semantics still requires the full amendment process and separate explicit user approval.
