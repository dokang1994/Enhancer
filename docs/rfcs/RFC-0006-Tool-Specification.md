# RFC-0006: Tool Specification

Status: Draft

## Purpose

Define Tool interfaces and invocation rules.

## Tool Categories

- File
- Git
- Terminal
- Search
- Browser
- MCP

## Rules

- Tool invocation must be explicit.
- Tool input and output must be structured.
- Dangerous tools require approval.
- Tool results must be added to agent context.
- Tools must not bypass repository source-of-truth rules.

## Prompt Book

### Codex Prompt

Implement only the Tool interface and safe result model when selected. Avoid real dangerous operations in the first slice.

### Claude Prompt

Review tool rules for safety gaps, missing approval cases, and unclear failure handling.

### GPT Prompt

Explain which tool category applies to a task and what safety checks are needed.
