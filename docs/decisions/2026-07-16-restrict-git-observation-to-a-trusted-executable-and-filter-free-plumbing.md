# 2026-07-16: Restrict Git Observation To A Trusted Executable And Filter-Free Plumbing

Status: Accepted Decision

Context:

- `git status --porcelain` and a worktree `git diff --no-textconv` still consult a repository-configured required clean filter when resolving stat-dirty content, so an observed repository can execute a command before Tool policy applies.
- Invoking `git` by name also delegates executable discovery to process-launch rules. On Windows, an Enhancer process started in the untrusted repository can therefore resolve a repository-controlled `git.exe` before PATH.
- Focused adversarial verification established that `git ls-files --stage --deleted --others --exclude-standard` avoids the configured clean filter, while both `ls-files --modified` and `diff-files --raw --no-ext-diff --no-textconv` re-enter that pipeline. A filter-free tracked-worktree comparison has therefore not been established.

Decision:

- Resolve Git from absolute PATH directory entries, canonicalize the executable, and reject any candidate whose real path is contained by the real observed project root. If no such executable exists, publish explicit `UNAVAILABLE` observations instead of attempting discovery by name.
- Reduce command authority to one fixed, shell-free, read-only `ls-files` invocation for index, deleted, and untracked metadata. Keep `--no-optional-locks`, invocation-scoped fsmonitor disable, inherited `GIT_*` removal, project discovery ceiling, timeout, output cap, and discarded stderr.
- Publish the `GIT_DIFF` observation as explicitly `UNAVAILABLE` without starting a process until a filter-free tracked-worktree method is separately established and verified.
- Treat Git output only as untrusted bytes entering a SHA-256 digest. Retain no paths, file contents, configuration, or command output.
- Do not rewrite repository configuration or temporarily disable filters. Safety comes from command choice and trusted executable resolution, not mutation of the observed project.

Rationale:

Observation must be less privileged than the project it inspects. An absolute executable outside the real project removes current-directory/PATH-name hijacking, while index/stat plumbing avoids the conversion pipeline that executes clean filters. Returning unavailable is safer than silently widening command authority when trusted resolution cannot be established.

Consequences:

- The status digest remains sensitive to index, deleted, and untracked path metadata. Tracked unstaged content modifications are deliberately not observed, and the diff observation explains that safety limitation instead of implying availability.
- PATH itself is still host input, not cryptographic installation provenance. Absolute-entry and project-containment checks close the repository-controlled lookup vector; signature/package verification remains out of scope.
- One external command remains authorized only inside `GitWorkspaceCollector`; no other component receives command authority.
