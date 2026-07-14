# RFC-0007: Skill Specification

Status: Accepted

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
- Every Skill is a `skills/<name>/SKILL.md` file with YAML frontmatter.
- A Skill description states only when to use it, not a workflow summary.
- Every Skill declares least-privilege `allowed-tools` permissions.

## Skill File Format

```yaml
---
name: example-skill
description: "Use when the documented trigger condition applies."
allowed-tools: [read]
verification: required
---
```

- `name`: lowercase kebab-case, at most 64 characters.
- `description`: third-person trigger conditions only, at most 1024 characters.
- `allowed-tools`: one or more values from `read`, `execute`, `write-docs`, and `write-code`.
- `verification`: currently the literal value `required`.
- Unknown fields or permission values fail validation.

## Permission Policy

- Design Skills normally use `read`.
- Diagnostic Skills normally use `read` and `execute`.
- Implementation Skills use only the write permission required by their declared targets.
- Permissions are capability categories. Runtime mapping to concrete tools remains out of scope.

## Skill Catalog

`skills/INDEX.md` is the discovery index. An entry marked `Proposed` is not installed or selectable. A Skill becomes `Available` only when its validated `SKILL.md` exists and the index is synchronized.

## Prompt Book

### Codex Prompt

Implement Skill loading only when selected. Start with validated repository Markdown definitions and tests. Do not load Proposed catalog entries.

### Claude Prompt

Review Skill rules for hidden assumptions, priority conflicts, and missing verification.

### GPT Prompt

Explain when to use a Skill and how it differs from a Tool.
