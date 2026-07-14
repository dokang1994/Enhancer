# 04 - Skill System

## Codex Prompt

Design the Skill System as reusable workflow knowledge. Do not build a plugin marketplace or dynamic loading until the basic skill format is stable.

## Goal

Skills let Enhancer reuse specialized instructions for repeated development tasks.

## Skill Definition

A skill should answer:

- What problem does it solve?
- When should it be used?
- What documents or files must be read?
- What steps must be followed?
- What verification is required?

## Candidate Model

```text
Skill
SkillMetadata
SkillTrigger
SkillInstruction
SkillRegistry
SkillComposition
SkillExecutionPlan
```

## Initial Skill Sources

- `.ai/coding_rules.md`
- `.ai/workflow.md`
- `.ai/prompt_rules.md`
- `prompts/*.md`

## Rules

- Skills are instructions, not hidden memory.
- Skills must be stored in the repository.
- Skills must not override `CONSTITUTION.md`.
- Implemented Skills use `skills/<name>/SKILL.md` with the RFC-0007 frontmatter schema.
- Descriptions contain trigger conditions only, and `allowed-tools` follows least privilege.
- Catalog entries marked Proposed are not installed, selectable, or executable.
- `.ai/skill_rules.md` contains operational authoring rules.
- Skills load progressively: metadata first and full instructions only after selection.
- Composition such as Spring -> Java -> Database -> Test is explicit and preserves order, conflicts, provenance, and verification.
- Composed permissions are intersected with task approval and execution policy; composition never unions authority.
- A Skill is a workflow recipe, not an Agent. Agent plugins provide schedulable expertise; Skills may select Agents and Tools through the Workflow Engine.
- A Spring REST API Skill may explicitly compose controller, DTO, entity, repository, service, test, API-documentation, and optional Git stages.

## Tests

Cover:

- loading a skill
- missing skill file
- skill priority
- invalid skill metadata
- unknown permission value
- Proposed catalog entry cannot be loaded

## Prompt Book

### Codex Prompt

Implement only repository-backed skill loading if `CURRENT_TASK.md` selects Skill System work. Skills must be explicit files and must not override `CONSTITUTION.md`.

### Claude Prompt

Review the Skill System for priority conflicts, hidden-memory risks, and unclear trigger rules. Recommend ADR updates for any policy decision.

### GPT Prompt

Explain how skills differ from tools and memory in Enhancer, then propose the smallest next Skill System task.
