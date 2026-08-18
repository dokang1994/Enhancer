# Accept RFC-0013 defining the Delivery Gate 9 model gateway minimum slice

Status: Accepted Decision

## Context

The roadmap requires detailed RFC work before Model Gateway implementation becomes
active, and the 2026-08-18 project analysis found prompt and LLM invocation absent
while unrelated subsystems kept absorbing increments. The accepted recommendation
track directed a minimum Delivery Gate 9 slice definition as its second increment.
Two bounded read-only Opus subagent surveys — one over the existing loop, tool,
evidence, RunRecord, and CLI seams, one over the documented Gate 9 scope, security
baseline, RFC conventions, and executable governance constraints — were reconciled
against repository authority as recommendations, not verification evidence.

## Decision

Accept RFC-0013: Model Gateway as the Delivery Gate 9 minimum-slice specification.
The slice is one provider-neutral `ModelGateway` port with immutable bounded
request/response/usage records in a new `com.enhancer.model` leaf package, one
deterministic fake as the only executed gateway, one never-invoked provider adapter
shape, and one `model-invoke` Tool composed into the existing executor so the
existing isolation, timeout, cancellation, policy, evidence, and RunRecord paths are
reused unchanged. Credentials exist only as an injected supplier boundary with no
default provider and no persisted or logged secret. Real provider invocation, paid
service use, MCP, routing, caching, fallback, streaming, and evaluation remain
excluded and require their own later authority.

## Rationale

The seam survey showed the existing Tool port already provides isolation, bounded
failure conversion, and integrity-checked persistence, so the smallest correct slice
attaches there without touching the scheduler or runtime packages. The constraint
survey showed provider neutrality, untrusted model output, explicit paid-service
authority, and local-only completeness are binding constitutional and roadmap rules,
which the slice satisfies by executing only a deterministic fake.

## Consequences

- RFC-0013 is registered in the RFC index and the roadmap RFC track; acceptance
  states design direction, not capability maturity.
- Implementation requires a separate bounded task with focused RED-first tests and
  the named promoting integration test.
- The shared five-second tool timeout becomes a per-tool value during
  implementation, as the specification requires.
- No model call, network connection, credential, or paid service is created by this
  decision.
