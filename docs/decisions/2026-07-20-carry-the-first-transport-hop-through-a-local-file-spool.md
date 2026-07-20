# 2026-07-20: Carry The First Transport Hop Through A Local File Spool

Status: Accepted Decision

Context:

- The 2026-07-16 transport decision deliberately left serialization, protocol negotiation, endpoint discovery, authentication, concrete adapters, buffering, threading, and persistence out of scope, so `MessageTransport` has had no implementation and no process boundary has ever been crossed.
- Connection 3 in the Gate 8 backlog is process-isolated worker plus local IPC, and the Roadmap records that it "requires a separately selected adapter and process lifecycle". Selecting the adapter is therefore its own decision rather than an implementation detail of the worker.
- Worker process isolation (3b) needs `ProcessBuilder`. Today the only external command authority in the codebase is `GitWorkspaceCollector`, scoped by its own accepted decision, and `.ai/workflow.md` step 6 requires that work needing new external authority be reported rather than implemented inside a RED cycle. 3b therefore cannot proceed on the strength of this decision.
- A Unix domain socket adapter was considered. Java 16 supports it without a dependency and it is closer to real IPC, but it introduces a socket capability that contradicts the currently documented absence of network access, and its tests need a live peer and are not deterministic.

Decision:

- Add `FileSpoolMessageTransport` as the first `MessageTransport`: it writes one encoded `TransportMessage` to its own file under a configured spool directory that a peer process reads.
- Separate the wire format into `MessageEnvelopeCodec` rather than inlining it in the adapter. The format's cases — four payload kinds, an optional causation identity, a nested execution input, and every corruption mode — are verifiable without a filesystem, and a second adapter reuses the codec unchanged.
- Carry occurrence time as epoch-second plus nanosecond. An earlier draft used `toEpochMilli`, which silently truncated an `Instant` and rewrote provenance the receiver is meant to trust; a test with a nanosecond-bearing instant proved the loss before the fix.
- Keep the frame free of wall-clock and random state, so one message always encodes to identical bytes and a peer may deduplicate on content. An earlier draft put the spool time in the header and digested over it, which made two hops of one message differ.
- Distinguish `CorruptedSpooledMessageException` from a plain `IOException`. A corrupt message stays corrupt and should be dead-lettered; an `IOException` is a filesystem condition that may be transient and worth retrying. Collapsing both into `IOException` would leave a caller unable to tell a permanent failure from a retryable one.
- Map the three transport statuses to conditions the adapter can actually observe. `ACCEPTED` means the message was durably spooled and nothing more; capacity exhaustion is `BACKPRESSURED`; an unusable spool root is `UNAVAILABLE`. A refused message spools nothing.
- Reuse the existing `BackpressurePolicy` for the capacity bound rather than introducing a second admission-bound type, since the shape and the 1-to-4096 range are identical.
- Reuse the established durable-artifact idiom: a magic-prefixed header carrying spool time, payload length, and a SHA-256 digest, length-prefixed strict UTF-8 strings, a temporary file published by atomic move, and fail-closed reads that reject bad magic, invalid lengths, digest mismatch, and trailing bytes.
- Give every hop a freshly generated file name, so resending the same envelope never overwrites an earlier hop and a reader never observes a partial message.
- Serialize all four payload kinds rather than only work payloads, so the adapter carries the envelope contract rather than a subset of it, and reject an unknown kind on both encode and decode.
- Make no ordering promise across separately spooled messages. The transport contract is per hop, so the round-trip test compares messages as a set rather than by directory order.
- Do not implement 3b in this increment, and do not add a receiving bus, endpoint discovery, authentication, retry, or production wiring.

Rationale:

The spool is the smallest adapter that proves the seam is real: it crosses a process boundary in the only direction the contract describes, it is decodable by an independent reader, and it needs no capability the project does not already exercise. Choosing it now also keeps the authority question honest — the transport can be built and verified today, while process spawning waits for the explicit approval its own rules require, instead of being smuggled in as part of an adapter increment.

Splitting the codec out is not speculative reuse. The wire format has ten-odd cases that are pure serialization concerns, and testing them through the adapter would require a temporary directory for each; the split makes those cases assertable directly and leaves the adapter small enough to read in one screen. The three corrections above — nanosecond precision, a deterministic frame, and a typed corruption failure — were each found by comparing this increment against an independently written implementation of the same contract, and each fixes a defect rather than a preference.

Consequences:

- `MessageTransport` has one production implementation and the boundary is exercised end to end for every payload kind, so the Gate 7 transport contract is no longer implementation-free.
- The spool is one-directional by construction. A peer reads with the static `read`; results returning the other way need their own spool or their own decision.
- Capacity is counted by listing the spool directory on each send, which is adequate at the 4096 bound and would need an index if the bound rose substantially.
- `BackpressurePolicy`'s documentation now covers two admission points rather than only the in-process bus.
- Nothing wires this adapter into production. No CLI, worker, or bus path constructs it, so it changes no runtime behaviour.
- Out of scope and unchanged: worker process isolation (3b) and its `ProcessBuilder` authority, a receiving-side bus, endpoint discovery, authentication, framing for streamed transports, retry policy, and spool retention or cleanup.
