# Enhancer

Enhancer는 Cursor Clone이 아니다.

Enhancer는 AI Development Operating System을 만드는 Self-hosting 프로젝트이다. 초반에는 ChatGPT와 Codex가 프로젝트를 만들지만, 일정 수준 이후에는 Enhancer 자체가 Repository Context를 읽고 현재 상태를 파악하며 다음 Task를 제안하고 개발을 보조하는 구조를 목표로 한다.

## 30-Day Goal

30일 후 목표는 사용자가 모든 다음 작업을 직접 정하지 않아도 Enhancer가 Repository 문서를 기반으로 다음 작업을 제안할 수 있는 수준에 도달하는 것이다.

## Current Development Maturity

Enhancer today covers authority-preserving planning, bounded read-only Tool execution, durable integrity-checked evidence, Tool-result-driven Agent Loop transitions, sequential independent verification, and replayable RunRecords, exposed as a narrow vertical slice through a local CLI. Workspace and Project Brain sit above that: every governed run reports its snapshot identity, observations (documents, prior run records, the run target, and Git state), memory freshness, and bounded graph/impact counts. Diagnostics, terminal, and selection observation belong to the Gate 12 interfaces that own those sources. A reference-only message envelope and deterministic in-process delivery, including finite non-blocking pending-queue backpressure, sit alongside a transport-neutral IPC interface and a local file-spool adapter used by the process-isolated Scheduler worker; there is no supported general-purpose message-bus entry point. The executable context reads `.ai/` before the canonical root documents, and the deterministic Planner is tested against the current Enhancer Delivery Gate Roadmap.

Enhancer is not yet the broader event-driven AI Development OS: LLM, production messaging, and multi-agent capabilities remain future gates. **Current gate maturity is in `PROJECT_STATE.md`** and the evidence behind it in `docs/verification-log.md`; this README does not restate it.

Use `ROADMAP.md` for the canonical promotion path: Specified → Contract Verified → Integrated → Operational → Released. A completed contract task must not be presented as an operational product capability.

Terminology is explicit: **self-hosting development** means Enhancer applies its governed workflow to the Enhancer repository, while **local or hybrid model execution** describes provider routing. Neither capability implies the other. V1-V3 describe product outcomes; dependency-ordered Delivery Gates define implementation and promotion order.

## Enhancer OS Direction

The target platform is event-driven rather than Chat -> Tool -> Stop. Enhancer OS is planned around Desktop, CLI, API, Workspace, Project Brain, Memory, MCP Server/Client, Agent Runtime, Event/Message Bus with IPC adapters, Skill Engine, Plugin Marketplace, Model Router, Scheduler, and governed Cloud Sync.

Planner, Coder, Reviewer, Tester, and Memory roles will communicate through typed queues rather than direct Agent calls. Workspace will provide governed file, Git, diagnostic, terminal-metadata, and selection context. MCP will allow Claude Code, Cursor, VS Code, and other clients to share the same approved Tool and memory layer.

The owner's rough 20-25% foundation estimate is directional planning context, not a verified completion percentage. Current verified maturity remains defined only by `PROJECT_STATE.md` and the delivery gates.

Enhancer evolves through three product milestones:

- **V1 AI Development Experience:** Cursor-level productivity through shared CLI, editor, Desktop, API, and Workspace surfaces.
- **V2 AI Development Platform:** Agent Runtime, workflows, Skills, Memory, MCP, model routing, plugins, and marketplace foundations.
- **V3 AI Operating System:** AI Kernel, Project Brain knowledge graphs, multi-agent scheduling, hybrid local/remote models, plugin ecosystem, governed synchronization, and self-improvement.

Git and canonical Markdown remain durable truth. Project Brain adds rebuildable Decision, Architecture, Dependency, Task, and Execution graphs with provenance and freshness, enabling impact reasoning without replacing their sources.

## Open Source Operating Model

Enhancer는 문서만 만드는 프로젝트가 아니다. GitHub에서 실제 오픈소스 프로젝트 수준으로 운영할 AI Development Operating System 프로젝트이다.

프로젝트 산출물은 다음을 모두 포함한다.

- 문서
- 코드
- ADR / Decision Log
- 테스트
- 명세에 포함된 예시와 실행 가능한 테스트
- Codex Prompt
- Claude Prompt
- GPT Prompt

운영 방식:

- 문서와 코드를 Git으로 버전 관리한다.
- 챕터별 Markdown 명세를 유지한다.
- Sprint 단위로 구현한다.
- ADR을 통해 설계 변경 이유를 보존한다.
- 리뷰 후 승인한다.
- Codex가 구현하고, ChatGPT가 아키텍처 리뷰와 문서 설계를 보조한다.

## Source Of Truth

대화는 기억이 아니다. Git Repository가 기억이다.

모든 AI Agent는 작업 전에 Repository 문서를 읽고, 작업 후에는 문서를 최신 상태로 갱신해야 한다.

## Start A Session

Codex를 프로젝트 루트에서 실행한 뒤 다음 프롬프트를 사용한다.

```text
항상 .ai 폴더를 읽고 시작해.
prompts/SESSION_START.md를 읽고 실행해라.
아직 코드는 수정하지 마라.
```

`.ai/` 폴더는 AI 전용 운영 문서이다.

- `.ai/constitution.md`
- `.ai/workflow.md`
- `.ai/coding_rules.md`
- `.ai/architecture.md`
- `.ai/prompt_rules.md`
- `.ai/memory.md`
- `.ai/skill_rules.md`

## Operational Read-Only CLI

The first supported command reads one UTF-8 file from a governed project, verifies its complete SHA-256 digest independently, persists evidence and a RunRecord, and prints only bounded metadata. `CURRENT_TASK.md` must be `In Progress`, its Task ID must match `--task-id`, and its Allowed Tools must contain `read-file`.

```powershell
$digest = (Get-FileHash -LiteralPath README.md -Algorithm SHA256).Hash.ToLowerInvariant()
.\scripts\gradle.ps1 run --args="run --project-root C:\Enhancer --task-id <active-task-id> --target-path README.md --expected-sha256 $digest --evidence-root C:\Enhancer\.enhancer\evidence --run-record-root C:\Enhancer\.enhancer\run-records"
```

Replay the printed opaque reference without re-executing the Tool:

```powershell
.\scripts\gradle.ps1 run --args="replay --run-record-root C:\Enhancer\.enhancer\run-records --reference run-record/<uuid>"
```

Discover a bounded newest-first list of opaque references before choosing one to replay:

```powershell
.\scripts\gradle.ps1 run --args="run-record-list --run-record-root C:\Enhancer\.enhancer\run-records --limit 12"
```

`--limit` is required and must be from `1` through `48`. The status is `AVAILABLE` when
at least one reference is returned and `EMPTY` otherwise; both exit `0`. A missing
RunRecord root is empty and is not created. Listing never resolves or reinterprets a
record, so use `replay` on a returned reference to validate integrity and inspect its
bounded task, policy, verification, and stop metadata.

Exit codes are stable: `0` completed, `2` usage/configuration, `10` verification failed, `20` policy denied, `21` Tool failed, `30` stagnated, `31` maximum iterations, `40` terminal Scheduler work failure, and `70` internal failure. Every `run` that produces a record also reports `workspaceSnapshotId`, `workspaceObservations` (repository documents plus prior run records), a `memoryFreshness` matched/diverged/notObserved summary, and bounded Project Brain graph counts (`graphNodes`, `graphEdges`, `graphDecisions`, `impactExecutions`); replay does not reproduce the snapshot identity because the RunRecord does not store it. Output is capped at 4096 characters and never includes complete file evidence. The example `.enhancer/` runtime directory is Git-ignored and is not removed by Gradle `clean`. `--evidence-root` and `--run-record-root` are explicit caller inputs and are not confined to the project root; each store refuses a symbolic-link root and only creates new UUID-named entries, so it can add files to the directory you name but cannot overwrite what is already there. For recovery, correct the reported configuration or target, retain the evidence and RunRecord roots, and use `replay` for any printed record reference before retrying with a new run.

## Submit Durable Scheduler Work

`scheduler-submit` persists one immutable submission intent and admits its exact
dependency-free work to a durable queue. It derives the approved task revision, allowed
Tools, and Workspace snapshot from the governed project, but every identity, occurrence
time, queue bound, capability, target, and expected digest remains an explicit input:

