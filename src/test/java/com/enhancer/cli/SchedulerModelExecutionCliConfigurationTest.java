package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enhancer.model.ModelRequest;
import com.enhancer.tool.EvidenceStoragePolicy;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SchedulerModelExecutionCliConfigurationTest {
    @Test
    void retainsOneBoundedImmutableConfiguration() {
        Set<String> denied = new LinkedHashSet<>(Set.of("read-file", "model-invoke"));

        SchedulerModelExecutionCliConfiguration configuration =
                new SchedulerModelExecutionCliConfiguration(
                        Duration.ofSeconds(1),
                        20_000,
                        65_536,
                        Duration.ofSeconds(2),
                        denied);
        denied.clear();

        assertEquals(Duration.ofSeconds(1), configuration.gatewayTimeout());
        assertEquals(20_000, configuration.maximumResponseCharacters());
        assertEquals(65_536, configuration.maximumReadBytes());
        assertEquals(Duration.ofSeconds(2), configuration.toolTimeout());
        assertEquals(Set.of("read-file", "model-invoke"), configuration.deniedTools());
        assertEquals(
                List.of("model-invoke", "read-file"),
                configuration.deniedTools().stream().toList());
    }

    @Test
    void rejectsInvalidResourceAndTimeoutBounds() {
        assertInvalid(Duration.ZERO, 1, 1, Duration.ofMillis(2));
        assertInvalid(ModelRequest.MAX_TIMEOUT.plusMillis(1), 1, 1,
                ModelRequest.MAX_TIMEOUT.plusMillis(2));
        assertInvalid(Duration.ofMillis(1), 0, 1, Duration.ofMillis(2));
        assertInvalid(Duration.ofMillis(1), ModelRequest.MAX_RESPONSE_LENGTH + 1, 1,
                Duration.ofMillis(2));
        assertInvalid(Duration.ofMillis(1), 1, 0, Duration.ofMillis(2));
        assertInvalid(
                Duration.ofMillis(1),
                1,
                EvidenceStoragePolicy.MAX_SUPPORTED_CONTENT_BYTES + 1,
                Duration.ofMillis(2));
        assertInvalid(Duration.ofMillis(1), 1, 1, Duration.ofMillis(1));
        assertInvalid(Duration.ofNanos(1_500_000), 1, 1, Duration.ofMillis(2));
    }

    @Test
    void acceptsEveryExactUpperBoundary() {
        Set<String> deniedTools = new LinkedHashSet<>();
        for (int index = 0;
                index < SchedulerModelExecutionCliConfiguration.MAX_DENIED_TOOLS;
                index++) {
            deniedTools.add("x".repeat(125) + "%03d".formatted(index));
        }

        SchedulerModelExecutionCliConfiguration configuration =
                new SchedulerModelExecutionCliConfiguration(
                        ModelRequest.MAX_TIMEOUT.minusMillis(1),
                        ModelRequest.MAX_RESPONSE_LENGTH,
                        EvidenceStoragePolicy.MAX_SUPPORTED_CONTENT_BYTES,
                        ModelRequest.MAX_TIMEOUT,
                        deniedTools);

        assertEquals(
                SchedulerModelExecutionCliConfiguration.MAX_DENIED_TOOLS,
                configuration.deniedTools().size());
        assertEquals(
                SchedulerModelExecutionCliConfiguration.MAX_DENIED_TOOL_CHARACTERS,
                configuration.deniedTools().iterator().next().length());
    }

    @Test
    void rejectsUnboundedOrMalformedDeniedTools() {
        assertThrows(IllegalArgumentException.class, () -> configuration(Set.of(" ")));
        assertThrows(IllegalArgumentException.class, () -> configuration(
                Set.of("x".repeat(129))));
        Set<String> tooMany = new LinkedHashSet<>();
        for (int index = 0; index < 17; index++) {
            tooMany.add("tool-" + index);
        }
        assertThrows(IllegalArgumentException.class, () -> configuration(tooMany));
        assertThrows(NullPointerException.class, () -> configuration(null));
    }

    private void assertInvalid(
            Duration gatewayTimeout,
            int maximumResponseCharacters,
            long maximumReadBytes,
            Duration toolTimeout) {
        assertThrows(IllegalArgumentException.class, () ->
                new SchedulerModelExecutionCliConfiguration(
                        gatewayTimeout,
                        maximumResponseCharacters,
                        maximumReadBytes,
                        toolTimeout,
                        Set.of()));
    }

    private SchedulerModelExecutionCliConfiguration configuration(Set<String> deniedTools) {
        return new SchedulerModelExecutionCliConfiguration(
                Duration.ofSeconds(1),
                20_000,
                65_536,
                Duration.ofSeconds(2),
                deniedTools);
    }
}
