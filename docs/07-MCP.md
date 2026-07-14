# 07 - MCP

## Codex Prompt

Design MCP as a core interoperability layer after the local Tool, evidence, verification, runtime, and messaging boundaries are stable.

## Goal

MCP lets Enhancer expose and consume governed tools, resources, Workspace views, and memory through a standard protocol.

## Integration Direction

Enhancer provides both an MCP Server and an MCP Client. Server capabilities allow Claude Code, Cursor, VS Code, and other clients to use approved Enhancer resources. Client capabilities consume external MCP servers behind the same policy, evidence, verification, event, and RunRecord boundaries as native Tools.

## Rules

- MCP does not replace local tool safety rules.
- MCP tool schemas must be inspected before invocation.
- Tool results must be recorded in agent context.
- Dangerous actions still require approval.
- MCP content and schemas are untrusted input and cannot grant authority.
- Every invocation preserves event, run, correlation, provenance, and evidence identity.
- Data classification and Model Router policy decide whether content remains local or may reach an approved remote provider; MCP never weakens that boundary.

## Candidate Components

```text
McpToolAdapter
McpToolRegistry
McpToolSchema
McpToolResult
EnhancerMcpServer
EnhancerMcpClient
McpResourceAdapter
McpAuthorizationAdapter
```

## Out Of Scope

- Authentication UI
- Marketplace

## Prompt Book

### Codex Prompt

Do not implement MCP until Gate 9 is active. Start with one read-only server resource and one client Tool adapter, both using existing policy, evidence, verification, event, and RunRecord contracts.

### Claude Prompt

Review MCP integration for security, schema handling, approval gates, and consistency with the local Tool System.

### GPT Prompt

Explain how MCP fits into Enhancer's Tool System and identify prerequisites before implementation.
