# User continuation request on 2026-08-13 after Windows rights-model conflict

Status: Accepted Decision

## Context

The Windows boundary paused before RED because rename/replace needs raw delete semantics
that the neutral authorized-operation matrix intentionally does not grant as a general
transaction operation. The Agent recommended separating platform raw enforcement rights
from authorized installation operations, and the user requested continuation.

## Decision

Authorize the recommended clarification and continuation of the existing Windows
fake-gateway task. The neutral matrix remains the typed transaction authority while the
Windows evidence layer truthfully records and validates the minimal raw-right closure
needed to implement it.

All existing exclusions remain: no host/API/filesystem/security-state observation or
mutation, no production gateway, no installer or real integration, and no external Git,
deployment, release, cleanup, or destructive action.

## Rationale

The clarification removes an impossible evidence claim without granting general delete
authority or broadening the task to real Windows effects.

## Consequences

- Focused RED and minimal implementation may proceed within the existing Active Task.
- Publisher raw delete/delete-child rights are evidence of the Windows primitive only;
  operator/runtime denial and typed operation scope remain unchanged.
