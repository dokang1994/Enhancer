# User authorization on 2026-08-24 to correct cross-platform ModelWork golden fixtures and complete delivery

Status: Accepted Decision

## Context

The first non-force delivery push placed commit `78633d1` on remote `main`, but its
push-triggered Linux/Temurin 17 verification failed one legacy-frame golden assertion.
The uploaded report proves that production encoded `agent-loop-🚀` as canonical UTF-8,
while the Windows-authored expected constants retained bytes for the mojibake text
`agent-loop-ðŸš€`. The user explicitly authorized continuation after this exact
correction scope and delivery sequence was presented.

## Decision

Authorize one bounded test-first correction and delivery workflow: reproduce the
platform-sensitive golden failure under explicit UTF-8 compilation, replace only the
affected legacy-frame test fixtures with canonical UTF-8 bytes, preserve production
codec behavior and the ModelWork-only v2 contract, run focused and full README-owned
Java 17 verification, commit the verified correction, and push local `main` to remote
`main` without force after fresh ancestry checks.

After the triggered GitHub Actions run succeeds, append the exact delivery evidence,
synchronize the active task and handoff, commit that bounded documentation increment,
push it without force after another fresh ancestry check, and observe the final
push-triggered verification to success.

## Consequences

- Production Java, message schemas, wire-family selection, runtime behavior, capability
  maturity, and architecture remain unchanged.
- Only test fixtures and lifecycle/delivery documents may change.
- Any unrelated failure, remote divergence, non-fast-forward refusal, unexpected ref,
  or failed external verification stops the workflow.
- No workflow rerun, force operation, history rewrite, tag, release, deployment,
  permission change, destructive cleanup, or additional implementation is authorized.
