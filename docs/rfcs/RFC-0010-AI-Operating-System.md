# RFC-0010: AI Operating System

Status: Draft

## Purpose

Define Enhancer as an AI Development Operating System.

## OS Model

```text
Kernel
↓
Scheduler
↓
Planner
↓
Memory
↓
Tool
↓
Skill
↓
Agent
↓
Plugin
↓
LLM
```

## Final Structure

The original linear OS model is superseded as the target topology by:

```text
Desktop | CLI | API | Editor
        -> Workspace + Project Brain
        -> Event Bus + Message Bus + IPC
        -> Agent Runtime + Scheduler + Memory
        -> Skill Engine + MCP + Plugin Runtime
        -> Tool System + Model Gateway
        -> Repository / Providers / Cloud Sync
```

```text
Kernel
AI Rules
Prompt Engine
Context Engine
Planner
Task Queue
Memory
Tool Manager
Skill Manager
Plugin Manager
Agent Runtime
Extension
Dashboard
SDK
Workspace
Project Brain
Event Bus
Message Bus and IPC
MCP Server and Client
Model Gateway and Router
Scheduler
Desktop and API
Plugin Marketplace
Cloud Sync
```

## Runtime Rule

The target Agent lifecycle is `Goal -> Planner -> Executor -> Memory -> Reflection -> Retry -> Done`. Planner, Coder, Reviewer, Tester, and Memory exchange typed messages through queues. Direct Agent-to-Agent calls are not the extensibility model.

## Product Milestones

- V1: AI Development Experience with Cursor-level productivity over shared interfaces.
- V2: AI Development Platform with Agents, workflows, Skills, Memory, MCP, models, plugins, and self-hosting.
- V3: AI Operating System with Kernel, Project Brain graphs, multi-agent scheduling, privacy routing, marketplace ecosystem, sync, and governed improvement.

## Project Brain Rule

Decision, Architecture, Dependency, Task, and Execution graphs are source-linked projections over repository documents, code, Git, tests, bugs, commits, issues, pull requests, and RunRecords. They are freshness-aware and rebuildable; they do not replace canonical sources.

## Extension Rule

Agent plugins provide schedulable expertise. Skills provide workflows. Tools perform governed external actions. Workflows compose all three with events, memory, verification, rollback, and approval gates.

## Hybrid Model Rule

The Model Router selects local or remote providers from capability, data sensitivity, policy, cost, latency, context, and availability. Sensitive code defaults to an approved local route and cannot be sent remotely without authority.

## Interoperability Rule

MCP is a core layer. Enhancer exposes governed resources to external clients and consumes external MCP capabilities through the same policy, evidence, verification, event, and RunRecord boundaries.

## Positioning

Enhancer is not an application like Cursor.

Enhancer is an operating layer for AI-assisted development. Cursor-like features may run on top of this layer.

## Prompt Book

### Codex Prompt

Use the OS model as long-term architecture context. Do not implement OS-scale components until the active task requires them.

### Claude Prompt

Review whether a proposed feature belongs to Kernel, Planner, Memory, Tool, Skill, Agent, Plugin, or UI layer.

### GPT Prompt

Explain Enhancer as an AI operating system and distinguish it from Cursor-like applications.
