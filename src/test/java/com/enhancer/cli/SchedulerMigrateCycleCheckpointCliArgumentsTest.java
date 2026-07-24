package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

class SchedulerMigrateCycleCheckpointCliArgumentsTest {

    @Test
    void parsesTheExplicitCycleCheckpointMigrationCommand() {
        assertInstanceOf(
                SchedulerMigrateCycleCheckpointCliCommand.class,
                CliArguments.parse(new String[] {
                        "scheduler-migrate-cycle-checkpoint",
                        "--cycle-checkpoint-root", "checkpoint"
                }));
    }
}
