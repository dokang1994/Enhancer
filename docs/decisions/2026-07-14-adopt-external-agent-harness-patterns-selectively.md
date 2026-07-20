# 2026-07-14: Adopt External Agent Harness Patterns Selectively

Status: Accepted Decision

Decision:

Enhancer will treat [MoAI-ADK](https://github.com/modu-ai/moai-adk) and similar agent harnesses as reference implementations rather than runtime dependencies. The first adopted pattern is an explicit terminal outcome for the deterministic Assisted Development Loop that composes repository context reading and task planning. Other useful patterns will be introduced only in the roadmap slice that owns them.

Rationale:

MoAI-ADK contains useful operational patterns, including explicit stop reasons, stagnation detection, bounded verification evidence, progressive Skill loading, artifact provenance, and approval-protected self-improvement. Importing its framework, provider-specific schemas, or Git workflow would duplicate Enhancer components and weaken the current document-driven approval model. Selective, provider-neutral adoption preserves the useful semantics without coupling the products.

Consequences:

- The current slice adds no MoAI package, command, generated configuration, or runtime dependency.
- The first Assisted Development Loop is a single read-and-plan pass with explicit outcomes and no repository mutation.
- Repeated-loop termination and stagnation are implemented in a separate Agent Loop slice.
- Verification evidence belongs to the Tool System; progressive loading belongs to the Skill System; provenance belongs to Plugin and template management.
- Token budgets follow LLM integration, while self-improvement requires snapshot, approval, verification, and rollback contracts before implementation.
- Claude-specific configuration, automatic commits or pushes, and parallel multi-agent orchestration are not adopted.

Adoption sequence:

1. Implement bounded repeated-loop termination and consecutive-state stagnation detection in the Agent Loop.
2. Define a bounded verification-evidence contract with Tool results.
3. Add a sequential independent verifier after the single-agent loop is stable.
4. Add progressive Skill loading while preserving the rule that Proposed catalog entries are not loadable.
5. Add artifact provenance when Plugin or template installation exists.
6. Add provider-neutral token and context budgets only after an LLM invocation boundary exists.
7. Implement self-improvement only after snapshot, human approval, independent verification, and rollback contracts exist.

The sequence does not conflict with `.ai/` rules: each item remains a small `CURRENT_TASK.md` scope, uses test-first verification for observable behavior, preserves proposal/decision/implemented-state separation, and cannot override the Constitution. The independent verifier begins as a sequential component, not multi-agent routing.
