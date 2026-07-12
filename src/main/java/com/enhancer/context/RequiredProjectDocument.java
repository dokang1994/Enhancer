package com.enhancer.context;

public enum RequiredProjectDocument {
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
