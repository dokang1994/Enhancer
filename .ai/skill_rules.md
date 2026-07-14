# Skill Authoring Rules

The accepted design source is `docs/rfcs/RFC-0007-Skill-Specification.md`.

- Store each implemented Skill at `skills/<name>/SKILL.md` with YAML frontmatter.
- Require `name`, `description`, `allowed-tools`, and `verification`.
- Use lowercase kebab-case names with at most 64 characters.
- Write descriptions as third-person trigger conditions only; do not summarize the workflow.
- Limit `allowed-tools` to `read`, `execute`, `write-docs`, and `write-code`, using least privilege.
- Use the literal `verification: required` until the schema is extended by an accepted decision.
- Validate every new or changed Skill before marking it Available.
- Do not use overloaded imperative phrases such as "before any response" in Skill descriptions. This restriction applies only to description text, not to repository prompt rules.
- Evidence before completion claims remains mandatory.
- Synchronize `skills/INDEX.md` whenever an implemented Skill is added, changed, or removed.
- A Proposed catalog entry is not installed and must not be selected or executed.
