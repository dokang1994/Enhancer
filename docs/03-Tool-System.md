# 03 - Tool System

## Codex Prompt

Maintain the integrated Delivery Gate 1 and 2 boundaries. Do not add Agent Loop, terminal, Git, network, or LLM behavior from this chapter unless its later delivery gate is active.

## Goal

Tools are the boundary between the Agent Loop and the outside world.

## Core Interface

Gate 1 uses this minimal shape:

```java
public interface Tool {
    String name();
    ToolResult execute(ToolRequest request, ExecutionPolicy policy) throws Exception;
}
```

Only add richer metadata when a real use case requires it.

## First Result Contract Slice

The bounded result records that every future Tool must return are implemented under `com.enhancer.tool`:

```text
ToolResult
├─ toolName
├─ status: SUCCESS or FAILURE
├─ optional exitCode
└─ VerificationEvidence
   ├─ summary: at most 512 characters
   ├─ outputTail: final 4096 characters at most
   ├─ originalOutputLength
   ├─ truncated
   └─ optional fullOutputReference
```

Truncated output requires a reference to the complete output. The result-contract slice models the reference, Gate 1 executes a Tool through it, and Gate 2 makes the reference durable and resolvable.

The result contract is **Contract Verified**. The bounded read-only execution and durable evidence paths are **Integrated**, but the Tool System is not Operational.

## Delivery Gate 1

Integrated scope:

- `ToolRequest` with Tool identity, arguments, and correlation identity;
- `Tool` interface;
- `ExecutionPolicy` for allow, deny, root path, size, timeout, and cancellation rules;
- `ToolExecutor` that applies policy before invocation;
- one UTF-8 read-only filesystem Tool;
- deterministic fake Tool support;
- a request-to-policy-to-Tool-to-result integration test.

`ReadFileTool` denies paths outside the approved real project root, enforces a bounded byte limit, decodes UTF-8 strictly, and returns bounded `VerificationEvidence`. It does not mutate the filesystem.

## Delivery Gate 2

Integrated scope:

- `EvidenceStore` and `FileSystemEvidenceStore`;
- UUID run and evidence identities;
- opaque contained evidence references;
- one versioned atomic envelope with timestamp, UTF-8 byte length, SHA-256 digest, and complete output;
- bounded resolution with missing and corruption failures;
- explicit maximum-content and retention-duration policy without automatic cleanup;
- `EvidenceRecorder` integration with large `ReadFileTool` output.

The initial in-memory implementation supports at most 64 MiB per configured evidence item. A lower configured policy limit is expected for normal use. Gate 2 detects corruption but does not provide encryption, signatures, external tamper-proof storage, or automatic deletion.

## Planned Tools

- ReadFile
- WriteFile
- SearchFile
- Terminal
- Git
- Directory

## Design Rules

- Tool execution must be explicit.
- Tool input and output should be structured.
- Tool failures should be represented clearly.
- Every Tool result must include bounded verification evidence.
- Retain the final diagnostic output rather than an unbounded full log.
- Success with an exit code requires zero; failure cannot carry exit code zero.
- Dangerous tools require user approval or a safety policy.

## Tests

Cover:

- tool name
- successful execution
- failure result
- invalid input
- bounded short output
- truncated output with a complete-output reference
- rejection of truncated output without a reference
- status and exit-code consistency
- structured failure-code consistency
- stable content digest independent of evidence storage reference

## Out Of Scope

- Running arbitrary shell commands
- Network tools
- MCP tools
- Background execution
- Independent verification
- Verified completion and RunRecord persistence

## Prompt Book

### Codex Prompt

Preserve Delivery Gates 1 through 3. Implement sequential independent verification and RunRecord only through the active Gate 4 task; terminal, Git, network, MCP, and LLM behavior remain deferred.

### Claude Prompt

Review the Tool System boundary for safety, typed inputs/outputs, and future MCP compatibility. Identify over-abstraction or missing failure handling.

### GPT Prompt

Summarize the Tool System design and propose the safest first concrete tool to implement after the interface exists.
