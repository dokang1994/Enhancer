# RFC-0003: Prompt Contract

Status: Draft

## Purpose

Define a common prompt interface for Codex, Claude, GPT, Gemini, and future Enhancer agents.

## Concept

```text
Task
↓
Prompt Builder
↓
Codex Prompt
Claude Prompt
GPT Prompt
Gemini Prompt
```

## Contract Fields

- task title
- goal
- context files
- constraints
- accepted decisions
- implementation scope
- tests required
- documentation updates
- output format

## Rules

- Prompts must preserve source-of-truth priority.
- Prompts must distinguish Proposal, Accepted Decision, and Implemented.
- Prompts must be model-specific at the final rendering layer only.
- Prompt generation must be deterministic where possible.

## Prompt Book

### Codex Prompt

Design the first prompt contract model only after the context reader exists. Keep the contract simple and testable.

### Claude Prompt

Review the prompt contract for missing fields, ambiguity, and model-specific leakage into the common layer.

### GPT Prompt

Explain how a single task becomes different prompts for Codex, Claude, GPT, and Gemini.
