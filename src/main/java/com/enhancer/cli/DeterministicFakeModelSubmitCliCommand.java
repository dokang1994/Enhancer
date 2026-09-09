package com.enhancer.cli;

import com.enhancer.runtime.SchedulerPriority;
import java.nio.file.Path;

/** Exact explicit interface input for deterministic-fake typed ModelWork submission. */
record DeterministicFakeModelSubmitCliCommand(
        Path projectRoot,
        Path submissionRoot,
        Path queueRoot,
        String taskId,
        String submissionId,
        int maxWorkItems,
        String producer,
        String targetPath,
        String expectedResponseSha256,
        Path modelExecutionProfileFile,
        SchedulerPriority priority) implements CliCommand {
}
