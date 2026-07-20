# 2026-07-15: Observe Stored Run Records As Explicit Metadata With Corruption Surfaced As Unavailable

Status: Accepted Decision

Context:

- The Workspace contract types a `RUN_RECORD` source kind and the Gate 6 scope names RunRecord metadata as a snapshot source, but no adapter observes stored records.
- The RunRecord store exposes only `persist` and `resolve`; observation needs a read-only listing.
- Resolution already validates envelope integrity, so a listed record either yields trustworthy metadata or a typed corruption failure.
- Silently skipping a corrupted record would make absence indistinguishable from health, which the snapshot's explicit state model exists to prevent.

Decision:

- Add a read-only `references()` listing to the `RunRecordStore` interface and its filesystem implementation: lexicographically ordered valid references, an empty list for a missing or empty root, and non-record files ignored.
- Add `RunRecordMetadataCollector` under `com.enhancer.workspace`, emitting one `RUN_RECORD` observation per listed reference with `run-record-store` provenance and a caller-supplied observation time.
- Emit `AVAILABLE` observations carrying the envelope SHA-256 as content digest and the stored time as source-update time when it does not postdate the observation time.
- Emit an explicit `UNAVAILABLE` observation with a bounded reason and no digest when integrity resolution fails.
- Store no payload, evidence, or Tool content, and add no write, delete, or retention capability.

Rationale:

The envelope digest is already the store's own integrity identity, so reusing it as the observation digest gives run-record observations the same content-addressed discipline as documents without decoding payloads a second time. Surfacing corruption as `UNAVAILABLE` uses the state the contract defined for exactly this case and keeps the snapshot an honest account of what the store could and could not vouch for.

Consequences:

- Observation cost grows with record count because each listed record is integrity-checked; retention and cleanup remain governed elsewhere.
- Files under the storage root that are not record artifacts are outside the store's contract and are not observed.
- The CLI does not yet include these observations; the production composition increment owns that integration.
