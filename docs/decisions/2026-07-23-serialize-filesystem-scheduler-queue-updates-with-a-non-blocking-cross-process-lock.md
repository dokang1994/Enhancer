# 2026-07-23: Serialize Filesystem Scheduler Queue Updates With A Non-Blocking Cross-Process Lock

Status: Accepted Decision

Context:

- Atomic replacement makes each queue snapshot complete or absent, but it does not make the
  preceding read-validate-replace sequence atomic across processes.
- Two `FileSystemSchedulerQueueStore` instances can resolve the same revision N, construct
  different revision N+1 values, and both replace the artifact. Exactly-one revision
  validation happens before replacement, so the last writer can erase the first transition.
- The supported submission and cycle commands are separate JVM invocations, and the accepted
  foreground drain will perform multiple queue transitions. Relying on an undocumented
  single-operator convention would make the supported filesystem boundary unsafe.

Decision:

- Give each filesystem Scheduler queue one stable sibling lock artifact derived only from
  its canonical queue identity. The lock artifact is separate from the atomically replaced
  queue snapshot so replacement cannot change the lock target.
- Acquire an exclusive operating-system file lock without waiting before every queue
  `update`. Hold it across resolving the current snapshot, validating the next revision and
  exact-history prefix, and atomically publishing the replacement.
- If another process or overlapping store instance owns the lock, fail immediately with a
  typed queue-write-conflict exception. Do not retry, wait, steal, expire, or infer owner
  identity.
- Keep `create` protected by its existing create-without-replacement publication. Queue
  snapshots and revisions remain the only state and recovery authority; the empty lock
  artifact records no lifecycle fact and may remain after release.
- Open the lock without following links and reject an unusable lock path. Preserve the
  existing caller-named storage-root boundary and make no network-filesystem guarantee.
- Prove the boundary with a real child JVM that retains the lock while another store update
  fails without changing the queue, plus a stale-writer test showing that a committed
  revision cannot be overwritten.

Alternatives considered:

- Rely on atomic move alone: rejected because atomic publication does not prevent lost
  updates between two valid read-modify-write transactions.
- Lock the queue snapshot file itself: rejected because atomic replacement changes the file
  object being locked and can also conflict with replacement semantics on Windows.
- Block until the lock becomes available: rejected because an operator command must not hang
  behind an unknown process; callers can inspect and retry explicitly.
- Add locks to every filesystem store now: deferred because queue mutation is the observed
  supported-command race and a generic locking framework would exceed the smallest coherent
  correction.
- Add a distributed lease or transactional database: deferred because no distributed or
  network-filesystem deployment is supported.

Rationale:

A stable OS file lock closes the observed local multi-process lost-update window without
changing queue data, revision semantics, or the Scheduler port. Non-blocking refusal keeps
failure visible and bounded, while validation inside the lock makes the committed snapshot
the source of truth for every attempted update.

Consequences:

- Supported local filesystem queue updates become single-writer across cooperating JVM
  processes and fail closed under contention.
- A lock file is an operational coordination artifact, not a durable queue state, receipt,
  lease, checkpoint, or authority grant.
- Stale in-memory queue instances fail revision validation after the current writer releases
  the lock and must be recovered before retry.
- General multi-process runtime coordination, cross-store transactions, distributed locks,
  lock-owner inspection, network-filesystem behavior, schema migration, and power-loss
  directory durability remain separate work.
