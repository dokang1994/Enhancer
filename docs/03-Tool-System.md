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
- Dangerous tools require user approval or a safety policy.

## Tests

Cover:

- tool name
- successful execution
- failure result
- invalid input

## Out Of Scope

- Running arbitrary shell commands
- Network tools
- MCP tools
- Background execution
