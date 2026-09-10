package com.enhancer.cli;

import com.enhancer.runtime.SchedulerPriority;
import java.nio.file.Path;

/** Exact explicit interface input for deterministic-fake typed ModelWork publication. */
record DeterministicFakeModelSpoolCliCommand(
        Path projectRoot,
        Path submissionRoot,
        Path transportSpoolRoot,
        String taskId,
        String submissionId,
        int maxWorkItems,
        int maxPendingPublications,
        String producer,
        String targetPath,
        String expectedResponseSha256,
        Path modelExecutionProfileFile,
        SchedulerPriority priority) implements CliCommand {
}
