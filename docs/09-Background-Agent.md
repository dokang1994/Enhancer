# 09 - Background Agent

## Codex Prompt

Design Background Agent behavior only after task planning and tool execution are safe.

## Goal

Background Agents handle long-running or scheduled project assistance.

## Candidate Jobs

- scan TODOs
- review stale tasks
- summarize recent commits
- suggest roadmap updates
- monitor failing tests

## Safety Rules

- Background Agent may propose changes.
- Background Agent may not push.
- Background Agent may not run destructive commands without approval.
- Background Agent must write summaries to repository documents or logs.

## Out Of Scope

- autonomous production deployment
- unattended destructive file changes
- secret handling
