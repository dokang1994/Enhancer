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

class RemoteModelCandidateCapabilityFactSourceBoundaryTest {
    private static final Path PRODUCTION_ROOT = Path.of("").toAbsolutePath().normalize()
            .resolve("src/main/java");
    private static final Path SOURCE = PRODUCTION_ROOT.resolve(
            "com/enhancer/model/PinnedFileRemoteModelCandidateCapabilityFactSource.java");

    @Test
    void sourceHasNoRuntimeProviderAmbientOrRoutingDependency() throws IOException {
        String source = Files.readString(SOURCE, StandardCharsets.UTF_8);
        for (String forbidden : List.of(
                "com.enhancer.application", "com.enhancer.bus", "com.enhancer.cli",
                "com.enhancer.evidence", "com.enhancer.run", "com.enhancer.runtime",
                "com.enhancer.tool", "java.net", "System.getenv", "System.getProperty",
                "Instant.now", "Clock.system", "ModelRouter", "ModelRouteOption",
                "RemoteModelCandidateSuitability", "ProfiledModelRequest",
                "ModelGateway", "ModelCredential", ".invoke(", ".execute(")) {
            assertFalse(source.contains(forbidden), () -> "source references " + forbidden);
        }
    }

    @Test
    void sourceIsTheOnlyFactsCreatorAndHasNoProductionCaller() throws IOException {
        assertEquals(List.of(SOURCE), filesContaining(
                "RemoteModelCandidateCapabilityFacts.create("));
        assertEquals(List.of(), filesContaining(
                "new PinnedFileRemoteModelCandidateCapabilityFactSource("));
    }

    @Test
    void sourceStateIsPrivateFinalAndCarriesOnlyTrustedConfiguration() throws Exception {
        Class<?> type = Class.forName(
                "com.enhancer.model.PinnedFileRemoteModelCandidateCapabilityFactSource");
        assertTrue(Modifier.isFinal(type.getModifiers()));
        List<java.lang.reflect.Field> state = Stream.of(type.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .toList();
        assertEquals(2, state.size());
        state.forEach(field -> {
            assertTrue(Modifier.isPrivate(field.getModifiers()));
            assertTrue(Modifier.isFinal(field.getModifiers()));
        });
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
