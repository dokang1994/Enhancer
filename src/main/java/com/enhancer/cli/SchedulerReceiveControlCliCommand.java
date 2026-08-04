package com.enhancer.cli;

import java.nio.file.Path;
import java.util.Optional;

record SchedulerReceiveControlCliCommand(
        Path transportSpoolRoot,
        String messageFile,
        String destinationName,
        Path runtimeRoot,
        String goalId,
        Optional<RuntimeEventPublicationCliConfiguration> runtimeEventPublication)
        implements CliCommand {
}
