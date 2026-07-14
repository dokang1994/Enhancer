# 03 - Tool System

## Codex Prompt

Implement the selected Delivery Gate 1 boundary: Tool request, policy, executor, and one allowlisted read-only filesystem Tool. Do not implement terminal, Git, network, or LLM behavior.

## Goal

Tools are the boundary between the Agent Loop and the outside world.

## Core Interface

Start with a minimal shape:

```java
public interface Tool<I, O> {
    String name();
    O execute(I input);
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

Truncated output requires a reference to the complete output. The first slice models the reference but does not persist evidence or execute a Tool.

Capability maturity: **Contract Verified**. The result contract is tested, but the Tool System is not Integrated or Operational.

## Delivery Gate 1

The next active product slice adds:

- `ToolRequest` with Tool identity, arguments, and correlation identity;
- `Tool` interface;
- `ExecutionPolicy` for allow, deny, root path, size, timeout, and cancellation rules;
- `ToolExecutor` that applies policy before invocation;
- one UTF-8 read-only filesystem Tool;
- deterministic fake Tool support;
- a request-to-policy-to-Tool-to-result integration test.

The first concrete Tool must deny paths outside the approved project root and must return bounded `VerificationEvidence`. It must not mutate the filesystem.

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

## Out Of Scope

- Running arbitrary shell commands
- Network tools
- MCP tools
- Background execution
- Evidence persistence
- Independent verification

## Prompt Book

### Codex Prompt

Implement only Delivery Gate 1 when active. A read-only filesystem Tool is explicitly selected; terminal, Git, network, MCP, evidence persistence, and verifier behavior remain deferred.

### Claude Prompt

Review the Tool System boundary for safety, typed inputs/outputs, and future MCP compatibility. Identify over-abstraction or missing failure handling.

### GPT Prompt

Summarize the Tool System design and propose the safest first concrete tool to implement after the interface exists.
