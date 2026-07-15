package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
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
}
