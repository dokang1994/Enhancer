# Session Handoff

Continuation context between work sessions. This file holds only what is true right
now and would otherwise be lost with the session.

It does not restate state, evidence, maturity, or delivery history. Current verified
state is in `PROJECT_STATE.md`, the evidence behind it in `docs/verification-log.md`,
the active task in `CURRENT_TASK.md`, and delivery history in `CHANGELOG.md` and
`git log`. Where this file disagrees with any of them, they win.

## Updated At

2026-08-18

## Session-Only State

- This Windows process exposes Java 21 first and Gradle toolchain discovery did not find
  Java 17 automatically. The repository-local verified toolchain is under
  `.tools/jdk17-runtime/jdk-17.0.19+10`; set `JAVA_HOME` explicitly or invoke
  `scripts/gradle.ps1` through an execution-policy bypass when verification is resumed.
- Direct invocation of `scripts/gradle.ps1` is blocked by the current PowerShell
  execution policy. This is a host constraint, not a repository test failure.
- Supplying `--no-daemon` keeps focused and full Gradle invocations returning normally
  through the outer PowerShell wrapper on this host. A fresh full regression completed
  in under two minutes on 2026-08-18 with all result XML regenerated; earlier
  seven-minute timings on this host also occurred, so keep a generous command bound.
- The checkpoint CLI can be invoked directly on this host without Gradle argument
  quoting problems:
  `.tools/jdk17-runtime/jdk-17.0.19+10/bin/java.exe -cp build/classes/java/main
  com.enhancer.cli.EnhancerCli checkpoint-...` (requires a current
  `build/classes/java/main`).
- Passing a quoted multi-word `--args` value through the PowerShell 5.1 wrapper to
  `gradlew.bat run` splits on the inner quotes and fails; use the direct `java -cp`
  form above for any multi-word checkpoint step text.
- `gh` is authenticated on this host as the repository owner; single `gh` API calls
  can transiently time out and succeed on retry.
- An interrupted 2026-08-18 delivery session was continued by a successor session
  after user confirmation; its checkpoint takeover pattern (reread `checkpoint-show`,
  perform the recorded pending step, record with the existing `runId`) worked and is
  recorded in `docs/verification-log.md`.
