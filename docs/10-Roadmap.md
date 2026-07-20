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

Current gate maturity and the current `Specified - Next` marker are in `PROJECT_STATE.md` and `ROADMAP.md`. This chapter describes the delivery sequence, not the position within it.

What the sequence has built so far, as capability rather than status: governed planning separated from explicit approval, with approved work passing through policy and the read-only Tool boundary into bounded resolvable evidence, a Tool-result-driven loop transition, independent verification, and a replayable RunRecord; the supported local `EnhancerCli` `run` and `replay` commands over that slice; Workspace and Project Brain with the production CLI composing its view and graph; and a reference-only envelope with deterministic in-process delivery covering topic/queue delivery, idempotency, replay, failure isolation, bounded retry, dead letters, cancellation, run-to-completion ordering, and finite non-blocking pending-queue backpressure.

Enhancer has one deliberately narrow read-only scenario end to end. There is no durable or IPC transport, LLM call, or multi-agent runtime.

## Delivery Sequence

| Gate | Capability | Promotion target |
|---|---|---|
| 1 | Tool Execution Boundary | Real allowlisted read-only ToolResult |
| 2 | Evidence Persistence | Resolvable and integrity-checked complete evidence |
| 3 | Agent Loop and Tool Integration | One real Tool-driven loop transition |
| 4 | Sequential Verification and Run Record | External decision and replayable run |
| 5 | First Operational CLI | End-to-end governed read-only Agent run |
| 6 | Workspace and Project Brain | Provenance-preserving development snapshot |
| 7 | Event Bus and IPC | Typed, replayable, transport-neutral messaging |
| 8 | Agent Runtime and Scheduler | Durable Goal-to-Done state machine |
| 9 | Model Gateway and MCP Core | Provider routing and shared protocol capabilities |
| 10 | Skill Engine and Memory Runtime | Composable Skills and governed memory |
| 11 | Tooling and Plugin Marketplace | Traceable external capabilities and extensions |
| 12 | Desktop, CLI, API and Editors | Shared control surfaces with Workspace context |
| 13 | Multi-Agent and Background | Queue-based roles and resumable work |
| 14 | Project Brain Graph and Cloud Sync | Provenance graphs plus optional governed synchronization |
| 15 | Governed Self-Improvement | Human-approved change with rollback |
| 16 | SDK and Release | Packaged, verified open-source distribution |

## Immediate Next Task

Activate the next Delivery Gate 7 task and define the transport-neutral IPC interface over the existing `MessageEnvelope` and delivery semantics. Preserve authorization, provenance, identity, cancellation, replay, ordering, backpressure, and failure contracts without selecting a concrete transport.

Do not add a local-process or remote adapter, threading, persistence, production wiring, network access, LLM calls, scheduling, or multi-agent routing in that interface increment.

V1, V2, and V3 are product milestones layered over delivery gates: V1 is the development experience, V2 is the Agent/workflow platform, and V3 is the AI Kernel and Project Brain operating system. They are not substitutes for verified maturity states.

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
