# Current Task

## Status

Completed

## Task

Execute the four user-selected recommendations from the 2026-08-18 project analysis in
order: record the installation-track freeze decision, define the minimum Delivery Gate 9
model gateway vertical slice as an accepted specification, add a host-independent
continuous-integration verification job, and amend the commit-cadence rule through the
constitutional amendment process.

## Task ID

execute-2026-08-18-recommendation-track

## Context

The installation filesystem store delivery is Completed and remotely visible at
`fb1380c5f452a04d5c69521b89565fb9ddcc08f8` with a clean worktree and an empty
checkpoint. The 2026-08-18 analysis found installation derivatives running ahead of
their Delivery Gate 16 consumers while prompt and LLM invocation remain absent, no
accepted Delivery Gate 9 specification, single-host-only verification evidence, and
verified work accumulating uncommitted under the current commit-cadence rule. The user
selected the four recommendations, their order, the Dynamic Workflow form, and Opus
subagents for delegated read-only analysis.

## Justified By

- User request on 2026-08-18 to execute the project-analysis recommendation track

## Approval

The accepted track decision authorizes document, decision, and RFC authoring, the
continuous-integration workflow file, the constitutional commit-cadence amendment
through its full Section 14 process, focused and full verification, development-session
checkpoints, bounded read-only Opus subagent dispatches, and one ordinary non-amending
commit plus one non-force push of `main` per completed verified increment.

It does not authorize force push, rebase, reset, amend, squash, synthetic merge commit,
tag, release, deployment, model or paid-service invocation, credential operation,
real installation or permission mutation, destructive cleanup, or any external message
beyond the named Git remote operations.

## Acceptance Criteria

- One accepted decision freezes installation-subsystem derivative work until its
  consumers exist, and the owning documents state the freeze without duplicating it.
- One accepted specification defines the minimum Delivery Gate 9 model gateway vertical
  slice with its port boundary, one concrete provider adapter shape, credential and
  budget handling, evidence persistence, verification plan, and explicit exclusions,
  and this document records the follow-up implementation task.
- One continuous-integration workflow file runs the complete Java 17 Markdown-sensitive
  Gradle verification on an external host for `main` pushes and pull requests, and its
  first triggered run is observed or its observation is explicitly recorded as pending.
- The commit-cadence sentence of the Constitution is amended through an approved
  decision, a version change, mirror review, and synchronized operating documents, so
  that a verified GREEN increment boundary inside an approved Active Task authorizes an
  ordinary local commit without a separate per-commit request while push authority
  remains explicit.
- Each increment ends with fresh focused governance verification, an ordinary commit,
  and a non-force `main` push; the final increment ends with a fresh full Java 17
  Markdown-sensitive regression.

## Out Of Scope

Gate 9 implementation code, model or paid-service invocation, credentials, installation
or evidence-resolver work of any kind, force push, rebase, reset, amend, squash, tag,
release, deployment, destructive cleanup, permission mutation, background execution,
and external messages beyond authorized Git pushes and the continuous-integration
service consuming the pushed workflow file.

## Allowed Tools

- read-file
- write-docs
- write-code
- build-output
- verify
- checkpoint
- git-inspect
- git-stage
- git-commit
- git-push-non-force
- git-remote-inspect
- subagent-read-only-opus

## Dynamic Workflow

Workflow ID: execute-2026-08-18-recommendation-track

Mode: Sequential

Increment Limit: 4

Selection Rule: Select the first dependency-ready Pending increment in document order.

Stop Conditions: Stop on failed verification, governance-test failure that cannot be
resolved inside the selected increment, remote divergence or non-fast-forward ancestry,
network or authentication failure that cannot be safely retried, subagent bound
exhaustion, task/checkpoint drift, scope expansion, or insufficient authority.

### Increment 1 - record-installation-track-freeze-decision

State: Completed

Depends On: none

Scope: Record one accepted decision that stops installation-subsystem derivative work
(evidence body, resolver, host revalidation, permission/native composition) until its
consumers exist, index it, and synchronize the owning documents.

Exit Criteria: The decision file and index entry exist and match, owning documents
state the freeze once, and focused governance tests pass.

