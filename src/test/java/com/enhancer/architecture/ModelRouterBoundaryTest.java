package com.enhancer.architecture;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.model.ModelOutboundRouteAdmissionDecision;
import com.enhancer.model.ModelRouteOption;
import com.enhancer.model.ModelRouteOptionSet;
import com.enhancer.model.ModelRouteSelectionDecision;
import com.enhancer.model.ModelRouteSelectionRefusalReason;
import com.enhancer.model.ModelRouter;
import com.enhancer.model.ProfiledModelRequest;
import com.enhancer.model.RemoteModelCandidateSuitability;
import com.enhancer.model.SelectedModelRoute;
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

class ModelRouterBoundaryTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Path PRODUCTION_ROOT = PROJECT_ROOT.resolve("src/main/java");
    private static final Path MODEL_ROOT =
            PRODUCTION_ROOT.resolve("com/enhancer/model");

    private static final Set<String> DEFINITION_FILES = Set.of(
            "ModelRouteOption.java",
            "ModelRouteOptionSet.java",
            "ModelRouteSelectionDecision.java",
            "ModelRouteSelectionRefusalReason.java",
            "ModelRouter.java",
            "RemoteModelCandidateSuitability.java",
            "SelectedModelRoute.java");

    private static final List<String> BOUNDARY_TYPE_NAMES = List.of(
            "ModelRouteOption",
            "ModelRouteOptionSet",
            "ModelRouteSelectionDecision",
            "ModelRouteSelectionRefusalReason",
            "ModelRouter",
            "RemoteModelCandidateSuitability",
            "SelectedModelRoute");

    @Test
    void definitionsDependOnlyOnPureModelValues() throws IOException {
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
                "Clock",
                "Instant.now",
                "ProcessBuilder",
                "Runtime.getRuntime",
                "System.getenv",
                "System.getProperty",
                "Comparator",
                ".sort(",
                "Random",
                "ServiceLoader",
                "Supplier",
                "EvidenceStore",
                "FileSystem",
                "HttpMessageApiModelProviderAdapter",
                "MessageTransport",
                "ModelCredentialSupplier",
                "ModelGateway",
                "ModelInvokeTool",
                "RunRecord",
                "Scheduler");
        for (String fileName : DEFINITION_FILES) {
            String source = readModelSource(fileName);
            for (String token : forbidden) {
                assertFalse(
                        source.contains(token),
                        fileName + " must not reference " + token);
            }
            source.lines()
                    .filter(line -> line.startsWith("import com.enhancer."))
                    .forEach(line -> assertTrue(
                            line.startsWith("import com.enhancer.model."),
                            () -> fileName + " has forbidden dependency " + line));
        }
    }

    @Test
    void exactFilesAloneConsumeRfc0028EligibilityAndFacts() throws IOException {
        Set<String> eligibilityConsumers = Set.of(
                "ModelRouteOption.java",
                "ModelRouter.java",
                "RemoteModelCandidateSuitability.java",
                "SelectedModelRoute.java");
        Set<String> candidateFactConsumers = Set.of(
                "ModelRouter.java",
                "RemoteModelCandidateSuitability.java",
                "SelectedModelRoute.java");
        for (String fileName : DEFINITION_FILES) {
            String source = readModelSource(fileName);
            assertEquals(
                    eligibilityConsumers.contains(fileName),
                    source.contains("ModelOutboundRouteAdmissionDecision.Eligible"),
                    fileName + " RFC-0028 eligibility dependency drifted");
            assertEquals(
                    candidateFactConsumers.contains(fileName),
                    source.contains("ModelRouteCandidateFacts"),
                    fileName + " candidate-facts dependency drifted");
        }
    }

    @Test
    void noOtherProductionSourceConsumesOrPersistsRouterValues() throws IOException {
        try (Stream<Path> files = Files.walk(PRODUCTION_ROOT)) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !DEFINITION_FILES.contains(
                            path.getFileName().toString()))
                    .forEach(path -> {
                        String source = read(path);
                        for (String typeName : BOUNDARY_TYPE_NAMES) {
                            assertFalse(
                                    source.contains(typeName),
                                    () -> path + " must not consume " + typeName);
                        }
                    });
        }
    }

    @Test
    void routerIsFinalFieldFreeAndHasOnlyTheExactPublicEvaluationSurface() {
        assertTrue(Modifier.isFinal(ModelRouter.class.getModifiers()));
        assertEquals(0, ModelRouter.class.getDeclaredFields().length);

        List<Method> publicMethods = Stream.of(ModelRouter.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .toList();
        assertEquals(1, publicMethods.size());
        Method evaluate = publicMethods.get(0);
        assertEquals("evaluate", evaluate.getName());
        assertEquals(ModelRouteSelectionDecision.class, evaluate.getReturnType());
        assertArrayEquals(
                new Class<?>[] {
                        ProfiledModelRequest.class,
                        ModelRouteOptionSet.class,
                        Instant.class
                },
                evaluate.getParameterTypes());
    }

    @Test
    void opaqueProofAndSelectedRouteHaveNoPublicConstructionPath() {
        for (Class<?> type : List.of(
                RemoteModelCandidateSuitability.class,
                SelectedModelRoute.class)) {
            assertTrue(Modifier.isFinal(type.getModifiers()));
            for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                assertTrue(
                        Modifier.isPrivate(constructor.getModifiers()),
                        () -> type.getSimpleName() + " constructor must stay private");
            }
            for (Method method : type.getDeclaredMethods()) {
                if (method.getName().equals("create")) {
                    assertFalse(
                            Modifier.isPublic(method.getModifiers()),
                            () -> type.getSimpleName() + " factory must not be public");
                }
            }
        }
    }

    @Test
    void recordsAndSealedDecisionRetainTheExactClosedShape() {
        assertTrue(ModelRouteOption.class.isRecord());
        assertArrayEquals(
                new Class<?>[] {
                        ProfiledModelRequest.class,
                        ModelOutboundRouteAdmissionDecision.Eligible.class,
                        RemoteModelCandidateSuitability.class
                },
                Stream.of(ModelRouteOption.class.getRecordComponents())
                        .map(component -> component.getType())
                        .toArray(Class<?>[]::new));
        assertTrue(ModelRouteSelectionDecision.class.isSealed());
        assertArrayEquals(
                new Class<?>[] {
                        ModelRouteSelectionDecision.Selected.class,
                        ModelRouteSelectionDecision.Refused.class
                },
                ModelRouteSelectionDecision.class.getPermittedSubclasses());
        assertArrayEquals(
                new Class<?>[] {SelectedModelRoute.class},
                Stream.of(ModelRouteSelectionDecision.Selected.class.getRecordComponents())
                        .map(component -> component.getType())
                        .toArray(Class<?>[]::new));
        assertArrayEquals(
                new Class<?>[] {ModelRouteSelectionRefusalReason.class, int.class},
                Stream.of(ModelRouteSelectionDecision.Refused.class.getRecordComponents())
                        .map(component -> component.getType())
                        .toArray(Class<?>[]::new));
    }

    @Test
    void remoteRouterNeverReusesTheDeterministicFakeLocalBoundary() throws IOException {
        for (String fileName : DEFINITION_FILES) {
            String source = readModelSource(fileName);
            for (String forbidden : List.of(
                    "DeterministicFakeModelCandidate",
                    "DeterministicFakeModelGateway",
                    "DeterministicFakeExactRequest",
                    "ModelCandidateSuitabilityDecision")) {
                assertFalse(
                        source.contains(forbidden),
                        fileName + " must not reference local boundary " + forbidden);
            }
        }
    }

    private static String readModelSource(String fileName) throws IOException {
        return Files.readString(MODEL_ROOT.resolve(fileName), StandardCharsets.UTF_8);
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new AssertionError("could not read " + path, exception);
        }
    }
}
