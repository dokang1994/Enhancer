package com.enhancer.context;

public enum RequiredProjectDocument {
    AI_CONSTITUTION(".ai/constitution.md"),
    AI_WORKFLOW(".ai/workflow.md"),
    AI_CODING_RULES(".ai/coding_rules.md"),
    AI_ARCHITECTURE(".ai/architecture.md"),
    AI_PROMPT_RULES(".ai/prompt_rules.md"),
    AI_MEMORY(".ai/memory.md"),
    AI_SKILL_RULES(".ai/skill_rules.md"),
    CONSTITUTION("CONSTITUTION.md"),
    AGENTS("AGENTS.md"),
    ARCHITECTURE("ARCHITECTURE.md"),
    PROJECT_STATE("PROJECT_STATE.md"),
    ROADMAP("ROADMAP.md"),
    CURRENT_TASK("CURRENT_TASK.md"),
    DECISION_LOG("DECISION_LOG.md"),
    SESSION_HANDOFF("SESSION_HANDOFF.md");

    private final String path;

    RequiredProjectDocument(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }
}
