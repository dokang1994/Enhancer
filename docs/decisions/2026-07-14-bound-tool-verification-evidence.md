# 2026-07-14: Bound Tool Verification Evidence

Status: Accepted Decision

Decision:

Every future Tool result will include structured verification evidence. The first contract limits summaries to 512 characters and retained output tails to 4096 characters. Output exceeding the tail limit must be marked truncated and include a non-blank reference to the complete output. The contract records original output length without implementing persistence.

Tool result status is explicit. An optional exit code supports process-like tools while allowing file or API tools to omit it. When present, success requires exit code zero and failure requires a non-zero code.

Rationale:

Unbounded command output would consume future Agent Context and obscure the most useful recent diagnostics. Keeping a bounded tail plus a reference preserves inspectability without introducing an LLM-specific token model, filesystem policy, or concrete Tool implementation.

Consequences:

- `VerificationEvidence` is mandatory on every `ToolResult`.
- Evidence summaries and output tails are bounded before they can enter Agent Context.
- Truncated output cannot be represented without a reference to the complete evidence.
- The contract does not claim that referenced evidence has been persisted or independently verified.
- Evidence storage, real Tool execution, Agent Loop integration, and the sequential independent verifier remain separate tasks.
