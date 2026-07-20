# 2026-07-10: Treat Docs As A Multi-Agent Prompt Book

Status: Accepted Decision

Decision:

Each major `docs/` chapter will end with a `Prompt Book` section containing separate prompts for Codex, Claude, and GPT.

Rationale:

Enhancer is developed by multiple AI agents with different strengths. A shared chapter can guide all agents, but each agent needs role-specific instructions to reduce ambiguity.

Consequences:

- Codex prompts focus on implementation and verification.
- Claude prompts focus on architecture and risk review.
- GPT prompts focus on explanation, task framing, and session continuity.
- New chapter documents should include all three prompt types.