```powershell
.\scripts\gradle.ps1 run --args="scheduler-submit --project-root C:\Enhancer --submission-root C:\Enhancer\.enhancer\submissions --queue-root C:\Enhancer\.enhancer\queue --task-id <active-task-id> --queue-id <canonical-queue-uuid> --max-work-items 256 --required-capability read-file --message-id <canonical-message-uuid> --correlation-id <correlation-id> --logical-run-id <logical-run-id> --producer local-operator --occurred-at 2026-07-22T00:00:00Z --target-path README.md --expected-sha256 <lowercase-sha256>"
```

The bounded status is `ADMITTED` when the queue revision advances and `REPLAYED` when
the exact submission is already present. Preserve and reuse every argument to recover an
interrupted submission. Reusing a message identity with changed content or naming a task
that does not match the active repository task exits `2` without admitting changed work.
The command does not execute the work; invoke `scheduler-cycle` separately.

## Inspect Durable Scheduler Queue Status

`scheduler-status` reads one persisted queue snapshot without recovering or changing it:

```powershell
.\scripts\gradle.ps1 run --args="scheduler-status --queue-root C:\Enhancer\.enhancer\queue --queue-id <canonical-queue-uuid> --limit 12"
```

`--limit` must be from `1` through `48`. The command reports complete counts for
`READY`, `BLOCKED`, `ACTIVE`, `VERIFIED`, and `FAILED` work plus an admission-ordered
bounded identity/state prefix. It exits `0` with `AVAILABLE` or `EMPTY`; a missing queue
is configuration exit `2`, and corrupt state is internal exit `70`.

Inspection never calls queue recovery, creates no missing root, and reads no runtime,
effect, checkpoint, RunRecord, submission, or invocation store. `ACTIVE` means only that
the persisted queue snapshot contains an active slot; it does not prove that a worker is
currently alive. Use the execution commands and their retained roots for recovery.

## Recover One Durable Scheduler Cycle

`scheduler-cycle` recovers an already-existing durable Scheduler queue and runs exactly
one process-isolated Worker cycle. It does not create a queue, submit work, or poll. All
storage roots, identities, retry bounds, and durations are explicit caller inputs:

```powershell
.\scripts\gradle.ps1 run --args="scheduler-cycle --project-root C:\Enhancer --queue-root C:\Enhancer\.enhancer\queue --queue-id <canonical-queue-uuid> --runtime-root C:\Enhancer\.enhancer\runtime --external-effect-root C:\Enhancer\.enhancer\effects --cycle-checkpoint-root C:\Enhancer\.enhancer\scheduler-checkpoint --evidence-root C:\Enhancer\.enhancer\evidence --run-record-root C:\Enhancer\.enhancer\run-records --invocation-root C:\Enhancer\.enhancer\invocations --owner-id local-scheduler --max-attempts 2 --lease-millis 300000 --process-timeout-millis 30000"
```

The bounded result status is `IDLE`, `VERIFIED_COMPLETED`, or `FAILED`. Idle and
verified completion exit `0`; terminal failed work exits `40`. Missing queue state or
malformed input exits `2`, while corrupt state and unexpected execution/storage errors
exit `70`. Preserve every named root to resume a checkpointed cycle after interruption.

## Drain Ready Scheduler Work

`scheduler-drain` uses the same recovery inputs as `scheduler-cycle` but invokes
sequential cycles in the foreground until it observes idle work, a failed disposition,
or the explicit cycle limit. It does not create a queue, submit work, wait for future
work, or poll:

```powershell
.\scripts\gradle.ps1 run --args="scheduler-drain --project-root C:\Enhancer --queue-root C:\Enhancer\.enhancer\queue --queue-id <canonical-queue-uuid> --runtime-root C:\Enhancer\.enhancer\runtime --external-effect-root C:\Enhancer\.enhancer\effects --cycle-checkpoint-root C:\Enhancer\.enhancer\scheduler-checkpoint --evidence-root C:\Enhancer\.enhancer\evidence --run-record-root C:\Enhancer\.enhancer\run-records --invocation-root C:\Enhancer\.enhancer\invocations --owner-id local-scheduler --max-attempts 2 --lease-millis 300000 --process-timeout-millis 30000 --max-cycles 8"
```

`--max-cycles` must be from `1` through `4096`. The bounded stop status is `IDLE`,
`FAILED`, or `LIMIT_REACHED`; `cyclesInvoked` includes the final idle or failed cycle.
The command continues only after `VERIFIED_COMPLETED`. `FAILED` exits `40`; `IDLE` and
`LIMIT_REACHED` exit `0`. `LIMIT_REACHED` does not prove that the queue is empty, so
another drain requires a new explicit operator invocation. Preserve every named root
when reinvoking after interruption so the existing per-cycle checkpoint can recover.

