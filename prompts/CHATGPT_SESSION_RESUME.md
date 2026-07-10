# ChatGPT Session Resume Prompt

Use this prompt when starting a new ChatGPT session for Enhancer.

## Required Files To Provide

Upload or paste these files first:

1. `CONSTITUTION.md`
2. `AGENTS.md`
3. `PROJECT_STATE.md`
4. `CURRENT_TASK.md`
5. `SESSION_HANDOFF.md`

For architecture or design work, also provide:

1. `ARCHITECTURE.md`
2. `DECISION_LOG.md`

## Prompt

```text
Enhancer 프로젝트를 이어서 진행할 거야.

첨부한 문서를 프로젝트의 최신 상태이자 Single Source of Truth로 사용해.

문서를 먼저 읽고 다음을 알려줘.

1. 현재 상태
2. 직전 세션에서 완료한 작업
3. 오늘 해야 할 작업
4. 설계상 주의점
5. Codex에 전달할 다음 프롬프트

문서와 대화가 충돌하면 문서를 우선해.
```

## Fixed Context Priority

When sources conflict, use this order:

1. Latest Git repository documents
2. Tests and actual code state
3. Accepted decisions in `DECISION_LOG.md`
4. Explicit changes in the current conversation
5. Past conversation memory

## State Labels

Use these labels consistently:

- `Proposal`: an idea that is not yet approved.
- `Accepted Decision`: a decision recorded in `DECISION_LOG.md`.
- `Implemented`: behavior verified in code and tests.

## Operating Flow

```text
ChatGPT
→ design, review, task definition, Codex prompts

Codex
→ file edits, tests, documentation updates, commits

Git
→ durable project memory

SESSION_HANDOFF.md
→ short-term memory between sessions

Human owner
→ final approval and push
```

## Important Constraint

ChatGPT cannot automatically read the user's local Enhancer repository in a new session. The user must provide the required files or summaries from the repository.
