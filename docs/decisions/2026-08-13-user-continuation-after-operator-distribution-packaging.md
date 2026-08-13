# User continuation request on 2026-08-13 after operator distribution packaging

Status: Accepted Decision

## Context

The separate cancellation-trust operator distribution was implemented and freshly
verified in build-owned and JUnit-owned temporary paths. The recorded next boundaries
were real operator installation, permission controls, deployment, signing/release,
cleanup, and external anti-rollback. The user requested continuation.

## Decision

Treat the continuation as authority for the minimum repository-local prerequisite:
specify the real operator installation and filesystem-permission boundary through one
bounded read-only security review, accepted decisions, architecture/contract/state/
roadmap documentation, checkpoints, and fresh document verification.

It grants no authority to implement or run an installer, inspect or mutate real ACLs,
ownership, modes, users, groups, services, registry, PATH, system paths, package-manager
state, or real trust artifacts; invoke maintenance on a real installation; deploy,
sign, publish, release, clean, uninstall, commit, push, merge, tag, or send an external
message.

## Rationale

The package can mutate trust authority when invoked. Principal separation, permission
evidence, publication ordering, and recovery must be accepted before privileged code or
external mutation exists.

## Consequences

- The next implementation may be proposed only inside the accepted three-principal and
  fail-closed publication contract.
- Every privileged, destructive, installation, delivery, and release action remains
  separately authorized.
