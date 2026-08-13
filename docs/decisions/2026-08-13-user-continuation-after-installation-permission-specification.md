# User continuation request on 2026-08-13 after installation permission specification

Status: Accepted Decision

## Context

The real-installation permission boundary is specified but no installer, platform
adapter, permission enforcement, or real installation exists. `CURRENT_TASK.md` records
the smallest next task as platform-neutral installation-plan and permission-adapter
contracts with fake-adapter tests only. The user requested continuation.

## Decision

Treat the continuation as authority for that exact repository-local pure contract
increment: immutable plan, principal, artifact, access, phase, policy, adapter, bounded
evidence, finite failure, fake-adapter tests, architecture separation guards, documents,
checkpoints, build output, and fresh verification.

It grants no authority for a real/default/platform adapter or installer; filesystem,
identity, ACL, ownership, mode, permission, staging, publication, durability, probing,
activation, registry, PATH, service, user, group, or deployment effects; launcher/CLI/
operator wiring; broker or audit storage; cleanup, uninstall, rollback, anti-rollback,
signing, release, credentials/private keys, external calls/messages, destructive action,
commit, push, merge, or tag.

## Rationale

Making the accepted matrix and ordered adapter boundary executable with pure types and a
test fake reduces later privileged implementation risk without claiming or exercising
operating-system enforcement.

## Consequences

- Production code may define only a port and validated immutable value contracts; it
  supplies no implementation capable of touching a real filesystem or OS identity.
- Fake-adapter tests are contract evidence, not evidence that Windows, POSIX, or a real
  installation is supported.
- Any platform adapter, installer composition, operator-to-publisher broker, or external
  effect requires a later bounded task and fresh authority.
