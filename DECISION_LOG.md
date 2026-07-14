# Decision Log

## Accepted Decisions

### 2026-07-14: Adopt V1-V3 Evolution And A Provenance-Preserving Project Brain

Status: Accepted Decision

Context:

- Repository Markdown and Git provide durable human-readable memory, but they do not directly represent the relationships among decisions, architecture, dependencies, tasks, executions, tests, bugs, commits, issues, and pull requests.
- The long-term product needs Cursor-level productivity, an Agent development platform, and finally an AI Operating System without confusing those maturity levels.
- Agents, Skills, workflows, and models have distinct responsibilities that must remain separable for plugins, marketplaces, security review, and routing.
- A one-sentence user intent should reduce human orchestration, but hidden Git publication, merge, deployment, or permission escalation would violate the Constitution.

Decision:

- Define three product milestones: **V1 AI Development Experience**, **V2 AI Development Platform**, and **V3 AI Operating System**.
- V1 provides Cursor-level productivity through CLI/editor/Desktop surfaces and Workspace awareness; it does not redefine Enhancer as an IDE or Cursor clone.
- V2 provides durable workflows, Skills, Memory, Agent Runtime, MCP, model routing, plugins, marketplace foundations, and self-hosting development support.
- V3 provides the AI Kernel, Project Brain knowledge graphs, multi-agent operating model, hybrid privacy-aware model routing, scheduler, plugin ecosystem, and governed synchronization/self-improvement.
- Define AI Kernel responsibilities as Agent lifecycle, memory/context allocation, resource budgets, locks and leases, scheduling, cancellation, policy, event routing, recovery, and audit state.
- Treat Git and canonical repository documents as authoritative records. Project Brain graphs are provenance-bearing, freshness-aware, rebuildable projections over documents, code, Git, RunRecords, issues, PRs, tests, and external metadata; they do not silently replace their sources.
- Project Brain includes Decision, Architecture, Dependency, Task, and Execution graphs, with explicit links to code, tests, bugs, commits, issues, and pull requests.
- Distinguish extension types: an Agent plugin supplies a role/capability worker, a Skill supplies a validated workflow, a Tool performs an external capability, and a Workflow composes events, Skills, Agents, Tools, verification, and approval gates.
- Add a privacy-aware Model Router that selects local or remote providers from task capability, data classification, policy, cost, latency, and availability. Sensitive content defaults to an approved local route and cannot be sent remotely without policy authority.
- The one-sentence user experience compiles intent into an inspectable goal, dependency plan, authorization scope, execution graph, verification, and audit trail. External or destructive workflow stages still require explicit or pre-authorized policy approval.

Rationale:

The differentiator is the Kernel below IDEs, not another editor shell. A graph projection enables impact reasoning while preserving the repository as recoverable memory. Separating Agents, Skills, Tools, workflows, and models prevents marketplace extensions or model output from silently gaining authority.

Consequences:

- VS Code, IntelliJ, Desktop, web, and CLI can share the same Kernel and Project Brain.
- V1, V2, and V3 are product milestones, not claims about current implementation maturity.
- Workflow automation may cover issue, branch, development, test, review, commit, push, PR, and merge, but each externally visible or destructive transition must satisfy the approval policy recorded in the run.
- Marketplace packages require provenance, signatures or integrity evidence appropriate to risk, compatibility metadata, permissions, isolation, review, disable, removal, and rollback.
- Knowledge Graph storage technology remains undecided until the Project Brain delivery gate; the contract is graph semantics and provenance, not a specific graph database.
- Local Llama or other on-device models and remote Claude, GPT, Gemini, DeepSeek, or future providers remain adapters behind the same Model Gateway.

### 2026-07-14: Make Enhancer An Event-Driven Interoperable AI Operating Platform

Status: Accepted Decision

Context:

