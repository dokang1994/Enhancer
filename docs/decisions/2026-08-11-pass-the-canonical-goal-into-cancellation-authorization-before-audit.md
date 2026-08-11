# 2026-08-11: Pass The Canonical Goal Into Cancellation Authorization Before Audit

Status: Accepted Decision

Context:

- The detached signed grant binds an exact Goal and the authorization audit must persist
  only after that target binding is verified.
- `ControlRequestAuthorizer.authorize(MessageEnvelope)` receives the retained request,
  but `MessageEnvelope` carries no Goal identity. The current application checks the
  returned approval Goal only after the authorizer has returned, which is too late for
  an audit-persisting authorizer.
- Capturing a caller-supplied expected Goal in one authorizer construction can work only
  if every future composition forwards the same Goal to both the authorizer and
  application. That convention is not enforced by the shared application contract.

Decision:

- Change `ControlRequestAuthorizer` to
  `authorize(String canonicalGoalId, MessageEnvelope retainedRequest)`.
- `AuthenticatedCancellationApplication` supplies its already canonicalized target
  Goal together with the exact retained request before any approval or audit work.
- Do not retain a default or legacy envelope-only authorization overload. The
  application's existing post-decision Goal, Control-message, and `CANCEL` checks remain
  defense in depth.

Rationale:

Passing the application target through the trusted port is the smallest contract that
makes exact Goal verification mandatory before an audit-backed authorizer can persist
authority evidence. It prevents a future adapter from accidentally separating the Goal
used to verify a grant from the Goal passed to the application.

Consequences:

- Existing authorizer lambdas and tests require a mechanical two-argument update.
- A wrong signed or invoked Goal is denied before authorization audit, runtime, or event
  mutation.
- This change adds no credential, trust source, CLI, queue disposition, process signal,
  or runtime/event schema.
