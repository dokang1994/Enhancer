# 2026-07-28: Return The Accepted File Spool Point Without Directory Discovery

Status: Accepted Decision

## Context

`FileSpoolMessageTransport` creates a fresh random point name for every accepted hop,
while the transport-neutral `TransportOutcome` intentionally carries only status and a
bounded refusal reason. The separately invoked Work receiver requires one explicit
canonical point filename. Directory discovery would add concurrency ambiguity and a
consumer behavior that the accepted publication increment excludes.

## Decision

Keep `MessageTransport.send` and `TransportOutcome` unchanged. Add a concrete
file-spool publication result and `FileSpoolMessageTransport.sendWithReference` so an
accepted hop returns exactly its generated canonical point filename. Refused outcomes
carry no filename. `scheduler-spool-work` reports that filename only for `ACCEPTED`,
allowing the caller to invoke `scheduler-receive-work` without scanning.

## Consequences

The point reference is adapter-local routing data, not a second message identity,
delivery acknowledgement, admission claim, journal offset, or execution evidence.
Every send still creates a fresh hop and never overwrites an earlier point. Other
`MessageTransport` implementations acquire no new contract.