- The current foundation proves repository context, planning, governed Tool execution, evidence, and bounded Agent Loop transitions, but it does not yet provide an operating substrate for long-lived or multi-role work.
- A linear Chat -> Tool -> Stop design cannot support Planner -> Coder -> Reviewer -> Tester pipelines, resumable scheduling, external clients, or independent evolution of runtime components.
- Workspace awareness, reusable Skills, MCP interoperability, and model routing have become core platform requirements rather than optional editor features.
- Event Bus and IPC Message Bus responsibilities overlap unless their semantic and transport layers are explicitly separated.

Decision:

- The final product target is **Enhancer OS**, composed of Desktop, CLI, API, Workspace, Project Brain, Memory, MCP, Agent Runtime, Event Bus, Skill Engine, Plugin Marketplace, Model Router, Scheduler, and governed Cloud Sync.
- Use one typed messaging architecture: the Event Bus defines domain events and subscriptions; the Message Bus provides envelopes, queues, delivery, replay, and backpressure; IPC is a transport adapter for the same envelopes across process boundaries.
- Runtime Agents MUST communicate through the bus once the runtime boundary exists. Direct Planner-to-Coder or Coder-to-Reviewer calls are not the target architecture.
- Build the Agent Runtime as a persisted state machine around Goal -> Planner -> Executor -> Memory -> Reflection -> Retry -> Done, with bounded budgets and explicit stop reasons.
- Add a first-class Workspace layer for project files, active and selected context, Git state, diagnostics, terminal-session metadata, and later editor state. Project Brain combines governed repository memory with Workspace observations; it does not replace either source.
- Treat Skills as validated, progressively loaded, composable workflows that can form chains such as Spring -> Java -> Database -> Test while preserving least-privilege Tool scope.
- Promote MCP to a core interoperability layer with both server and client boundaries so Claude Code, Cursor, VS Code, and other model clients can share governed Tools, resources, and memory.
- Keep the immediate Gate 4 verification and RunRecord dependency order. Introduce Workspace, messaging, runtime, MCP/model gateway, Skill, plugin, interface, multi-agent, sync, and self-improvement capabilities only through their delivery gates.

Rationale:

An AI operating system needs durable state, shared context, asynchronous coordination, reusable behavior, and interoperable capability exposure. Separating semantic events from transport avoids duplicate buses while allowing the first implementation to remain in-process and later gain IPC or durable queues without rewriting Agent contracts.

Consequences:

- The current sequential Agent controller remains a verified bootstrap slice and will later become a runtime worker behind bus contracts.
- Event envelopes require identity, causation, correlation, schema version, provenance, authorization context, and idempotency semantics before asynchronous execution.
- Workspace is a governed snapshot boundary, not unrestricted editor or terminal access.
- MCP, plugins, Skills, and models cannot bypass Tool policy, evidence, independent verification, RunRecord, or user approval.
- Multi-agent execution follows a stable single-agent runtime and durable messaging; it is not introduced as direct Agent-to-Agent calls.
- Cloud Sync remains opt-in and must define encryption, conflict resolution, ownership, and secret exclusion before implementation.
- The owner's qualitative assessment that roughly 20-25% of the intended foundation is established is recorded as planning context, not verified capability maturity or release progress.

### 2026-07-14: Harden Agent Runs With Repository Approval And Semantic Progress

Status: Accepted Decision

Context:

- Gate 3 integrates real Tool results, but `approvedTask` is only a non-blank string and the integration test does not derive it from repository context.
- Progress currently includes an opaque evidence reference, so identical content stored under a new UUID can appear to be progress and evade stagnation detection.
- Tool failures expose only success or failure; a real retry policy cannot distinguish timeout, cancellation, denial, invalid requests, or temporary failures without parsing prose.
- The public `AgentRunState` record constructor accepts caller-supplied progress keys and structurally valid states outside the governed transition path.

Decision:

