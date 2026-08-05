package com.enhancer.cli;

import com.enhancer.runtime.SchedulerServicePolicy;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

record SchedulerServiceCliCommand(
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
        Duration processTimeout,
        Optional<RuntimeEventPublicationCliConfiguration> runtimeEventPublication,
        SchedulerServicePolicy policy) implements SchedulerExecutionCliCommand {
}
