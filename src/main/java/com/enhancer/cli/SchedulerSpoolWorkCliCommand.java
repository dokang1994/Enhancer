package com.enhancer.cli;

import java.nio.file.Path;
import java.time.Instant;

record SchedulerSpoolWorkCliCommand(
        Path projectRoot,
        Path transportSpoolRoot,
        String destinationName,
        String taskId,
        int maxPendingPublications,
        String messageId,
        String correlationId,
        String logicalRunId,
        String producer,
        Instant occurredAt,
        String targetPath,
        String expectedSha256) implements CliCommand {
}
