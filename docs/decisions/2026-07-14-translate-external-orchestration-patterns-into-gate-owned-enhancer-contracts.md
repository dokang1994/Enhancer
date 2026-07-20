# 2026-07-14: Translate External Orchestration Patterns Into Gate-Owned Enhancer Contracts

Status: Accepted Decision

Context:

- The user requested that useful Multi-Agent orchestration lessons from Archon and meta-harness be preserved for future Enhancer implementation.
- Archon demonstrates an operational control plane with dynamic capability rosters, centralized execution profiles, dependency-aware work, heartbeats, interventions, and resumable sessions, but its provider CLI subprocesses, shared working directory, file polling, and quality-gradient completion model do not satisfy Enhancer authority or evidence rules.
- meta-harness provides a portable pattern-selection ladder, deterministic handoffs, Producer-Reviewer and supervisor guidance, normal/failure scenarios, and removable provider-specific logic, but it is a design-time meta-skill rather than a runtime with scheduling, authorization, idempotency, replay, or evidence integrity.
- The reviewed reference snapshots are Archon commit `263cf3658a7cadefa0c5fbe82cc527a00ffb4c16` under MIT and meta-harness commit `ccab9a677878f72b3316de464c99b36f56a3f2e7` under Apache-2.0.

Decision:

- Treat both repositories as pinned reference implementations, never as hidden runtime, governance, Skill-layout, prompt, or file-format dependencies.
- Select the smallest orchestration topology that satisfies the work: one worker first; then a sequential pipeline; then Producer-Reviewer; then bounded fan-out/fan-in; and only then expert routing, supervisor allocation, or a hierarchy no deeper than one subordinate coordination layer.
- Require every parallel branch to consume the same immutable `WorkspaceSnapshot` and approved task revision. Branch ownership, expected output, synthesis criteria, budget, and conflict policy are fixed before dispatch.
- Carry handoffs through versioned Message Bus envelopes with run, task, message, correlation, causation, producer, schema, authorization, input-snapshot, and artifact/evidence-reference identity. Free-form Markdown may be an inspectable projection but cannot be the authoritative queue or control signal.
- Keep one Kernel-owned coordinator responsible for terminal task and run state. Workers may propose progress, artifacts, or follow-up work; they cannot approve tasks, broaden Tool or model authority, create final completion, or verify their own output.
- Make dependency readiness, cycle rejection, leases, duplicate suppression, cancellation, retry, timeout, dead-letter, replay, pause, resume, reassignment, and recovery Scheduler or Message Bus responsibilities rather than prompt conventions.
- Represent execution profiles as provider-neutral capability, model class, reasoning budget, context budget, Tool scope, data classification, and locality requirements. Provider adapters translate an approved profile only after Kernel policy intersects it with task, Skill, and Tool authority.
- Represent pause, resume, cancel, inject-proposal, reprioritize, reassign, mediate, and scale decisions as typed, authenticated, auditable control commands. A control command cannot silently create accepted work or external-action authority.
- Treat heartbeats, quality gradients, confidence, prompt adherence, and other worker telemetry as diagnostic observations only. They may trigger inspection or a proposal, but never lifecycle promotion, verification, completion, or release.
- Preserve independent verification and durable RunRecord finalization outside the producing worker. Producer-Reviewer revision loops are bounded and remain distinct from the independent verifier required for completion.
- Keep model-specific retries, prompt heuristics, CLI flags, and provider recovery logic behind removable adapters or reference sections so deleting one provider does not rewrite the orchestration contract.

Rationale:

The useful common pattern is observable, resumable, role-aware coordination with explicit handoffs and bounded parallelism. Enhancer already has stronger authority, evidence, verification, and replay requirements than either reference. Translating the patterns into existing Workspace, Event/Message Bus, Scheduler, Model Gateway, Skill, Verification, and RunRecord boundaries preserves those strengths while avoiding provider and storage coupling.

Consequences:

- Gate 6 owns immutable shared input snapshots and provenance; Gate 7 owns typed handoffs and control/event delivery; Gate 8 owns the durable dependency graph, leases, sequential worker, Scheduler, and recovery.
- Gate 9 owns provider-neutral execution profiles and model budgets; Gate 10 owns validated workflow-pattern and Skill selection; Gate 12 owns user-facing run controls; Gate 13 owns dynamic capability rosters, bounded fan-out/fan-in, Producer-Reviewer roles, supervisor allocation, and background execution.
- Gate 15 alone may consume autonomous experiment-ledger patterns, and only after approved snapshots, fixed evaluation, budgets, independent verification, and rollback are Operational.
- Direct peer calls, prompt-only coordination, shared-worktree parallel mutation without isolation, silent ring-buffer loss, self-reported completion, optional verification, unlimited timeouts, and file polling as the core bus are rejected.
- This decision changes documentation only. It does not promote Workspace, Message Bus, Agent Runtime, Scheduler, Model Gateway, Skill Engine, Multi-Agent, background execution, or self-improvement maturity, and it does not displace Delivery Gate 5.
- No external code or templates are copied by this decision. Any later copying must preserve applicable license, attribution, and modification notices.
