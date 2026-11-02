# RFC-0033: Protected Remote Capability Catalog Runtime Binding Composition

Status: Accepted

## Purpose

Define the read-only protected runtime composition that turns RFC-0032's sole fixed
canonical binding into the exact absolute catalog path and whole-file SHA-256 consumed
by RFC-0031 for one stable capability-fact acquisition.

This RFC specifies a trust-input bridge only. It does not implement the bridge, install
or rotate a catalog, compose RFC-0032's publisher, create deployment configuration, call
RFC-0030 or RFC-0029, select or execute a remote route, contact a provider, or grant
credential, network, transmission, spend, persistence, runtime, or MCP authority.

## Relationship To Existing Contracts

RFC-0032 remains the sole owner of the canonical installed binding grammar, fixed
artifact names, catalog-first/binding-last publication, INSTALL/ROTATE, and publication
recovery. Its package-private binding codec is the future reader's canonical parser; a
second runtime grammar is forbidden.

RFC-0031 remains the sole production creator of `RemoteModelCandidateCapabilityFacts`.
It accepts one absolute normalized catalog path and one independently supplied lowercase
whole-file SHA-256, reads the selected catalog defensively, validates the complete
catalog, resolves one exact candidate/destination record, and checks freshness at the
explicit evaluation time. RFC-0033 must delegate to a fresh RFC-0031 source and must not
copy, reinterpret, default, repair, or construct any capability value itself.

The permitted read path is:

```text
protected deployment composition selects the one fixed binding point
-> RFC-0033 reads and canonically validates one binding snapshot
-> RFC-0033 derives only the fixed sibling catalog directory and selected path/pin
-> fresh RFC-0031 source acquires exact capability facts at evaluationTime
-> RFC-0033 rereads the binding and accepts only an unchanged canonical selection
-> a later same-invocation consumer may pass the exact acquired facts to RFC-0030
   and construct one RFC-0029 route option
```

That later consumer is separate. No current production caller reaches RFC-0031,
RFC-0030, RFC-0029, a provider, or a network through this RFC.

## Trust Root And Composition Authority

Protected deployment composition supplies exactly one absolute normalized path whose
leaf is RFC-0032's fixed
`enhancer-remote-capability-catalog-binding-v1`. The path is immutable construction
input to the future runtime source and is not accepted by an acquisition call.

The binding path cannot come from or be overridden by a request, profiled model request,
RFC-0028 proposal or eligibility, RFC-0029 option, RFC-0030 capability facts, catalog
content, provider label, CLI argument, environment variable, JVM property, repository,
working directory, filename discovery, publication result, log, cache, or ambient user
identity. A future deployment adapter must bind the path to protected installation
metadata or another independently authorized configuration source. This RFC does not
define that adapter or claim an installed production binding.

The binding parent is the installation root for this read. The catalog directory is
derived only as its fixed immediate child
`enhancer-remote-capability-catalogs-v1`; it is never caller-selected. The selected
catalog path and pin come only from the canonical binding snapshot. The runtime reader
has no write, create, rename, delete, repair, rotate, fallback, scan, discovery, cleanup,
or permission-mutation operation.

## Acquisition Input And Result

One future acquisition accepts only:

- one bounded stable lowercase candidate identity;
- one bounded stable lowercase destination identity; and
- one explicit evaluation time.

It accepts no binding path, installation root, catalog directory, catalog path, digest,
provider endpoint, credential, secret, fallback, retry, cache, policy override, or
execution authority.

The result is one closed immutable decision:

- `Acquired` retains the exact RFC-0031-created capability-facts instance and the
  lowercase SHA-256 identity of the stable canonical binding snapshot; or
- `Refused` retains one binding-composition refusal and, only for delegated source
  refusal, the exact existing RFC-0031 refusal reason.

The binding digest is non-secret provenance, not truth, freshness, anti-rollback,
provider attestation, route eligibility, or execution authority. Rendering never
exposes absolute paths, binding or catalog bytes, capability values, prices, capacities,
timestamps, request content, exceptions, platform descriptors, credentials, or secrets.

## Fixed Binding Acquisition

Every call starts from the construction-time fixed binding path and performs a fresh
bounded read. No binding or source instance is cached across calls.

The future reader must:

1. require the binding path to remain absolute, normalized, and fixed-name-bearing;
2. require the installation root and fixed catalog directory to be existing exact-real
   non-symbolic directories without an accepted intermediate link, junction, reparse,
   or alternate real-path identity;