- Introduce `ApprovedTask` and `ApprovedTaskReader`. The reader consumes `CURRENT_TASK.md` from `ProjectContext` and requires explicit `Task ID`, `Status: In Progress`, `Task`, `Approval`, and `Allowed Tools` sections.
- Bind every initial `ToolRequest` to the approved task's Tool-name scope before execution policy is applied. Repository approval evidence is explicit provenance, not a cryptographic signature or permission escalation.
- Add structured `ToolFailureCode` to every failed `ToolResult`; successful results carry no failure code. The executor assigns codes at its policy and execution boundaries.
- Provide a standard failure classifier that retries only explicitly temporary failures and timeouts; all other codes are terminal by default.
- Add an optional SHA-256 content digest to `VerificationEvidence.capture` and use it for semantic progress. Opaque storage references and human-readable summaries do not define progress identity.
- Replace the public Agent run record constructor with a final immutable class whose constructor is private. Only the initial approval factory and package-owned controller transitions can construct states.

Rationale:

These changes turn the RED scenarios into production invariants instead of test conventions. Approval, failure semantics, and progress equality become structured and deterministic, while state transitions remain controlled by the orchestration boundary.

Consequences:

- A repository task without an active status, explicit approval evidence, or Tool scope cannot start an Agent run.
- Execution policy remains an additional deny-over-allow boundary; repository approval cannot broaden it.
- Identical evidence content remains identical progress even when persisted at different references.
- Retry behavior no longer depends on diagnostic message text.
- Gate 4 can consume structured approval, failure, and evidence identities in its verifier and RunRecord.
- Signature-backed approval, argument-level authorization, identity federation, mutation Tools, independent verification, and RunRecord persistence remain deferred.

### 2026-07-14: Stop Tool Success At The Independent Verification Boundary

Status: Accepted Decision

Context:

- Delivery Gate 3 must connect a real `ToolResult` to the bounded Agent Loop without implementing the Gate 4 independent verifier.
- Treating Tool success as task completion would let worker output bypass the accepted rule that completion requires successful independent verification.
- Retry behavior must be deterministic without parsing diagnostic prose or allowing a Tool to grant itself execution authority.

Decision:

- Add immutable `AgentRunState` with an externally approved task, a caller-supplied pending `ToolRequest`, the last `ToolResult`, loop status, and deterministic progress key.
- Add `AWAITING_VERIFICATION` as an explicit terminal loop state and stop reason; a successful Tool result reaches this boundary but cannot reach `COMPLETED` in Gate 3.
- Add `AgentRunController` as the orchestration owner. It consumes an existing `ToolExecutor`, immutable `ExecutionPolicy`, and external `ToolFailureClassifier`; it does not implement Tools, create requests, expand policy, or approve tasks.
- Keep terminal failures as `FAILED`; retain the same pending request only for failures classified `RETRYABLE`.
- Derive progress keys from canonical Tool request/result content so identical retry results activate the existing maximum-iteration and stagnation rules.
- Reuse the existing bounded loop engine for both the small `AgentLoopState` contract and the richer Agent run state without weakening the 20/3 defaults or precedence rules.

Rationale:

The verification-wait boundary preserves separation of duties while still proving the real Context -> approved task -> Tool -> evidence -> loop transition. External retry classification avoids unreliable message parsing and prevents a Tool from deciding its own authority or finality.

Consequences:

- Gate 3 can stop successfully executed work without claiming it is verified or completed.
- Gate 4 becomes the only component allowed to turn an independently accepted result into `COMPLETED`.
- Repeated identical retryable failures remain bounded and observable as `STAGNATED`.
- A denied Tool remains uninvoked even if its implementation would attempt mutation; the controller cannot alter the caller's allow/deny policy.
- LLM decisions, Tool mutation, Git or network authorization, independent verification, RunRecord persistence, CLI wiring, and multi-agent routing remain deferred.

### 2026-07-14: Persist Complete Evidence As Atomic Integrity-Checked Envelopes

Status: Accepted Decision

Context:

- `VerificationEvidence` requires a complete-output reference when its 4096-character tail is truncated, but no current component makes that reference real.
- Gate 2 must provide durable resolution and corruption detection without broadening Tool authority or fabricating references.
- Separate payload and metadata files would make atomic publication across both files difficult in the first filesystem implementation.
- Automatic retention cleanup would delete data and is not required to prove the Gate 2 exit criteria.

Decision:

