# Current Task

## Status

Completed

## Task

Add a cross-cutting product-journey and evaluation track, truthful Scheduler delivery semantics, shared interface ordering, change-centered UX, and a layered default-security model without changing the Constitution or overstating implementation maturity.

## Task ID

product-journey-evaluation-and-security-track

## Justified By

- 2026-07-16: Add Product Journeys Evaluation And Layered Security Across Delivery Gates

## Context

The delivery gates define strong component boundaries and capability maturity, but they do not yet prove that a user can complete an end-to-end development job. Evaluation metrics, shared-interface ordering, change-centered review, and the default treatment of repository/model/MCP/plugin/Tool content as untrusted input are also distributed across future gates rather than expressed as one product contract. Scheduler wording must avoid an impossible general exactly-once claim and instead define the enforceable composition of at-least-once delivery, idempotency, fenced leases, checkpoints, and replay-safe effects.

## Acceptance Criteria

- Define three to five canonical user journeys that cross delivery gates and end in an inspectable, approval-aware user outcome.
- Define a versioned evaluation harness and release-quality metrics with stable denominators, baselines, thresholds, and evidence rather than Agent self-report.
- State that Gate 8 scheduling uses at-least-once delivery with stable idempotency, fenced leases, checkpoint/recovery, state migration, and orphan reclamation; make no universal exactly-once claim.
- Require one shared Run, approval, verification, evidence, and control API before interface-specific policy, with CLI first, VS Code second, and Desktop as a later supervisory surface.
- Make one change-centered review model covering plan, files, diff, tests, evidence, risk, approvals, recovery, and commit readiness a Gate 12 exit criterion.
- Add an Architecture-level default-security model that treats repository instructions, Tool output, model responses, MCP content, plugins, and dependencies as untrusted data, with Gate-owned enforcement for secrets, outbound transmission, sandboxing, manifests, audit, and rollback.
- Strengthen Gate 13 and Gate 16 exit criteria so multi-agent and release claims depend on measured improvement and journey-quality thresholds.
- Preserve every existing capability maturity state and the sole Gate 8 `Specified - Next` marker.
- Synchronize Architecture, compact architecture, Roadmap, Project State, Changelog, Current Task, Decision Log, and Session Handoff.

## Out Of Scope

- Constitution or `.ai/constitution.md` amendment
- Runtime, Scheduler, API, interface, evaluation-harness, security-scanner, sandbox, plugin, MCP, model, or release implementation
- New Delivery Gate numbers or changes to existing capability maturity
- Selecting numeric release thresholds before representative fixtures and baselines exist
- Commit, push, PR, merge, release, or deployment

## Approval

Approved by the user's 2026-07-16 request to incorporate the reviewed product improvements.

## Verification Plan

- Resolve the accepted decision from `Justified By` and preserve exactly one Gate 8 `Specified - Next` marker.
- Check that every canonical journey names an outcome, evidence/approval boundary, and evaluation linkage.
- Check that every metric names its denominator and that multi-agent comparison uses the same task set and budget envelope as the single-agent baseline.
- Check that exactly-once is not claimed and that the enforceable Scheduler semantics are consistently named in Architecture and Roadmap.
- Check that interface ordering, shared APIs, change-centered UX, and default-security ownership are consistent across canonical and compact documents.
- Run actual-document Context Reader, Planner, and Assisted Loop tests.
- Review the complete diff and run tracked/untracked whitespace checks.

## Implementation

- Added a cross-cutting Product Journey and Evaluation model to Architecture and Roadmap without adding or reordering Delivery Gates.
- Defined four initial journeys and the fifth priority of a versioned evaluation/release-quality harness with explicit-denominator metrics and immutable result provenance.
- Defined truthful Gate 8 Scheduler delivery semantics: at-least-once delivery, stable logical-work/effect idempotency, fenced leases, durable checkpoints, supported state migration, orphan reclamation, and replay-safe or compensatable effects.
- Ordered interfaces around one shared application API, with CLI as reference, VS Code second, Desktop later as supervision, and one change-centered review projection as a Gate 12 exit criterion.
- Added an Architecture-level default-security model and strengthened the owning Gate 9, 11, 12, 14, and 16 scope/exit criteria for model/MCP, Tool/plugin, UX, cloud, and distribution controls.
- Strengthened Gate 13 and Gate 16 so multi-agent and release claims require versioned evaluation evidence, comparable baselines, and predeclared thresholds.
- Kept the Constitution, code, Delivery Gate numbering, current maturity states, and sole Gate 8 next marker unchanged.

## Verification

- Structural checks found all 17 sequential Delivery Gates, exactly one `Specified - Next` marker at Gate 8, exactly four canonical journeys, and exactly one accepted decision resolving `Justified By`.
- Metric review confirmed explicit denominators for success, incorrect changes, recovery, cost/time, intervention, held-out regression, and multi-agent delta.
- Consistency review confirmed the same at-least-once Scheduler semantics, interface order, change-centered UX, and security ownership across canonical Architecture, compact architecture, and Roadmap; no positive universal exactly-once claim remains.
- `CONSTITUTION.md` and `.ai/constitution.md` have no diff.
- Actual-document self-hosting passed 15 of 16 Context Reader, Planner, and Assisted Loop tests with 1 existing Windows symbolic-link setup skip and no failure or error.
- Tracked and untracked whitespace checks passed, and the complete diff was reviewed.

## Next

Resume the recorded Gate 7 Integrated maturity assessment after this cross-cutting documentation task is complete.
