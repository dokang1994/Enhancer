# 03 - Tool System

## Codex Prompt

Design the Tool System interface first. Do not implement real filesystem, terminal, or Git behavior until a later task selects one concrete tool.

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

Implement only the minimal Tool interface and supporting result model if this task is active. Do not implement real filesystem, terminal, Git, network, or MCP behavior unless explicitly selected.

### Claude Prompt

Review the Tool System boundary for safety, typed inputs/outputs, and future MCP compatibility. Identify over-abstraction or missing failure handling.

### GPT Prompt

Summarize the Tool System design and propose the safest first concrete tool to implement after the interface exists.
