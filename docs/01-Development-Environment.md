# 01 - Development Environment

## Codex Prompt

Set up the minimal development environment for Enhancer.

Do not install tools automatically unless the user asks. Detect what exists, document what is missing, and create project files that can be used once dependencies are available.

## Target Environment

- Java 17
- Gradle
- Spring Boot 3
- Git
- VSCode
- Codex CLI
- Ollama
- Qwen coder model

## Required Checks

Run or document these checks:

```powershell
java -version
gradle --version
git --version
ollama --version
```

If Gradle is unavailable, prefer adding a Gradle wrapper later instead of assuming global Gradle.

## Project Bootstrap Target

The first buildable Java project should include:

- `settings.gradle`
- `build.gradle`
- `src/main/java/...`
- `src/test/java/...`
- JUnit5
- Mockito

## First Executable Goal

Create a minimal "Hello Agent" entry point only after the context reader task is completed or explicitly selected.

## Checklist

- [ ] Java 17 available.
- [ ] Gradle or Gradle wrapper available.
- [ ] Git status works.
- [ ] Tests can run.
- [ ] Project documents are updated with actual environment state.

## Prompt Book

### Codex Prompt

Inspect the local environment and project files. Do not install tools without approval. Create or update only the minimal build/environment files required by `CURRENT_TASK.md`, then document what was verified and what is missing.

### Claude Prompt

Review the environment plan for portability, missing prerequisites, and unsafe assumptions. Recommend changes that keep setup reproducible for Windows and future CI.

### GPT Prompt

Summarize the environment status for a new session. Explain what is installed, what is missing, and what Codex should do next.