## Run The Explicit Two-Command Scheduler Workflow

The supported operator workflow keeps submission separate from either execution command.
There is no submission-and-execution wrapper and no polling loop:

1. Choose and retain every `scheduler-submit` argument before the first invocation.
2. Invoke `scheduler-submit` and stop if it exits nonzero. `ADMITTED` and `REPLAYED` both
   mean the exact work is durably present; neither status executes it.
3. After separately deciding to execute, invoke `scheduler-cycle` for exactly one cycle
   or `scheduler-drain` for a bounded sequence, using the same `--project-root`,
   `--queue-root`, and `--queue-id`. Preserve every execution-specific root for recovery.
4. Interpret the execution result independently. A cycle or another bounded drain occurs
   only through another explicit operator action.

The queue root and identity are the handoff between the commands. Submission roots and
all submission identities/time must be retained for exact replay. Runtime, effect,
checkpoint, evidence, RunRecord, and invocation roots belong to cycle recovery and must
not be replaced after an interrupted cycle. Exact submission replay also requires the
governed repository documents used to derive the task revision and Workspace snapshot to
remain unchanged; changed authority or snapshot content under the same message identity
fails closed.

| Observed state | Operator action |
|---|---|
| Submission interrupted or produced no trusted result | Reinvoke `scheduler-submit` with every original argument against the unchanged governed project. Accept `ADMITTED` or `REPLAYED`; do not invoke the cycle while submission remains an error. |
| `ADMITTED` or `REPLAYED` | The work is durable but not necessarily executed. Use `scheduler-status` when queue inspection is needed, then invoke `scheduler-cycle` or `scheduler-drain` separately only when execution is intended. |
| Cycle or drain interrupted or exits `70` | Preserve the queue and every execution root. Use `run-record-list` against the retained RunRecord root and `replay` a selected reference when evidence exists, correct only the reported environmental problem, and reinvoke the same execution command so the worker checkpoint can recover. Do not resubmit work to repair execution. |
| Cycle reports `VERIFIED_COMPLETED` | The WorkItem is terminally verified. Exact submission replay remains a no-op, and a later cycle for an otherwise empty queue reports `IDLE`. |
| Cycle reports `FAILED` and exits `40` | The WorkItem is terminally failed. Discover retained evidence with `run-record-list`, inspect a selected record with `replay`, and retain runtime state; resubmitting the same identity is not a retry. New work requires separately approved inputs and a new message identity. |
| Cycle reports `IDLE` | No ready work was executed. Do not loop automatically; use `scheduler-status` to distinguish an empty queue from blocked work and verify the queue root/identity and preceding submission result. |
| Drain reports `LIMIT_REACHED` | The requested number of cycles completed, but ready work may remain. Use `scheduler-status`, then explicitly invoke another cycle or drain only when intended. |
| Drain reports `FAILED` and exits `40` | The first terminal work failure stopped the drain. Use `run-record-list` and `replay` to inspect retained RunRecord evidence alongside runtime state before deciding whether separately approved new work is required. |
| Drain reports `IDLE` | The final cycle found no ready work. The command does not wait for later submissions or blocked dependencies to become ready. |

## Submit Generated-Input Scheduler Work

`scheduler-submit-generated` is a replay-safe variant of `scheduler-submit` for operators
who prefer to retain one identity instead of the whole replay tuple. It takes a single
caller-retained canonical submission UUID and derives the queue, correlation, and
logical-run identities from it through fixed versioned domain-separated transforms, and it
generates the occurrence time from the clock on first use. No explicit queue, message,
correlation, logical-run identity, or occurrence time is supplied, and the explicit
`scheduler-submit` command is unchanged:

```powershell
.\scripts\gradle.ps1 run --args="scheduler-submit-generated --project-root C:\Enhancer --submission-root C:\Enhancer\.enhancer\submissions --queue-root C:\Enhancer\.enhancer\queue --task-id <active-task-id> --submission-id <canonical-submission-uuid> --max-work-items 256 --required-capability read-file --producer local-operator --target-path README.md --expected-sha256 <lowercase-sha256>"
```

