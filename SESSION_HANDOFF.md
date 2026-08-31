# Session Handoff

Continuation context between work sessions. This file holds only what is true right
now and would otherwise be lost with the session.

It does not restate state, evidence, maturity, task, or delivery history. Those belong
to `PROJECT_STATE.md`, `docs/verification-log.md`, `CURRENT_TASK.md`, `CHANGELOG.md`,
and git.

## Updated At

2026-08-31

## Session-Only State

- This Windows host exposes Java 21 first and Gradle toolchain discovery does not find
  Java 17 automatically. The repository-local verified toolchain is under
  `.tools/jdk17-runtime/jdk-17.0.19+10`; `scripts/gradle.ps1` selects it and works
  through the required PowerShell execution-policy bypass.
- If Gradle argument quoting becomes unreliable, the checkpoint CLI can be invoked
  with the repository-local Java 17 binary and `build/classes/java/main`. Use a forward-
  slashed absolute `--project-root`; a backslashed drive path can become drive-relative
  in some outer shells.
- Checkpoint `--artifact` values must name contained regular files; directories are
  rejected.
