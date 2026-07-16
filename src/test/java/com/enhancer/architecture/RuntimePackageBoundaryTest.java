package com.enhancer.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuntimePackageBoundaryTest {
    private static final Path SOURCES = Path.of(
            System.getProperty("user.dir"),
            "src",
            "main",
            "java",
            "com",
            "enhancer");

    @Test
    void runtimePersistenceAndVerificationPackagesRemainAcyclic()
            throws IOException {
        List<String> violations = new ArrayList<>();
        forbidImports("loop", List.of("run", "verification"), violations);
        forbidImports("run", List.of("verification"), violations);
        forbidImports(
                "kernel",
                List.of("application", "loop", "run", "verification"),
                violations);

        assertTrue(
                violations.isEmpty(),
                () -> "forbidden runtime package imports: " + violations);
    }

    private static void forbidImports(
            String sourcePackage,
            List<String> forbiddenPackages,
            List<String> violations) throws IOException {
        Path directory = SOURCES.resolve(sourcePackage);
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (var files = Files.walk(directory)) {
            for (Path file : files
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList()) {
                String source = Files.readString(
                        file,
                        StandardCharsets.UTF_8);
                for (String forbidden : forbiddenPackages) {
                    String imported = "import com.enhancer."
                            + forbidden + ".";
                    if (source.contains(imported)) {
                        violations.add(
                                sourcePackage + "/"
                                        + directory.relativize(file)
                                        + " -> " + forbidden);
                    }
                }
            }
        }
    }
}
