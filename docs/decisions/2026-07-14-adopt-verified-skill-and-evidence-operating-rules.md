# 2026-07-14: Adopt Verified Skill And Evidence Operating Rules

Status: Accepted Decision

Decision:

Enhancer will adopt repository-defined Skill authoring rules, memory distillation, test-first behavior for observable feature and bug-fix changes, and fresh verification evidence before completion claims. The initial Skill catalog remains explicitly proposed until corresponding `SKILL.md` files exist. Task cycles do not force commits; commits remain controlled by repository policy and user instruction.

Rationale:

These rules strengthen repeatability and verification while preserving Document Driven Development, least privilege, proposal-state separation, and the existing human approval boundary for Git operations.

Consequences:

- RFC-0002, RFC-0005, RFC-0007, RFC-0008, and RFC-0009 describe the accepted direction.
- `.ai/skill_rules.md` defines operational authoring constraints for future Skills.
- Proposed catalog entries cannot be treated as installed or available Skills.
- `allowed-tools` uses a small documented permission vocabulary rather than undeclared tool names.
- Actual Skill workflows, loading, and runtime enforcement remain future tasks.
