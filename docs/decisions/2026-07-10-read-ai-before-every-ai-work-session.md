# 2026-07-10: Read `.ai/` Before Every AI Work Session

Status: Accepted Decision

Decision:

Every AI agent should read the `.ai/` folder before starting work. The folder contains AI-only operational documents: `constitution.md`, `workflow.md`, `coding_rules.md`, `architecture.md`, `prompt_rules.md`, and `memory.md`.

Rationale:

The root documents are canonical, but `.ai/` gives agents a compact operational entry point. This allows the user to say "항상 .ai 폴더를 읽고 시작해" and have a consistent startup rule across Codex, Claude, GPT, and future Enhancer agents.

Consequences:

- `prompts/SESSION_START.md` includes `.ai/` in the required reading order.
- `AGENTS.md` requires agents to read `.ai/` before work.
- `.ai/` must mirror operational rules without replacing root canonical documents.
