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

class DeterministicFakeModelSubmitCliCommandTest {

    @TempDir
    Path temporaryRoot;

    @Test
    void parsesEveryExactExplicitInputWithoutCapabilityOrDerivedIdentity() {
        DeterministicFakeModelSubmitCliCommand command =
                (DeterministicFakeModelSubmitCliCommand) CliArguments.parse(arguments());

        assertEquals(temporaryRoot.resolve("project").toAbsolutePath().normalize(),
                command.projectRoot());
        assertEquals(temporaryRoot.resolve("submissions").toAbsolutePath().normalize(),
                command.submissionRoot());
        assertEquals(temporaryRoot.resolve("queue").toAbsolutePath().normalize(),
                command.queueRoot());
        assertEquals("typed-model-submit", command.taskId());
        assertEquals("00000000-0000-0000-0000-000000000f01", command.submissionId());
        assertEquals(8, command.maxWorkItems());
        assertEquals("typed-model-cli", command.producer());
        assertEquals("prompts/request.txt", command.targetPath());
        assertEquals("a".repeat(64), command.expectedResponseSha256());
        assertEquals(Path.of("profiles/model.profile"), command.modelExecutionProfileFile());
        assertEquals(SchedulerPriority.EXPEDITED, command.priority());
    }

    @Test
    void hasOnlyTheExactClosedCommandValueShape() {
        assertTrue(DeterministicFakeModelSubmitCliCommand.class.isRecord());
        assertTrue(Modifier.isFinal(
                DeterministicFakeModelSubmitCliCommand.class.getModifiers()));
        assertFalse(Modifier.isPublic(
                DeterministicFakeModelSubmitCliCommand.class.getModifiers()));
        assertArrayEquals(
                new String[] {
                        "projectRoot",
                        "submissionRoot",
                        "queueRoot",
                        "taskId",
                        "submissionId",
                        "maxWorkItems",
                        "producer",
                        "targetPath",
                        "expectedResponseSha256",
                        "modelExecutionProfileFile",
                        "priority"
                },
                Arrays.stream(DeterministicFakeModelSubmitCliCommand.class
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
                        String.class,
                        String.class,
                        String.class,
                        Path.class,
                        SchedulerPriority.class
                },
                Arrays.stream(DeterministicFakeModelSubmitCliCommand.class
                                .getRecordComponents())
                        .map(RecordComponent::getType)
                        .toArray(Class<?>[]::new));

        Set<String> names = Arrays.stream(
                        DeterministicFakeModelSubmitCliCommand.class.getRecordComponents())
                .map(RecordComponent::getName)
                .map(String::toLowerCase)
                .collect(Collectors.toUnmodifiableSet());
        for (String forbidden : new String[] {
                "capability", "queueid", "messageid", "correlation", "logicalrun",
                "occurredat", "allowedtool", "snapshot", "executionpolicy",
                "candidate", "gateway", "provider", "endpoint", "credential",
                "price", "network", "process"
        }) {
            assertTrue(names.stream().noneMatch(name -> name.contains(forbidden)),
                    () -> "command must not expose forbidden input: " + forbidden);
        }
        assertThrows(NoSuchMethodException.class,
                () -> DeterministicFakeModelSubmitCliCommand.class
                        .getDeclaredMethod("requiredCapability"));
    }

    @Test
    void requiresEveryOptionIncludingPriority() {
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
    void rejectsDuplicateUnknownExtraAndForbiddenOptions() {
        assertThrows(CliUsageException.class,
                () -> CliArguments.parse(appending("--producer", "duplicate")));
        assertThrows(CliUsageException.class,
                () -> CliArguments.parse(appending("--unknown", "value")));
        String[] extraToken = Arrays.copyOf(arguments(), arguments().length + 1);
        extraToken[extraToken.length - 1] = "extra";
        assertThrows(CliUsageException.class, () -> CliArguments.parse(extraToken));

        for (String forbidden : new String[] {
                "required-capability", "capability", "queue-id", "message-id",
                "correlation-id", "logical-run-id", "occurred-at", "allowed-tool",
                "snapshot-id", "execution-policy", "candidate", "gateway",
                "model-execution", "provider", "endpoint", "credential", "price",
                "network", "process-configuration", "expected-sha256"
        }) {
            assertThrows(CliUsageException.class,
                    () -> CliArguments.parse(appending("--" + forbidden, "value")));
        }
    }

    @Test
    void rejectsMalformedCapacityIdentityDigestPriorityAndProfilePath() {
        for (String capacity : new String[] {
                "0", "4097", "+1", "01", "9223372036854775808", "not-a-number"
        }) {
            assertThrows(CliUsageException.class,
                    () -> CliArguments.parse(replacing("--max-work-items", capacity)));
        }
        assertThrows(CliUsageException.class,
                () -> CliArguments.parse(replacing("--submission-id", "not-a-uuid")));
        assertThrows(CliUsageException.class,
                () -> CliArguments.parse(replacing(
                        "--expected-response-sha256", "NOT-A-DIGEST")));
        assertThrows(CliUsageException.class,
                () -> CliArguments.parse(replacing("--priority", "normal")));
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
    }

    @Test
    void acceptsBothInclusiveCapacityBoundaries() {
        DeterministicFakeModelSubmitCliCommand minimum =
                (DeterministicFakeModelSubmitCliCommand) CliArguments.parse(
                        replacing("--max-work-items", "1"));
        DeterministicFakeModelSubmitCliCommand maximum =
                (DeterministicFakeModelSubmitCliCommand) CliArguments.parse(
                        replacing("--max-work-items", "4096"));

        assertEquals(1, minimum.maxWorkItems());
        assertEquals(4096, maximum.maxWorkItems());
    }

    private String[] arguments() {
        return new String[] {
                "scheduler-submit-deterministic-fake-model-work",
                "--project-root", temporaryRoot.resolve("project").toString(),
                "--submission-root", temporaryRoot.resolve("submissions").toString(),
                "--queue-root", temporaryRoot.resolve("queue").toString(),
                "--task-id", "typed-model-submit",
                "--submission-id", "00000000-0000-0000-0000-000000000f01",
                "--max-work-items", "8",
                "--producer", "typed-model-cli",
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
}
