package com.enhancer.architecture;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.model.ModelContentInspectionFacts;
import com.enhancer.model.ModelContentRedactionFacts;
import com.enhancer.model.ModelDestinationAllowRule;
import com.enhancer.model.ModelDestinationPolicyFacts;
import com.enhancer.model.ModelOutboundContent;
import com.enhancer.model.ModelOutboundRouteAdmission;
import com.enhancer.model.ModelOutboundRouteAdmissionDecision;
import com.enhancer.model.ModelOutboundRouteProposal;
import com.enhancer.model.ModelOutboundRouteRejectionReason;
import com.enhancer.model.ModelRouteCandidateFacts;
import com.enhancer.model.ModelRoutePricingFacts;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ModelOutboundRouteBoundaryTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Path PRODUCTION_ROOT = PROJECT_ROOT.resolve("src/main/java");
    private static final Path MODEL_ROOT =
            PRODUCTION_ROOT.resolve("com/enhancer/model");

    private static final Set<String> DEFINITION_FILES = Set.of(
            "ModelContentInspectionFacts.java",
            "ModelContentInspectionStatus.java",
            "ModelContentRedactionFacts.java",
            "ModelDestinationAllowRule.java",
            "ModelDestinationPolicyFacts.java",
            "ModelInspectionCoverage.java",
            "ModelOutboundContent.java",
            "ModelOutboundContentTransformation.java",
            "ModelOutboundPurpose.java",
            "ModelOutboundRouteAdmission.java",
            "ModelOutboundRouteAdmissionDecision.java",
            "ModelOutboundRouteProposal.java",
            "ModelOutboundRouteRejectionReason.java",
            "ModelOutboundRouteValueChecks.java",
            "ModelRetentionFacts.java",
            "ModelRetentionUse.java",
            "ModelRouteCandidateFacts.java",
            "ModelRouteKind.java",
            "ModelRoutePricingFacts.java");

    private static final List<String> BOUNDARY_TYPE_NAMES = List.of(
            "ModelContentInspectionFacts",
            "ModelContentInspectionStatus",
            "ModelContentRedactionFacts",
            "ModelDestinationAllowRule",
            "ModelDestinationPolicyFacts",
            "ModelInspectionCoverage",
            "ModelOutboundContent",
            "ModelOutboundContentTransformation",
            "ModelOutboundPurpose",
            "ModelOutboundRouteAdmission",
            "ModelOutboundRouteAdmissionDecision",
            "ModelOutboundRouteProposal",
            "ModelOutboundRouteRejectionReason",
            "ModelOutboundRouteValueChecks",
            "ModelRetentionFacts",
            "ModelRetentionUse",
            "ModelRouteCandidateFacts",
            "ModelRouteKind",
            "ModelRoutePricingFacts");

    private static final Set<String> RFC_0029_DEFINITION_FILES = Set.of(
            "ModelRouteOption.java",
            "ModelRouteOptionSet.java",
            "ModelRouteSelectionDecision.java",
            "ModelRouteSelectionRefusalReason.java",
            "RemoteModelCandidateSuitability.java",
            "SelectedModelRoute.java");

    @Test
    void definitionsDependOnlyOnPureValuesAndTheExactRfc0016AuthorityInputs()
            throws IOException {
        Set<String> allowedEnhancerImports = Set.of(
                "import com.enhancer.loop.ApprovedTask;",
                "import com.enhancer.tool.ExecutionPolicy;");
        List<String> forbidden = List.of(
                "com.enhancer.application",
                "com.enhancer.bus",
                "com.enhancer.cli",
                "com.enhancer.evidence",
                "com.enhancer.run",
                "com.enhancer.runtime",
                "java.io",
                "java.net",
                "java.nio.file",
                "ProcessBuilder",
                "Runtime.getRuntime",
                "System.getenv",
                "System.getProperty",
                "EvidenceStore",
                "FileSystem",
                "HttpMessageApiModelProviderAdapter",
                "MessageTransport",
                "ModelCredentialSupplier",
                "ModelGateway",
                "ModelInvokeTool",
                "ModelRouter",
                "RunRecord",
                "SelectedModelRoute");

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
                            allowedEnhancerImports.contains(line),
                            () -> fileName + " has forbidden dependency " + line));
        }

        String evaluator = readModelSource("ModelOutboundRouteAdmission.java");
        assertTrue(evaluator.contains("new ModelInvocationAdmission().evaluate("));
        assertFalse(evaluator.contains(".invoke("));
    }

    @Test
    void noProductionCallerConsumesOrPersistsTheOutboundBoundary() throws IOException {
        try (Stream<Path> files = Files.walk(PRODUCTION_ROOT)) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !DEFINITION_FILES.contains(
                            path.getFileName().toString()))
                    .filter(path -> !RFC_0029_DEFINITION_FILES.contains(
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
    void evaluatorAndOutcomeRemainFieldFreeClosedAndEphemeral() {
        assertTrue(Modifier.isFinal(ModelOutboundRouteAdmission.class.getModifiers()));
        assertEquals(0, ModelOutboundRouteAdmission.class.getDeclaredFields().length);

        assertTrue(ModelOutboundRouteProposal.class.isRecord());
        assertTrue(ModelOutboundRouteAdmissionDecision.class.isSealed());
        assertArrayEquals(
                new Class<?>[] {
                        ModelOutboundRouteAdmissionDecision.Eligible.class,
                        ModelOutboundRouteAdmissionDecision.Refused.class
                },
                ModelOutboundRouteAdmissionDecision.class.getPermittedSubclasses());
        assertTrue(ModelOutboundRouteAdmissionDecision.Eligible.class.isRecord());
        assertTrue(ModelOutboundRouteAdmissionDecision.Refused.class.isRecord());
        assertArrayEquals(
                new Class<?>[] {
                        ModelOutboundRouteProposal.class,
                        com.enhancer.model.ModelDataClassification.class
                },
                Stream.of(ModelOutboundRouteAdmissionDecision.Eligible.class
                                .getRecordComponents())
                        .map(component -> component.getType())
                        .toArray(Class<?>[]::new));
        assertArrayEquals(
                new Class<?>[] {ModelOutboundRouteRejectionReason.class},
                Stream.of(ModelOutboundRouteAdmissionDecision.Refused.class
                                .getRecordComponents())
                        .map(component -> component.getType())
                        .toArray(Class<?>[]::new));
    }

    @Test
    void governedFactsAreFinalAndCannotBePubliclyConstructedOrExtended() {
        for (Class<?> type : List.of(
                ModelRouteCandidateFacts.class,
                ModelDestinationAllowRule.class,
                ModelDestinationPolicyFacts.class,
                ModelOutboundContent.class,
                ModelContentInspectionFacts.class,
                ModelContentRedactionFacts.class,
                ModelRoutePricingFacts.class)) {
            assertTrue(
                    Modifier.isFinal(type.getModifiers()),
                    () -> type.getSimpleName() + " must be final");
            for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                assertTrue(
                        Modifier.isPrivate(constructor.getModifiers()),
                        () -> type.getSimpleName()
                                + " must not expose a constructor");
            }
            for (Method method : type.getDeclaredMethods()) {
                if (method.getName().equals("create")) {
                    assertFalse(
                            Modifier.isPublic(method.getModifiers()),
                            () -> type.getSimpleName()
                                    + " create factory must stay package-private");
                }
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
