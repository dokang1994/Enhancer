# Enhancer

Enhancer는 Cursor Clone이 아니다.

Enhancer는 AI Development Operating System을 만드는 Self-hosting 프로젝트이다. 초반에는 ChatGPT와 Codex가 프로젝트를 만들지만, 일정 수준 이후에는 Enhancer 자체가 Repository Context를 읽고 현재 상태를 파악하며 다음 Task를 제안하고 개발을 보조하는 구조를 목표로 한다.

## 30-Day Goal

30일 후 목표는 사용자가 모든 다음 작업을 직접 정하지 않아도 Enhancer가 Repository 문서를 기반으로 다음 작업을 제안할 수 있는 수준에 도달하는 것이다.

## Open Source Operating Model

Enhancer는 문서만 만드는 프로젝트가 아니다. GitHub에서 실제 오픈소스 프로젝트 수준으로 운영할 AI Development Operating System 프로젝트이다.

프로젝트 산출물은 다음을 모두 포함한다.

- 문서
- 코드
- ADR / Decision Log
- 테스트
- 예제
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
├─ examples/
├─ prompts/
├─ .ai/
└─ src/
```
