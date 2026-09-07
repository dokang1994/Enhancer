package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.model.ModelExecutionProfile;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class DeterministicFakeModelSubmissionRequestTest {

    private static final String SUBMISSION_ID =
            "00000000-0000-0000-0000-000000000e01";
    private static final String TASK_ID = "model-submission-task";
    private static final String PRODUCER = "internal-deterministic-model-submission";
    private static final String TARGET = "docs/model-prompt.md";
    private static final String DIGEST = "a".repeat(64);

    @Test
    void retainsExactCallerIntentAndUnchangedMismatchedProfile() {
        ModelExecutionProfile profile = ModelWorkFixtures.profile("other-capability");

        DeterministicFakeModelSubmissionRequest request = request(
                SUBMISSION_ID,
                TASK_ID,
                PRODUCER,
                TARGET,
                DIGEST,
                profile,
                4,
                SchedulerPriority.EXPEDITED);

        assertEquals(SUBMISSION_ID, request.submissionId());
        assertEquals(TASK_ID, request.taskId());
        assertEquals(PRODUCER, request.producer());
        assertEquals(TARGET, request.targetPath());
        assertEquals(DIGEST, request.expectedResponseSha256());
        assertSame(profile, request.executionProfile());
        assertEquals(4, request.maxWorkItems());
        assertEquals(SchedulerPriority.EXPEDITED, request.priority());
        assertEquals(
                "deterministic-echo",
                DeterministicFakeModelSubmissionCapabilitySource.requiredCapability());
        assertNotEquals(
                profile.requiredCapability(),
                DeterministicFakeModelSubmissionCapabilitySource.requiredCapability());
        assertEquals(
                request,
                request(
                        SUBMISSION_ID,
                        TASK_ID,
                        PRODUCER,
                        TARGET,
                        DIGEST,
                        profile,
                        4,
                        SchedulerPriority.EXPEDITED));
    }

    @Test
    void rejectsInvalidIntrinsicIntentAndAcceptsExactBounds() {
        ModelExecutionProfile profile = ModelWorkFixtures.profile();

        assertThrows(NullPointerException.class, () -> request(
                null, TASK_ID, PRODUCER, TARGET, DIGEST, profile, 1,
                SchedulerPriority.NORMAL));
        assertThrows(IllegalArgumentException.class, () -> request(
                "00000000-0000-0000-0000-0000000000AA",
                TASK_ID, PRODUCER, TARGET, DIGEST, profile, 1,
                SchedulerPriority.NORMAL));
        for (String invalid : new String[] {"", " ", "t".repeat(257)}) {
            assertThrows(IllegalArgumentException.class, () -> request(
                    SUBMISSION_ID, invalid, PRODUCER, TARGET, DIGEST, profile, 1,
                    SchedulerPriority.NORMAL));
        }
        for (String invalid : new String[] {"", " ", "p".repeat(257)}) {
            assertThrows(IllegalArgumentException.class, () -> request(
                    SUBMISSION_ID, TASK_ID, invalid, TARGET, DIGEST, profile, 1,
                    SchedulerPriority.NORMAL));
        }
        for (String invalid : new String[] {
                "", " ", "p".repeat(1025),
                Path.of("").toAbsolutePath().resolve("prompt.md").toString()
        }) {
            assertThrows(IllegalArgumentException.class, () -> request(
                    SUBMISSION_ID, TASK_ID, PRODUCER, invalid, DIGEST, profile, 1,
                    SchedulerPriority.NORMAL));
        }
        for (String invalid : new String[] {"a".repeat(63), "A".repeat(64)}) {
            assertThrows(IllegalArgumentException.class, () -> request(
                    SUBMISSION_ID, TASK_ID, PRODUCER, TARGET, invalid, profile, 1,
                    SchedulerPriority.NORMAL));
        }
        assertThrows(NullPointerException.class, () -> request(
                SUBMISSION_ID, TASK_ID, PRODUCER, TARGET, DIGEST, null, 1,
                SchedulerPriority.NORMAL));
        assertThrows(IllegalArgumentException.class, () -> request(
                SUBMISSION_ID, TASK_ID, PRODUCER, TARGET, DIGEST, profile, 0,
                SchedulerPriority.NORMAL));
        assertThrows(IllegalArgumentException.class, () -> request(
                SUBMISSION_ID, TASK_ID, PRODUCER, TARGET, DIGEST, profile,
                SingleWorkerSchedulerQueue.MAX_WORK_ITEMS + 1,
                SchedulerPriority.NORMAL));
        assertThrows(NullPointerException.class, () -> request(
                SUBMISSION_ID, TASK_ID, PRODUCER, TARGET, DIGEST, profile, 1, null));

        assertEquals(
                1,
                request(
                        SUBMISSION_ID,
                        TASK_ID,
                        PRODUCER,
                        TARGET,
                        DIGEST,
                        profile,
                        1,
                        SchedulerPriority.NORMAL).maxWorkItems());
        assertEquals(
                SingleWorkerSchedulerQueue.MAX_WORK_ITEMS,
                request(
                        SUBMISSION_ID,
                        "t".repeat(256),
                        "p".repeat(256),
                        "p".repeat(1024),
                        DIGEST,
                        profile,
                        SingleWorkerSchedulerQueue.MAX_WORK_ITEMS,
                        SchedulerPriority.EXPEDITED).maxWorkItems());
        assertEquals(
                "../outside.md",
                request(
                        SUBMISSION_ID,
                        TASK_ID,
                        PRODUCER,
                        "../outside.md",
                        DIGEST,
                        profile,
                        1,
                        SchedulerPriority.NORMAL).targetPath());
    }

    @Test
    void hasOnlyTheClosedEightComponentValueShape() {
        assertTrue(DeterministicFakeModelSubmissionRequest.class.isRecord());
        assertTrue(Modifier.isFinal(
                DeterministicFakeModelSubmissionRequest.class.getModifiers()));
        assertFalse(Modifier.isPublic(
                DeterministicFakeModelSubmissionRequest.class.getModifiers()));
        assertArrayEquals(
                new String[] {
                        "submissionId",
                        "taskId",
                        "producer",
                        "targetPath",
                        "expectedResponseSha256",
                        "executionProfile",
                        "maxWorkItems",
                        "priority"
                },
                Arrays.stream(
                                DeterministicFakeModelSubmissionRequest.class
                                        .getRecordComponents())
                        .map(RecordComponent::getName)
                        .toArray(String[]::new));
        assertArrayEquals(
                new Class<?>[] {
                        String.class,
                        String.class,
                        String.class,
                        String.class,
                        String.class,
                        ModelExecutionProfile.class,
                        int.class,
                        SchedulerPriority.class
                },
                Arrays.stream(
                                DeterministicFakeModelSubmissionRequest.class
                                        .getRecordComponents())
                        .map(RecordComponent::getType)
                        .toArray(Class<?>[]::new));

        Set<String> names = Arrays.stream(
                        DeterministicFakeModelSubmissionRequest.class.getRecordComponents())
                .map(RecordComponent::getName)
                .map(String::toLowerCase)
                .collect(Collectors.toUnmodifiableSet());
        for (String forbidden : new String[] {
                "capability", "authority", "provider", "endpoint", "credential",
                "queueid", "correlation", "logicalrun", "occurredat", "snapshot",
                "allowedtools", "candidate", "gateway", "policy", "admission"
        }) {
            assertFalse(
                    names.stream().anyMatch(name -> name.contains(forbidden)),
                    () -> "request must not contain authority field: " + forbidden);
        }
        assertThrows(
                NoSuchMethodException.class,
                () -> DeterministicFakeModelSubmissionRequest.class
                        .getDeclaredMethod("requiredCapability"));
    }

    private static DeterministicFakeModelSubmissionRequest request(
            String submissionId,
            String taskId,
            String producer,
            String targetPath,
            String expectedResponseSha256,
            ModelExecutionProfile executionProfile,
            int maxWorkItems,
            SchedulerPriority priority) {
        return new DeterministicFakeModelSubmissionRequest(
                submissionId,
                taskId,
                producer,
                targetPath,
                expectedResponseSha256,
                executionProfile,
                maxWorkItems,
                priority);
    }
}
