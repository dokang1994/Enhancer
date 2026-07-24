package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

class SchedulerInvocationStatusCliArgumentsTest {

    @Test
    void parsesTheBoundedReadOnlyInvocationStatusCommand() {
        assertInstanceOf(
                SchedulerInvocationStatusCliCommand.class,
                CliArguments.parse(new String[] {
                        "scheduler-invocation-status",
                        "--queue-root", "queue",
                        "--queue-id", "00000000-0000-0000-0000-000000000b01",
                        "--runtime-root", "runtime",
                        "--cycle-checkpoint-root", "checkpoint",
                        "--run-record-root", "records",
                        "--invocation-root", "invocations",
                        "--limit", "8"
                }));
    }
}