Verification: Focused decision-index, document-ownership, and dynamic-workflow tests,
`git diff --check`, and staged-boundary review before the increment commit and push.

Next Action: Define the minimum Delivery Gate 9 model gateway slice specification.

### Increment 2 - define-gate-9-model-gateway-minimum-slice

State: Completed

Depends On: record-installation-track-freeze-decision

Scope: Using at most three read-only Opus subagent dispatches for seam and constraint
analysis, author the Gate 9 minimum-slice specification as RFC-0013 plus one accepted
decision covering the provider-neutral gateway port, one concrete provider adapter
shape, credential and budget handling, evidence persistence, verification plan, and
exclusions, and record the follow-up implementation task.

Exit Criteria: RFC-0013 and its accepted decision exist and are indexed, the roadmap
RFC track lists RFC-0013, and focused governance tests pass.

Verification: Focused decision-index, document-ownership, and dynamic-workflow tests,
`git diff --check`, and staged-boundary review before the increment commit and push.

Next Action: Add the continuous-integration verification job.

### Increment 3 - add-continuous-integration-verification-job

State: Completed

Depends On: define-gate-9-model-gateway-minimum-slice

Scope: Add one continuous-integration workflow file that runs the complete Java 17
Markdown-sensitive Gradle verification on an external Linux host for `main` pushes and
pull requests, and synchronize the owning documentation.

Exit Criteria: The workflow file is delivered to `main`, its first triggered run is
observed passing or its observation is explicitly recorded as pending with the reason,
and focused governance tests pass locally.

Verification: Local focused governance tests, `git diff --check`, staged-boundary
review, the increment commit and push, and best-effort observation of the first remote
run.

Next Action: Amend the commit-cadence rule through the constitutional process.

### Increment 4 - amend-commit-cadence-rule

State: Completed

Depends On: add-continuous-integration-verification-job

Scope: Amend the constitutional commit-cadence sentence so a verified GREEN increment
boundary inside an approved Active Task authorizes an ordinary local commit without a
separate per-commit request, keep push authority explicit, record the accepted
amendment decision, change the Constitution version, review the `.ai/` mirror, and
synchronize `AGENTS.md` and `.ai/workflow.md`.

Exit Criteria: The amended sentence, version change, mirror review, index entry, and
synchronized operating documents exist, and a fresh full Java 17 Markdown-sensitive
regression passes.

Verification: Full Java 17 Markdown-sensitive regression, focused governance tests,
`git diff --check`, staged-boundary review, and the final increment commit and push.

Next Action: Define the Gate 9 implementation task recorded by the accepted
specification.

## Verification

- Increment 1: the track and freeze decisions, their index entries, the reshaped task,
  and the changelog entry passed all 13 focused governance tests across the five
  architecture suites with zero failures, errors, or skips; `git diff --check` was
  clean. Evidence is appended once in `docs/verification-log.md`.
- Increment 2: RFC-0013, its accepted decision and index entry, and both RFC
  registrations were authored from two reconciled read-only Opus subagent surveys
  and passed the focused governance suites with `git diff --check` clean. Evidence
  is appended once in `docs/verification-log.md`.
- Increment 3: the continuous-verification workflow file, the restored `gradlew`
  executable bit, and the owning-document updates passed YAML parsing and the
  focused governance suites with `git diff --check` clean; observation of the first
  triggered run is recorded as pending in `docs/verification-log.md` because the
  delivering push itself triggers it.
- Increment 3 observation: the first triggered run completed successfully on the
  Linux host with a green complete `test` task execution on a fresh checkout, as
  recorded in `docs/verification-log.md`.
- Increment 4: the Section 14 amendment (Constitution 1.2.0), its accepted decision
  and index entry, the synchronized operating documents, and the reviewed mirror
  passed the fresh full Java 17 Markdown-sensitive regression and the focused
  governance suites recorded in `docs/verification-log.md`.

## Next

Define the Delivery Gate 9 minimum-slice implementation task under RFC-0013: the
`com.enhancer.model` package with the provider-neutral gateway port, the deterministic
fake as the only executed gateway, the bounded provider adapter shape, and the
`model-invoke` Tool composed into the existing executor, delivered test-first with the
named promoting integration test.
