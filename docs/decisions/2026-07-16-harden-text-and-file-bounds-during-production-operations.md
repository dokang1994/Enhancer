# 2026-07-16: Harden Text And File Bounds During Production Operations

Status: Accepted Decision

Context:

- `VerificationEvidence` and several diagnostic surfaces truncate by UTF-16 index and can split a supplementary Unicode surrogate pair, producing malformed Java text that strict UTF-8 persistence rejects.
- `ReadFileTool`, `ProjectContextReader`, and `TargetFileMetadataCollector` preflight file size but then read or hash without enforcing the byte ceiling during the operation. The filesystem Evidence, RunRecord, and Scheduler queue resolvers have the same size-check/read window.
- Preflight metadata is useful for early refusal but cannot enforce a resource bound after another process changes the file.
- Tool process isolation, package modularization, and power-loss directory durability are separate architectural concerns and must not be hidden inside a text/file-bound correction.

Decision:

- Add one neutral Unicode truncation utility with prefix and suffix operations that preserve existing UTF-16 length ceilings while never returning half of a surrogate pair at the truncation boundary.
- Apply it to VerificationEvidence tails, ToolExecutor failure diagnostics, CLI bounded output and values, and bounded Workspace failure reasons.
- Add one neutral bounded-input utility that reads or hashes at most the configured bytes and probes at most one additional byte to detect overflow.
- Keep existing preflight size checks for early diagnostics, but route production file content, digest, and persisted-envelope reads through the bounded operation.
- Treat an in-operation target-file overflow as an explicit Unavailable observation; the other governed readers and stores fail with IOException or their existing typed corruption/failure conversion.
- Preserve strict UTF-8 decoding and persistence, existing configured byte ceilings, evidence identity over complete content, and current authority boundaries.
- Defer Tool process isolation/global stuck-worker policy, lifecycle package extraction, parent-directory synchronization, and power-loss fault-injection to separate accepted decisions.

Rationale:

Unicode truncation is a representation boundary and byte limits are operational resource boundaries. Centralizing both prevents repeated off-by-one and TOCTOU mistakes while keeping the change behaviorally narrow. Reading at most the limit plus one detection byte enforces memory and hashing work independently of mutable file metadata.

Consequences:

- A Unicode-safe tail may contain one fewer UTF-16 code unit than the configured maximum when the exact boundary would split a supplementary character.
- File growth after the metadata check cannot cause unbounded allocation or hashing on the corrected paths.
- This task does not solve stuck in-process Tool threads, package cycles, or power-loss durability.
