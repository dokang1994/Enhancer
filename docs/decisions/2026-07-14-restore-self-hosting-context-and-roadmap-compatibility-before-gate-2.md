# 2026-07-14: Restore Self-Hosting Context And Roadmap Compatibility Before Gate 2

Status: Accepted Decision

Context:

- The Planner still recognizes only the retired `## Phase ...` and `Status: Ready` grammar.
- The canonical Roadmap now uses `## Delivery Gate ...` and `Status: Specified - Next`, so the Planner cannot propose the actual next Enhancer task.
- The executable Context Reader loads only eight root documents even though repository startup governance requires `.ai/` to be read first.
- Existing tests use the retired Roadmap grammar and synthetic root-only context, so they do not detect either self-hosting regression.

Decision:

- Restore the self-hosting planning path before beginning Delivery Gate 2.
- Make the seven governed `.ai/` Markdown files explicit required context inputs before the eight canonical root documents.
- Replace the retired phase parser with the accepted Delivery Gate maturity grammar and select the first gate marked `Specified - Next`.
- Map the selected gate's required-capability or scope bullets into proposal scope and its exit criteria into proposal acceptance criteria.
- Add a regression test that reads the actual Enhancer `ROADMAP.md`, plus context-order tests that prove `.ai/` is loaded first.

Rationale:

Repository-backed memory and deterministic next-task proposal are core self-hosting promises. Later Tool and evidence work must not proceed while the front of that flow is known to reject the repository's own source of truth.

Consequences:

- Delivery Gate 2 remains next but is temporarily blocked by this foundation recovery task.
- The Planner intentionally follows the current canonical Delivery Gate grammar rather than retaining undocumented compatibility with the retired Phase/Ready format.
- Adding or removing governed `.ai/` bootstrap documents requires synchronizing the explicit executable context list.
- Dynamic Markdown interpretation, proposal ranking, LLM planning, and automatic task acceptance remain out of scope.
