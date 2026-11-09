package com.enhancer.architecture;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.model.ModelOutboundRouteAdmissionDecision;
import com.enhancer.model.ProfiledModelRequest;
import com.enhancer.model.ProtectedRemoteModelRouteOptionPreparationResult;
import com.enhancer.model.ProtectedRemoteModelRouteOptionPreparer;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ProtectedRemoteModelRouteOptionPreparationBoundaryTest {
    private static final Path PRODUCTION_ROOT = Path.of("").toAbsolutePath().normalize()
            .resolve("src/main/java");
    private static final Path MODEL_ROOT =
            PRODUCTION_ROOT.resolve("com/enhancer/model");
    private static final Path PREPARER = MODEL_ROOT.resolve(
            "ProtectedRemoteModelRouteOptionPreparer.java");

    @Test
    void preparerIsFinalWithOnePathOnlyPublicConstructionAndExactMethod() {
        assertTrue(Modifier.isFinal(
                ProtectedRemoteModelRouteOptionPreparer.class.getModifiers()));
        List<Constructor<?>> publicConstructors = Stream.of(
                        ProtectedRemoteModelRouteOptionPreparer.class
                                .getDeclaredConstructors())
                .filter(constructor -> Modifier.isPublic(constructor.getModifiers()))
                .toList();
        assertEquals(1, publicConstructors.size());
        assertArrayEquals(
                new Class<?>[] {Path.class},
                publicConstructors.get(0).getParameterTypes());

        List<Method> publicMethods = Stream.of(
                        ProtectedRemoteModelRouteOptionPreparer.class
                                .getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .toList();
        assertEquals(1, publicMethods.size());
        Method prepare = publicMethods.get(0);
        assertEquals("prepare", prepare.getName());
        assertEquals(
                ProtectedRemoteModelRouteOptionPreparationResult.class,
                prepare.getReturnType());
        assertArrayEquals(
                new Class<?>[] {
                        ProfiledModelRequest.class,
                        ModelOutboundRouteAdmissionDecision.Eligible.class,
                        Instant.class
                },
                prepare.getParameterTypes());
    }

    @Test
    void exactPreparerAloneConstructsTheAcceptedBoundariesAndOption()
            throws IOException {
        assertEquals(
                List.of(PREPARER),
                filesContaining("new ProtectedRemoteCapabilityCatalogFactSource("));
        assertEquals(
                List.of(PREPARER),
                filesContaining("new RemoteModelCandidateSuitabilityEvaluator("));
        assertEquals(
                List.of(PREPARER),
                filesContaining("new ModelRouteOption("));
        assertEquals(
                List.of(),
                filesContaining("new ProtectedRemoteModelRouteOptionPreparer("));
        assertEquals(List.of(), filesContaining("new ModelRouter("));
    }

    @Test
    void preparerStopsBeforeRoutingExecutionPersistenceOrAmbientAuthority()
            throws IOException {
        String source = read(PREPARER);
        for (String forbidden : List.of(
                "com.enhancer.application",
                "com.enhancer.bus",
                "com.enhancer.cli",
                "com.enhancer.evidence",
                "com.enhancer.run",
                "com.enhancer.runtime",
                "com.enhancer.scheduler",
                "com.enhancer.tool",
                "java.net",
                "java.nio.file.Files",
                "System.getenv",
                "System.getProperty",
                "Instant.now",
                "Clock",
                "ModelRouteOptionSet",
                "ModelRouter",
                "SelectedModelRoute",
                "ModelGateway",
                "ModelCredential",
                "DeterministicFake",
                ".invoke(",
                ".execute(",
                ".persist(",
                ".save(")) {
            assertFalse(source.contains(forbidden),
                    () -> "preparer references " + forbidden);
        }
        assertEquals(
                1,
                occurrences(source, "new ProtectedRemoteCapabilityCatalogFactSource("));
        assertEquals(
                1,
                occurrences(source, "new RemoteModelCandidateSuitabilityEvaluator("));
        assertEquals(1, occurrences(source, "new ModelRouteOption("));
    }

    private static int occurrences(String source, String token) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
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
