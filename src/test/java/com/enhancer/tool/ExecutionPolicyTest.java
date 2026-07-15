package com.enhancer.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExecutionPolicyTest {
    @TempDir
    Path projectRoot;

    @Test
    void normalizesTheRootAndMakesDenyOverrideAllow() {
        ExecutionPolicy policy = new ExecutionPolicy(
                projectRoot.resolve("."),
                Set.of("read-file", "other"),
                Set.of("other"),
                1024,
                Duration.ofSeconds(1),
                CancellationToken.none());

        assertEquals(projectRoot.toAbsolutePath().normalize(), policy.projectRoot());
        assertTrue(policy.allows("read-file"));
        assertFalse(policy.allows("other"));
        assertFalse(policy.allows("missing"));
        assertFalse(policy.cancellationToken().isCancellationRequested());
    }

    @Test
    void allowsLargerReadsForPersistedEvidenceAndRejectsInvalidPolicyValues() {
        ExecutionPolicy persistedEvidencePolicy = policy(
                VerificationEvidence.MAX_OUTPUT_TAIL_CHARACTERS + 1,
                Duration.ofSeconds(1),
                Set.of("read-file"));

        assertEquals(
                VerificationEvidence.MAX_OUTPUT_TAIL_CHARACTERS + 1,
                persistedEvidencePolicy.maxReadBytes());
        assertThrows(
                IllegalArgumentException.class,
                () -> policy(0, Duration.ofSeconds(1), Set.of("read-file")));
        assertThrows(
                IllegalArgumentException.class,
                () -> policy(
                        EvidenceRetentionPolicy.MAX_SUPPORTED_CONTENT_BYTES + 1,
                        Duration.ofSeconds(1),
                        Set.of("read-file")));
        assertThrows(
                IllegalArgumentException.class,
                () -> policy(10, Duration.ZERO, Set.of("read-file")));
        assertThrows(
                IllegalArgumentException.class,
                () -> policy(10, Duration.ofNanos(1), Set.of("read-file")));
        assertThrows(
                IllegalArgumentException.class,
                () -> policy(10, Duration.ofSeconds(Long.MAX_VALUE), Set.of("read-file")));
        assertThrows(
                IllegalArgumentException.class,
                () -> policy(10, Duration.ofSeconds(1), Set.of(" ")));

        ExecutionPolicy minimumRepresentable = policy(
                10,
                Duration.ofMillis(1),
                Set.of("read-file"));
        assertEquals(Duration.ofMillis(1), minimumRepresentable.timeout());
    }

    private ExecutionPolicy policy(
            long maxReadBytes,
            Duration timeout,
            Set<String> allowedTools) {
        return new ExecutionPolicy(
                projectRoot,
                allowedTools,
                Set.of(),
                maxReadBytes,
                timeout,
                CancellationToken.none());
    }
}
