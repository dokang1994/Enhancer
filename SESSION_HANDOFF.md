# Session Handoff

Continuation context between work sessions. This file holds only what is true right
now and would otherwise be lost with the session.

It does not restate state, evidence, maturity, or delivery history. Current verified
state is in `PROJECT_STATE.md`, the evidence behind it in `docs/verification-log.md`,
the active task in `CURRENT_TASK.md`, and delivery history in `CHANGELOG.md` and
`git log`. Where this file disagrees with any of them, they win.

## Updated At

2026-08-19

## Session-Only State

- This Windows process exposes Java 21 first and Gradle toolchain discovery did not
  find Java 17 automatically. The repository-local verified toolchain is under
  `.tools/jdk17-runtime/jdk-17.0.19+10`; setting `JAVA_HOME` to it explicitly and
  invoking `gradlew.bat --no-daemon` through the outer shell works on this host,
  as does `scripts/gradle.ps1` through an execution-policy bypass.
- Direct invocation of `scripts/gradle.ps1` is blocked by the current PowerShell
  execution policy. This is a host constraint, not a repository test failure.
- The checkpoint CLI can be invoked directly on this host without Gradle argument
  quoting problems:
  `.tools/jdk17-runtime/jdk-17.0.19+10/bin/java.exe -cp build/classes/java/main
  com.enhancer.cli.EnhancerCli checkpoint-...` (requires a current
  `build/classes/java/main`). Pass `--project-root` with forward slashes; a
  backslashed drive path can lose its separator in some outer shells and resolve
  as a drive-relative path.
- Checkpoint `--artifact` values must name contained regular files; a directory is
  rejected.
- `gh` is authenticated on this host as the repository owner; single `gh` API calls
  can transiently time out and succeed on retry. A continuous-verification run can
  also fail at the infrastructure level with its job stuck `queued` and no logs;
  an unchanged re-run recovered it on 2026-08-19.
