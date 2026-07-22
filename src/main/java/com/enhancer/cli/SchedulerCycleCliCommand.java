package com.enhancer.cli;

import java.nio.file.Path;
import java.time.Duration;

record SchedulerCycleCliCommand(
        Path projectRoot,
        Path queueRoot,
        String queueId,
        Path runtimeRoot,
        Path externalEffectRoot,
        Path cycleCheckpointRoot,
        Path evidenceRoot,
        Path runRecordRoot,
        Path invocationRoot,
        String ownerId,
        int maxAttempts,
        Duration leaseDuration,
        Duration processTimeout) implements CliCommand {
}
