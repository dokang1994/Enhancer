# Session Start Prompt

Read and execute this prompt from the project root.

## Required Reading

Read `.ai/` first, then read these files in order:

0. `.ai/constitution.md`
0. `.ai/workflow.md`
0. `.ai/coding_rules.md`
0. `.ai/architecture.md`
0. `.ai/prompt_rules.md`
0. `.ai/memory.md`
1. `CONSTITUTION.md`
2. `AGENTS.md`
3. `ARCHITECTURE.md`
4. `PROJECT_STATE.md`
5. `ROADMAP.md`
6. `CURRENT_TASK.md`
7. `DECISION_LOG.md`
8. `SESSION_HANDOFF.md`

## Report

After reading, report:

1. Project goal
2. Current implementation state
3. Current task
4. Relevant design decisions
5. Files that may be changed in this session
6. Risks
7. Work plan

## Constraint

Do not edit code or documents during session start unless the user explicitly asks.

## Note For ChatGPT Sessions

ChatGPT cannot automatically read the user's local repository in a new session. If this prompt is being used outside Codex, the user must provide the required files listed above or use `prompts/CHATGPT_SESSION_RESUME.md`.
