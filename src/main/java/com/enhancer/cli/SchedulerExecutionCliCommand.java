package com.enhancer.cli;

import java.nio.file.Path;
import java.time.Duration;

/** Inputs shared by one-cycle and bounded foreground Scheduler execution commands. */
interface SchedulerExecutionCliCommand extends CliCommand {
    Path projectRoot();

    Path queueRoot();

    String queueId();

    Path runtimeRoot();

    Path externalEffectRoot();

    Path cycleCheckpointRoot();

    Path evidenceRoot();

    Path runRecordRoot();

    Path invocationRoot();

    String ownerId();

    int maxAttempts();

    Duration leaseDuration();

    Duration processTimeout();
}
