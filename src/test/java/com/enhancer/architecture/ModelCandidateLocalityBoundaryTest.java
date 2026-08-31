package com.enhancer.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ModelCandidateLocalityBoundaryTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Path PRODUCTION_ROOT = PROJECT_ROOT.resolve("src/main/java");
    private static final Set<String> DEFINITION_FILES = Set.of(
            "DeterministicFakeModelCandidate.java",
            "ModelCandidateSuitability.java",
            "ModelCandidateSuitabilityDecision.java",
            "ModelCandidateSuitabilityRejectionReason.java");

    @Test
    void candidateBoundaryHasNoIoExecutionOrGenericGatewayDependencies() throws IOException {
        List<String> forbidden = List.of(
                "java.io",
                "java.nio.file",
                "java.net",
                "ProcessBuilder",
                "Runtime.getRuntime",
                "System.getenv",
                "System.getProperty",
                "ModelCredentialSupplier",
                "HttpMessageApiModelProviderAdapter",
                "ModelInvokeTool",
                "ModelUsage",
                "RunRecord");
        Pattern genericGateway = Pattern.compile("\\bModelGateway\\b");
        Pattern toolType = Pattern.compile("\\bTool(?:Request|Result|Executor)?\\b");

        for (String fileName : DEFINITION_FILES) {
            String source = readModelSource(fileName);
            for (String token : forbidden) {
                assertFalse(source.contains(token), fileName + " must not reference " + token);
            }
            assertFalse(genericGateway.matcher(source).find(),
                    fileName + " must not reference the generic gateway port");
            assertFalse(toolType.matcher(source).find(),
                    fileName + " must not reference Tool execution types");
        }

        String evaluator = readModelSource("ModelCandidateSuitability.java");
        assertFalse(Pattern.compile("\\binvoke\\s*\\(").matcher(evaluator).find());
        assertFalse(Pattern.compile("\\bSuitable\\b").matcher(evaluator).find(),
                "the initial evaluator must have no reachable suitable construction");
    }

    @Test
    void noProductionCallerOrPersistenceBoundaryUsesTheNewTypes() throws IOException {
        try (Stream<Path> files = Files.walk(PRODUCTION_ROOT)) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !DEFINITION_FILES.contains(path.getFileName().toString()))
                    .forEach(path -> {
                        String source = read(path);
                        assertFalse(source.contains("DeterministicFakeModelCandidate"),
                                () -> path + " must not bind or consume candidates yet");
                        assertFalse(source.contains("ModelCandidateSuitability"),
                                () -> path + " must not call or persist suitability yet");
                    });
        }
    }

    @Test
    void exactFakeBindingIsVisibleInTheCandidateDefinition() throws IOException {
        String source = readModelSource("DeterministicFakeModelCandidate.java");
        assertTrue(source.contains(
                "private final DeterministicFakeModelGateway gateway;"));
        assertTrue(source.contains(
                "bind(DeterministicFakeModelGateway gateway)"));
    }

    private static String readModelSource(String fileName) throws IOException {
        return Files.readString(
                PRODUCTION_ROOT.resolve("com/enhancer/model").resolve(fileName),
                StandardCharsets.UTF_8);
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new AssertionError("could not read " + path, exception);
        }
    }
}