- Add an `EvidenceStore` boundary with a filesystem implementation that generates UUID run and evidence identities.
- Store metadata and UTF-8 payload together in one versioned binary envelope.
- Publish evidence by atomic move from a temporary file in the final run directory; do not fall back to a non-atomic move.
- Record creation time, UTF-8 byte length, and SHA-256 digest and validate all of them during resolution.
- Use opaque `evidence/<run-id>/<evidence-id>` references and reject malformed, missing, oversized, or corrupted artifacts.
- Add an explicit maximum-content and retention-duration policy, but perform no automatic cleanup in Gate 2.
- Connect a persistence-enabled `ReadFileTool` through `EvidenceRecorder`; the request correlation identity is a run identity previously created by the store.

Rationale:

A single atomic envelope is the smallest durable format that keeps reference metadata and content consistent. Digest, length, strict decoding, bounded reads, and reference containment make evidence failures observable before later verification consumes them.

Consequences:

- Truncated Tool output can carry a real reference that resolves after the Tool call.
- Short output remains in memory and does not create unnecessary evidence artifacts.
- Gate 2 detects accidental or unauthorized artifact modification but does not provide encryption, signatures, or external tamper-proof storage.
- Evidence deletion, compaction, migration, distributed storage, Agent Loop integration, independent verification, and RunRecord remain deferred.

### 2026-07-14: Restore Self-Hosting Context And Roadmap Compatibility Before Gate 2

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

### 2026-07-14: Implement Gate 1 As A Bounded Read-Only Tool Boundary

Status: Accepted Decision

Context:

- Delivery Gate 0 verifies Tool result and evidence invariants but executes no Tool.
- Gate 1 must produce one real `ToolResult` without introducing evidence persistence, shell mutation, Git writes, network access, or LLM behavior.
- A path-prefix check alone is insufficient because traversal and symbolic links can escape an approved project root.
- Truncated evidence cannot truthfully reference complete output until Gate 2 provides an EvidenceStore.

Decision:

- Introduce immutable `ToolRequest`, `ExecutionPolicy`, and a minimal `Tool` interface under `com.enhancer.tool`.
- Use an in-process `ToolExecutor` registry with unique names and structured failure conversion.
- Make deny override allow and enforce cancellation before and after invocation plus a positive execution timeout.
- Implement `ReadFileTool` as the only production Tool in Gate 1.
- Require relative paths, real-path containment, regular files, strict UTF-8, and a policy size ceiling no greater than the existing 4096-character evidence boundary.
- Keep deterministic fake Tools in tests as the immediate consumer of the generic executor contract.

Rationale:

This is the smallest real external-boundary slice that exercises policy, execution, result, and evidence together. Strict path and output limits prevent the read-only first Tool from creating hidden authority or unverifiable truncated evidence.

Consequences:

- Allowed temporary project files can produce real successful `ToolResult` values.
- Denial, malformed input, traversal, missing files, invalid UTF-8, cancellation, timeout, and Tool exceptions remain observable failure results.
- The first size ceiling is intentionally conservative and may be raised only after Gate 2 persists full evidence.
- Agent Loop integration, evidence persistence, independent verification, CLI, shell, Git, network, and LLM behavior remain deferred.

### 2026-07-14: Promote Foundation Contracts Through Executable Vertical Slices

Status: Accepted Decision

Context:

- The repository has 21 production Java files and approximately 479 production lines centered on contracts and deterministic control rules.
- Context, planning, loop termination, and Tool evidence invariants are tested, but there is no application entry point, concrete Tool execution, Agent-Loop/Tool integration, evidence persistence, LLM boundary, or end-to-end runtime.
- The existing roadmap uses `Implemented` for narrow slices, which can be mistaken for operational product completeness.
- Continuing to add isolated contracts would increase skeleton breadth without proving that Enhancer can execute a useful governed workflow.

Decision:

- Use capability maturity states: Specified, Contract Verified, Integrated, Operational, and Released.
- Replace ambiguous standalone `Implemented` roadmap labels with the most precise verified maturity.
- Promote capabilities through executable vertical slices with explicit integration and operational exit gates.
- Supersede the sequential independent verifier as a standalone next task. First implement the bounded Tool execution boundary and evidence persistence, then integrate the Agent Loop and sequential verifier in the same E2E delivery track.
- Require every new foundation contract to name its current or immediately following integration consumer.

