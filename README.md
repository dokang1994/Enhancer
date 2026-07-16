# Enhancer

Enhancer는 Cursor Clone이 아니다.

Enhancer는 AI Development Operating System을 만드는 Self-hosting 프로젝트이다. 초반에는 ChatGPT와 Codex가 프로젝트를 만들지만, 일정 수준 이후에는 Enhancer 자체가 Repository Context를 읽고 현재 상태를 파악하며 다음 Task를 제안하고 개발을 보조하는 구조를 목표로 한다.

## 30-Day Goal

30일 후 목표는 사용자가 모든 다음 작업을 직접 정하지 않아도 Enhancer가 Repository 문서를 기반으로 다음 작업을 제안할 수 있는 수준에 도달하는 것이다.

## Current Development Maturity

Delivery Gates 0 through 4 are **Integrated** across authority-preserving planning, bounded read-only Tool execution, durable integrity-checked evidence, Tool-result-driven Agent Loop transitions, sequential independent verification, and replayable RunRecords. Delivery Gate 5 exposes that narrow vertical slice through an **Operational** local CLI. Gate 6 Workspace and Project Brain Foundation is **Integrated**, and the production `run` path composes the view and graph **Operationally**: every governed run reports its snapshot identity, observations (documents, prior run records, the run target, and Git state), memory freshness, and bounded graph/impact counts. Diagnostics, terminal, and selection observation moved to the Gate 12 interfaces that own those sources. Gate 7 Event Bus and IPC Foundation is **Specified - Next**; its envelope and deterministic in-process delivery semantics, including finite non-blocking pending-queue backpressure, are Contract Verified, with the transport-neutral IPC interface next. The executable context reads `.ai/` before the canonical root documents, and the deterministic Planner is tested against the current Enhancer Delivery Gate Roadmap. Enhancer is not yet the broader event-driven AI Development OS: LLM, scheduler, production messaging, and multi-agent capabilities remain future gates.

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

Exit codes are stable: `0` completed, `2` usage/configuration, `10` verification failed, `20` policy denied, `21` Tool failed, `30` stagnated, `31` maximum iterations, and `70` internal failure. Every `run` that produces a record also reports `workspaceSnapshotId`, `workspaceObservations` (repository documents plus prior run records), a `memoryFreshness` matched/diverged/notObserved summary, and bounded Project Brain graph counts (`graphNodes`, `graphEdges`, `graphDecisions`, `impactExecutions`); replay does not reproduce the snapshot identity because the RunRecord does not store it. Output is capped at 4096 characters and never includes complete file evidence. The example `.enhancer/` runtime directory is Git-ignored and is not removed by Gradle `clean`. For recovery, correct the reported configuration or target, retain the evidence and RunRecord roots, and use `replay` for any printed record reference before retrying with a new run.

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

- `CONSTITUTION.md`: 최상위 헌법
- `AGENTS.md`: AI Agent 작업 규칙
- `ARCHITECTURE.md`: 현재 아키텍처
- `PROJECT_STATE.md`: 실제 구현 상태
- `CURRENT_TASK.md`: 현재 단 하나의 Task
- `ROADMAP.md`: 단계별 진행 계획
- `DECISION_LOG.md`: 승인된 설계 결정
- `SESSION_HANDOFF.md`: 다음 세션 인수인계
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
