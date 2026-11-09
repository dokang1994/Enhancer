package com.enhancer.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class RemoteCapabilityCatalogValidationBoundaryTest {
    private static final Path PRODUCTION_ROOT = Path.of("").toAbsolutePath().normalize()
            .resolve("src/main/java");
    private static final Path MODEL_ROOT = PRODUCTION_ROOT.resolve("com/enhancer/model");
    private static final Path PREPARER = MODEL_ROOT.resolve(
            "ProtectedRemoteModelRouteOptionPreparer.java");

    @Test
    void sharedCatalogCodecIsPackagePrivatePureAndHasOnlyTwoValidationCallers()
            throws Exception {
        Class<?> codec = Class.forName(
                "com.enhancer.model.RemoteModelCapabilityCatalogCodec");
        assertTrue(Modifier.isFinal(codec.getModifiers()));
        assertFalse(Modifier.isPublic(codec.getModifiers()));
        String source = read(MODEL_ROOT.resolve(
                "RemoteModelCapabilityCatalogCodec.java"));
        for (String forbidden : List.of(
                "java.nio.file", "java.net", "com.enhancer.application",
                "com.enhancer.cli", "com.enhancer.evidence", "com.enhancer.runtime",
                "ModelRouter", "RemoteModelCandidateSuitabilityEvaluator",
                "ModelGateway", "ModelCredential", ".invoke(", ".execute(")) {
            assertFalse(source.contains(forbidden), () -> "codec references " + forbidden);
        }
        assertEquals(List.of(
                        MODEL_ROOT.resolve(
                                "PinnedFileRemoteModelCandidateCapabilityFactSource.java"),
                        MODEL_ROOT.resolve("RemoteCapabilityCatalogCandidateSnapshot.java")),
                filesContaining("RemoteModelCapabilityCatalogCodec.decode("));
    }

    @Test
    void validationContractsHaveNoFilesystemMutationOrHigherLayerDependency()
            throws IOException {
        for (String name : List.of(
                "RemoteCapabilityCatalogBinding.java",
                "RemoteCapabilityCatalogBindingCodec.java",
                "RemoteCapabilityCatalogCandidateSnapshot.java",
                "RemoteCapabilityCatalogEvidenceDecision.java",
                "RemoteCapabilityCatalogEvidenceVerificationRequest.java",
                "RemoteCapabilityCatalogEvidenceVerifier.java")) {
            String source = read(MODEL_ROOT.resolve(name));
            for (String forbidden : List.of(
                    "java.nio.file.Files", "java.nio.channels", "java.net",
                    "com.enhancer.application", "com.enhancer.bus", "com.enhancer.cli",
                    "com.enhancer.evidence", "com.enhancer.maintenance",
                    "com.enhancer.run", "com.enhancer.runtime", "com.enhancer.scheduler",
                    "ModelRouter", "RemoteModelCandidateSuitabilityEvaluator",
                    "ModelGateway", "ModelCredential", ".invoke(", ".execute(")) {
                assertFalse(source.contains(forbidden),
                        () -> name + " references " + forbidden);
            }
        }
        assertEquals(List.of(), filesContaining(
                "implements RemoteCapabilityCatalogEvidenceVerifier"));
    }

    @Test
    void capabilityFactsCreatorAndProductionCallersRemainClosed() throws IOException {
        assertEquals(List.of(MODEL_ROOT.resolve(
                        "PinnedFileRemoteModelCandidateCapabilityFactSource.java")),
                filesContaining("RemoteModelCandidateCapabilityFacts.create("));
        assertEquals(List.of(MODEL_ROOT.resolve(
                        "ProtectedRemoteCapabilityCatalogFactSource.java")),
                filesContaining(
                        "new PinnedFileRemoteModelCandidateCapabilityFactSource("));
        assertEquals(List.of(PREPARER), filesContaining(
                "new ProtectedRemoteCapabilityCatalogFactSource("));
        assertEquals(List.of(PREPARER), filesContaining(
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
