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

## Accepted Result Contract Slice

Implementation status: Implemented

- Every Tool result includes structured verification evidence.
- Evidence summaries are limited to 512 characters.
- Only the final 4096 output characters are retained in the result.
- Truncated output requires a reference to the complete output.
- Result status is explicit, and an available exit code must agree with success or failure.
- Evidence persistence, concrete Tool execution, and independent verification remain out of scope for this slice.

## Prompt Book

### Codex Prompt

Implement only the Tool interface and safe result model when selected. Avoid real dangerous operations in the first slice.

### Claude Prompt

Review tool rules for safety gaps, missing approval cases, and unclear failure handling.

### GPT Prompt

Explain which tool category applies to a task and what safety checks are needed.
