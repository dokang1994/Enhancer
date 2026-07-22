package com.enhancer.cli;

import java.nio.file.Path;
import java.time.Instant;

record SchedulerSubmitCliCommand(
        Path projectRoot,
        Path submissionRoot,
        Path queueRoot,
        String taskId,
        String queueId,
        int maxWorkItems,
        String requiredCapability,
        String messageId,
        String correlationId,
        String logicalRunId,
        String producer,
        Instant occurredAt,
        String targetPath,
        String expectedSha256) implements CliCommand {
}