The bounded status is `ADMITTED` when the queue revision advances and `REPLAYED` when the
exact submission is already present. The output prints the generated `queueId`,
`correlationId`, `logicalRunId`, `occurredAt`, and `workspaceSnapshotId` for auditing; pass
the printed `queueId` to `scheduler-cycle` or `scheduler-drain`. On the first invocation
the occurrence time and governed repository snapshot are captured, the immutable
submission manifest is persisted, and the queue is created and the work admitted. On any
later invocation the manifest is resolved before the clock or repository context is
consulted, so the exact occurrence time and envelope are reused; changing any caller-owned
intent (task, capacity, capability, producer, target, or digest) under the same submission
UUID exits `2` without admitting changed work.

A real-repository smoke run reads `README.md` and observes
`ADMITTED -> VERIFIED_COMPLETED -> REPLAYED -> IDLE` with one retained manifest, one
RunRecord, and no duplicate execution. Generated-input recovery follows the same handoff as
the explicit workflow, differing only in what the operator preserves:

| Observed state | Operator action |
|---|---|
| Submission interrupted before a trusted result | Reinvoke `scheduler-submit-generated` with the same submission UUID and caller-owned intent against the unchanged governed project. If the interruption preceded manifest persistence, a fresh occurrence time is generated safely because no durable work was created; if it followed persistence, the stored time and envelope are reused. Accept `ADMITTED` or `REPLAYED`. |
| `ADMITTED` or `REPLAYED` | The work is durable but not necessarily executed. Take the printed `queueId`, use `scheduler-status` when inspection is needed, and invoke `scheduler-cycle` or `scheduler-drain` separately only when execution is intended. |
| Conflicting intent exits `2` | The submission UUID already names durable work with different caller-owned intent. Do not reuse it for changed work; choose a new submission UUID. |
| Cycle `VERIFIED_COMPLETED` then a later cycle `IDLE` | The generated work is terminally verified; exact submission replay stays a no-op and the empty queue reports `IDLE`. |

Retain only the submission UUID, the caller-owned intent, and the submission/queue roots for
exact replay; the cycle-specific roots follow the same recovery rules as the explicit
workflow. Submission remains separate from execution, and there is no wrapper or polling.

## Development Session Checkpoints

Forced termination recovery does not depend on `SESSION_HANDOFF.md` being updated at
session close. One machine-written checkpoint lives under the Git-ignored
`.enhancer/session-checkpoint/` directory and records only execution position, evidence
references, and artifact identities bound to the active task contract.

Use the application commands `checkpoint-start`, `checkpoint-record`,
`checkpoint-show`, and `checkpoint-clear`. `checkpoint-start` records a pending atomic
step and refuses to replace an active run. `checkpoint-record` requires the current
`runId` and `expected-revision`, accepts repeatable `--artifact` and `--evidence`
options, and records `STEP_PENDING`, `STEP_SUCCEEDED`, `STEP_FAILED`, or `STABLE`.
`checkpoint-show` is the first recovery command in a new session. `checkpoint-clear`
works only for a stable checkpoint whose task contract and artifact manifest still
match.

Example through the Gradle application runner:

```powershell
.\gradlew.bat run --args="checkpoint-show --project-root C:\Enhancer"
```

The checkpoint is not verification, task, maturity, or delivery authority. Resume still
requires canonical document loading, `git status`/diff inspection, and fresh applicable
tests.

## Development Setup

