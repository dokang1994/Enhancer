# 2026-08-13: Separate Platform Enforcement Rights From Authorized Installation Operations

Status: Accepted Decision

## Context

The platform-neutral permission matrix describes which typed installation operations a
principal may request or perform within an approved transaction. Windows rename and
replace primitives, however, require raw target `DELETE` or source-parent
`DELETE_CHILD`, even when the transaction does not authorize general deletion. Treating
raw Windows delete authority as normalized `RENAME` while claiming normalized `DELETE`
is denied would record false effective-access evidence.

## Decision

- Keep `InstallationAccess` and the fixed neutral matrix as the authorized-operation
  contract consumed by typed installation plans. It does not claim a one-to-one mapping
  to operating-system permission bits.
- Model each platform's raw enforcement rights separately and retain them in platform-
  specific evidence. A platform adapter derives the exact raw-right closure necessary
  to enforce the neutral allowed operations and refuses missing or surplus rights.
- For Windows publisher rename/replace, the closure may include target `DELETE` or
  parent `DELETE_CHILD` plus destination-parent add rights. Those raw rights do not add
  neutral `InstallationAccess.DELETE`, authorize a new transaction operation, or permit
  cleanup/uninstall.
- Operator and runtime remain strict: their raw rights may not include mutation,
  ownership, DACL, delete, delete-child, rename, or replace capability beyond the fixed
  neutral matrix. Dangerous group or privilege bypass fails closed.
- Platform evidence must expose both the raw partition and the derived neutral operation
  partition. It must never suppress a raw granted right to make the neutral policy look
  narrower.

## Rationale

Authorization answers what a transaction may do; OS enforcement answers which primitive
rights the executing principal necessarily possesses. Keeping both truthful preserves
least privilege without making an impossible Windows ACL claim.

## Consequences

- Publisher raw rights may be a minimal documented superset of neutral operations, but
  only where a specific platform primitive requires it and tests prove no broader raw
  closure is accepted.
- The typed adapter API remains the only contract that can invoke publication. No raw
  right grants cleanup, uninstall, arbitrary deletion, or ambient authority.
- Actual enforcement still requires a separately authorized platform gateway and
  isolated integration evidence.