3. require the fixed binding to be an exact-real regular non-symbolic file;
4. read one 1-byte-through-4-KiB snapshot with no-follow semantics, a one-byte overflow
   probe, and before/after identity, size, creation-time, and modification-time checks;
5. decode through RFC-0032's one canonical binding codec and require byte-for-byte
   re-encoding equality;
6. require the selected catalog path to be the digest-named immediate child of the
   fixed exact-real catalog directory; and
7. retain the exact canonical binding bytes and binding digest only for this call.

Java path and attribute checks establish only this bounded local contract. They do not
prove runtime-principal read-only permissions, publisher-only write permissions,
hostile-writer exclusion, directory durability, platform support, or privileged
anti-rollback. Those remain deployment and platform-adapter work under separate
authority.

## RFC-0031 Delegation

After the first binding snapshot passes, the future boundary constructs one fresh
`PinnedFileRemoteModelCandidateCapabilityFactSource` with exactly the snapshot's
selected absolute catalog path and lowercase pin. It invokes that source once with the
unchanged candidate identity, destination identity, and evaluation time.

RFC-0031 alone owns selected-catalog no-follow acquisition, whole-file pin comparison,
complete canonical catalog parsing, exact record lookup, freshness evaluation, and
capability-facts creation. RFC-0033 must not preselect a row, weaken an RFC-0031 refusal,
retry with another catalog, query a provider, or construct facts from binding or request
data.

The old immutable catalog remains a valid acquisition target while a cooperating
publisher rotates the fixed binding because RFC-0032 forbids automatic deletion. That
retention does not by itself authorize returning a result: the stable-observation check
below determines whether the same binding remained selected across this call.

## Stable Sequential Observation

After RFC-0031 returns either acquired facts or a refusal, RFC-0033 rereads the fixed
binding through the complete acquisition rules above. The call may return the delegated
result only when the second canonical bytes and binding digest exactly equal the first.

If the second point is missing, unsafe, unreadable, oversized, changed while read,
invalid, or different, the call returns `BINDING_CHANGED_DURING_ACQUISITION` and exposes
neither acquired facts nor the earlier delegated refusal. A valid rotation from one
binding to another is still a changed acquisition and is retried only by a new explicit
caller invocation.

This is a bounded stable sequential observation, not a lock shared with the publisher
and not an atomic snapshot of deployment state. A rotation after the final sample is a
later state. The result proves only that one canonical selection remained stable around
one RFC-0031 acquisition. The later same-invocation consumer must use the returned
decision directly rather than cache or reconstruct it.

## Closed Refusal Contract

After programming errors for missing or malformed acquisition input, the first
applicable top-level reason is:

1. `INSTALLATION_ROOT_UNAVAILABLE_OR_UNSAFE`
2. `BINDING_UNAVAILABLE_OR_UNSAFE`
3. `BINDING_OUTSIDE_SIZE_BOUND`
4. `BINDING_CHANGED_DURING_READ`
5. `BINDING_INVALID`
6. `BINDING_CHANGED_DURING_ACQUISITION`
7. `SOURCE_REFUSED`

Reasons one through five classify the first binding sample. The second sample is always
attempted after RFC-0031 returns; every second-sample failure or unequal canonical value
collapses to reason six, which therefore precedes delegated source classification.
`SOURCE_REFUSED` retains exactly one existing
`RemoteModelCandidateCapabilityFactSourceRefusalReason`; it never parses prose or
relabels a catalog failure as a binding success.

Programming errors, checked filesystem failures, and unexpected runtime exceptions are
not printed or embedded in results. A future implementation must map only the bounded
expected failure regions above and let composition/programming defects fail through the
repository's normal internal-error boundary without leaking path or exception detail.

## Concurrency, Rotation, And Recovery

The reader never acquires RFC-0032's maintenance lock. The publisher owns serialization
among cooperating writers; readers rely on immutable catalogs and atomic fixed-binding
replacement. A reader sees an old complete binding, a new complete binding, or refuses
a changed/unsafe sample. It never treats catalog candidates, publication results,
timestamps, directory order, or the newest filename as authority.

There is no automatic retry, fallback, previous-binding selection, repair, or cache.
Callers may make a new acquisition after a refusal only under their own bounded policy.
The boundary records no durable route provenance and creates no cleanup obligation.
Previous catalogs and abandoned candidates remain RFC-0032 retention concerns.