Windows에서 관리자 권한이나 전역 Gradle 설치 없이 개발 환경을 구성한다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup-dev.ps1
```

이 명령은 공식 Microsoft OpenJDK 17을 Git에서 제외된 `.tools/`에 구성하고 Gradle Wrapper 8.4로 전체 테스트를 실행한다. 이후에는 다음 명령을 사용한다.

```powershell
.\scripts\gradle.ps1 test
```

## Resume In A New ChatGPT Session

새 ChatGPT 세션은 도원님 PC의 로컬 `Enhancer` 저장소를 자동으로 읽을 수 없다.

따라서 새 세션을 시작할 때는 [prompts/CHATGPT_SESSION_RESUME.md](prompts/CHATGPT_SESSION_RESUME.md)를 사용한다.

필수로 전달할 파일:

- `CONSTITUTION.md`
- `AGENTS.md`
- `PROJECT_STATE.md`
- `CURRENT_TASK.md`
- `SESSION_HANDOFF.md`

설계 작업 시 추가:

- `ARCHITECTURE.md`
- `DECISION_LOG.md`

## Close A Session

작업을 종료하기 전에 다음 프롬프트를 사용한다.

```text
prompts/SESSION_CLOSE.md를 읽고 세션을 종료해라.
임의로 push하지 마라.
```

## Core Documents

각 사실은 정확히 하나의 문서가 소유한다. 다른 문서는 그 사실을 다시 적지 않고 참조한다.

- `CONSTITUTION.md`: 최상위 헌법
- `AGENTS.md`: AI Agent 작업 규칙
- `ARCHITECTURE.md`: 현재 아키텍처 (성숙도는 기술하지 않음)
- `PROJECT_STATE.md`: 검증된 현재 구현 상태, 성숙도 판단, 알려진 한계
- `docs/verification-log.md`: 그 상태의 근거가 되는 증분별 검증 기록 (append-only)
- `CURRENT_TASK.md`: 현재 단 하나의 Task와 다음 Task
- `ROADMAP.md`: 단계별 진행 계획
- `DECISION_LOG.md`: 승인된 설계 결정의 인덱스 (제목 + 승인 상태)
- `docs/decisions/`: 결정별 파일 하나씩 — 맥락·결정·근거·결과. 제목 문자열이 곧 결정의 식별자
- `SESSION_HANDOFF.md`: 지금 사실이면서 세션과 함께 사라질 내용만
- `CHANGELOG.md`: 변경 기록

## Codex-Ready Specification Documents

`docs/` 아래 문서는 설치 문서가 아니라 Codex, Claude, GPT에게 그대로 전달할 수 있는 Prompt형 프로젝트 명세서이다.

권장 진행 순서:

1. `docs/00-Project-Overview.md`
2. `docs/01-Development-Environment.md`
3. `docs/05-Memory.md`
4. `docs/11-Architecture.md`
5. `docs/02-Agent-Loop.md`
6. `docs/03-Tool-System.md`
7. `docs/04-Skill-System.md`
8. `docs/06-Planner.md`
9. `docs/07-MCP.md`
10. `docs/08-Multi-Agent.md`
11. `docs/09-Background-Agent.md`
12. `docs/10-Roadmap.md`

각 문서는 목표, 설계 기준, 구현 범위, 테스트 기준, Codex Prompt를 포함한다.

또한 각 챕터 끝에는 `Prompt Book` 섹션을 두고 `Codex Prompt`, `Claude Prompt`, `GPT Prompt`를 분리해서 제공한다.

## RFC Documents

대형 설계 주제는 `docs/rfcs/` 아래 RFC 스타일로 관리한다.

- `RFC-0001`: Constitution
- `RFC-0002`: AI Behavior Specification
- `RFC-0003`: Prompt Contract
- `RFC-0004`: Context Builder
- `RFC-0005`: Planner
- `RFC-0006`: Tool Specification
- `RFC-0007`: Skill Specification
- `RFC-0008`: Memory Specification
- `RFC-0009`: Multi Agent
- `RFC-0010`: AI Operating System
- `RFC-0011`: Plugin SDK
- `RFC-0012`: Self Improvement

RFC는 장기 설계 의도와 참조 관계를 보존하기 위한 문서이다.

## Shared Prompts

- `prompts/coding-rules.md`: 구현 Agent 공통 규칙
- `prompts/architect-rules.md`: 설계 검토 규칙
- `prompts/review-rules.md`: 코드 리뷰 규칙
- `prompts/SESSION_START.md`: 세션 시작 절차
- `prompts/IMPLEMENT_TASK.md`: 구현 절차
- `prompts/REVIEW_TASK.md`: 리뷰 절차
- `prompts/SESSION_CLOSE.md`: 세션 종료 절차
- `prompts/CHATGPT_SESSION_RESUME.md`: 새 ChatGPT 세션 재개 절차

## Required Repository Structure

```text
Enhancer/
├─ README.md
├─ CONSTITUTION.md
├─ AGENTS.md
├─ ARCHITECTURE.md
├─ PROJECT_STATE.md
├─ CURRENT_TASK.md
├─ ROADMAP.md
├─ DECISION_LOG.md
├─ SESSION_HANDOFF.md
├─ CHANGELOG.md
├─ docs/
├─ prompts/
├─ skills/
├─ .ai/
└─ src/
```
