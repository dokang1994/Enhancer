package com.enhancer.cli;

import java.nio.file.Path;
import java.util.Objects;

record SchedulerMigrateCycleCheckpointCliCommand(
        Path cycleCheckpointRoot) implements CliCommand {

    SchedulerMigrateCycleCheckpointCliCommand {
        Objects.requireNonNull(
                cycleCheckpointRoot,
                "cycleCheckpointRoot must not be null");
        cycleCheckpointRoot =
                cycleCheckpointRoot.toAbsolutePath().normalize();
    }
}
