# Freeze installation-subsystem derivative work until its Delivery Gate 16 consumers exist

Status: Accepted Decision

## Context

Between 2026-08-11 and 2026-08-18 every delivered increment extended the cancellation
trust maintenance operator's installation subsystem: the Windows permission boundary,
installation transaction contracts, pure evidence reconciliation, deterministic
integrity file formats, and the locked filesystem transaction cursor. Each increment
was bounded and verified, and each one's closing next action proposed a further
derivative — evidence body and reference schemas, host revalidation, a production
evidence resolver, permission and native adapter composition. The roadmap places
installer distribution, offline installation, and rollback evidence at Delivery Gate
16, while prompt and LLM invocation — the platform's core capability — remain absent
and Delivery Gate 9 has no accepted specification. The 2026-08-18 project analysis
identified this divergence, and the user selected freezing the installation track as
the first recommendation.

## Decision

Stop starting new installation-subsystem derivative work. In particular, do not begin
the evidence body/reference/redaction schema, host observation or revalidation, a
production evidence resolver or evidence store adapter, permission or native gateway
composition, installer packaging, retention or cleanup, or anti-rollback work until a
Delivery Gate 16 distribution task — or an earlier accepted decision that names a real
consumer — requires it.

The already delivered installation contracts, formats, stores, and tests remain in the
tree, remain covered by the existing regression, and keep their recorded limitations.
Defect fixes in delivered installation code remain allowed as ordinary bounded tasks.

## Rationale

The installation subsystem was consuming successive increments ahead of any consumer
while the platform's defining capability had none. Freezing at a completed, verified,
delivered boundary loses no work, keeps the tree green, and redirects capacity to the
dependency-ordered gates the roadmap already prioritizes.

## Consequences

- The previously recorded next action to define the evidence body/reference and host
  revalidation contract is withdrawn rather than deferred piecemeal.
- Roadmap authority is unchanged: installation distribution work resumes under
  Delivery Gate 16, or earlier only through an accepted decision naming a real
  consumer.
- No delivered code, test, document, or evidence is removed or rewritten by this
  decision.
