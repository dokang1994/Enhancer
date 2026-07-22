package com.enhancer.cli;

import java.nio.file.Path;

/**
 * Generated-input Scheduler submission. The operator retains only the canonical submission UUID
 * and the caller-owned intent; the queue, correlation, and logical-run identities and the
 * occurrence time are generated, so no explicit message/correlation/logical-run/queue identity or
 * time is supplied. Submission remains separate from execution.
 */
record GeneratedSubmitCliCommand(
        Path projectRoot,
        Path submissionRoot,
        Path queueRoot,
        String taskId,
        String submissionId,
        int maxWorkItems,
        String requiredCapability,
        String producer,
        String targetPath,
        String expectedSha256) implements CliCommand {
}
