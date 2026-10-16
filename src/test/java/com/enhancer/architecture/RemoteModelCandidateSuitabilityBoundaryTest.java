package com.enhancer.architecture;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.model.ModelOutboundRouteAdmissionDecision;
import com.enhancer.model.ProfiledModelRequest;
import com.enhancer.model.RemoteModelCandidateCapabilityFacts;
import com.enhancer.model.RemoteModelCandidateSuitabilityDecision;
import com.enhancer.model.RemoteModelCandidateSuitabilityEvaluator;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class RemoteModelCandidateSuitabilityBoundaryTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Path PRODUCTION_ROOT = PROJECT_ROOT.resolve("src/main/java");
    private static final Path TEST_ROOT = PROJECT_ROOT.resolve("src/test/java");
    private static final Path MODEL_ROOT =
            PRODUCTION_ROOT.resolve("com/enhancer/model");

    private static final Set<String> DEFINITION_FILES = Set.of(
            "RemoteModelCandidateCapabilityFacts.java",
            "RemoteModelCandidateSuitabilityDecision.java",
            "RemoteModelCandidateSuitabilityEvaluator.java",
            "RemoteModelCandidateSuitabilityRefusalReason.java");

    @Test
    void evaluatorIsFinalFieldFreeAndHasOnlyTheExactPublicSurface() {
        assertTrue(Modifier.isFinal(
                RemoteModelCandidateSuitabilityEvaluator.class.getModifiers()));
        assertEquals(0,
                RemoteModelCandidateSuitabilityEvaluator.class.getDeclaredFields().length);

        List<Method> publicMethods = Stream.of(
                        RemoteModelCandidateSuitabilityEvaluator.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .toList();
        assertEquals(1, publicMethods.size());
        Method evaluate = publicMethods.get(0);
        assertEquals("evaluate", evaluate.getName());
        assertEquals(RemoteModelCandidateSuitabilityDecision.class,
                evaluate.getReturnType());
        assertArrayEquals(
                new Class<?>[] {
                        ProfiledModelRequest.class,
                        ModelOutboundRouteAdmissionDecision.Eligible.class,
                        RemoteModelCandidateCapabilityFacts.class,
                        Instant.class
                },
                evaluate.getParameterTypes());
    }

    @Test
    void factsAndDecisionRemainOpaqueImmutableAndClosed() throws Exception {
        assertTrue(Modifier.isFinal(
                RemoteModelCandidateCapabilityFacts.class.getModifiers()));
        for (Constructor<?> constructor
                : RemoteModelCandidateCapabilityFacts.class.getDeclaredConstructors()) {
            assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        }
        Method creator = Stream.of(
                        RemoteModelCandidateCapabilityFacts.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("create"))
                .findFirst()
                .orElseThrow();
        assertTrue(Modifier.isStatic(creator.getModifiers()));
        assertFalse(Modifier.isPublic(creator.getModifiers()));
        assertFalse(Modifier.isProtected(creator.getModifiers()));

        assertTrue(RemoteModelCandidateSuitabilityDecision.class.isSealed());
        assertArrayEquals(
                new Class<?>[] {
                        RemoteModelCandidateSuitabilityDecision.Suitable.class,
                        RemoteModelCandidateSuitabilityDecision.Refused.class
                },
                RemoteModelCandidateSuitabilityDecision.class.getPermittedSubclasses());
    }

    @Test
    void definitionsDependOnlyOnPureModelValuesAndHaveNoExternalEffectSurface()
            throws IOException {
        List<String> forbidden = List.of(
                "com.enhancer.application",
                "com.enhancer.bus",
                "com.enhancer.cli",
                "com.enhancer.evidence",
                "com.enhancer.run",
                "com.enhancer.runtime",
                "com.enhancer.tool",
                "java.io",
                "java.net",
                "java.nio.file",
                "java.sql",
                "System.",
                "Runtime.",
                "ProcessBuilder",
                "FileInputStream",
                "FileOutputStream",
                "Socket",
                "HttpClient",
                ".invoke(",
                ".execute(",
                ".persist(",
                ".save(",
                "DeterministicFake");

        for (String fileName : DEFINITION_FILES) {
            String source = read(MODEL_ROOT.resolve(fileName));
            for (String token : forbidden) {
                assertFalse(
                        source.contains(token),
                        fileName + " must not reference " + token);
            }
        }
    }

    @Test
    void productionHasNoCapabilityFactsCreatorOrEvaluatorCallerAndOnlyEvaluatorCreatesProof()
            throws IOException {
        assertEquals(
                List.of(),
                productionFilesContaining(
                        "RemoteModelCandidateCapabilityFacts.create("));
        assertEquals(
                List.of(),
                productionFilesContaining(
                        "new RemoteModelCandidateSuitabilityEvaluator("));
        assertEquals(
                List.of("RemoteModelCandidateSuitabilityEvaluator.java"),
                productionFilesContaining("RemoteModelCandidateSuitability." + "create("));
    }

    @Test
    void onlyTheDedicatedDefensiveRouterFixtureBypassesEvaluatorProofSuccess()
            throws IOException {
        assertEquals(
                List.of("ModelRouterTest.java"),
                testFilesContaining("RemoteModelCandidateSuitability." + "create("));
    }

    private static List<String> productionFilesContaining(String token) throws IOException {
        return filesContaining(PRODUCTION_ROOT, token);
    }

    private static List<String> testFilesContaining(String token) throws IOException {
        return filesContaining(TEST_ROOT, token);
    }

    private static List<String> filesContaining(Path root, String token) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> read(path).contains(token))
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
        }
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read " + path, exception);
        }
    }
}
