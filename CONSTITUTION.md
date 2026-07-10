# Enhancer Constitution

Version: 1.0.0

---

# Purpose

이 문서는 Enhancer 프로젝트의 최상위 문서이다.

모든 AI Agent(ChatGPT, Codex, Claude, Gemini)는 이 문서를 가장 먼저 읽는다.

이 문서보다 우선하는 것은 없다.

---

# Project Vision

Enhancer는 Cursor Clone 프로젝트가 아니다.

Enhancer는 AI Development Operating System을 만드는 프로젝트이다.

목표는 AI가 프로젝트를 잊지 않고 언제든 이어서 개발할 수 있는 Framework를 만드는 것이다.

---

# Self-Hosting Vision

Enhancer는 Enhancer를 만드는 Self-hosting 프로젝트이다.

초반에는 ChatGPT와 Codex가 프로젝트 생성을 돕지만, 일정 수준 이후에는 Enhancer 자체가 자신의 Repository Context를 읽고, 현재 상태를 파악하고, 다음 Task를 계획하며, 스스로 개발을 보조하는 구조를 목표로 한다.

30일 후의 목표는 사용자가 직접 다음 작업을 정하지 않아도 Enhancer가 Repository 문서를 기반으로 다음 작업을 제안할 수 있는 수준에 도달하는 것이다.

이 목표가 성공하면 Enhancer는 Cursor를 따라 만드는 프로젝트가 아니라, Cursor와 다른 방향의 AI 개발 플랫폼이 된다.

---

# Open Source Operating Vision

Enhancer는 문서만 만드는 프로젝트가 아니다.

Enhancer는 GitHub에서 실제 오픈소스 프로젝트 수준으로 운영되는 AI Development Operating System을 목표로 한다.

프로젝트는 아래 산출물을 모두 포함해야 한다.

- 문서
- 코드
- ADR / Decision Log
- 테스트
- 예제
- Codex Prompt
- Claude Prompt
- GPT Prompt

이 프로젝트는 한 번의 채팅으로 완성하지 않는다.

300페이지 이상의 문서와 실제 구현을 여러 Sprint에 걸쳐 축적한다.

품질을 유지하기 위해 아래 운영 방식을 따른다.

- 문서는 Git으로 버전 관리한다.
- 챕터별 Markdown으로 관리한다.
- ADR은 `DECISION_LOG.md`에 기록한다.
- 중요한 변경은 리뷰 후 승인한다.
- Codex가 구현한다.
- ChatGPT는 Technical Architect, Senior Backend Engineer, AI Agent Researcher, Documentation Lead 역할로 설계와 리뷰를 돕는다.
- 모든 판단 기준은 Chat History가 아니라 Git Repository 문서이다.

최종 목표는 Cursor 80% 구현을 넘어, Enhancer라는 독자적인 AI 개발 플랫폼으로 성장하는 것이다.

---

# Philosophy

대화는 기억이 아니다.

Git Repository가 기억이다.

모든 중요한 정보는 반드시 Repository 안에 존재해야 한다.

AI는 기억하지 않는다.

문서를 읽는다.

---

# Single Source of Truth

모든 프로젝트 상태는 Git Repository를 기준으로 한다.

Chat 내용보다 Repository가 우선한다.

만약 Chat과 Repository가 충돌한다면 Repository를 따른다.

---

# AI Working Rules

AI는 추측하지 않는다.

AI는 반드시 문서를 읽는다.

AI는 작은 Task 단위로 개발한다.

AI는 문서를 항상 최신 상태로 유지한다.

---

# Startup Procedure

새로운 Session이 시작되면 반드시 아래 순서대로 읽는다.

1. `CONSTITUTION.md`
2. `AGENTS.md`
3. `ARCHITECTURE.md`
4. `PROJECT_STATE.md`
5. `ROADMAP.md`
6. `CURRENT_TASK.md`
7. `DECISION_LOG.md`
8. `SESSION_HANDOFF.md`

읽지 않았다면 개발을 시작하지 않는다.

---

# Shutdown Procedure

작업 종료 시 반드시 아래 문서를 최신 상태로 만든다.

- `PROJECT_STATE.md`
- `CURRENT_TASK.md`
- `ROADMAP.md`
- `DECISION_LOG.md`
- `SESSION_HANDOFF.md`

---

# Context Priority

Context는 아래 순서를 따른다.

1. `CURRENT_TASK.md`
2. `SESSION_HANDOFF.md`
3. `DECISION_LOG.md`
4. `PROJECT_STATE.md`
5. `ARCHITECTURE.md`
6. `ROADMAP.md`
7. `README.md`
8. Chat History

---

# Project Goal

Cursor의 핵심 기능 80% 이상을 직접 구현한다.

