# User continuation request on 2026-08-13 after pure installation permission contracts

Status: Accepted Decision

## Context

The platform-neutral plan, fixed matrix, evidence values, and permission-adapter port are
Contract Verified with fake-adapter tests. `CURRENT_TASK.md` records a platform-specific
adapter or installer layer as the next separately authorized boundary. The user requested
continuation on a Windows host, while no real identities or installation fixture are
authorized or provisioned.

## Decision

Authorize the smallest Windows-specific repository-local layer: immutable Windows
security evidence contracts, one injected native-gateway port, one gateway-backed
`WindowsInstallationPermissionAdapter`, fake-gateway tests, architecture guards,
documents, checkpoints, build output, and fresh verification.

The gateway remains unimplemented in production. This decision grants no authority to
call or inspect Windows APIs, PowerShell, commands, native libraries, Java ACL views,
registry, services, users/groups, tokens, SIDs, DACLs, filesystems, reparse points,
volumes, or real paths; mutate permissions or files; install, activate, deploy, clean,
uninstall, roll back, sign, release, commit, push, merge, tag, or send an external message.

## Rationale

Validating platform-specific evidence and fail-closed translation behind an injected
port makes the security contract executable without pretending that a test fake proves
host enforcement or taking authority to inspect or mutate the machine.

## Consequences

- The adapter may be Contract Verified only against fake gateway evidence; Windows
  installation and permission enforcement remain unsupported.
- A production gateway requires a later task naming native technology, exact privileges,
  isolated pre-provisioned identities/fixtures, integration evidence, and recovery.
- POSIX, installer orchestration, persistence, deployment, cleanup, release, and external
  anti-rollback remain separate.
