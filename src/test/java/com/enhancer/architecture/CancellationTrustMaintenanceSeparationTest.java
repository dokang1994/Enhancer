package com.enhancer.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CancellationTrustMaintenanceSeparationTest {
    private static final Path PROJECT = Path.of(System.getProperty("user.dir"));

    @Test
    void runtimeCliAndApplicationEntrypointDoNotExposeMaintenance() throws IOException {
        Path cli = PROJECT.resolve("src/main/java/com/enhancer/cli");
        try (var files = Files.walk(cli)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file, StandardCharsets.UTF_8);
                assertFalse(source.contains("com.enhancer.maintenance"), file.toString());
                assertFalse(source.contains("cancellation-trust-maintenance"), file.toString());
            }
        }
        String build = Files.readString(
                PROJECT.resolve("build.gradle"), StandardCharsets.UTF_8);
        assertTrue(build.contains(
                "mainClass = 'com.enhancer.cli.EnhancerCli'"));
        assertTrue(build.contains(
                "tasks.register('cancellationTrustMaintenance', JavaExec)"));
        assertEquals(1, occurrences(
                build,
                "mainClass = 'com.enhancer.maintenance.CancellationTrustMaintenanceOperator'"));
        assertFalse(build.contains("startScripts"));
        assertFalse(build.contains("installDist"));
        assertFalse(build.contains("distributions"));

        String operator = Files.readString(
                PROJECT.resolve("src/main/java/com/enhancer/maintenance/"
                        + "CancellationTrustMaintenanceOperator.java"),
                StandardCharsets.UTF_8);
        assertFalse(operator.contains("com.enhancer.cli"));
        assertFalse(operator.contains("com.enhancer.runtime"));
        assertFalse(operator.contains("scheduler-"));
        assertFalse(operator.contains("AgentRuntime"));
        assertFalse(operator.contains("Audit"));
        assertFalse(operator.contains("Event"));
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = text.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }
}
