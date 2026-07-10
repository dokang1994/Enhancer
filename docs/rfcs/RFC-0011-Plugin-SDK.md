# RFC-0011: Plugin SDK

Status: Draft

## Purpose

Define how external developers can extend Enhancer.

## Example Plugins

- Oracle Plugin
- MyBatis Plugin
- Spring Plugin
- WebSquare Plugin
- React Plugin
- Vue Plugin

## Plugin Capabilities

- tools
- skills
- prompts
- context providers
- templates
- validation rules

## Rules

- Plugins must declare capabilities.
- Plugins must not override core safety rules.
- Plugins must be versioned.
- Plugin behavior must be testable.

## Prompt Book

### Codex Prompt

Do not implement Plugin SDK until Tool and Skill systems are stable. Start with a manifest schema when selected.

### Claude Prompt

Review plugin boundaries for security, versioning, and core-rule override risks.

### GPT Prompt

Explain how a plugin extends Enhancer and propose a first plugin manifest.
