package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ProtectedRemoteModelRouteOptionPreparationResultTest {

    @Test
    void resultIsExactlyThreeSealedRecordShapes() {
        assertTrue(ProtectedRemoteModelRouteOptionPreparationResult.class.isSealed());
        assertArrayEquals(
                new Class<?>[] {
                        ProtectedRemoteModelRouteOptionPreparationResult.Prepared.class,
                        ProtectedRemoteModelRouteOptionPreparationResult
                                .AcquisitionRefused.class,
                        ProtectedRemoteModelRouteOptionPreparationResult
                                .SuitabilityRefused.class
                },
                ProtectedRemoteModelRouteOptionPreparationResult.class
                        .getPermittedSubclasses());

        assertRecordComponents(
                ProtectedRemoteModelRouteOptionPreparationResult.Prepared.class,
                ModelRouteOption.class,
                String.class);
        assertRecordComponents(
                ProtectedRemoteModelRouteOptionPreparationResult
                        .AcquisitionRefused.class,
                ProtectedRemoteCapabilityCatalogFactSourceResult.Refused.class);
        assertRecordComponents(
                ProtectedRemoteModelRouteOptionPreparationResult
                        .SuitabilityRefused.class,
                RemoteModelCandidateSuitabilityRefusalReason.class,
                String.class);
    }

    @Test
    void resultRetainsExactUpstreamValuesAndValidatesDigest() {
        ProtectedRemoteModelRouteOptionPreparationTestFixture fixture =
                new ProtectedRemoteModelRouteOptionPreparationTestFixture();
        ModelRouteOption option = fixture.option();
        ProtectedRemoteCapabilityCatalogFactSourceResult.Refused acquisitionRefusal =
                new ProtectedRemoteCapabilityCatalogFactSourceResult.Refused(
                        ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                                .SOURCE_REFUSED,
                        Optional.of(
                                RemoteModelCandidateCapabilityFactSourceRefusalReason
                                        .RECORD_NOT_FOUND));

        ProtectedRemoteModelRouteOptionPreparationResult.Prepared prepared =
                new ProtectedRemoteModelRouteOptionPreparationResult.Prepared(
                        option,
                        ProtectedRemoteModelRouteOptionPreparationTestFixture
                                .BINDING_SHA_256);
        ProtectedRemoteModelRouteOptionPreparationResult.AcquisitionRefused refused =
                new ProtectedRemoteModelRouteOptionPreparationResult
                        .AcquisitionRefused(acquisitionRefusal);
        ProtectedRemoteModelRouteOptionPreparationResult.SuitabilityRefused unsuitable =
                new ProtectedRemoteModelRouteOptionPreparationResult
                        .SuitabilityRefused(
                                RemoteModelCandidateSuitabilityRefusalReason
                                        .COST_CEILING_EXCEEDED,
                                ProtectedRemoteModelRouteOptionPreparationTestFixture
                                        .BINDING_SHA_256);

        assertSame(option, prepared.option());
        assertEquals(
                ProtectedRemoteModelRouteOptionPreparationTestFixture.BINDING_SHA_256,
                prepared.bindingSha256());
        assertSame(acquisitionRefusal, refused.refusal());
        assertSame(
                RemoteModelCandidateSuitabilityRefusalReason.COST_CEILING_EXCEEDED,
                unsuitable.reason());
        assertEquals(
                ProtectedRemoteModelRouteOptionPreparationTestFixture.BINDING_SHA_256,
                unsuitable.bindingSha256());

        assertThrows(NullPointerException.class,
                () -> new ProtectedRemoteModelRouteOptionPreparationResult.Prepared(
                        null,
                        ProtectedRemoteModelRouteOptionPreparationTestFixture
                                .BINDING_SHA_256));
        assertThrows(NullPointerException.class,
                () -> new ProtectedRemoteModelRouteOptionPreparationResult
                        .AcquisitionRefused(null));
        assertThrows(NullPointerException.class,
                () -> new ProtectedRemoteModelRouteOptionPreparationResult
                        .SuitabilityRefused(
                                null,
                                ProtectedRemoteModelRouteOptionPreparationTestFixture
                                        .BINDING_SHA_256));
        assertInvalidDigest(null, option);
        assertInvalidDigest("b".repeat(63), option);
        assertInvalidDigest("B".repeat(64), option);
        assertInvalidDigest("z".repeat(64), option);
    }

    @Test
    void resultRenderingExposesOnlyKindAndClosedReasons() {
        ProtectedRemoteModelRouteOptionPreparationTestFixture fixture =
                new ProtectedRemoteModelRouteOptionPreparationTestFixture();
        String digest = ProtectedRemoteModelRouteOptionPreparationTestFixture
                .BINDING_SHA_256;
        String prepared = new ProtectedRemoteModelRouteOptionPreparationResult.Prepared(
                fixture.option(), digest).toString();
        String acquisitionRefused =
                new ProtectedRemoteModelRouteOptionPreparationResult.AcquisitionRefused(
                        new ProtectedRemoteCapabilityCatalogFactSourceResult.Refused(
                                ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                                        .SOURCE_REFUSED,
                                Optional.of(
                                        RemoteModelCandidateCapabilityFactSourceRefusalReason
                                                .RECORD_NOT_FOUND)))
                        .toString();
        String suitabilityRefused =
                new ProtectedRemoteModelRouteOptionPreparationResult.SuitabilityRefused(
                        RemoteModelCandidateSuitabilityRefusalReason
                                .COST_CEILING_EXCEEDED,
                        digest)
                        .toString();

        assertTrue(prepared.startsWith("Prepared["));
        assertTrue(prepared.contains("option=<redacted>"));
        assertTrue(prepared.contains("bindingSha256=<redacted>"));
        assertFalse(prepared.contains(digest));
        assertFalse(prepared.contains(fixture.profiledRequest.request().prompt()));

        assertTrue(acquisitionRefused.startsWith("AcquisitionRefused["));
        assertTrue(acquisitionRefused.contains("SOURCE_REFUSED"));
        assertTrue(acquisitionRefused.contains("RECORD_NOT_FOUND"));
        assertFalse(acquisitionRefused.contains("path"));

        assertTrue(suitabilityRefused.startsWith("SuitabilityRefused["));
        assertTrue(suitabilityRefused.contains("COST_CEILING_EXCEEDED"));
        assertTrue(suitabilityRefused.contains("bindingSha256=<redacted>"));
        assertFalse(suitabilityRefused.contains(digest));
    }

    private static void assertRecordComponents(
            Class<?> recordType,
            Class<?>... expectedTypes) {
        assertTrue(recordType.isRecord());
        assertArrayEquals(
                expectedTypes,
                Stream.of(recordType.getRecordComponents())
                        .map(component -> component.getType())
                        .toArray(Class<?>[]::new));
    }

    private static void assertInvalidDigest(String digest, ModelRouteOption option) {
        Class<? extends RuntimeException> expected = digest == null
                ? NullPointerException.class
                : IllegalArgumentException.class;
        assertThrows(expected,
                () -> new ProtectedRemoteModelRouteOptionPreparationResult.Prepared(
                        option, digest));
        assertThrows(expected,
                () -> new ProtectedRemoteModelRouteOptionPreparationResult
                        .SuitabilityRefused(
                                RemoteModelCandidateSuitabilityRefusalReason
                                        .COST_CEILING_EXCEEDED,
                                digest));
    }
}
