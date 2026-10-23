package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PinnedFileRemoteModelCandidateCapabilityFactSourceTest {

    private static final String SHA_256 = "0".repeat(64);
    private static final Instant EVALUATION_TIME =
            Instant.parse("2026-10-23T00:01:00Z");

    @TempDir
    Path temporaryRoot;

    @Test
    void refusalVocabularyIsClosedInRfcOrder() {
        assertArrayEquals(
                new RemoteModelCandidateCapabilityFactSourceRefusalReason[] {
                        RemoteModelCandidateCapabilityFactSourceRefusalReason
                                .CATALOG_UNAVAILABLE_OR_UNSAFE,
                        RemoteModelCandidateCapabilityFactSourceRefusalReason
                                .CATALOG_OUTSIDE_SIZE_BOUND,
                        RemoteModelCandidateCapabilityFactSourceRefusalReason
                                .CATALOG_CHANGED_DURING_READ,
                        RemoteModelCandidateCapabilityFactSourceRefusalReason
                                .CATALOG_PIN_MISMATCH,
                        RemoteModelCandidateCapabilityFactSourceRefusalReason
                                .CATALOG_INVALID_UTF8,
                        RemoteModelCandidateCapabilityFactSourceRefusalReason
                                .CATALOG_SCHEMA_UNSUPPORTED,
                        RemoteModelCandidateCapabilityFactSourceRefusalReason
                                .CATALOG_NOT_CANONICAL,
                        RemoteModelCandidateCapabilityFactSourceRefusalReason
                                .CATALOG_CONTENT_INVALID,
                        RemoteModelCandidateCapabilityFactSourceRefusalReason
                                .RECORD_NOT_FOUND,
                        RemoteModelCandidateCapabilityFactSourceRefusalReason
                                .RECORD_STALE
                },
                RemoteModelCandidateCapabilityFactSourceRefusalReason.values());
    }

    @Test
    void resultIsSealedToAcquiredAndRefusedRecords() {
        assertTrue(RemoteModelCandidateCapabilityFactSourceResult.class.isSealed());
        assertArrayEquals(
                new Class<?>[] {
                        RemoteModelCandidateCapabilityFactSourceResult.Acquired.class,
                        RemoteModelCandidateCapabilityFactSourceResult.Refused.class
                },
                RemoteModelCandidateCapabilityFactSourceResult.class
                        .getPermittedSubclasses());
        assertTrue(RemoteModelCandidateCapabilityFactSourceResult.Acquired.class.isRecord());
        assertTrue(RemoteModelCandidateCapabilityFactSourceResult.Refused.class.isRecord());
        assertFalse(Serializable.class.isAssignableFrom(
                RemoteModelCandidateCapabilityFactSourceResult.class));
        assertFalse(Serializable.class.isAssignableFrom(
                RemoteModelCandidateCapabilityFactSourceResult.Acquired.class));
        assertFalse(Serializable.class.isAssignableFrom(
                RemoteModelCandidateCapabilityFactSourceResult.Refused.class));

        assertThrows(NullPointerException.class,
                () -> new RemoteModelCandidateCapabilityFactSourceResult.Acquired(null));
        assertThrows(NullPointerException.class,
                () -> new RemoteModelCandidateCapabilityFactSourceResult.Refused(null));

        RemoteModelCandidateCapabilityFacts facts = facts();
        RemoteModelCandidateCapabilityFactSourceResult.Acquired acquired =
                new RemoteModelCandidateCapabilityFactSourceResult.Acquired(facts);
        assertSame(facts, acquired.facts());
        RemoteModelCandidateCapabilityFactSourceResult.Refused refused =
                new RemoteModelCandidateCapabilityFactSourceResult.Refused(
                        RemoteModelCandidateCapabilityFactSourceRefusalReason
                                .CATALOG_PIN_MISMATCH);
        assertEquals(
                RemoteModelCandidateCapabilityFactSourceRefusalReason
                        .CATALOG_PIN_MISMATCH,
                refused.reason());
    }

    @Test
    void sourceIsFinalAndExposesOnlyTheExactPublicSurface() {
        assertTrue(Modifier.isFinal(
                PinnedFileRemoteModelCandidateCapabilityFactSource.class.getModifiers()));

        List<Constructor<?>> publicConstructors = Arrays.stream(
                        PinnedFileRemoteModelCandidateCapabilityFactSource.class
                                .getDeclaredConstructors())
                .filter(constructor -> Modifier.isPublic(constructor.getModifiers()))
                .toList();
        assertEquals(1, publicConstructors.size());
        assertArrayEquals(
                new Class<?>[] {Path.class, String.class},
                publicConstructors.get(0).getParameterTypes());

        List<Method> publicMethods = Arrays.stream(
                        PinnedFileRemoteModelCandidateCapabilityFactSource.class
                                .getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .toList();
        assertEquals(1, publicMethods.size());
        Method acquire = publicMethods.get(0);
        assertEquals("acquire", acquire.getName());
        assertEquals(RemoteModelCandidateCapabilityFactSourceResult.class,
                acquire.getReturnType());
        assertArrayEquals(
                new Class<?>[] {String.class, String.class, Instant.class},
                acquire.getParameterTypes());
        assertArrayEquals(new Class<?>[0], acquire.getExceptionTypes());
    }

    @Test
    void constructorRejectsInvalidTrustInputsWithoutObservingFilesystem() {
        Path missing = temporaryRoot.resolve("secret-catalog-name");
        assertDoesNotThrow(() -> source(missing, SHA_256));
        assertThrows(NullPointerException.class, () -> source(null, SHA_256));
        assertThrows(NullPointerException.class, () -> source(missing, null));
        assertThrows(IllegalArgumentException.class,
                () -> source(Path.of("relative-catalog"), SHA_256));
        assertThrows(IllegalArgumentException.class,
                () -> source(temporaryRoot.resolve("child/../catalog"), SHA_256));
        assertThrows(IllegalArgumentException.class,
                () -> source(missing, "0".repeat(63)));
        assertThrows(IllegalArgumentException.class,
                () -> source(missing, "0".repeat(65)));
        assertThrows(IllegalArgumentException.class,
                () -> source(missing, "A".repeat(64)));
        assertThrows(IllegalArgumentException.class,
                () -> source(missing, "g".repeat(64)));
        assertThrows(IllegalArgumentException.class,
                () -> source(missing, "0".repeat(63) + " "));
    }

    @Test
    void acquireRejectsProgrammingInputsBeforeFilesystemState() {
        PinnedFileRemoteModelCandidateCapabilityFactSource source = source(
                temporaryRoot.resolve("missing-catalog"), SHA_256);
        assertThrows(NullPointerException.class,
                () -> source.acquire(null, "destination-a", EVALUATION_TIME));
        assertThrows(NullPointerException.class,
                () -> source.acquire("candidate-a", null, EVALUATION_TIME));
        assertThrows(NullPointerException.class,
                () -> source.acquire("candidate-a", "destination-a", null));

        for (String invalid : List.of(
                "",
                "Candidate-a",
                "candidate_a",
                "candidate/a",
                "-candidate",
                "candidate-",
                "candidate--a",
                "candidaté",
                "a".repeat(257))) {
            assertThrows(IllegalArgumentException.class,
                    () -> source.acquire(invalid, "destination-a", EVALUATION_TIME));
            assertThrows(IllegalArgumentException.class,
                    () -> source.acquire("candidate-a", invalid, EVALUATION_TIME));
        }

        assertInstanceOf(
                RemoteModelCandidateCapabilityFactSourceResult.Refused.class,
                source.acquire("a".repeat(256), "b".repeat(256), EVALUATION_TIME));
    }

    @Test
    void missingCatalogReturnsFirstClosedRefusal() {
        RemoteModelCandidateCapabilityFactSourceResult.Refused refused = assertInstanceOf(
                RemoteModelCandidateCapabilityFactSourceResult.Refused.class,
                source(temporaryRoot.resolve("missing-catalog"), SHA_256).acquire(
                        "candidate-a", "destination-a", EVALUATION_TIME));
        assertEquals(
                RemoteModelCandidateCapabilityFactSourceRefusalReason
                        .CATALOG_UNAVAILABLE_OR_UNSAFE,
                refused.reason());
    }

    @Test
    void sourceAndResultsUseSafeRendering() {
        String secretPath = "secret-catalog-name";
        String secretPin = "a".repeat(64);
        PinnedFileRemoteModelCandidateCapabilityFactSource source = source(
                temporaryRoot.resolve(secretPath), secretPin);
        RemoteModelCandidateCapabilityFactSourceResult.Refused refused =
                new RemoteModelCandidateCapabilityFactSourceResult.Refused(
                        RemoteModelCandidateCapabilityFactSourceRefusalReason
                                .CATALOG_UNAVAILABLE_OR_UNSAFE);
        RemoteModelCandidateCapabilityFactSourceResult.Acquired acquired =
                new RemoteModelCandidateCapabilityFactSourceResult.Acquired(facts());

        for (String rendered : List.of(
                source.toString(), refused.toString(), acquired.toString())) {
            assertFalse(rendered.contains(secretPath));
            assertFalse(rendered.contains(secretPin));
            assertFalse(rendered.contains("987654321"));
            assertFalse(rendered.contains("1000"));
            assertFalse(rendered.contains("2026-10-23T00:00:00Z"));
            assertFalse(rendered.contains("exception-sentinel"));
        }
        assertTrue(refused.toString().contains("CATALOG_UNAVAILABLE_OR_UNSAFE"));
    }

    private static PinnedFileRemoteModelCandidateCapabilityFactSource source(
            Path path, String sha256) {
        return new PinnedFileRemoteModelCandidateCapabilityFactSource(path, sha256);
    }

    private static RemoteModelCandidateCapabilityFacts facts() {
        return RemoteModelCandidateCapabilityFacts.create(
                RemoteModelCandidateCapabilityFacts.SCHEMA_VERSION,
                "catalog-source",
                "b".repeat(64),
                "capacity-evidence-a",
                Instant.parse("2026-10-23T00:00:00Z"),
                Instant.parse("2026-10-23T00:05:00Z"),
                "candidate-a",
                "destination-a",
                "reasoning-standard",
                "repository-analysis",
                ModelRoutePricingFacts.TOKEN_SEMANTICS_IDENTITY,
                "schedule-a",
                new ModelTokenBudget(10, 5, 15),
                ModelReasoningRequirement.EXTENDED,
                1_000,
                new ModelTokenBudget(100, 50, 150),
                Duration.ofSeconds(60),
                "USD",
                987_654_321,
                ModelDataClassification.RESTRICTED);
    }
}
