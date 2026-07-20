# 2026-07-12: Implement Context Reader As A Single Java Module

Status: Accepted Decision

Decision:

The first Repository Context Reader is implemented in a single Gradle Java 17 project under `com.enhancer.context`. The required document order is represented by an enum, and the returned context uses immutable records.

Rationale:

This matches the existing architecture guide and provides a stable structured input for future planning without premature modules, Spring wiring, or domain abstractions.

Consequences:

- Required startup documents have one canonical code-level order.
- Missing documents fail with a checked exception that identifies the path.
- Future context sources can build on `ProjectContext` without changing this task into a full Context Builder.
