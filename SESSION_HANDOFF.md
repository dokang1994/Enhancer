# Session Handoff

Continuation context between work sessions. This file holds only what is true right
now and would otherwise be lost with the session.

It does not restate state, evidence, maturity, or delivery history. Current verified
state is in `PROJECT_STATE.md`, the evidence behind it in `docs/verification-log.md`,
the active task in `CURRENT_TASK.md`, and delivery history in `CHANGELOG.md` and
`git log`. Where this file disagrees with any of them, they win.

## Updated At

2026-08-12

## Session-Only State

- This Windows process exposes Java 21 first and Gradle toolchain discovery did not find
  Java 17 automatically. The repository-local verified toolchain is under
  `.tools/jdk17-runtime/jdk-17.0.19+10`; set `JAVA_HOME` explicitly or invoke
  `scripts/gradle.ps1` through an execution-policy bypass when verification is resumed.
- Direct invocation of `scripts/gradle.ps1` is blocked by the current PowerShell
  execution policy. This is a host constraint, not a repository test failure.
- The complete process-isolated regression suite can exceed five minutes on this host;
  use a command bound of at least fifteen minutes for a claim-bearing full build.
- On this host a focused `scripts/gradle.ps1 test` invocation without `--no-daemon`
  produced complete test XML but did not return through the outer PowerShell wrapper.
  Supplying `--no-daemon` returned normally after stopping its single-use daemon.
- Windows atomic file moves and overlapping `FileLock` contention are supported by this
  host's test filesystem; maintenance verification used only JUnit-owned temporary
  installation trees and did not address any real installation path.
- The direct JVM operator subprocess tests preserve exit codes `2`, `20`, and `70` on
  this host. The Gradle `JavaExec` task selects the operator main but Gradle may translate
  a nonzero child exit to its own task-failure code; the emitted `exitCode` token retains
  the operator classification.
