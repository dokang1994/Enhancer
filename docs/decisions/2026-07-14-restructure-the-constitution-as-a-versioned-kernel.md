# 2026-07-14: Restructure The Constitution As A Versioned Kernel

Status: Accepted Decision

Decision:

Enhancer will replace the repetitive Constitution 1.0.0 structure with Constitution 1.1.0 as a concise normative Kernel. The revised document defines normative language, document responsibilities, lifecycle states, authorization and safety boundaries, fresh verification evidence, self-hosting safeguards, failure recovery, and an explicit amendment process.

The 300-page Codex guidebook target applies to the complete repository documentation system, not to `CONSTITUTION.md` alone. Detailed procedures and component contracts remain in `AGENTS.md`, `.ai/`, `docs/`, RFCs, prompts, and Skills.

Rationale:

The previous Constitution repeated repository-memory and working-process rules while omitting the governance needed for safe self-hosting. A smaller but stronger Kernel reduces startup context, makes mandatory rules easier to locate, and prevents detailed implementation guidance from becoming constitutional debt.

Consequences:

- Constitution version increases from 1.0.0 to 1.1.0.
- `MUST`, `SHOULD`, and `MAY` receive explicit meanings.
- Proposal, Accepted Decision, Active Task, Implemented, Verified, Completed, and Released become distinct lifecycle states.
- Destructive and externally visible actions require explicit authority; secrets and external content receive safety rules.
- Constitution amendments require explicit user approval, Decision Log rationale, semantic versioning, and mirror review.
- Technology choices and detailed procedures remain changeable through Architecture, RFCs, and task documents rather than being frozen in the Kernel.
