# 10 - Roadmap Guide

## Canonical Source

ROADMAP.md is the canonical live roadmap. This chapter explains how an Agent must use its delivery gates without mistaking contract code for an operational product.

## Capability Maturity

Use these capability states:

1. Specified
2. Contract Verified
3. Integrated
4. Operational
5. Released

These states do not replace the Constitution task lifecycle. A task can be Completed while its capability remains only Contract Verified.

## Current Boundary

Delivery Gate 0 is Contract Verified. Context reading, deterministic planning, loop termination, and Tool evidence records have focused tests.

Enhancer is not yet an integrated Agent runtime. It has no concrete Tool execution, evidence persistence, independent verification, run record, CLI, or LLM call.

## Delivery Sequence

| Gate | Capability | Promotion target |
|---|---|---|
| 1 | Tool Execution Boundary | Real allowlisted read-only ToolResult |
| 2 | Evidence Persistence | Resolvable and integrity-checked complete evidence |
| 3 | Agent Loop and Tool Integration | One real Tool-driven loop transition |
| 4 | Sequential Verification and Run Record | External decision and replayable run |
| 5 | First Operational CLI | End-to-end governed read-only Agent run |
| 6 | Prompt and LLM Boundary | Bounded provider-backed decision step |
| 7 | Skill and Memory Runtime | Progressive loading and governed memory |
| 8 | Extensible Tooling | Git, terminal, MCP, plugin, provenance |
| 9 | User Interfaces | VSCode and web control surfaces |
| 10 | Multi-Agent and Background | Bounded delegation and resumable work |
| 11 | Governed Self-Improvement | Human-approved change with rollback |
| 12 | SDK and Release | Packaged, verified open-source distribution |

## Immediate Next Task

Implement Delivery Gate 1 as a small test-first slice:

- ToolRequest;
- Tool;
- ExecutionPolicy;
- ToolExecutor;
- read-only filesystem Tool;
- deterministic fake Tool;
- request-to-result integration test.

Do not include shell mutation, Git writes, LLM calls, evidence persistence, or the independent verifier in that first task.

## Promotion Checklist

Before promoting a capability:

- confirm its dependency gate has exited;
- identify the real upstream and downstream consumer;
- run focused tests;
- run the gate integration test;
- verify failure and denial behavior;
- update Project State with the exact maturity;
- keep the next gate scoped in Current Task;
- do not call Contract Verified behavior Operational.

## Contract Continuation

New contracts remain allowed after Gate 0. They must be created inside an active delivery gate and name their integration consumer. A speculative contract without an immediate consumer remains a proposal.

## Prompt Book

### Codex Prompt

Read ROADMAP.md and CURRENT_TASK.md. Implement only the active delivery-gate increment. State the capability maturity before and after the task and do not claim integration or operation from unit tests alone.

### Claude Prompt

Review the active gate for dependency order, skeleton expansion, missing consumers, authorization gaps, and insufficient integration evidence.

### GPT Prompt

Explain the current capability maturity, the active delivery gate, its exit criteria, and why later gates must remain deferred.
