# User authorization on 2026-07-27 to commit, push, and merge the completed work to

Status: Accepted Decision

## Context

The priority/fairness selector, queue schema-v3 migration, and durable priority-aware
claim increments are completed and freshly verified in the working tree. The user
explicitly authorized committing and pushing all completed work and merging it to
`origin/main`.

The working branch is already local `main`, and a fresh fetch showed local `main` and
`origin/main` at the same commit before delivery. There is therefore no distinct topic
branch whose history requires a merge commit.

## Decision

Deliver the completed increments directly from local `main` to `origin/main` after a
fresh strict build and complete staged-diff review. Use ordinary commits and a
non-forced push. Do not manufacture an empty or content-free merge commit merely to
describe the update as a merge.

After the push, fetch or query the remote branch and require local `main`,
`origin/main`, and the remote-advertised `refs/heads/main` identity to agree before
recording delivery completion.

## Rationale

Directly advancing `main` is the only history-preserving operation when the completed
work already exists in that branch's working tree and the local and remote branch tips
share the same base. A redundant merge commit would add no integration evidence and
would misrepresent the actual branch topology.

## Consequences

- The completed increment may be committed and pushed to `origin/main` under the
  user's explicit authority.
- Force push, history rewrite, branch deletion, release packaging, and deployment
  remain unauthorized.
- Remote delivery is not complete until the remote branch identity is freshly
  verified.
