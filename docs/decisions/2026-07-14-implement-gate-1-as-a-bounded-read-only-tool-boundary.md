# 2026-07-14: Implement Gate 1 As A Bounded Read-Only Tool Boundary

Status: Accepted Decision

Context:

- Delivery Gate 0 verifies Tool result and evidence invariants but executes no Tool.
- Gate 1 must produce one real `ToolResult` without introducing evidence persistence, shell mutation, Git writes, network access, or LLM behavior.
- A path-prefix check alone is insufficient because traversal and symbolic links can escape an approved project root.
- Truncated evidence cannot truthfully reference complete output until Gate 2 provides an EvidenceStore.

Decision:

- Introduce immutable `ToolRequest`, `ExecutionPolicy`, and a minimal `Tool` interface under `com.enhancer.tool`.
- Use an in-process `ToolExecutor` registry with unique names and structured failure conversion.
- Make deny override allow and enforce cancellation before and after invocation plus a positive execution timeout.
- Implement `ReadFileTool` as the only production Tool in Gate 1.
- Require relative paths, real-path containment, regular files, strict UTF-8, and a policy size ceiling no greater than the existing 4096-character evidence boundary.
- Keep deterministic fake Tools in tests as the immediate consumer of the generic executor contract.

Rationale:

This is the smallest real external-boundary slice that exercises policy, execution, result, and evidence together. Strict path and output limits prevent the read-only first Tool from creating hidden authority or unverifiable truncated evidence.

Consequences:

- Allowed temporary project files can produce real successful `ToolResult` values.
- Denial, malformed input, traversal, missing files, invalid UTF-8, cancellation, timeout, and Tool exceptions remain observable failure results.
- The first size ceiling is intentionally conservative and may be raised only after Gate 2 persists full evidence.
- Agent Loop integration, evidence persistence, independent verification, CLI, shell, Git, network, and LLM behavior remain deferred.
