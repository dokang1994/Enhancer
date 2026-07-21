package com.enhancer.session;

import java.util.Objects;
import java.util.regex.Pattern;

record DevelopmentTaskContract(String taskId, String status, String sha256) {
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    DevelopmentTaskContract {
        Objects.requireNonNull(taskId, "taskId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        if (taskId.isBlank() || status.isBlank()) {
            throw new IllegalArgumentException("task identity and status must not be blank");
        }
        if (sha256 == null || !SHA_256.matcher(sha256).matches()) {
            throw new IllegalArgumentException("sha256 must be lowercase SHA-256");
        }
    }
}
