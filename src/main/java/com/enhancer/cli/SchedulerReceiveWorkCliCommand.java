package com.enhancer.cli;

import com.enhancer.runtime.SchedulerPriority;
import java.nio.file.Path;

record SchedulerReceiveWorkCliCommand(
        Path transportSpoolRoot,
        String messageFile,
        String destinationName,
        Path queueRoot,
        String queueId,
        String requiredCapability,
        SchedulerPriority priority) implements CliCommand {
}
