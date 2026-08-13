# 2026-08-13: Package Cancellation Trust Maintenance As A Separate Distribution

Status: Accepted Decision

## Context

`CancellationTrustMaintenanceOperator` is a distinct typed main with strict explicit
arguments, bounded output, and direct-JVM exit classification. The repository-local
`cancellationTrustMaintenance` `JavaExec` task proves selection but is not an installed
launcher. The existing Gradle application distribution belongs to `EnhancerCli`; adding
the operator to that launcher's command grammar or replacing its main would collapse
runtime and maintenance authority.

Packaging is still code assembly, not authority to invoke maintenance on a real
installation, provision operator identity, mutate permissions, deploy, or release.

## Decision

- Keep `application.mainClass` and the default application distribution bound only to
  `com.enhancer.cli.EnhancerCli`.
- Add one custom Gradle distribution whose base name and sole launcher name are exactly
  `enhancer-cancellation-trust-maintenance`. Its launcher main is exactly
  `com.enhancer.maintenance.CancellationTrustMaintenanceOperator`.
- Generate Unix and Windows scripts through Gradle's `CreateStartScripts` support and
  package those scripts with the project runtime classpath under `lib/`. Do not hand-code
  argument parsing, Java discovery, classpath construction, or exit translation in a
  second wrapper.
- Make the distribution depend on the generated operator scripts and runtime JARs and
  expose its conventional install, ZIP, and TAR assembly tasks without changing the
  default runtime distribution.
- The generated launcher supplies no default maintenance operation, application path,
  candidate path, metadata digest, trust path, pin, environment-derived authority, or
  permission behavior. It only forwards explicit caller arguments to the typed operator
  main.
- Test the installed layout and launcher as a child process. Every INSTALL/ROTATE target
  is a JUnit-owned temporary fake application installation; build output is used only as
  the source distribution. Verify success and direct exit `2`, `20`, and `70`, bounded
  output, exact replay, classpath/layout, and default-runtime distribution separation.

## Rationale

A separate generated launcher makes the operator executable without turning
maintenance into a runtime command. Gradle-generated scripts reuse the project's
standard Java/classpath conventions and avoid an unreviewed wrapper protocol. Explicit
temporary-tree subprocess tests prove the packaged boundary rather than only inspecting
task declarations.

## Consequences

- Operators may later install this distribution through separately authorized
  deployment work and then invoke it with the existing explicit maintenance contract.
- The repository can assemble install, ZIP, and TAR artifacts locally, but this decision
  makes no signed, published, deployed, or Released claim.
- Real installation selection, operator principal and credentials, filesystem
  protections, installer/package-manager integration, PATH/service registration,
  cleanup, privileged anti-rollback, signing, SBOM, publication, and release remain
  separate work.
