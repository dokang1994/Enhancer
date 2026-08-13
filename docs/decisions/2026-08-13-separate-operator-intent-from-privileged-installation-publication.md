# 2026-08-13: Separate Operator Intent From Privileged Installation Publication

Status: Accepted Decision

## Context

The cancellation-trust operator distribution is separately launched but packages the
same project JAR as the runtime distribution. Launcher separation is not bytecode or
filesystem-authority isolation. The installed metadata is also a sibling of the
application JAR, and maintenance must create candidates, replace metadata, and create or
open the lock in that directory.

On POSIX systems, directory write permission controls rename and deletion of every
child. An operator principal that may directly replace sibling metadata could therefore
also replace the application JAR. File modes or a sticky directory do not establish the
required portable boundary. Windows likewise requires effective DACL and token evidence,
not username or ownership strings.

## Decision

- Define three distinct stable operating-system principals: installer/publisher,
  operator, and runtime. The installer/publisher alone owns protected final-path
  creation, permission application, atomic publication, activation, and future upgrade.
  The operator may execute/read the separate launcher and write only an operator-private
  candidate inbox. The runtime may read/execute the application and read installed trust
  metadata/policies, and has no access to the operator launcher or mutation rights in
  the protected installation.
- Treat an explicit application path as request data, never authority. A privileged
  publisher must match it to independently installed allowlisted configuration and bind
  the verified operator identity, exact application identity, operation, candidate
  digest, and expected current state before publication.
- The existing direct maintenance state machine may run only as the privileged
  installer/publisher or behind its narrow authenticated broker. Do not claim that an
  unprivileged operator can safely mutate sibling metadata while being unable to replace
  the sibling JAR.
- Define a future platform-neutral permission adapter that consumes an already
  authorized typed installation plan. It resolves stable principal and filesystem
  identities, rejects links/reparse points and cross-volume publication, applies final
  permissions to private same-filesystem candidates, verifies positive and negative
  effective access, publishes atomically, performs supported durability barriers, and
  revalidates bytes, identity, ownership, permissions, and access afterward. It never
  approves a request.
- Windows support requires SID/token-group, owner, protected DACL/inheritance, effective
  file/directory access, bypass-privilege, reparse/volume/file-identity, atomic-move, and
  post-publication evidence. POSIX support requires numeric UID/GID/supplementary-group,
  mode/ACL/default-ACL, effective parent-directory rights, capability, link/device/inode,
  same-filesystem, atomic rename, and post-publication evidence. A platform is unsupported
  until its non-skipped isolated suites prove those facts.
- Install immutable version directories and activate last. Persist a transaction intent,
  stage privately, apply and verify final permissions before exposure, publish/exact-
  resolve policy first and fixed metadata last, run a non-mutating trust-loader probe as
  the actual runtime principal, and only then switch the activation point. File existence,
  audit presence, or a candidate name never proves success.
- Retain partial candidates, old policies, inactive versions, lock, and audit evidence.
  No failure automatically broadens permissions, deletes artifacts, restores metadata,
  or activates an older version. Cleanup, uninstall, rollback, and retention remain
  separately authorized.

## Rationale

Final-path mutation and runtime immutability cannot both belong to the operator while
metadata and the application JAR share a directory. Separating request authority from a
narrow privileged publisher preserves least privilege and makes platform evidence,
publication ordering, and recovery explicit.

## Consequences

- A future implementation needs an authenticated operator-to-publisher boundary and
  native/platform permission evidence; portable Java owner or ACL inspection is
  insufficient.
- The existing launcher remains useful as the publisher-owned execution surface but is
  not itself authorization, isolation, provenance, or deployment.
- Current atomic moves and file `force(true)` make no parent-directory or sudden-power-
  loss durability claim. Current lock/CAS makes no privileged rollback claim.
- Signed source provenance, real installation, OS permission enforcement, activation,
  deployment, cleanup, uninstall, release, and independently protected anti-rollback
  remain unimplemented.
