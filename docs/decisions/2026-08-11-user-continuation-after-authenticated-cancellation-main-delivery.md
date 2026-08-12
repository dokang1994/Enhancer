# User continuation request after authenticated cancellation main delivery on 2026-08-11

Status: Accepted Decision

## Context

The authenticated-cancellation application and detached signed-grant authorization core
are delivered to `main`. `CURRENT_TASK.md` records the next bounded work: define and
implement the operator-owned production trust bootstrap before composing
`scheduler-apply-cancel`. The repository checkpoint is empty, local and remote `main`
match, and the working tree is clean.

## Decision

Treat the user's continuation request as authority for one architecture-first local
implementation task that adds a read-only pinned filesystem loader for the existing
public-only `CancellationGrantTrustPolicy`. The task may define a deterministic
configuration format, require an independently injected exact-byte SHA-256 pin, add
strict parsing/path/bounds tests, and prove the loaded immutable policy through the
existing audit-backed authorizer.

The task does not authorize a CLI or interface adapter, a default/discovered trust
source, production signing or private-key handling, credential or identity-provider
integration, configuration writing/provisioning/rotation, permission or trust-store
mutation, queue/process/Tool/effect cancellation, pause/resume, commit, push, merge,
release, deployment, destructive action, paid service, external message, or unrelated
work.

## Rationale

A pinned loader is the smallest next step that turns separately provisioned public
trust bytes into the existing immutable verifier policy without letting the cancellation
request choose its own trust root. Keeping installation metadata and CLI composition
separate prevents a constructor seam from being mistaken for a supported self-approval
path.

## Consequences

- The new task must record the exact pin/source and canonical-format architecture before
  production implementation.
- The pin and policy path remain authority-bearing construction inputs and cannot become
  proof-command arguments, environment/JVM-property fallbacks, repository discoveries,
  or retained-request fields.
- No private material or policy writer may enter production code or durable state.
- The next interface task must still bind the loader to protected installed metadata
  before exposing `scheduler-apply-cancel`.
