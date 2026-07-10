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

## Tests

Cover:

- loading a skill
- missing skill file
- skill priority
- invalid skill metadata