Rationale:

Control-plane safety contracts were necessary before Tool or LLM execution, but their value is realized only when they participate in an observable, recoverable run. Maturity gates prevent focused unit tests from being reported as product readiness and make the path from foundation to usable system explicit.

Consequences:

- The next product task is the first Tool Execution Boundary slice, not an isolated verifier record.
- The first operational milestone must run Context → Tool → Evidence → Verification → Stop → Run Record through a supported entry point.
- Independent verification remains mandatory but moves after real ToolResult production and evidence persistence.
- Skill, LLM, MCP, plugin, multi-agent, and self-improvement work remains gated behind the executable single-agent path.
- Roadmap, architecture guides, state, and handoff documents must use the new maturity vocabulary.

### 2026-07-14: Recover Existing Git Metadata Instead Of Initializing New History

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

### 2026-07-14: Restructure The Constitution As A Versioned Kernel

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

### 2026-07-14: Colocate Examples With Specifications And Tests

Status: Accepted Decision

Decision:

Enhancer will not maintain a standalone `examples/` directory. Conceptual examples belong in the `docs/` or RFC document that owns the contract, while executable examples belong in focused tests.

Rationale:

The standalone Agent Loop and Tool examples were already behind the implemented contracts. Colocation reduces duplicate descriptions, prevents conceptual samples from drifting away from code, and keeps the repository structure smaller.

Consequences:

- Remove `examples/agent-loop.md`, `examples/tool-example.md`, `examples/skill-example.md`, and the empty-directory marker.
- Do not treat examples as a separate source of truth.
- New conceptual examples must be updated with their owning specification.
- Observable executable behavior remains demonstrated and verified through tests.

### 2026-07-14: Adopt External Agent Harness Patterns Selectively

Status: Accepted Decision

Decision:

