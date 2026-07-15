# RFC-0006: Tool Specification

Status: Accepted

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
- Evidence persistence and independent verification remain out of scope for the result-contract slice; concrete execution is delivered by Gate 1 below.

## Integrated Delivery Slice

Delivery Gate 1 integrates `ToolRequest`, `Tool`, `ExecutionPolicy`, `ToolExecutor`, one allowlisted read-only filesystem Tool, and deterministic test doubles.

The Gate connects a real request to a real `ToolResult` in an integration test.

Delivery Gate 2 integrates `EvidenceStore`, atomic filesystem envelopes, UUID identities, SHA-256 and length validation, strict UTF-8 resolution, bounded retention policy, and a real large-file Tool result with a resolvable reference.

Delivery Gate 3 integrates externally approved work, a prebuilt Tool request, immutable execution policy, real Tool results, retry classification, and bounded Agent Loop transitions. Tool success stops at `AWAITING_VERIFICATION`.

Delivery Gate 4 integrates the next consumer: a deterministic sequential read verifier resolves complete evidence, recomputes content identity, and permits completion only after an external expected digest matches. The controller binds the immutable execution policy to its worker result, and the finalization boundary derives the audit snapshot from that result before atomically persisting a lifecycle-valid replayable RunRecord.

Gate 3 hardening requires every failed result to carry a structured `ToolFailureCode`. Complete evidence capture also carries a SHA-256 content identity so retry progress remains stable when an opaque storage reference changes. Diagnostic prose and storage location are not control-plane identity.

## Prompt Book

### Codex Prompt

Preserve Delivery Gates 1 through 4. Implement Gate 5 CLI wiring without shell mutation, Git writes, network calls, or LLM use.

### Claude Prompt

Review tool rules for safety gaps, missing approval cases, and unclear failure handling.

### GPT Prompt

Explain which tool category applies to a task and what safety checks are needed.
