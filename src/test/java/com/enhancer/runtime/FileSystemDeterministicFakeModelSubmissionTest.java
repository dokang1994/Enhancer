package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.model.ModelCostBudget;
import com.enhancer.model.ModelDataClassification;
import com.enhancer.model.ModelExecutionProfile;
import com.enhancer.model.ModelLocalityRequirement;
import com.enhancer.model.ModelReasoningRequirement;
import com.enhancer.model.ModelTokenBudget;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class FileSystemDeterministicFakeModelSubmissionTest {

    private static final String SUBMISSION_ID =
            "00000000-0000-0000-0000-000000000f21";

    @Test
    void exposesOnlyTheThreeFilesystemRootsAndEightSemanticSubmitInputs()
            throws Exception {
        assertTrue(Modifier.isPublic(
                FileSystemDeterministicFakeModelSubmission.class.getModifiers()));
        assertTrue(Modifier.isFinal(
                FileSystemDeterministicFakeModelSubmission.class.getModifiers()));
        assertTrue(Modifier.isPublic(
                FileSystemDeterministicFakeModelSubmission.class
                        .getConstructor(Path.class, Path.class, Path.class)
                        .getModifiers()));
        assertEquals(1, Arrays.stream(
                        FileSystemDeterministicFakeModelSubmission.class
                                .getDeclaredConstructors())
                .filter(constructor -> Modifier.isPublic(constructor.getModifiers()))
                .count());

        Method submit = FileSystemDeterministicFakeModelSubmission.class.getMethod(
                "submit",
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                ModelExecutionProfile.class,
                int.class,
                SchedulerPriority.class);
        assertEquals(DurableSubmissionResult.class, submit.getReturnType());
        assertArrayEquals(
                new String[] {"submit"},
                Arrays.stream(FileSystemDeterministicFakeModelSubmission.class
                                .getDeclaredMethods())
                        .filter(method -> Modifier.isPublic(method.getModifiers()))
                        .map(Method::getName)
                        .sorted()
                        .toArray(String[]::new));
        assertFalse(Arrays.stream(submit.getParameterTypes())
                .anyMatch(type -> type == Clock.class));
    }

    @Test
    void injectedBoundaryConstructsOneExactInternalRequestAndDelegatesOnce()
            throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<DeterministicFakeModelSubmissionRequest> captured =
                new AtomicReference<>();
        DurableSubmissionResult expected = new DurableSubmissionResult(
                SUBMISSION_ID,
                "00000000-0000-0000-0000-000000000f22",
                true,
                true,
                true,
                1);
        FileSystemDeterministicFakeModelSubmission facade =
                new FileSystemDeterministicFakeModelSubmission(request -> {
                    calls.incrementAndGet();
                    captured.set(request);
                    return expected;
                });
        ModelExecutionProfile profile = profile();

        DurableSubmissionResult actual = facade.submit(
                SUBMISSION_ID,
                "typed-submit-task",
                "typed-submit-cli",
                "prompts/request.txt",
                "a".repeat(64),
                profile,
                8,
                SchedulerPriority.EXPEDITED);

        assertSame(expected, actual);
        assertEquals(1, calls.get());
        assertEquals(new DeterministicFakeModelSubmissionRequest(
                SUBMISSION_ID,
                "typed-submit-task",
                "typed-submit-cli",
                "prompts/request.txt",
                "a".repeat(64),
                profile,
                8,
                SchedulerPriority.EXPEDITED), captured.get());
        assertSame(profile, captured.get().executionProfile());
    }

    private static ModelExecutionProfile profile() {
        return new ModelExecutionProfile(
                ModelExecutionProfile.SCHEMA_VERSION,
                "different-requirement",
                "deterministic-fake",
                ModelLocalityRequirement.LOCAL_ONLY,
                ModelReasoningRequirement.STANDARD,
                16_384,
                new ModelTokenBudget(4096, 2048, 8192),
                new ModelCostBudget("USD", 0),
                Duration.ofSeconds(30),
                ModelDataClassification.PUBLIC);
    }
}
