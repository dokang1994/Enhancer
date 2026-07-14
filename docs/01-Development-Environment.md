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
powershell -ExecutionPolicy Bypass -File .\scripts\setup-dev.ps1
.\scripts\gradle.ps1 --version
git --version
ollama --version
```

The setup script installs Microsoft OpenJDK 17 into the ignored `.tools/` directory and uses the repository Gradle Wrapper. A global Gradle installation is not required.

For later builds, run:

```powershell
.\scripts\gradle.ps1 test
```

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

- [x] Java 17 available through project-local setup.
- [x] Gradle Wrapper 8.4 available.
- [x] Git status works.
- [x] Tests can run.
- [x] Project documents are updated with actual environment state.
- [ ] Ollama and a Qwen coder model are installed.

## Prompt Book

### Codex Prompt

Inspect the local environment and project files. Do not install tools without approval. Create or update only the minimal build/environment files required by `CURRENT_TASK.md`, then document what was verified and what is missing.

### Claude Prompt

Review the environment plan for portability, missing prerequisites, and unsafe assumptions. Recommend changes that keep setup reproducible for Windows and future CI.

### GPT Prompt

Summarize the environment status for a new session. Explain what is installed, what is missing, and what Codex should do next.
