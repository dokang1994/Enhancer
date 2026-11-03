package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProtectedRemoteCapabilityCatalogFactSourceTest {
    private static final Instant EVALUATION_TIME =
            Instant.parse("2026-11-03T00:01:00Z");

    @TempDir
    Path temporaryRoot;

    @Test
    void refusalVocabularyIsClosedInFirstApplicableOrder() {
        assertArrayEquals(
                new ProtectedRemoteCapabilityCatalogFactSourceRefusalReason[] {
                        ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                                .INSTALLATION_ROOT_UNAVAILABLE_OR_UNSAFE,
                        ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                                .BINDING_UNAVAILABLE_OR_UNSAFE,
                        ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                                .BINDING_OUTSIDE_SIZE_BOUND,
                        ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                                .BINDING_CHANGED_DURING_READ,
                        ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                                .BINDING_INVALID,
                        ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                                .BINDING_CHANGED_DURING_ACQUISITION,
                        ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                                .SOURCE_REFUSED
                },
                ProtectedRemoteCapabilityCatalogFactSourceRefusalReason.values());
    }

    @Test
    void resultIsClosedImmutableAndEnforcesNestedRefusalShape() {
        assertTrue(ProtectedRemoteCapabilityCatalogFactSourceResult.class.isSealed());
        assertArrayEquals(
                new Class<?>[] {
                        ProtectedRemoteCapabilityCatalogFactSourceResult.Acquired.class,
                        ProtectedRemoteCapabilityCatalogFactSourceResult.Refused.class
                },
                ProtectedRemoteCapabilityCatalogFactSourceResult.class
                        .getPermittedSubclasses());
        assertTrue(ProtectedRemoteCapabilityCatalogFactSourceResult.Acquired.class
                .isRecord());
        assertTrue(ProtectedRemoteCapabilityCatalogFactSourceResult.Refused.class
                .isRecord());

        RemoteModelCandidateCapabilityFacts facts = facts();
        String bindingDigest = "b".repeat(64);
        ProtectedRemoteCapabilityCatalogFactSourceResult.Acquired acquired =
                new ProtectedRemoteCapabilityCatalogFactSourceResult.Acquired(
                        facts, bindingDigest);
        assertSame(facts, acquired.facts());
        assertEquals(bindingDigest, acquired.bindingSha256());
        assertThrows(NullPointerException.class,
                () -> new ProtectedRemoteCapabilityCatalogFactSourceResult.Acquired(
                        null, bindingDigest));
        assertThrows(IllegalArgumentException.class,
                () -> new ProtectedRemoteCapabilityCatalogFactSourceResult.Acquired(
                        facts, "B".repeat(64)));

        ProtectedRemoteCapabilityCatalogFactSourceResult.Refused sourceRefused =
                new ProtectedRemoteCapabilityCatalogFactSourceResult.Refused(
                        ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                                .SOURCE_REFUSED,
                        Optional.of(RemoteModelCandidateCapabilityFactSourceRefusalReason
                                .RECORD_NOT_FOUND));
        assertEquals(Optional.of(
                        RemoteModelCandidateCapabilityFactSourceRefusalReason
                                .RECORD_NOT_FOUND),
                sourceRefused.sourceReason());
        assertThrows(IllegalArgumentException.class,
                () -> new ProtectedRemoteCapabilityCatalogFactSourceResult.Refused(
                        ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                                .SOURCE_REFUSED,
                        Optional.empty()));
        assertThrows(IllegalArgumentException.class,
                () -> new ProtectedRemoteCapabilityCatalogFactSourceResult.Refused(
                        ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                                .BINDING_INVALID,
                        Optional.of(RemoteModelCandidateCapabilityFactSourceRefusalReason
                                .CATALOG_CONTENT_INVALID)));
    }

    @Test
    void sourceIsFinalAndPublicSurfaceAcceptsOnlyProtectedPathAndAcquisitionInput() {
        Class<?> type = ProtectedRemoteCapabilityCatalogFactSource.class;
        assertTrue(Modifier.isFinal(type.getModifiers()));
        List<Constructor<?>> publicConstructors = Arrays.stream(
                        type.getDeclaredConstructors())
                .filter(constructor -> Modifier.isPublic(constructor.getModifiers()))
                .toList();
        assertEquals(1, publicConstructors.size());
        assertArrayEquals(new Class<?>[] {Path.class},
                publicConstructors.get(0).getParameterTypes());

        List<Method> publicMethods = Arrays.stream(type.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .toList();
        assertEquals(1, publicMethods.size());
        Method acquire = publicMethods.get(0);
        assertEquals("acquire", acquire.getName());
        assertEquals(ProtectedRemoteCapabilityCatalogFactSourceResult.class,
                acquire.getReturnType());
        assertArrayEquals(
                new Class<?>[] {String.class, String.class, Instant.class},
                acquire.getParameterTypes());
        assertArrayEquals(new Class<?>[0], acquire.getExceptionTypes());
    }

    @Test
    void constructorValidatesOnlyTheFixedTrustLocatorWithoutFilesystemObservation() {
        Path missing = temporaryRoot.resolve(
                RemoteCapabilityCatalogBindingCodec.BINDING_NAME)
                .toAbsolutePath().normalize();
        assertDoesNotThrow(
                () -> new ProtectedRemoteCapabilityCatalogFactSource(missing));
        assertThrows(NullPointerException.class,
                () -> new ProtectedRemoteCapabilityCatalogFactSource(null));
        assertThrows(IllegalArgumentException.class,
                () -> new ProtectedRemoteCapabilityCatalogFactSource(
                        Path.of(RemoteCapabilityCatalogBindingCodec.BINDING_NAME)));
        assertThrows(IllegalArgumentException.class,
                () -> new ProtectedRemoteCapabilityCatalogFactSource(
                        temporaryRoot.resolve("child/../")
                                .resolve(RemoteCapabilityCatalogBindingCodec.BINDING_NAME)));
        assertThrows(IllegalArgumentException.class,
                () -> new ProtectedRemoteCapabilityCatalogFactSource(
                        temporaryRoot.resolve("other-binding")
                                .toAbsolutePath().normalize()));
    }

    @Test
    void programmingInputsFailBeforeMissingInstallationState() {
        ProtectedRemoteCapabilityCatalogFactSource source =
                new ProtectedRemoteCapabilityCatalogFactSource(bindingPath());
        assertThrows(NullPointerException.class,
                () -> source.acquire(null, "destination-a", EVALUATION_TIME));
        assertThrows(NullPointerException.class,
                () -> source.acquire("candidate-a", null, EVALUATION_TIME));
        assertThrows(NullPointerException.class,
                () -> source.acquire("candidate-a", "destination-a", null));
        for (String invalid : List.of(
                "", "Candidate-a", "candidate_a", "candidate/a", "-candidate",
                "candidate-", "candidate--a", "candidatÃ©", "a".repeat(257))) {
            assertThrows(IllegalArgumentException.class,
                    () -> source.acquire(invalid, "destination-a", EVALUATION_TIME));
            assertThrows(IllegalArgumentException.class,
                    () -> source.acquire("candidate-a", invalid, EVALUATION_TIME));
        }
    }

    @Test
    void firstSampleClassifiesRootBindingSizeAndCanonicalFailuresInOrder()
            throws IOException {
        assertEquals(
                ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                        .INSTALLATION_ROOT_UNAVAILABLE_OR_UNSAFE,
                refusal(new ProtectedRemoteCapabilityCatalogFactSource(bindingPath())));

        Files.createDirectories(catalogDirectory());
        assertEquals(
                ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                        .BINDING_UNAVAILABLE_OR_UNSAFE,
                refusal(new ProtectedRemoteCapabilityCatalogFactSource(bindingPath())));

        Files.write(bindingPath(), new byte[0]);
        assertEquals(
                ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                        .BINDING_OUTSIDE_SIZE_BOUND,
                refusal(new ProtectedRemoteCapabilityCatalogFactSource(bindingPath())));

        Files.write(bindingPath(), "not-canonical\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(
                ProtectedRemoteCapabilityCatalogFactSourceRefusalReason.BINDING_INVALID,
                refusal(new ProtectedRemoteCapabilityCatalogFactSource(bindingPath())));
    }

    @Test
    void canonicalFirstSampleDelegatesExactSelectedInputsOnceAndMapsSourceRefusal()
            throws IOException {
        Files.createDirectories(catalogDirectory());
        String catalogDigest = "a".repeat(64);
        RemoteCapabilityCatalogBinding binding =
                RemoteCapabilityCatalogBindingCodec.create(
                        catalogDirectory(), catalogDigest);
        Files.write(bindingPath(), binding.canonicalBytes());

        AtomicInteger calls = new AtomicInteger();
        AtomicReference<Path> delegatedPath = new AtomicReference<>();
        AtomicReference<String> delegatedPin = new AtomicReference<>();
        ProtectedRemoteCapabilityCatalogFactSource source =
                new ProtectedRemoteCapabilityCatalogFactSource(
                        bindingPath(),
                        (path, pin, candidate, destination, evaluationTime) -> {
                            calls.incrementAndGet();
                            delegatedPath.set(path);
                            delegatedPin.set(pin);
                            assertEquals("candidate-a", candidate);
                            assertEquals("destination-a", destination);
                            assertEquals(EVALUATION_TIME, evaluationTime);
                            return new RemoteModelCandidateCapabilityFactSourceResult.Refused(
                                    RemoteModelCandidateCapabilityFactSourceRefusalReason
                                            .RECORD_NOT_FOUND);
                        });

        ProtectedRemoteCapabilityCatalogFactSourceResult.Refused refused =
                assertInstanceOf(
                        ProtectedRemoteCapabilityCatalogFactSourceResult.Refused.class,
                        source.acquire(
                                "candidate-a", "destination-a", EVALUATION_TIME));
        assertEquals(1, calls.get());
        assertEquals(binding.catalogPath(), delegatedPath.get());
        assertEquals(catalogDigest, delegatedPin.get());
        assertEquals(
                ProtectedRemoteCapabilityCatalogFactSourceRefusalReason.SOURCE_REFUSED,
                refused.reason());
        assertEquals(Optional.of(
                        RemoteModelCandidateCapabilityFactSourceRefusalReason
                                .RECORD_NOT_FOUND),
                refused.sourceReason());
    }

    @Test
    void sourceAndResultsRenderWithoutTrustPathsDigestsOrCapabilityValues() {
        String pathSentinel = "secret-binding-path";
        String digestSentinel = "b".repeat(64);
        ProtectedRemoteCapabilityCatalogFactSource source =
                new ProtectedRemoteCapabilityCatalogFactSource(
                        temporaryRoot.resolve(pathSentinel)
                                .resolve(RemoteCapabilityCatalogBindingCodec.BINDING_NAME)
                                .toAbsolutePath().normalize());
        ProtectedRemoteCapabilityCatalogFactSourceResult.Acquired acquired =
                new ProtectedRemoteCapabilityCatalogFactSourceResult.Acquired(
                        facts(), digestSentinel);
        ProtectedRemoteCapabilityCatalogFactSourceResult.Refused refused =
                new ProtectedRemoteCapabilityCatalogFactSourceResult.Refused(
                        ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                                .BINDING_INVALID,
                        Optional.empty());
        for (String rendering : List.of(
                source.toString(), acquired.toString(), refused.toString())) {
            assertFalse(rendering.contains(pathSentinel));
            assertFalse(rendering.contains(digestSentinel));
            assertFalse(rendering.contains("987654321"));
            assertFalse(rendering.contains("1000"));
            assertFalse(rendering.contains("2026-10-23T00:00:00Z"));
        }
        assertTrue(refused.toString().contains("BINDING_INVALID"));
    }

    private ProtectedRemoteCapabilityCatalogFactSourceRefusalReason refusal(
            ProtectedRemoteCapabilityCatalogFactSource source) {
        return assertInstanceOf(
                ProtectedRemoteCapabilityCatalogFactSourceResult.Refused.class,
                source.acquire("candidate-a", "destination-a", EVALUATION_TIME))
                .reason();
    }

    private Path bindingPath() {
        return temporaryRoot.toAbsolutePath().normalize()
                .resolve(RemoteCapabilityCatalogBindingCodec.BINDING_NAME);
    }

    private Path catalogDirectory() {
        return temporaryRoot.toAbsolutePath().normalize()
                .resolve(RemoteCapabilityCatalogBindingCodec.CATALOG_DIRECTORY_NAME);
    }

    private static RemoteModelCandidateCapabilityFacts facts() {
        return new PinnedFileRemoteModelCandidateCapabilityFactSourceTestFacts().facts();
    }
}
