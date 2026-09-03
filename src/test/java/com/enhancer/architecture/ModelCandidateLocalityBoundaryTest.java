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
            "DeterministicFakeTokenCounter.java",
            "DeterministicFakeModelCandidate.java",
            "ModelCandidateSuitability.java",
            "ModelCandidateSuitabilityDecision.java",
            "ModelCandidateSuitabilityRejectionReason.java",
            "DeterministicFakeExactRequestPreparation.java",
            "DeterministicFakeExactRequestDecision.java",
            "DeterministicFakeExactRequestRejectionReason.java",
            "DeterministicFakeExactRequestInvoker.java",
            "DeterministicFakeExactRequestInvocationResult.java",
            "DeterministicFakeExactRequestInvocationRejectionReason.java");

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
                "java.text.Normalizer",
                "java.nio.charset",
                "getBytes(",
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
        assertFalse(evaluator.contains("DeterministicFakeTokenCounter"));
        assertFalse(Pattern.compile("\\b(?:request|prompt|maxResponseLength)\\s*\\(")
                .matcher(evaluator)
                .find());

        String counter = readModelSource("DeterministicFakeTokenCounter.java");
        assertFalse(counter.contains("codePoints()"));
        assertFalse(counter.contains("codePointCount("));
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
                        assertFalse(source.contains("DeterministicFakeTokenCounter"),
                                () -> path + " must not count fake tokens yet");
                        assertFalse(source.contains("deterministic-fake-v2"),
                                () -> path + " must not consume the candidate identity yet");
                        assertFalse(source.contains("deterministic-unicode-scalar-v1"),
                                () -> path + " must not consume token semantics yet");
                        assertFalse(source.contains("DeterministicFakeExactRequestPreparation"),
                                () -> path + " must not prepare exact model requests yet");
                        assertFalse(source.contains("DeterministicFakeExactRequestDecision"),
                                () -> path + " must not consume exact request decisions yet");
                        assertFalse(source.contains("DeterministicFakeExactRequestRejectionReason"),
                                () -> path + " must not consume exact request reasons yet");
                        assertFalse(source.contains("DeterministicFakeExactRequestInvoker"),
                                () -> path + " must not invoke exact model requests yet");
                        assertFalse(source.contains("DeterministicFakeExactRequestInvocationResult"),
                                () -> path + " must not consume exact invocation results yet");
                        assertFalse(source.contains(
                                        "DeterministicFakeExactRequestInvocationRejectionReason"),
                                () -> path + " must not consume invocation reasons yet");
                    });
        }
    }

    @Test
    void exactFakeGatewayRenderingAndGenericUsageRemainUnchanged() throws IOException {
        String source = readModelSource("DeterministicFakeModelGateway.java");
        assertFalse(source.contains("DeterministicFakeTokenCounter"));
        assertFalse(source.contains("DeterministicFakeModelCandidate"));
        assertTrue(source.contains("deterministic-fake-v1\\n"));
        assertTrue(source.contains(
                "new ModelUsage(request.prompt().length(), text.length())"));
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
