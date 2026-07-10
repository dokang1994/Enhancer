# RFC-0007: Skill Specification

Status: Draft

## Purpose

Define Skills as reusable workflows.

## Example

Spring CRUD Skill:

```text
DTO
↓
Entity
↓
Mapper
↓
Service
↓
Test
```

## Rules

- Skills are workflow documents.
- Skills are stored in the repository.
- Skills must not override `CONSTITUTION.md`.
- Skills may guide planning and implementation.
- Skills must define verification steps.

## Prompt Book

### Codex Prompt

Implement Skill loading only when selected. Start with repository Markdown skill definitions and tests.

### Claude Prompt

Review Skill rules for hidden assumptions, priority conflicts, and missing verification.

### GPT Prompt

Explain when to use a Skill and how it differs from a Tool.
