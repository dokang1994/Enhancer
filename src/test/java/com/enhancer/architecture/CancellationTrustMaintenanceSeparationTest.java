package com.enhancer.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
        assertTrue(build.contains(
                "mainClass = cancellationTrustMaintenanceMainClass"));
        assertTrue(build.contains(
                "tasks.register('cancellationTrustMaintenanceStartScripts', CreateStartScripts)"));
        assertTrue(build.contains(
                "applicationName = 'enhancer-cancellation-trust-maintenance'"));
        assertTrue(build.contains(
                "distributions.register('cancellationTrustMaintenance')"));
        assertTrue(build.contains(
                "distributionBaseName = 'enhancer-cancellation-trust-maintenance'"));
        assertTrue(build.contains(
                ".dir('generated-cancellation-trust-maintenance-scripts')"));
        assertFalse(build.contains(".dir('scripts/"));
        assertTrue(build.contains("installCancellationTrustMaintenanceDist"));
        assertEquals(1, occurrences(
                build,
                "com.enhancer.maintenance.CancellationTrustMaintenanceOperator"));
        assertEquals(2, occurrences(
                build,
                "mainClass = cancellationTrustMaintenanceMainClass"));
        assertFalse(build.contains("applicationDefaultJvmArgs"));
        assertFalse(build.contains("defaultJvmOpts"));
        assertFalse(build.contains(", Exec)"));

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

    @Test
    void installationPermissionPackageRemainsAPureUnwiredPort() throws IOException {
        Path installation = PROJECT.resolve(
                "src/main/java/com/enhancer/maintenance/installation");
        assertTrue(Files.isDirectory(installation));
        List<Path> neutralSources;
        try (var files = Files.list(installation)) {
            neutralSources = files.filter(path -> path.toString().endsWith(".java")).toList();
        }
        assertEquals(20, neutralSources.size());
        Path windows = installation.resolve("windows");
        assertTrue(Files.isDirectory(windows));
        List<Path> windowsSources;
        try (var files = Files.walk(windows)) {
            windowsSources = files.filter(path -> path.toString().endsWith(".java")).toList();
        }
        assertEquals(23, windowsSources.size());
        List<Path> sources = new java.util.ArrayList<>(neutralSources);
        sources.addAll(windowsSources);
        for (Path source : sources) {
            String text = Files.readString(source, StandardCharsets.UTF_8);
            assertFalse(text.contains("java.nio.file.Files"), source.toString());
            assertFalse(text.contains("FileChannel"), source.toString());
            assertFalse(text.contains("AclFileAttributeView"), source.toString());
            assertFalse(text.contains("PosixFileAttributeView"), source.toString());
            assertFalse(text.contains("ProcessBuilder"), source.toString());
            assertFalse(text.contains("Runtime.getRuntime"), source.toString());
            assertFalse(text.contains("powershell"), source.toString());
            assertFalse(text.contains("cmd.exe"), source.toString());
            assertFalse(text.contains("com.sun.jna"), source.toString());
            assertFalse(text.contains("java.lang.foreign"), source.toString());
            assertFalse(text.contains("com.enhancer.cli"), source.toString());
            assertFalse(text.contains("com.enhancer.runtime"), source.toString());
        }
        assertEquals(1, sourceOccurrences(
                windowsSources, "implements InstallationPermissionAdapter"));
        assertEquals(0, sourceOccurrences(
                windowsSources, "implements WindowsInstallationPermissionGateway"));
        assertEquals(0, productionOccurrencesOutsideInstallation(
                "com.enhancer.maintenance.installation"));
        assertEquals(0, productionOccurrencesOutsideInstallation(
                "implements InstallationPermissionAdapter"));
        String build = Files.readString(
                PROJECT.resolve("build.gradle"), StandardCharsets.UTF_8);
        assertFalse(build.contains("InstallationPermissionAdapter"));
        assertFalse(build.contains("installation-permission"));
    }

    private static int productionOccurrencesOutsideInstallation(String needle)
            throws IOException {
        Path production = PROJECT.resolve("src/main/java");
        int count = 0;
        try (var files = Files.walk(production)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                if (file.startsWith(production.resolve(
                        "com/enhancer/maintenance/installation"))) {
                    continue;
                }
                count += occurrences(Files.readString(file, StandardCharsets.UTF_8), needle);
            }
        }
        return count;
    }

    private static int sourceOccurrences(List<Path> sources, String needle)
            throws IOException {
        int count = 0;
        for (Path source : sources) {
            count += occurrences(Files.readString(source, StandardCharsets.UTF_8), needle);
        }
        return count;
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
