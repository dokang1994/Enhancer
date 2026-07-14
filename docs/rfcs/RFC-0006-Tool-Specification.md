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

Capability maturity: Contract Verified

- Every Tool result includes structured verification evidence.
- Evidence summaries are limited to 512 characters.
- Only the final 4096 output characters are retained in the result.
- Truncated output requires a reference to the complete output.
- Result status is explicit, and an available exit code must agree with success or failure.
- Evidence persistence, concrete Tool execution, and independent verification remain out of scope for this slice.

## Next Delivery Slice

Delivery Gate 1 introduces `ToolRequest`, `Tool`, `ExecutionPolicy`, `ToolExecutor`, one allowlisted read-only filesystem Tool, and deterministic test doubles.

The Gate must connect a real request to a real `ToolResult` in an integration test. Evidence persistence and the sequential independent verifier follow in later dependency gates; they are not implemented as disconnected contracts before Tool execution exists.

## Prompt Book

### Codex Prompt

Implement Delivery Gate 1 only when it is the active task. Use test-first behavior for the Tool request, policy, executor, and one read-only filesystem Tool. Avoid shell mutation, Git writes, network calls, and LLM use.

### Claude Prompt

Review tool rules for safety gaps, missing approval cases, and unclear failure handling.

### GPT Prompt

Explain which tool category applies to a task and what safety checks are needed.
