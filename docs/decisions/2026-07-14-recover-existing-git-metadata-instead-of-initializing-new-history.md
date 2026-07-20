# 2026-07-14: Recover Existing Git Metadata Instead Of Initializing New History

Status: Accepted Decision

Context:

- The active C:\Enhancer directory contains the project files but no .git metadata.
- PowerShell history shows that the GitHub repository was cloned into the nested C:\Enhancer\Enhancer path.
- Windows Recycle Bin metadata identifies the deleted nested clone and its Enhancer origin, main branch, and commit history.
- Running git init in the active directory would create unrelated history and lose the existing repository relationship.

Decision:

- Prefer the existing Recycle Bin metadata, but if it is unavailable before copying, create a temporary no-checkout clone from the verified Enhancer origin and copy only its .git metadata into C:\Enhancer.
- Preserve the available recovery source until validation completes and verify that non-.git working files are unchanged.
- Do not perform checkout, reset, clean, commit, push, or other worktree reconciliation as part of recovery.

Consequences:

- Existing Git history and origin can be restored without overwriting current work; a fresh clone may be used when the deleted metadata is no longer available.
- The restored status may show substantial local differences that must be reviewed separately.
- Recovery success does not authorize committing or pushing those differences.
