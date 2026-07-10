# Tool Example

This is a conceptual example for the future Tool System.

```text
Tool: ReadFile
Input:
  path: CONSTITUTION.md
Output:
  success: true
  content: ...
```

Key rule:

Tool input and output should be structured. Avoid returning untyped strings when behavior depends on the result.
