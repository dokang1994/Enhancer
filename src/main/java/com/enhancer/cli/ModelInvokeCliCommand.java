package com.enhancer.cli;

import java.nio.file.Path;
import java.util.Objects;

record ModelInvokeCliCommand(
        Path projectRoot,
        String taskId,
        String prompt,
        String modelClass,
        String expectedSha256,
        Path evidenceRoot,
        Path runRecordRoot,
        long timeoutMillis,
        int maxResponseLength) implements CliCommand {

    ModelInvokeCliCommand {
        Objects.requireNonNull(projectRoot, "projectRoot must not be null");
        Objects.requireNonNull(taskId, "taskId must not be null");
        Objects.requireNonNull(prompt, "prompt must not be null");
        Objects.requireNonNull(modelClass, "modelClass must not be null");
        Objects.requireNonNull(expectedSha256, "expectedSha256 must not be null");
        Objects.requireNonNull(evidenceRoot, "evidenceRoot must not be null");
        Objects.requireNonNull(runRecordRoot, "runRecordRoot must not be null");
    }
}
