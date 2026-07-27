package com.enhancer.cli;

import java.nio.file.Path;

record SchedulerMigrateQueueCliCommand(
        Path queueRoot,
        String queueId) implements CliCommand {
}