그러나 Cursor를 복제하는 것이 아니다.

동작 원리를 이해하고 더 좋은 구조를 만드는 것이 목표이다.

---

# Project Scope

Enhancer는 아래 기능을 가진다.

- Agent Loop
- Planner
- Task Queue
- Tool System
- Skill System
- Memory
- Context Builder
- Prompt Builder
- Git Tool
- Terminal Tool
- Search Tool
- File Tool
- RAG
- MCP
- Plugin
- Background Agent
- Multi Agent
- VSCode Extension
- CLI
- Web Dashboard

---

# Out Of Scope

- LLM 개발
- Transformer 구현
- Fine Tuning
- AI 모델 제작

---

# Technology

- Java 17
- Spring Boot 3
- Gradle
- Ollama
- Qwen
- Codex CLI
- VSCode
- Git
- JUnit5
- Mockito

---

# Development Principles

항상 최소 코드, Minimal Code를 우선한다.

항상 SOLID를 지킨다.

항상 Clean Architecture를 지향한다.

DDD는 초기에는 사용하지 않는다.

불필요한 추상화를 금지한다.

---

# Document Driven Development

Enhancer는 문서 기반 개발(Document Driven Development)을 따른다.

새 제안이 생겨도 바로 코드부터 작성하지 않는다.

항상 아래 순서를 따른다.

```text
CONSTITUTION
↓
Architecture
↓
ADR / Decision Log
↓
Task
↓
Implementation
↓
Test
↓
Documentation Update
```

각 단계의 의미는 다음과 같다.

- `CONSTITUTION.md`: 프로젝트 정체성, 철학, 최상위 규칙을 확인한다.
- `ARCHITECTURE.md`: 구조적으로 맞는 방향인지 검토한다.
- `DECISION_LOG.md`: 중요한 결정은 ADR로 기록한다.
- `CURRENT_TASK.md`: 하나의 작은 Task로 범위를 고정한다.
- Implementation: Task 범위 안에서 최소 코드로 구현한다.
- Test: 컴파일과 테스트로 확인한다.
- Documentation Update: 구현 결과와 상태 변화를 문서에 반영한다.

이 순서를 거치지 않은 구현은 완료된 작업으로 보지 않는다.

---

# Repository Structure

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

---

# AI Folder

`.ai` 안에는 AI가 사용하는 문서만 존재한다.

```text
.ai/
├─ constitution.md
├─ workflow.md
├─ coding_rules.md
├─ architecture.md
├─ prompt_rules.md
└─ memory.md
```

---

# Working Process

```text
새 Session
↓
문서 읽기
↓
현재 상태 파악
↓
Task 선정
↓
구현
↓
Test
↓
문서 수정
↓
Commit
↓
Session Handoff 작성
↓
종료
```

---

# Definition of Done

- 컴파일 성공
- 테스트 성공
- 문서 최신화
- Task 완료
- Commit 완료
- Session Handoff 작성

---

# AI Memory Policy

AI는 기억을 신뢰하지 않는다.

항상 Repository를 신뢰한다.

새로운 Session에서는 기억이 아니라 문서를 기준으로 행동한다.

---

# Design Decision Policy

설계가 변경되면 반드시 `DECISION_LOG.md`를 수정한다.

Architecture가 변경되면 `ARCHITECTURE.md`를 수정한다.

Roadmap이 변경되면 `ROADMAP.md`를 수정한다.

Task가 완료되면 `CURRENT_TASK.md`를 수정한다.

---

# Golden Rules

1. 문서를 먼저 읽는다.
2. Task를 이해한다.
3. 작게 개발한다.
4. Test한다.
5. 문서를 수정한다.
6. Commit한다.
7. Session Handoff를 남긴다.

---

# Long Term Goal

Enhancer는 Cursor Clone이 아니다.

Enhancer는 AI Development Operating System이다.

모든 AI가 동일한 프로젝트를 동일한 방식으로 개발할 수 있도록 만드는 것이 최종 목표이다.

최종 목표는 어떤 LLM(Codex, GPT, Claude, Gemini)이라도 같은 프로젝트를 같은 품질로 이어서 개발할 수 있게 하는 AI Development Operating System을 만드는 것이다.

Cursor는 Enhancer의 정체성이 아니다.

Cursor와 같은 개발 도구는 Enhancer 위에서 동작할 수 있는 Application 중 하나로 본다.

Enhancer의 장기 구조는 다음을 포함한다.

- Kernel
- AI Rules
- Prompt Engine
- Context Engine
- Planner
- Task Queue
- Memory
- Tool Manager
- Skill Manager
- Plugin Manager
- Agent Runtime
- Extension
- Dashboard
- SDK
