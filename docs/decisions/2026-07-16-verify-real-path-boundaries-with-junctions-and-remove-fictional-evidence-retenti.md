# 2026-07-16: Verify Real-Path Boundaries With Junctions And Remove Fictional Evidence Retention

Status: Accepted Decision

Context:

- The only tests covering `toRealPath()` escape rejection attempt symbolic-link creation and are skipped on this Windows host, leaving the production boundary unverified here. Windows directory junctions can exercise the same real-path escape without symbolic-link privilege.
- `EvidenceRetentionPolicy.retentionPeriod` is validated and tested but never read by production. The store neither expires nor deletes evidence, so the API suggests a 30-day lifecycle contract that does not exist.
- Destructive retention needs explicit lifecycle, replay, audit, and cleanup authority. Inventing deletion inside this corrective task would widen behavior beyond the observed defect.

Decision:

- Add Windows-only directory-junction integration tests for both `ReadFileTool` and `ProjectContextReader`. On Windows, junction creation failure is a test failure rather than a skip; other platforms retain the existing symbolic-link coverage and skip only the Windows-specific test.
- Rename `EvidenceRetentionPolicy` to `EvidenceStoragePolicy`, remove `retentionPeriod`, and expose only the actually enforced `maxContentBytes` contract through `EvidenceStore.storagePolicy()`.
- Remove the CLI's unused 30-day constant and update all production and test callers to the truthful storage policy.
- Do not delete, expire, hide, or rewrite existing evidence. A future retention feature requires a separate accepted decision with cleanup authority and replay/audit semantics.

Rationale:

Junctions make the real-path security invariant executable on the host that previously skipped it. Removing an unused promise is safer than implementing surprise deletion: the API should name only behavior the store enforces today.

Consequences:

- Both lexical traversal and Windows real-path redirection are covered by fresh tests at the actual boundaries.
- Evidence remains durable until external/user-authorized lifecycle management exists; storage size is bounded per artifact, not by age or aggregate capacity.
- The policy type and accessor are intentionally source-incompatible inside this pre-release repository, preventing callers from continuing to rely on a fictional retention period.