Digest equality detects exact bytes; it does not establish factual truth, provider
availability, permission protection, or anti-rollback. A privileged actor able to
replace binding, catalog, and application together remains outside this contract.

## Safe Observability

Allowed bounded observations are the top-level outcome, closed refusal, nested RFC-0031
refusal when applicable, candidate and destination identities already supplied by the
caller, catalog identity already present in acquired facts, and binding/catalog digest
identities. Absolute paths and raw bytes remain redacted.

Possessing `Acquired`, a binding digest, capability facts, or a successful log grants no
Router eligibility, provider construction, transmission, spend, invocation, Tool,
task, policy, persistence, or retry authority. The future consumer must freshly apply
RFC-0030 and RFC-0029 under their exact existing contracts.

## Rejected Shortcuts

- Accepting a binding, path, pin, installation root, or catalog directory per request is
  rejected as caller-selected trust.
- Reading the catalog directly in RFC-0033 or constructing capability facts outside
  RFC-0031 is rejected as duplicated authority and parser drift.
- Reading the binding once at process startup, caching a source across rotation, or
  returning a result without the second exact sample is rejected as stale selection.
- Scanning for the latest catalog, falling back to another binding or pin, trusting a
  publisher result, or repairing invalid state is rejected.
- Holding the maintenance lock for reads is rejected because the immutable-catalog and
  atomic-binding protocol already defines the reader boundary; lock possession would
  not prove platform permissions or anti-rollback.
- Treating a stable sequential sample as an atomic deployment snapshot, factual truth,
  provider availability, or permission evidence is rejected.
- Adding a CLI, environment/JVM-property source, repository configuration, runtime
  registry, service locator, or default path is rejected.

## Verification Contract

A later implementation begins RED-first with pure immutable result/refusal values and
one read-only source constructed from a protected fixed binding path. Focused tests must
cover every programming input and refusal precedence; fixed leaf and sibling derivation;
missing, symbolic, junction/reparse, non-regular, oversized, malformed, noncanonical,
foreign-directory and digest-mismatched binding state; bounded one-byte overflow and
before/after drift; exact RFC-0031 input delegation; acquired and every delegated
refusal; rotation before, during and after delegation; second-sample failure; safe
rendering; repeated fresh acquisition; and no cache, scan, fallback, write, repair, or
maintenance-lock use.

Architecture and source guards must prove RFC-0031 remains the sole production creator
of remote capability facts; RFC-0033 contains no catalog value mapping, provider,
credential, network, spend, execution, persistence, Router, evaluator, Tool,
Scheduler, runtime, MCP, CLI, environment, repository, generic installer, publisher,
permission-adapter, cleanup, rollback, or anti-rollback dependency; and no production
caller exists until separately accepted.

Temporary filesystem tests can establish bounded read and rotation behavior only.
Operational platform support requires separate real installation, effective-identity,
permission, indirection, atomicity, durability, negative-access, and anti-rollback
evidence.

For this documentation-only task, RFC/decision/index/document-ownership/Architecture-
mirror/Roadmap/Planner/Assisted-Loop/dynamic-workflow governance, reference inspection,
`git diff --check`, and the full README-owned Java 17 regression are the verification
boundary.

## Exclusions

- Java production or test implementation, production caller, deployment configuration,
  installed binding/catalog, publisher or evidence composition, writer, installer,
  operator, broker, authenticated transport, permission or identity adapter, real
  installation, platform-support claim, cleanup, retention, uninstall, rollback, or
  privileged anti-rollback anchor;
- RFC-0031 source caller beyond the future internal delegation, RFC-0030 evaluator
  caller, RFC-0029 route-option consumer, Router execution preparation, provider,
  endpoint, credential, secret, network, transmission, spend, billing, retry, fallback,
  cache, or durable route provenance;
- Tool, Evidence, RunRecord, Scheduler, AgentRuntime, runtime-event, durable schema,
  migration, MCP, plugin, interface, background work, capability promotion, push,
  merge, tag, release, deployment, or another external effect.

## Codex Implementation Prompt

Implement RFC-0033 only under a separately accepted Active Task, RED-first, as one
read-only protected fixed-binding source that reuses RFC-0032's canonical binding codec,
delegates exact path/pin acquisition to a fresh RFC-0031 source, and accepts only a
stable before/after binding selection. Add no writer, installation/configuration source,
production caller, Router/evaluator consumer, provider, credential, network, spend,
execution, persistence, runtime, MCP, fallback, scan, cache, repair, or cleanup path.
