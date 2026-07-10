# Review Task Prompt

Use this prompt for code or design review.

## Review Focus

Prioritize:

1. Bugs and behavioral regressions
2. Missing or weak tests
3. Architecture violations
4. Source-of-truth conflicts in documents
5. Maintainability risks

## Output Format

Report findings first, ordered by severity, with file and line references where possible.

Then include:

- Open questions
- Test gaps
- Brief summary

If no issues are found, say so clearly and mention remaining residual risk.
