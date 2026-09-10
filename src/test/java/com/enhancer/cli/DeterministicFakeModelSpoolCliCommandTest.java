package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.runtime.SchedulerPriority;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeterministicFakeModelSpoolCliCommandTest {

    @TempDir
    Path temporaryRoot;

    @Test
    void parsesExactlyTheTwelveRequiredPublisherInputs() {
        DeterministicFakeModelSpoolCliCommand command =
                (DeterministicFakeModelSpoolCliCommand) CliArguments.parse(arguments());

        assertEquals(temporaryRoot.resolve("project").toAbsolutePath().normalize(),
                command.projectRoot());
        assertEquals(temporaryRoot.resolve("submissions").toAbsolutePath().normalize(),
                command.submissionRoot());
        assertEquals(temporaryRoot.resolve("spool").toAbsolutePath().normalize(),
                command.transportSpoolRoot());
        assertEquals("typed-model-spool", command.taskId());
        assertEquals("00000000-0000-0000-0000-000000000f27", command.submissionId());
        assertEquals(8, command.maxWorkItems());
        assertEquals(16, command.maxPendingPublications());
        assertEquals("typed-model-publisher", command.producer());
        assertEquals("prompts/request.txt", command.targetPath());
        assertEquals("a".repeat(64), command.expectedResponseSha256());
        assertEquals(Path.of("profiles/model.profile"),
                command.modelExecutionProfileFile());
        assertEquals(SchedulerPriority.EXPEDITED, command.priority());
    }

    @Test
    void hasOnlyTheExactClosedPublisherValueShape() {
        assertTrue(DeterministicFakeModelSpoolCliCommand.class.isRecord());
        assertTrue(Modifier.isFinal(
                DeterministicFakeModelSpoolCliCommand.class.getModifiers()));
        assertFalse(Modifier.isPublic(
                DeterministicFakeModelSpoolCliCommand.class.getModifiers()));
        assertArrayEquals(
                new String[] {
                        "projectRoot",
                        "submissionRoot",
                        "transportSpoolRoot",
                        "taskId",
                        "submissionId",
                        "maxWorkItems",
                        "maxPendingPublications",
                        "producer",
                        "targetPath",
                        "expectedResponseSha256",
                        "modelExecutionProfileFile",
                        "priority"
                },
                Arrays.stream(DeterministicFakeModelSpoolCliCommand.class
                                .getRecordComponents())
                        .map(RecordComponent::getName)
                        .toArray(String[]::new));
        assertArrayEquals(
                new Class<?>[] {
                        Path.class,
                        Path.class,
                        Path.class,
                        String.class,
                        String.class,
                        int.class,
                        int.class,
                        String.class,
                        String.class,
                        String.class,
                        Path.class,
                        SchedulerPriority.class
                },
                Arrays.stream(DeterministicFakeModelSpoolCliCommand.class
                                .getRecordComponents())
                        .map(RecordComponent::getType)
                        .toArray(Class<?>[]::new));

        Set<String> names = Arrays.stream(
                        DeterministicFakeModelSpoolCliCommand.class.getRecordComponents())
                .map(RecordComponent::getName)
                .map(String::toLowerCase)
                .collect(Collectors.toUnmodifiableSet());
        for (String forbidden : new String[] {
                "capability", "queueroot", "queueid", "destination", "messageid",
                "correlation", "logicalrun", "occurredat", "allowedtool", "snapshot",
                "executionpolicy", "candidate", "gateway", "provider", "endpoint",
                "credential", "price", "network", "process"
        }) {
            assertTrue(names.stream().noneMatch(name -> name.contains(forbidden)),
                    () -> "publisher command must not expose forbidden input: " + forbidden);
        }
    }

    @Test
    void requiresEveryOptionAndRejectsDuplicateUnknownAndForbiddenOptions() {
        String[] complete = arguments();
        for (int index = 1; index < complete.length; index += 2) {
            List<String> missing = new ArrayList<>(Arrays.asList(complete));
            missing.remove(index);
            missing.remove(index);
            assertThrows(CliUsageException.class,
                    () -> CliArguments.parse(missing.toArray(String[]::new)));
        }

        assertThrows(CliUsageException.class,
                () -> CliArguments.parse(appending("--producer", "duplicate")));
        assertThrows(CliUsageException.class,
                () -> CliArguments.parse(appending("--unknown", "value")));
        String[] extraToken = Arrays.copyOf(arguments(), arguments().length + 1);
        extraToken[extraToken.length - 1] = "extra";
        assertThrows(CliUsageException.class, () -> CliArguments.parse(extraToken));

        for (String forbidden : new String[] {
                "queue-root", "queue-id", "destination-name", "required-capability",
                "capability", "message-id", "message-file", "correlation-id",
                "logical-run-id",
                "occurred-at", "allowed-tool", "snapshot-id", "execution-policy",
                "candidate", "gateway", "model-execution", "provider", "endpoint",
                "credential", "price", "network", "process-configuration",
                "expected-sha256"
        }) {
            assertThrows(CliUsageException.class,
                    () -> CliArguments.parse(appending("--" + forbidden, "value")));
        }
    }

    @Test
    void rejectsMalformedBoundsIdentityDigestPriorityAndProfilePath() {
        for (String option : new String[] {
                "--max-work-items", "--max-pending-publications"
        }) {
            for (String value : new String[] {
                    "0", "4097", "+1", "01", "9223372036854775808", "not-a-number"
            }) {
                assertThrows(CliUsageException.class,
                        () -> CliArguments.parse(replacing(option, value)));
            }
        }
        assertThrows(CliUsageException.class,
                () -> CliArguments.parse(replacing("--submission-id", "not-a-uuid")));
        assertThrows(CliUsageException.class,
                () -> CliArguments.parse(replacing(
                        "--submission-id", "00000000-0000-0000-0000-000000000F27")));
        assertThrows(CliUsageException.class,
                () -> CliArguments.parse(replacing(
                        "--expected-response-sha256", "NOT-A-DIGEST")));
        assertThrows(CliUsageException.class,
                () -> CliArguments.parse(replacing("--priority", "normal")));
        assertThrows(CliUsageException.class,
                () -> CliArguments.parse(replacing("--priority", "URGENT")));
        assertThrows(CliUsageException.class,
                () -> CliArguments.parse(replacing("--target-path", " ")));
        assertThrows(CliUsageException.class,
                () -> CliArguments.parse(replacing("--project-root", "bad\0root")));
        assertThrows(CliUsageException.class,
                () -> CliArguments.parse(replacing(
                        "--model-execution-profile-file", "bad\0path")));
        assertThrows(CliUsageException.class,
                () -> CliArguments.parse(replacing(
                        "--model-execution-profile-file", "../profile")));
        assertThrows(CliUsageException.class,
                () -> CliArguments.parse(replacing(
                        "--model-execution-profile-file",
                        temporaryRoot.resolve("profile").toAbsolutePath().toString())));

        DeterministicFakeModelSpoolCliCommand missingProfile =
                (DeterministicFakeModelSpoolCliCommand) CliArguments.parse(
                        replacing("--model-execution-profile-file", "missing.profile"));
        assertEquals(Path.of("missing.profile"),
                missingProfile.modelExecutionProfileFile());
    }

    @Test
    void acceptsBothInclusiveBoundsIndependently() {
        DeterministicFakeModelSpoolCliCommand minimum =
                (DeterministicFakeModelSpoolCliCommand) CliArguments.parse(
                        replacingBothBounds("1"));
        DeterministicFakeModelSpoolCliCommand maximum =
                (DeterministicFakeModelSpoolCliCommand) CliArguments.parse(
                        replacingBothBounds("4096"));

        assertEquals(1, minimum.maxWorkItems());
        assertEquals(1, minimum.maxPendingPublications());
        assertEquals(4096, maximum.maxWorkItems());
        assertEquals(4096, maximum.maxPendingPublications());
    }

    private String[] arguments() {
        return new String[] {
                "scheduler-spool-deterministic-fake-model-work",
                "--project-root", temporaryRoot.resolve("project").toString(),
                "--submission-root", temporaryRoot.resolve("submissions").toString(),
                "--transport-spool-root", temporaryRoot.resolve("spool").toString(),
                "--task-id", "typed-model-spool",
                "--submission-id", "00000000-0000-0000-0000-000000000f27",
                "--max-work-items", "8",
                "--max-pending-publications", "16",
                "--producer", "typed-model-publisher",
                "--target-path", "prompts/request.txt",
                "--expected-response-sha256", "a".repeat(64),
                "--model-execution-profile-file", "profiles/model.profile",
                "--priority", "EXPEDITED"
        };
    }

    private String[] appending(String option, String value) {
        List<String> result = new ArrayList<>(Arrays.asList(arguments()));
        result.add(option);
        result.add(value);
        return result.toArray(String[]::new);
    }

    private String[] replacing(String option, String value) {
        String[] result = arguments();
        int index = List.of(result).indexOf(option);
        result[index + 1] = value;
        return result;
    }

    private String[] replacingBothBounds(String value) {
        String[] result = replacing("--max-work-items", value);
        int index = List.of(result).indexOf("--max-pending-publications");
        result[index + 1] = value;
        return result;
    }
}
