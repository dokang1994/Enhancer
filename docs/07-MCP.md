# 07 - MCP

## Codex Prompt

Design MCP integration only after local tool interfaces are stable.

## Goal

MCP lets Enhancer use external tools and resources through a standard protocol.

## Integration Direction

Enhancer should treat MCP tools as external tools behind the same Tool System boundary.

## Rules

- MCP does not replace local tool safety rules.
- MCP tool schemas must be inspected before invocation.
- Tool results must be recorded in agent context.
- Dangerous actions still require approval.

## Candidate Components

```text
McpToolAdapter
McpToolRegistry
McpToolSchema
McpToolResult
```

## Out Of Scope

- Custom MCP server implementation in the first MCP task
- Authentication UI
- Marketplace

## Prompt Book

### Codex Prompt

Do not implement MCP until the local Tool System is stable. If MCP work is selected, build only an adapter boundary and tests around schema inspection and safe invocation.

### Claude Prompt

Review MCP integration for security, schema handling, approval gates, and consistency with the local Tool System.

### GPT Prompt

Explain how MCP fits into Enhancer's Tool System and identify prerequisites before implementation.
