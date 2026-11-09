package com.enhancer.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ProtectedRemoteCapabilityCatalogFactSourceBoundaryTest {
    private static final Path PRODUCTION_ROOT = Path.of("").toAbsolutePath().normalize()
            .resolve("src/main/java");
    private static final Path MODEL_ROOT =
            PRODUCTION_ROOT.resolve("com/enhancer/model");
    private static final Path SOURCE = MODEL_ROOT.resolve(
            "ProtectedRemoteCapabilityCatalogFactSource.java");
    private static final Path PREPARER = MODEL_ROOT.resolve(
            "ProtectedRemoteModelRouteOptionPreparer.java");

    @Test
    void sourceHasOnlyTheRequiredPinnedSourceCallerAndNoHigherAuthorityDependency()
            throws IOException {
        String source = Files.readString(SOURCE, StandardCharsets.UTF_8);
        for (String forbidden : List.of(
                "com.enhancer.application", "com.enhancer.bus", "com.enhancer.cli",
                "com.enhancer.evidence", "com.enhancer.maintenance",
                "com.enhancer.run", "com.enhancer.runtime", "com.enhancer.scheduler",
                "com.enhancer.tool", "java.net", "System.getenv",
                "System.getProperty", "Instant.now", "Clock.system", "ModelRouter",
                "RemoteModelCandidateSuitabilityEvaluator", "ProfiledModelRequest",
                "ModelGateway", "ModelCredential", "RemoteCapabilityCatalogPublisher",
                "LOCK_NAME", ".invoke(", ".execute(")) {
            assertFalse(source.contains(forbidden),
                    () -> "protected source references " + forbidden);
        }
        assertEquals(List.of(SOURCE), filesContaining(
                "new PinnedFileRemoteModelCandidateCapabilityFactSource("));
        assertEquals(List.of(PREPARER), filesContaining(
                "new ProtectedRemoteCapabilityCatalogFactSource("));
    }

    @Test
    void pinnedSourceRemainsTheSoleCapabilityFactsCreator() throws IOException {
        assertEquals(List.of(MODEL_ROOT.resolve(
                        "PinnedFileRemoteModelCandidateCapabilityFactSource.java")),
                filesContaining("RemoteModelCandidateCapabilityFacts.create("));
    }

    private static List<Path> filesContaining(String token) throws IOException {
        try (Stream<Path> files = Files.walk(PRODUCTION_ROOT)) {
            return files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> read(path).contains(token))
                    .sorted()
                    .toList();
        }
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }
}
