package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
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

    @Test
    void parsesEveryExplicitSchedulerSubmitInput() {
        SchedulerSubmitCliCommand submit = (SchedulerSubmitCliCommand) CliArguments.parse(
                schedulerSubmitArguments("8", "2026-07-22T16:00:00Z"));

        assertEquals(temporaryRoot.resolve("project").toAbsolutePath().normalize(),
                submit.projectRoot());
        assertEquals(temporaryRoot.resolve("submissions").toAbsolutePath().normalize(),
                submit.submissionRoot());
        assertEquals(temporaryRoot.resolve("queue").toAbsolutePath().normalize(),
                submit.queueRoot());
        assertEquals("scheduler-submit-test", submit.taskId());
        assertEquals("00000000-0000-0000-0000-000000000c01", submit.queueId());
        assertEquals(8, submit.maxWorkItems());
        assertEquals("read-file-worker", submit.requiredCapability());
        assertEquals("00000000-0000-0000-0000-000000000c02", submit.messageId());
        assertEquals("scheduler-submit-correlation", submit.correlationId());
        assertEquals("scheduler-submit-logical-run", submit.logicalRunId());
        assertEquals("scheduler-submit-cli-test", submit.producer());
        assertEquals(Instant.parse("2026-07-22T16:00:00Z"), submit.occurredAt());
        assertEquals("target.txt", submit.targetPath());
        assertEquals("a".repeat(64), submit.expectedSha256());
    }

    @Test
    void rejectsMalformedSchedulerSubmitInputs() {
        assertThrows(CliUsageException.class, () -> CliArguments.parse(new String[] {
                "scheduler-submit", "--project-root", temporaryRoot.toString()
        }));
        assertThrows(CliUsageException.class, () -> CliArguments.parse(
                schedulerSubmitArguments("0", "2026-07-22T16:00:00Z")));
        assertThrows(CliUsageException.class, () -> CliArguments.parse(
                schedulerSubmitArguments("4097", "2026-07-22T16:00:00Z")));
        assertThrows(CliUsageException.class, () -> CliArguments.parse(
                schedulerSubmitArguments("8", "not-an-instant")));
        String[] badUuid = schedulerSubmitArguments("8", "2026-07-22T16:00:00Z");
        badUuid[10] = "not-a-uuid";
        assertThrows(CliUsageException.class, () -> CliArguments.parse(badUuid));
        String[] badDigest = schedulerSubmitArguments("8", "2026-07-22T16:00:00Z");
        badDigest[28] = "NOT-A-DIGEST";
        assertThrows(CliUsageException.class, () -> CliArguments.parse(badDigest));
        String[] duplicate = Arrays.copyOf(
                schedulerSubmitArguments("8", "2026-07-22T16:00:00Z"), 31);
        duplicate[29] = "--producer";
        duplicate[30] = "duplicate-producer";
        assertThrows(CliUsageException.class, () -> CliArguments.parse(duplicate));
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

    private String[] schedulerSubmitArguments(String maxWorkItems, String occurredAt) {
        return new String[] {
                "scheduler-submit",
                "--project-root", temporaryRoot.resolve("project").toString(),
                "--submission-root", temporaryRoot.resolve("submissions").toString(),
                "--queue-root", temporaryRoot.resolve("queue").toString(),
                "--task-id", "scheduler-submit-test",
                "--queue-id", "00000000-0000-0000-0000-000000000c01",
                "--max-work-items", maxWorkItems,
                "--required-capability", "read-file-worker",
                "--message-id", "00000000-0000-0000-0000-000000000c02",
                "--correlation-id", "scheduler-submit-correlation",
                "--logical-run-id", "scheduler-submit-logical-run",
                "--producer", "scheduler-submit-cli-test",
                "--occurred-at", occurredAt,
                "--target-path", "target.txt",
                "--expected-sha256", "a".repeat(64)
        };
    }
}
