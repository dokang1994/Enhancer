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
```

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
