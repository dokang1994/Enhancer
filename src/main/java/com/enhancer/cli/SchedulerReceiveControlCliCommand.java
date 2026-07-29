package com.enhancer.cli;

import java.nio.file.Path;

record SchedulerReceiveControlCliCommand(
        Path transportSpoolRoot,
        String messageFile,
        String destinationName,
        Path runtimeRoot,
        String goalId) implements CliCommand {
}
