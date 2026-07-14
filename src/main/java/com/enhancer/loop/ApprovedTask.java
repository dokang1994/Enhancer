package com.enhancer.loop;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public record ApprovedTask(
        String taskId,
        String description,
        String approvalEvidence,
        Set<String> allowedTools,
        String sourceDocument) {

    private static final Pattern TASK_ID = Pattern.compile("[a-z0-9][a-z0-9._-]{0,127}");

    public ApprovedTask {
        Objects.requireNonNull(taskId, "taskId must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(approvalEvidence, "approvalEvidence must not be null");
        Objects.requireNonNull(allowedTools, "allowedTools must not be null");
        Objects.requireNonNull(sourceDocument, "sourceDocument must not be null");
        if (!TASK_ID.matcher(taskId).matches()) {
            throw new IllegalArgumentException("taskId must be a stable lowercase identifier");
        }
        if (description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        if (approvalEvidence.isBlank()) {
            throw new IllegalArgumentException("approvalEvidence must not be blank");
        }
        if (sourceDocument.isBlank()) {
            throw new IllegalArgumentException("sourceDocument must not be blank");
        }

        Set<String> scope = new LinkedHashSet<>();
        for (String toolName : allowedTools) {
            Objects.requireNonNull(toolName, "allowedTools must not contain null");
            if (toolName.isBlank()) {
                throw new IllegalArgumentException("allowedTools must not contain blank names");
            }
            scope.add(toolName);
        }
        if (scope.isEmpty()) {
            throw new IllegalArgumentException("allowedTools must not be empty");
        }
        allowedTools = Set.copyOf(scope);
    }

    public boolean allows(String toolName) {
        return toolName != null && allowedTools.contains(toolName);
    }
}
