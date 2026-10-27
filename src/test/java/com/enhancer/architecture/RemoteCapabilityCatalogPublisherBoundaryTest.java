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

class RemoteCapabilityCatalogPublisherBoundaryTest {
    private static final Path PRODUCTION_ROOT = Path.of("").toAbsolutePath().normalize()
            .resolve("src/main/java");
    private static final Path MODEL_ROOT = PRODUCTION_ROOT.resolve("com/enhancer/model");
    private static final Path PORT = MODEL_ROOT.resolve(
            "RemoteCapabilityCatalogPublisher.java");

    @Test
    void publisherPortHasOnlyFilesystemImplementationAndNoProductionComposition()
            throws IOException {
        assertEquals(List.of(
                        MODEL_ROOT.resolve(
                                "FileSystemRemoteCapabilityCatalogPublisher.java"),
                        PORT),
                filesContaining("RemoteCapabilityCatalogPublisher"));
    }

    @Test
    void publicationContractHasNoRuntimeProviderInstallerOrEffectDependency()
            throws IOException {
        List<Path> sources;
        try (Stream<Path> files = Files.list(MODEL_ROOT)) {
            sources = files.filter(path -> path.getFileName().toString()
                            .startsWith("RemoteCapabilityCatalogPublication")
                            || path.equals(PORT))
                    .sorted()
                    .toList();
        }
        assertFalse(sources.isEmpty());
        for (Path source : sources) {
            String content = read(source);
            for (String forbidden : List.of(
                    "com.enhancer.application", "com.enhancer.bus", "com.enhancer.cli",
                    "com.enhancer.evidence", "com.enhancer.maintenance",
                    "com.enhancer.run", "com.enhancer.runtime", "com.enhancer.scheduler",
                    "com.enhancer.tool", "java.io", "java.net", "java.nio.file",
                    "System.getenv", "System.getProperty", "ModelRouter",
                    "RemoteModelCandidateSuitabilityEvaluator", "ModelGateway",
                    "ModelCredential", ".invoke(", ".execute(")) {
                assertFalse(content.contains(forbidden),
                        () -> source + " references " + forbidden);
            }
        }
    }

    @Test
    void existingCapabilityFactsCreationAndCallerAbsenceRemainClosed()
            throws IOException {
        Path source = MODEL_ROOT.resolve(
                "PinnedFileRemoteModelCandidateCapabilityFactSource.java");
        assertEquals(List.of(source), filesContaining(
                "RemoteModelCandidateCapabilityFacts.create("));
        assertEquals(List.of(), filesContaining(
                "new PinnedFileRemoteModelCandidateCapabilityFactSource("));
        assertEquals(List.of(), filesContaining(
                "new RemoteModelCandidateSuitabilityEvaluator("));
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
