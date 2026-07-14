# 02 - Agent Loop

## Codex Prompt

Implement only the Agent Loop slice.

Do not implement tools, LLM integration, memory, or planner in this task unless `CURRENT_TASK.md` explicitly says so.

## Goal

The Agent Loop coordinates repeated work until a task is complete or a maximum iteration count is reached.

The preceding Assisted Development Loop slice is intentionally smaller: it performs one read-and-plan pass and reports either `PROPOSAL_AVAILABLE` or `ACTIVE_TASK_PRESERVED`. It is orchestration groundwork, not the repeated Agent Loop described below.

## Concepts

- `AgentLoopStep`: caller-supplied deterministic state transition.
- `AgentLoop`: owns repeated execution and termination safety.
- `AgentLoopState`: immutable status and progress key.
- `AgentLoopStatus`: running, completed, or failed state.
- `AgentLoopStopReason`: completed, failed, maximum iterations, or stagnated.
- `AgentLoopResult`: latest state, stop reason, and executed iteration count.
- `AgentRunState`: approved task, pending Tool request, last Tool result, loop status, and progress key.
- `AgentRunController`: executes a pre-authorized request and maps its result into the next run state.
- `ToolFailureClassifier`: external deterministic retry-or-terminal decision.
- `ApprovedTaskReader`: derives active task identity, approval evidence, and Tool scope from repository context.

## Rules

- Use a `while` based loop.
- Limit max iterations to 20.
- Stop on task completion.
- Stop on task failure.
- Stop on max iteration.
- Default to 20 maximum iterations.
- Stop after 3 consecutive executed steps return the same progress key by default.
- Check completed or failed status before classifying a step as stagnated.
- Prefer maximum iteration when its ceiling and stagnation threshold coincide.
- Count only executed steps as iterations; an initially terminal state reports zero.
- Keep implementation deterministic and testable.
- Stop successful Tool execution at `AWAITING_VERIFICATION`; only the later independent verifier may enable task completion.
- Keep retry classification outside Tool implementations and do not infer it from diagnostic text.
- Never let the controller create approvals, expand execution policy, or authorize Git, shell, network, or other external action.
- Use structured Tool failure codes for retry decisions; do not parse diagnostic summaries.
- Treat evidence content digests as progress identity and exclude opaque storage references.
- Allow public callers to create only a ready Agent run state from an approved in-scope request.

## Gate 3 Tool Integration

```text
Repository Context
-> externally approved task and ToolRequest
-> AgentRunController
-> ToolExecutor under immutable ExecutionPolicy
-> ToolResult and bounded evidence
-> AWAITING_VERIFICATION | FAILED | retry
-> bounded stop reason
```

On retry, the pending request is preserved and canonical request/result content produces the progress key. Identical results therefore reach `STAGNATED`; changing results reset stagnation. A successful result is not an independent verification decision.

## Candidate Classes

```text
AgentLoopStep
AgentLoop
AgentLoopState
AgentLoopStatus
AgentLoopStopReason
AgentLoopResult
```

## Sequence

```text
AgentLoopState
↓
AgentLoop
↓
AgentLoopStep
↓
AgentLoopStatus and progress check
```

## Tests

Cover:

- completes before max iteration
- stops at max iteration
- failed task stops the loop
- unchanged progress stops as stagnated
- terminal status wins over stagnation
- iteration count is reported
- a real read-only Tool result reaches `AWAITING_VERIFICATION`
- terminal Tool failure cannot be completion
- repeated identical retryable failure reaches stagnation
- a denied Tool cannot invoke itself or broaden policy

## Out Of Scope

- Prompt building
- LLM calls
- Multi-agent routing
- Independent verification and task completion
- Git, shell, network, or browser mutation
- RunRecord persistence and CLI wiring

## Prompt Book

### Codex Prompt

Implement only the Agent Loop task if `CURRENT_TASK.md` selects it. Keep the loop deterministic, limit iterations to 20, add focused unit tests, and do not implement tools or LLM calls.

### Claude Prompt

Review the Agent Loop design for termination safety, state clarity, and testability. Flag any hidden coupling to tools, memory, planner, or LLM integration.

### GPT Prompt

Explain the Agent Loop concept and produce a small task plan that preserves the out-of-scope boundaries.
