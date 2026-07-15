package com.enhancer.run;

import com.enhancer.tool.ExecutionPolicy;
import com.enhancer.tool.ToolRequest;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record PolicyDecision(
        PolicyDecisionStatus status,
        String projectRoot,
        Set<String> allowedTools,
        Set<String> deniedTools,
        long maxReadBytes,
        long timeoutMillis) {

    public PolicyDecision {
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(projectRoot, "projectRoot must not be null");
        Objects.requireNonNull(allowedTools, "allowedTools must not be null");
        Objects.requireNonNull(deniedTools, "deniedTools must not be null");
        if (projectRoot.isBlank()) {
            throw new IllegalArgumentException("projectRoot must not be blank");
        }
        if (maxReadBytes <= 0) {
            throw new IllegalArgumentException("maxReadBytes must be positive");
        }
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeoutMillis must be positive");
        }
        allowedTools = snapshot(allowedTools, "allowedTools");
        deniedTools = snapshot(deniedTools, "deniedTools");
    }

    public static PolicyDecision from(ExecutionPolicy policy, ToolRequest request) {
        Objects.requireNonNull(policy, "policy must not be null");
        Objects.requireNonNull(request, "request must not be null");
        PolicyDecisionStatus status;
        if (policy.deniedTools().contains(request.toolName())) {
            status = PolicyDecisionStatus.DENIED_EXPLICITLY;
        } else if (!policy.allowedTools().contains(request.toolName())) {
            status = PolicyDecisionStatus.DENIED_NOT_ALLOWLISTED;
        } else {
            status = PolicyDecisionStatus.ALLOWED;
        }
        return new PolicyDecision(
                status,
                policy.projectRoot().toString(),
                policy.allowedTools(),
                policy.deniedTools(),
                policy.maxReadBytes(),
                policy.timeout().toMillis());
    }

    private static Set<String> snapshot(Set<String> values, String fieldName) {
        Set<String> copy = new LinkedHashSet<>();
        for (String value : values) {
            Objects.requireNonNull(value, fieldName + " must not contain null");
            if (value.isBlank()) {
                throw new IllegalArgumentException(fieldName + " must not contain blank values");
            }
            copy.add(value);
        }
        return Set.copyOf(copy);
    }
}
