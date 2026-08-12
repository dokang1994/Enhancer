# 2026-08-12: Expose Maintenance Through A Distinct Typed Operator Launcher

Status: Accepted Decision

## Context

The installed cancellation-trust maintenance state machine is Contract Verified but has
no separately executable operator entry point. Adding it to `EnhancerCli` or interpreting
untyped `IOException` text would collapse boundaries: runtime would name maintenance,
and exit behavior would depend on mutable English messages rather than the failure
contract.

The repository's application plugin already owns the runtime `EnhancerCli` main class.
Installing a second launcher into distributions or executing against a real installation
would be an external/deployment change beyond this task.

## Decision

- Add `CancellationTrustMaintenanceOperator` in `com.enhancer.maintenance` with its own
  `main` and testable `execute` surface. It accepts exactly operation `install` or
  `rotate`, absolute normalized `--application-jar`, absolute normalized
  `--candidate-policy`, and ROTATE-only lowercase
  `--expected-current-metadata-sha256`.
- Reject missing/empty/duplicate/unknown options, unexpected positionals, INSTALL CAS,
  ROTATE without CAS, and every trust-pin/runtime/permission/cleanup override before
  state-machine invocation.
- Add a fixed `cancellationTrustMaintenance` Gradle `JavaExec` task whose main class is
  only the operator launcher. Keep `application.mainClass` equal to
  `com.enhancer.cli.EnhancerCli`; create no distribution/start-script installation in
  this task.
- Introduce `CancellationTrustMaintenanceException` with finite reason and category
  values. Configuration covers invalid application/candidate/trust/current binding;
  refusal covers existing INSTALL binding, active lock contention, stale CAS, and exact
  digest-path corruption; durability covers atomic/candidate/publication/post-switch I/O
  failures. The launcher maps configuration to `2`, refusal to `20`, durability to `70`,
  and successful `INSTALLED`, `ROTATED`, or `EXACT_REPLAY` to `0` without inspecting
  exception messages.
- Output one bounded line per public result field: status, public policy path, policy
  digest, and metadata digest. Errors contain only bounded category and reason tokens,
  never stack traces, exception detail, policy/proof bytes, secrets, environment, or
  scanned filesystem content.
- Verify all executions under isolated test-owned installation trees. Do not invoke the
  launcher against any real installation or claim permission/deployment operation.

## Rationale

A distinct main preserves operator/runtime separation. Typed finite errors make refusal
and failure automation stable and reviewable, while a Gradle-only task proves a concrete
entry point without silently installing or deploying it.

## Consequences

- The repository can execute operator maintenance explicitly in a separately selected
  JVM entry point after the caller independently supplies deployment authority.
- Existing state-machine callers receive a more precise checked exception subtype.
- Packaging an installed launcher, choosing an operator identity, real-install
  invocation, permissions, deployment, cleanup, and privileged anti-rollback remain
  separately authorized.
