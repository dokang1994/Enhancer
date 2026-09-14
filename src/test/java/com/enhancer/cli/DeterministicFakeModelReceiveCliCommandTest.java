package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeterministicFakeModelReceiveCliCommandTest {
    @TempDir
    Path temporaryRoot;

    @Test
    void parsesExactlyFourRequiredLocatorOptions() {
        DeterministicFakeModelReceiveCliCommand command =
                (DeterministicFakeModelReceiveCliCommand) CliArguments.parse(arguments());
        assertEquals(temporaryRoot.resolve("spool").toAbsolutePath().normalize(),
                command.transportSpoolRoot());
        assertEquals("00000000-0000-0000-0000-000000000f67.transport",
                command.messageFile());
        assertEquals(temporaryRoot.resolve("submissions").toAbsolutePath().normalize(),
                command.submissionRoot());
        assertEquals(temporaryRoot.resolve("queue").toAbsolutePath().normalize(),
                command.queueRoot());
    }

    @Test
    void hasOnlyTheClosedLocatorShapeAndRequiresEveryOption() {
        assertTrue(DeterministicFakeModelReceiveCliCommand.class.isRecord());
        assertTrue(Modifier.isFinal(
                DeterministicFakeModelReceiveCliCommand.class.getModifiers()));
        assertFalse(Modifier.isPublic(
                DeterministicFakeModelReceiveCliCommand.class.getModifiers()));
        assertArrayEquals(
                new String[] {"transportSpoolRoot", "messageFile",
                        "submissionRoot", "queueRoot"},
                Arrays.stream(DeterministicFakeModelReceiveCliCommand.class
                                .getRecordComponents())
                        .map(RecordComponent::getName)
                        .toArray(String[]::new));
        String[] complete = arguments();
        for (int index = 1; index < complete.length; index += 2) {
            List<String> missing = new ArrayList<>(Arrays.asList(complete));
            missing.remove(index);
            missing.remove(index);
            assertThrows(CliUsageException.class,
                    () -> CliArguments.parse(missing.toArray(String[]::new)));
        }
    }

    @Test
    void rejectsNoncanonicalFilenameAndEveryAuthorityOption() {
        for (String file : new String[] {
                "not-a-uuid.transport",
                "00000000-0000-0000-0000-000000000F67.transport",
                "00000000-0000-0000-0000-000000000f67.received",
                "../00000000-0000-0000-0000-000000000f67.transport"
        }) {
            assertThrows(CliUsageException.class,
                    () -> CliArguments.parse(replacing("--message-file", file)));
        }
        for (String forbidden : new String[] {
                "project-root", "task-id", "model-execution-profile-file",
                "required-capability", "priority", "max-work-items", "queue-id",
                "destination-name", "producer", "target-path",
                "expected-response-sha256", "occurred-at", "model-execution",
                "provider", "endpoint", "credential"
        }) {
            List<String> augmented = new ArrayList<>(Arrays.asList(arguments()));
            augmented.add("--" + forbidden);
            augmented.add("value");
            assertThrows(CliUsageException.class,
                    () -> CliArguments.parse(augmented.toArray(String[]::new)));
        }
    }

    private String[] arguments() {
        return new String[] {
                "scheduler-receive-deterministic-fake-model-work",
                "--transport-spool-root", temporaryRoot.resolve("spool").toString(),
                "--message-file",
                "00000000-0000-0000-0000-000000000f67.transport",
                "--submission-root", temporaryRoot.resolve("submissions").toString(),
                "--queue-root", temporaryRoot.resolve("queue").toString()
        };
    }

    private String[] replacing(String option, String value) {
        String[] result = arguments();
        result[List.of(result).indexOf(option) + 1] = value;
        return result;
    }
}
