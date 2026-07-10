# Agent Loop Example

This is a conceptual example. It is not the first implementation task unless `CURRENT_TASK.md` says so.

```text
Task: Read project context
Status: Pending

AgentLoop starts
Iteration 1:
  Agent reads required documents
  Agent updates context
  Task status becomes Complete
Loop stops
Result: Completed in 1 iteration
```

Key rule:

The loop must stop on completion or max iteration.
