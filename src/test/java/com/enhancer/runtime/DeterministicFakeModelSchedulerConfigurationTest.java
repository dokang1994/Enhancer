package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enhancer.model.ModelRequest;
import com.enhancer.tool.EvidenceStoragePolicy;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DeterministicFakeModelSchedulerConfigurationTest {
    @Test
    void retainsOnlyTheFiveBoundedImmutableInputs() {
        Set<String> deniedTools = new LinkedHashSet<>(
                List.of("read-file", "model-invoke"));

        DeterministicFakeModelSchedulerConfiguration configuration = configuration(
                Duration.ofSeconds(1),
                20_000,
                65_536,
                Duration.ofSeconds(2),
                deniedTools);
        deniedTools.clear();

        assertEquals(
                List.of(
                        "gatewayTimeout",
                        "maximumResponseCharacters",
                        "maximumReadBytes",
                        "toolTimeout",
                        "deniedTools"),
                Arrays.stream(DeterministicFakeModelSchedulerConfiguration.class
                                .getRecordComponents())
                        .map(component -> component.getName())
                        .toList());
        assertEquals(
                List.of("model-invoke", "read-file"),
                configuration.deniedTools().stream().toList());
        assertThrows(
                UnsupportedOperationException.class,
                () -> configuration.deniedTools().add("write-code"));
    }

    @Test
    void projectsExactlyToThePackagePrivateProcessConfiguration() throws Exception {
        DeterministicFakeModelSchedulerConfiguration configuration = configuration(
                Duration.ofSeconds(1),
                20_000,
                65_536,
                Duration.ofSeconds(2),
                Set.of("read-file"));

        ModelProcessExecutionConfiguration process =
                configuration.toProcessConfiguration();

        assertEquals(Duration.ofSeconds(1), process.invocationLimits().gatewayTimeout());
        assertEquals(20_000, process.invocationLimits().maximumResponseCharacters());
        assertEquals(65_536, process.maximumReadBytes());
        assertEquals(Duration.ofSeconds(2), process.toolTimeout());
        assertEquals(Set.of("read-file"), process.deniedTools());
        assertFalse(Modifier.isPublic(ModelProcessExecutionConfiguration.class
                .getModifiers()));
        assertFalse(Modifier.isPublic(
                DeterministicFakeModelSchedulerConfiguration.class
                        .getDeclaredMethod("toProcessConfiguration")
                        .getModifiers()));
    }

    @Test
    void rejectsEveryUnsupportedScalarAndDeniedToolBoundary() {
        assertInvalid(Duration.ZERO, 1, 1, Duration.ofMillis(2), Set.of());
        assertInvalid(Duration.ofNanos(1), 1, 1, Duration.ofMillis(2), Set.of());
        assertInvalid(
                ModelRequest.MAX_TIMEOUT.plusMillis(1),
                1,
                1,
                ModelRequest.MAX_TIMEOUT.plusMillis(2),
                Set.of());
        assertInvalid(Duration.ofMillis(1), 0, 1, Duration.ofMillis(2), Set.of());
        assertInvalid(
                Duration.ofMillis(1),
                ModelRequest.MAX_RESPONSE_LENGTH + 1,
                1,
                Duration.ofMillis(2),
                Set.of());
        assertInvalid(Duration.ofMillis(1), 1, 0, Duration.ofMillis(2), Set.of());
        assertInvalid(
                Duration.ofMillis(1),
                1,
                EvidenceStoragePolicy.MAX_SUPPORTED_CONTENT_BYTES + 1,
                Duration.ofMillis(2),
                Set.of());
        assertInvalid(Duration.ofMillis(1), 1, 1, Duration.ofMillis(1), Set.of());
        assertInvalid(Duration.ofMillis(1), 1, 1, Duration.ofNanos(1_500_000), Set.of());
        assertInvalid(Duration.ofMillis(1), 1, 1, Duration.ofMillis(2), Set.of(" "));
        assertInvalid(
                Duration.ofMillis(1),
                1,
                1,
                Duration.ofMillis(2),
                Set.of("x".repeat(129)));
        Set<String> tooMany = new LinkedHashSet<>();
        for (int index = 0; index < 17; index++) {
            tooMany.add("tool-" + index);
        }
        assertInvalid(Duration.ofMillis(1), 1, 1, Duration.ofMillis(2), tooMany);
    }

    @Test
    void workerFactoryRejectsNonNestedOuterTimeoutBeforeComposition() {
        DeterministicFakeModelSchedulerConfiguration configuration = configuration(
                Duration.ofMillis(1),
                1,
                1,
                Duration.ofMillis(2),
                Set.of());

        assertThrows(IllegalArgumentException.class, () ->
                DurableAgentRunWorker.processIsolatedWithDeterministicFakeModel(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        configuration,
                        null,
                        null,
                        Duration.ofMillis(2),
                        null));
    }

    private void assertInvalid(
            Duration gatewayTimeout,
            int maximumResponseCharacters,
            long maximumReadBytes,
            Duration toolTimeout,
            Set<String> deniedTools) {
        assertThrows(IllegalArgumentException.class, () -> configuration(
                gatewayTimeout,
                maximumResponseCharacters,
                maximumReadBytes,
                toolTimeout,
                deniedTools));
    }

    private DeterministicFakeModelSchedulerConfiguration configuration(
            Duration gatewayTimeout,
            int maximumResponseCharacters,
            long maximumReadBytes,
            Duration toolTimeout,
            Set<String> deniedTools) {
        return new DeterministicFakeModelSchedulerConfiguration(
                gatewayTimeout,
                maximumResponseCharacters,
                maximumReadBytes,
                toolTimeout,
                deniedTools);
    }
}
