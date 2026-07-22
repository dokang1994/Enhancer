package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CliArgumentsTest {
    @TempDir
    Path temporaryRoot;

    @Test
    void parsesEveryExplicitRunInput() {
        Path project = temporaryRoot.resolve("project");
        Path evidence = temporaryRoot.resolve("evidence");
        Path records = temporaryRoot.resolve("records");

        CliCommand command = CliArguments.parse(new String[] {
                "run",
                "--project-root", project.toString(),
                "--task-id", "gate-5-test",
                "--target-path", "target.txt",
                "--expected-sha256", "a".repeat(64),
                "--evidence-root", evidence.toString(),
                "--run-record-root", records.toString()
        });

        RunCliCommand run = (RunCliCommand) command;
        assertEquals(project.toAbsolutePath().normalize(), run.projectRoot());
        assertEquals("gate-5-test", run.taskId());
        assertEquals("target.txt", run.targetPath());
        assertEquals("a".repeat(64), run.expectedSha256());
        assertEquals(evidence.toAbsolutePath().normalize(), run.evidenceRoot());
        assertEquals(records.toAbsolutePath().normalize(), run.runRecordRoot());
    }

    @Test
    void parsesReplayAndRejectsMissingDuplicateUnknownOrMalformedInputs() {
        ReplayCliCommand replay = (ReplayCliCommand) CliArguments.parse(new String[] {
                "replay",
                "--run-record-root", temporaryRoot.resolve("records").toString(),
                "--reference", "run-record/00000000-0000-0000-0000-000000000001"
        });

        assertEquals(
                temporaryRoot.resolve("records").toAbsolutePath().normalize(),
                replay.runRecordRoot());
        assertEquals(
                "run-record/00000000-0000-0000-0000-000000000001",
                replay.reference());

        assertThrows(CliUsageException.class, () -> CliArguments.parse(new String[] {"run"}));
        assertThrows(CliUsageException.class, () -> CliArguments.parse(new String[] {
                "replay", "--reference", "run-record/00000000-0000-0000-0000-000000000001"
        }));
        assertThrows(CliUsageException.class, () -> CliArguments.parse(new String[] {
                "unknown", "--value", "x"
        }));
        assertThrows(CliUsageException.class, () -> CliArguments.parse(new String[] {
                "replay", "--run-record-root", "records", "--run-record-root", "again",
                "--reference", "run-record/00000000-0000-0000-0000-000000000001"
        }));
        assertThrows(CliUsageException.class, () -> CliArguments.parse(new String[] {
                "run",
                "--project-root", temporaryRoot.toString(),
                "--task-id", "gate-5-test",
                "--target-path", "target.txt",
                "--expected-sha256", "NOT-A-DIGEST",
                "--evidence-root", temporaryRoot.resolve("evidence").toString(),
                "--run-record-root", temporaryRoot.resolve("records").toString()
        }));
    }

    @Test
    void parsesEveryExplicitSchedulerCycleInput() {
        SchedulerCycleCliCommand cycle = (SchedulerCycleCliCommand) CliArguments.parse(
                schedulerCycleArguments("2", "300000", "20000"));

        assertEquals(temporaryRoot.resolve("project").toAbsolutePath().normalize(),
                cycle.projectRoot());
        assertEquals(temporaryRoot.resolve("queue").toAbsolutePath().normalize(),
                cycle.queueRoot());
        assertEquals("00000000-0000-0000-0000-000000000801", cycle.queueId());
        assertEquals(temporaryRoot.resolve("runtime").toAbsolutePath().normalize(),
                cycle.runtimeRoot());
        assertEquals(temporaryRoot.resolve("effects").toAbsolutePath().normalize(),
                cycle.externalEffectRoot());
        assertEquals(temporaryRoot.resolve("checkpoint").toAbsolutePath().normalize(),
                cycle.cycleCheckpointRoot());
        assertEquals(temporaryRoot.resolve("evidence").toAbsolutePath().normalize(),
                cycle.evidenceRoot());
        assertEquals(temporaryRoot.resolve("records").toAbsolutePath().normalize(),
                cycle.runRecordRoot());
        assertEquals(temporaryRoot.resolve("invocations").toAbsolutePath().normalize(),
                cycle.invocationRoot());
        assertEquals("scheduler-owner", cycle.ownerId());
        assertEquals(2, cycle.maxAttempts());
        assertEquals(Duration.ofMinutes(5), cycle.leaseDuration());
        assertEquals(Duration.ofSeconds(20), cycle.processTimeout());
    }

    @Test
    void rejectsMissingAndOutOfRangeSchedulerCycleInputs() {
        assertThrows(CliUsageException.class, () -> CliArguments.parse(new String[] {
                "scheduler-cycle", "--project-root", temporaryRoot.toString()
        }));
        assertThrows(CliUsageException.class, () -> CliArguments.parse(
                schedulerCycleArguments("0", "300000", "20000")));
        assertThrows(CliUsageException.class, () -> CliArguments.parse(
                schedulerCycleArguments("17", "300000", "20000")));
        assertThrows(CliUsageException.class, () -> CliArguments.parse(
                schedulerCycleArguments("2", "0", "20000")));
        assertThrows(CliUsageException.class, () -> CliArguments.parse(
                schedulerCycleArguments("2", "300000", "not-a-number")));
    }

    private String[] schedulerCycleArguments(
            String maxAttempts,
            String leaseMillis,
            String processTimeoutMillis) {
        return new String[] {
                "scheduler-cycle",
                "--project-root", temporaryRoot.resolve("project").toString(),
                "--queue-root", temporaryRoot.resolve("queue").toString(),
                "--queue-id", "00000000-0000-0000-0000-000000000801",
                "--runtime-root", temporaryRoot.resolve("runtime").toString(),
                "--external-effect-root", temporaryRoot.resolve("effects").toString(),
                "--cycle-checkpoint-root", temporaryRoot.resolve("checkpoint").toString(),
                "--evidence-root", temporaryRoot.resolve("evidence").toString(),
                "--run-record-root", temporaryRoot.resolve("records").toString(),
                "--invocation-root", temporaryRoot.resolve("invocations").toString(),
                "--owner-id", "scheduler-owner",
                "--max-attempts", maxAttempts,
                "--lease-millis", leaseMillis,
                "--process-timeout-millis", processTimeoutMillis
        };
    }
}