Enhancer will treat [MoAI-ADK](https://github.com/modu-ai/moai-adk) and similar agent harnesses as reference implementations rather than runtime dependencies. The first adopted pattern is an explicit terminal outcome for the deterministic Assisted Development Loop that composes repository context reading and task planning. Other useful patterns will be introduced only in the roadmap slice that owns them.

Rationale:

MoAI-ADK contains useful operational patterns, including explicit stop reasons, stagnation detection, bounded verification evidence, progressive Skill loading, artifact provenance, and approval-protected self-improvement. Importing its framework, provider-specific schemas, or Git workflow would duplicate Enhancer components and weaken the current document-driven approval model. Selective, provider-neutral adoption preserves the useful semantics without coupling the products.

Consequences:

- The current slice adds no MoAI package, command, generated configuration, or runtime dependency.
- The first Assisted Development Loop is a single read-and-plan pass with explicit outcomes and no repository mutation.
- Repeated-loop termination and stagnation are implemented in a separate Agent Loop slice.
- Verification evidence belongs to the Tool System; progressive loading belongs to the Skill System; provenance belongs to Plugin and template management.
- Token budgets follow LLM integration, while self-improvement requires snapshot, approval, verification, and rollback contracts before implementation.
- Claude-specific configuration, automatic commits or pushes, and parallel multi-agent orchestration are not adopted.

Adoption sequence:

1. Implement bounded repeated-loop termination and consecutive-state stagnation detection in the Agent Loop.
2. Define a bounded verification-evidence contract with Tool results.
3. Add a sequential independent verifier after the single-agent loop is stable.
4. Add progressive Skill loading while preserving the rule that Proposed catalog entries are not loadable.
5. Add artifact provenance when Plugin or template installation exists.
6. Add provider-neutral token and context budgets only after an LLM invocation boundary exists.
7. Implement self-improvement only after snapshot, human approval, independent verification, and rollback contracts exist.

The sequence does not conflict with `.ai/` rules: each item remains a small `CURRENT_TASK.md` scope, uses test-first verification for observable behavior, preserves proposal/decision/implemented-state separation, and cannot override the Constitution. The independent verifier begins as a sequential component, not multi-agent routing.

### 2026-07-14: Use Bounded Deterministic Agent Loop Termination

Status: Accepted Decision

Decision:

The first repeated Agent Loop uses immutable state transitions and explicit `COMPLETED`, `FAILED`, `MAX_ITERATIONS`, and `STAGNATED` stop reasons. The default ceiling is 20 executed steps. Stagnation means the progress key remains unchanged for 3 consecutive executed steps; both limits are constructor-configurable for focused tests and later runtime configuration.

Rationale:

Explicit bounded exits prevent silent infinite work before Tool or LLM execution is introduced. A caller-provided deterministic step keeps the loop independently testable and avoids premature Agent, Tool, prompt, or provider abstractions.

Consequences:

- Terminal status wins over stagnation after a step.
- Maximum iteration wins when its ceiling and the stagnation threshold coincide.
- Iteration count reports executed steps, including the terminal step.
- Maximum-iteration and stagnation results retain the latest running state for diagnosis.
- Tool execution, verification evidence, independent verification, LLM calls, and multi-agent routing remain out of scope.

### 2026-07-14: Bound Tool Verification Evidence

Status: Accepted Decision

Decision:

Every future Tool result will include structured verification evidence. The first contract limits summaries to 512 characters and retained output tails to 4096 characters. Output exceeding the tail limit must be marked truncated and include a non-blank reference to the complete output. The contract records original output length without implementing persistence.

Tool result status is explicit. An optional exit code supports process-like tools while allowing file or API tools to omit it. When present, success requires exit code zero and failure requires a non-zero code.

Rationale:

Unbounded command output would consume future Agent Context and obscure the most useful recent diagnostics. Keeping a bounded tail plus a reference preserves inspectability without introducing an LLM-specific token model, filesystem policy, or concrete Tool implementation.

Consequences:

- `VerificationEvidence` is mandatory on every `ToolResult`.
- Evidence summaries and output tails are bounded before they can enter Agent Context.
- Truncated output cannot be represented without a reference to the complete evidence.
- The contract does not claim that referenced evidence has been persisted or independently verified.
- Evidence storage, real Tool execution, Agent Loop integration, and the sequential independent verifier remain separate tasks.

### 2026-07-14: Use A Repository Gradle Wrapper

Status: Accepted Decision

Decision:

Enhancer will store a Gradle Wrapper in the repository and use Java 17 as the build runtime. A global Gradle installation is not required for normal project builds.

Rationale:

The Wrapper makes local development and future CI reproducible while matching the existing Java 17 Gradle build. It also removes reliance on user-specific cached Gradle paths.

Consequences:

- Developers run `gradlew.bat` on Windows or `./gradlew` on Unix-like systems.
- Wrapper scripts, properties, and the wrapper JAR are version-controlled.
- Java 17 remains an external prerequisite and is not committed to the repository.

### 2026-07-14: Adopt Verified Skill And Evidence Operating Rules

Status: Accepted Decision

Decision:

Enhancer will adopt repository-defined Skill authoring rules, memory distillation, test-first behavior for observable feature and bug-fix changes, and fresh verification evidence before completion claims. The initial Skill catalog remains explicitly proposed until corresponding `SKILL.md` files exist. Task cycles do not force commits; commits remain controlled by repository policy and user instruction.

Rationale:

These rules strengthen repeatability and verification while preserving Document Driven Development, least privilege, proposal-state separation, and the existing human approval boundary for Git operations.

Consequences:

- RFC-0002, RFC-0005, RFC-0007, RFC-0008, and RFC-0009 describe the accepted direction.
- `.ai/skill_rules.md` defines operational authoring constraints for future Skills.
- Proposed catalog entries cannot be treated as installed or available Skills.
- `allowed-tools` uses a small documented permission vocabulary rather than undeclared tool names.
- Actual Skill workflows, loading, and runtime enforcement remain future tasks.

### 2026-07-12: Start Planner With Deterministic Repository Proposals

Status: Accepted Decision

Decision:

The first Planner consumes `ProjectContext`, blocks proposals while `CURRENT_TASK.md` is active, and otherwise proposes the first ready roadmap phase. Its output has explicit `PROPOSAL` state and structured reason, scope, acceptance criteria, out-of-scope items, and risks.

Rationale:

This reaches the first self-hosting planning behavior without introducing an LLM, hidden chat context, document mutation, or premature ranking logic.

Consequences:

- Planner behavior is deterministic and unit-testable.
- A proposal cannot be confused with an accepted decision.
- Natural-language planning, proposal ranking, persistence, and execution remain future work.

### 2026-07-12: Implement Context Reader As A Single Java Module

Status: Accepted Decision

Decision:

The first Repository Context Reader is implemented in a single Gradle Java 17 project under `com.enhancer.context`. The required document order is represented by an enum, and the returned context uses immutable records.

Rationale:

This matches the existing architecture guide and provides a stable structured input for future planning without premature modules, Spring wiring, or domain abstractions.

Consequences:

- Required startup documents have one canonical code-level order.
- Missing documents fail with a checked exception that identifies the path.
- Future context sources can build on `ProjectContext` without changing this task into a full Context Builder.

### 2026-07-10: Manage Major Designs As RFCs

Status: Accepted Decision

Decision:

Enhancer will manage major design areas as RFC-style Markdown documents under `docs/rfcs/`, starting with RFC-0001 through RFC-0012.

Rationale:

The project is large enough that design topics need stable identifiers, reviewable history, and clear references. RFC-style documents make long-term architecture easier to maintain across multiple AI agents and sessions.

Consequences:

- Major architecture changes should add or update an RFC.
- Accepted direction should still be summarized in `DECISION_LOG.md`.
- RFC statuses distinguish Draft, Accepted, Implemented, and Superseded.
- The initial RFC track covers Constitution, AI Behavior, Prompt Contract, Context Builder, Planner, Tool, Skill, Memory, Multi-Agent, AI Operating System, Plugin SDK, and Self Improvement.

### 2026-07-10: Adopt Six-Month AI Development OS Roadmap

Status: Accepted Decision

Decision:

Enhancer will use a six-month open-source roadmap that evolves from Constitution, Architecture, Context, Agent Loop, and Tool toward Planner, Skill, Memory, Prompt Engine, MCP, Plugin, Git, Terminal, VSCode Extension, CLI, Dashboard, Multi-Agent, Scheduler, Background Agent, Self Improvement, Plugin SDK, and Open Source Release.

Rationale:

The target is larger than a 30-day prototype. A six-month roadmap gives the project realistic phases while keeping the 30-day self-hosting milestone as the first checkpoint.

Consequences:

- The 30-day goal remains the first self-hosting milestone.
- Long-term architecture is tracked separately from immediate implementation tasks.
- Work remains Sprint-based and document-driven.

### 2026-07-10: Read `.ai/` Before Every AI Work Session

Status: Accepted Decision

Decision:

Every AI agent should read the `.ai/` folder before starting work. The folder contains AI-only operational documents: `constitution.md`, `workflow.md`, `coding_rules.md`, `architecture.md`, `prompt_rules.md`, and `memory.md`.

Rationale:

The root documents are canonical, but `.ai/` gives agents a compact operational entry point. This allows the user to say "항상 .ai 폴더를 읽고 시작해" and have a consistent startup rule across Codex, Claude, GPT, and future Enhancer agents.

Consequences:

- `prompts/SESSION_START.md` includes `.ai/` in the required reading order.
- `AGENTS.md` requires agents to read `.ai/` before work.
- `.ai/` must mirror operational rules without replacing root canonical documents.

### 2026-07-10: Treat Docs As A Multi-Agent Prompt Book

Status: Accepted Decision

Decision:

Each major `docs/` chapter will end with a `Prompt Book` section containing separate prompts for Codex, Claude, and GPT.

Rationale:

Enhancer is developed by multiple AI agents with different strengths. A shared chapter can guide all agents, but each agent needs role-specific instructions to reduce ambiguity.

Consequences:

- Codex prompts focus on implementation and verification.
- Claude prompts focus on architecture and risk review.
- GPT prompts focus on explanation, task framing, and session continuity.
- New chapter documents should include all three prompt types.

### 2026-07-10: Use Explicit Session Resume Protocol

Status: Accepted Decision

Decision:

New ChatGPT sessions must be resumed by providing the core repository documents, because ChatGPT cannot automatically read the user's local Enhancer repository across sessions.

Rationale:

The project depends on repository-backed memory. Without an explicit resume protocol, a new session may rely on incomplete chat memory and drift away from the source of truth.

Consequences:

- `prompts/CHATGPT_SESSION_RESUME.md` defines the required upload/paste workflow.
- `SESSION_HANDOFF.md` must remain complete enough to recover short-term state.
- Documents override chat history when conflicts occur.
- The human owner controls final approval and push.

### 2026-07-10: Operate Enhancer As A Real Open Source Project

Status: Accepted Decision

Decision:

Enhancer will be operated as a real open source project, not as a one-off chat artifact or documentation-only repository. The project will include documentation, code, ADRs, tests, inline specification examples, and shared prompts for Codex, Claude, and GPT.

Rationale:

The expected scope is too large for a single chat session. A Git-managed, chapter-based, reviewed workflow allows the project to grow over months without losing architectural consistency.

Consequences:

- Work proceeds by Sprint and small tasks.
- Documentation and code evolve together.
- ADR review is required for meaningful design changes.
- AI roles are explicit: Codex implements; ChatGPT supports architecture, backend design, agent research, documentation, and review.
- Git repository documents remain the source of truth.

### 2026-07-10: Use Codex-Ready Chapter Documents

Status: Accepted Decision

Decision:

Enhancer will maintain feature documents under `docs/` as Codex-ready prompts. Each document should describe the goal, architecture, task boundary, tests, and out-of-scope items for a major platform capability.

Rationale:

The project is too large to drive from chat history. Chapter-based Markdown specifications allow Codex, Claude, GPT, and future Enhancer agents to implement one slice at a time from repository state.

Consequences:

- `docs/` is part of the operating system for development, not passive documentation.
- New major capabilities should receive a prompt-style specification before implementation.
- Implementation should proceed sprint by sprint rather than attempting a full Cursor-like platform at once.

### 2026-07-10: Use Document Driven Development

Status: Accepted Decision

Decision:

Enhancer will follow Document Driven Development. New work must move through Constitution, Architecture, ADR / Decision Log, Task, Implementation, Test, and Documentation Update before it is considered complete.

Rationale:

Enhancer depends on repository documents as durable memory. If code changes happen before architecture, decisions, and tasks are clarified, future AI sessions will lose the reason behind the implementation.

Consequences:

- Agents must not jump directly from idea to code.
- Important architectural changes must be recorded before or during implementation.
- `CURRENT_TASK.md` remains the scope boundary for implementation.
- Documentation update is part of Definition of Done.

### 2026-07-10: Build Enhancer As A Self-Hosting AI Development OS

Status: Accepted Decision

Decision:

Enhancer is not a Cursor clone. Enhancer is a self-hosting AI Development Operating System that should eventually read its own repository context, understand project state, propose the next task, and assist its own development.

Rationale:

The project goal is not to copy Cursor's interface or behavior. The goal is to build a durable framework where AI agents can resume work from repository state and eventually help operate the project themselves.

Consequences:

- The first product slice should prioritize context reading and task planning over UI polish.
- Repository documents are product inputs, not only project management artifacts.
- The 30-day milestone is for Enhancer to propose next tasks from repository context.

### 2026-07-10: Use Repository Documents As Durable Memory

Status: Accepted Decision

Decision:

Enhancer will use repository documents as the durable memory for future ChatGPT and Codex sessions.

Rationale:

Conversation memory is unreliable across sessions. Repository files can be read, reviewed, committed, and treated as the single source of truth.

Consequences:

- Agents must read the required documents at session start.
- `SESSION_HANDOFF.md` must be updated at session close.
- Proposals must not be treated as accepted decisions until recorded here.

## Proposals

- Define the product scope for Enhancer.
- Choose the initial implementation stack.
