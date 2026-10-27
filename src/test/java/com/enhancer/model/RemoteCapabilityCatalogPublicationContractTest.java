package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RemoteCapabilityCatalogPublicationContractTest {
    private static final String INSTALLATION = "installation-a";
    private static final String CANDIDATE = "candidate-input-a";
    private static final String EVIDENCE = "evidence-a";
    private static final String CATALOG = "catalog-a";
    private static final String CATALOG_DIGEST = "1".repeat(64);
    private static final String BINDING_DIGEST = "2".repeat(64);

    @Test
    void vocabulariesAndResultShapeAreClosedInRfcOrder() {
        assertArrayEquals(
                new RemoteCapabilityCatalogPublicationOperation[] {
                        RemoteCapabilityCatalogPublicationOperation.INSTALL,
                        RemoteCapabilityCatalogPublicationOperation.ROTATE
                },
                RemoteCapabilityCatalogPublicationOperation.values());
        assertArrayEquals(
                new RemoteCapabilityCatalogPublicationStatus[] {
                        RemoteCapabilityCatalogPublicationStatus.INSTALLED,
                        RemoteCapabilityCatalogPublicationStatus.ROTATED,
                        RemoteCapabilityCatalogPublicationStatus.EXACT_REPLAY
                },
                RemoteCapabilityCatalogPublicationStatus.values());
        assertArrayEquals(
                new RemoteCapabilityCatalogPublicationRefusalReason[] {
                        RemoteCapabilityCatalogPublicationRefusalReason
                                .INSTALLATION_ROOT_UNAVAILABLE_OR_UNSAFE,
                        RemoteCapabilityCatalogPublicationRefusalReason
                                .MAINTENANCE_LOCK_UNAVAILABLE_OR_CONTENDED,
                        RemoteCapabilityCatalogPublicationRefusalReason
                                .CURRENT_BINDING_REQUIRED,
                        RemoteCapabilityCatalogPublicationRefusalReason
                                .CURRENT_BINDING_ALREADY_EXISTS,
                        RemoteCapabilityCatalogPublicationRefusalReason
                                .CURRENT_BINDING_INVALID_OR_UNSAFE,
                        RemoteCapabilityCatalogPublicationRefusalReason
                                .CANDIDATE_UNAVAILABLE_OR_UNSAFE,
                        RemoteCapabilityCatalogPublicationRefusalReason
                                .CANDIDATE_OUTSIDE_SIZE_BOUND,
                        RemoteCapabilityCatalogPublicationRefusalReason
                                .CANDIDATE_CHANGED_DURING_READ,
                        RemoteCapabilityCatalogPublicationRefusalReason.CATALOG_INVALID,
                        RemoteCapabilityCatalogPublicationRefusalReason
                                .EVIDENCE_NOT_APPROVED_OR_MISMATCHED,
                        RemoteCapabilityCatalogPublicationRefusalReason
                                .EXPECTED_CURRENT_MISMATCH,
                        RemoteCapabilityCatalogPublicationRefusalReason
                                .IMMUTABLE_CATALOG_CONFLICT,
                        RemoteCapabilityCatalogPublicationRefusalReason
                                .ATOMIC_PUBLICATION_UNSUPPORTED_OR_FAILED,
                        RemoteCapabilityCatalogPublicationRefusalReason
                                .POST_PUBLICATION_STATE_UNKNOWN
                },
                RemoteCapabilityCatalogPublicationRefusalReason.values());

        assertTrue(RemoteCapabilityCatalogPublicationResult.class.isSealed());
        assertArrayEquals(
                new Class<?>[] {
                        RemoteCapabilityCatalogPublicationResult.Succeeded.class,
                        RemoteCapabilityCatalogPublicationResult.Refused.class
                },
                RemoteCapabilityCatalogPublicationResult.class.getPermittedSubclasses());
        assertTrue(RemoteCapabilityCatalogPublicationResult.Succeeded.class.isRecord());
        assertTrue(RemoteCapabilityCatalogPublicationResult.Refused.class.isRecord());
        assertFalse(Serializable.class.isAssignableFrom(
                RemoteCapabilityCatalogPublicationResult.class));
    }

    @Test
    void intentRetainsOnlyBoundedIdentityAndOperationSpecificCasInput() {
        RemoteCapabilityCatalogPublicationIntent install =
                new RemoteCapabilityCatalogPublicationIntent(
                        RemoteCapabilityCatalogPublicationOperation.INSTALL,
                        INSTALLATION,
                        CANDIDATE,
                        EVIDENCE,
                        Optional.empty());
        RemoteCapabilityCatalogPublicationIntent rotate =
                new RemoteCapabilityCatalogPublicationIntent(
                        RemoteCapabilityCatalogPublicationOperation.ROTATE,
                        INSTALLATION,
                        CANDIDATE,
                        EVIDENCE,
                        Optional.of(BINDING_DIGEST));

        assertSame(RemoteCapabilityCatalogPublicationOperation.INSTALL,
                install.operation());
        assertEquals(INSTALLATION, install.installationIdentity());
        assertEquals(CANDIDATE, install.candidateInputIdentity());
        assertEquals(EVIDENCE, install.evidenceIdentity());
        assertEquals(Optional.empty(), install.expectedCurrentBindingSha256());
        assertEquals(Optional.of(BINDING_DIGEST),
                rotate.expectedCurrentBindingSha256());

        assertArrayEquals(
                new String[] {
                        "operation", "installationIdentity", "candidateInputIdentity",
                        "evidenceIdentity", "expectedCurrentBindingSha256"
                },
                java.util.Arrays.stream(
                                RemoteCapabilityCatalogPublicationIntent.class
                                        .getRecordComponents())
                        .map(java.lang.reflect.RecordComponent::getName)
                        .toArray(String[]::new));
    }

    @Test
    void intentRejectsNullInvalidOrOperationInapplicableInput() {
        assertThrows(NullPointerException.class,
                () -> intent(null, INSTALLATION, CANDIDATE, EVIDENCE, Optional.empty()));
        assertThrows(NullPointerException.class,
                () -> intent(RemoteCapabilityCatalogPublicationOperation.INSTALL,
                        null, CANDIDATE, EVIDENCE, Optional.empty()));
        assertThrows(NullPointerException.class,
                () -> intent(RemoteCapabilityCatalogPublicationOperation.INSTALL,
                        INSTALLATION, null, EVIDENCE, Optional.empty()));
        assertThrows(NullPointerException.class,
                () -> intent(RemoteCapabilityCatalogPublicationOperation.INSTALL,
                        INSTALLATION, CANDIDATE, null, Optional.empty()));
        assertThrows(NullPointerException.class,
                () -> intent(RemoteCapabilityCatalogPublicationOperation.INSTALL,
                        INSTALLATION, CANDIDATE, EVIDENCE, null));

        for (String invalid : new String[] {
                "", "UPPER", "leading-", "-trailing", "two--hyphens",
                "contains/slash", "contains\\slash", "contains\ncontrol",
                "a".repeat(257)
        }) {
            assertThrows(IllegalArgumentException.class,
                    () -> intent(RemoteCapabilityCatalogPublicationOperation.INSTALL,
                            invalid, CANDIDATE, EVIDENCE, Optional.empty()));
        }
        assertEquals(256,
                intent(RemoteCapabilityCatalogPublicationOperation.INSTALL,
                        "a".repeat(256), CANDIDATE, EVIDENCE, Optional.empty())
                        .installationIdentity().length());

        assertThrows(IllegalArgumentException.class,
                () -> intent(RemoteCapabilityCatalogPublicationOperation.INSTALL,
                        INSTALLATION, CANDIDATE, EVIDENCE,
                        Optional.of(BINDING_DIGEST)));
        assertThrows(IllegalArgumentException.class,
                () -> intent(RemoteCapabilityCatalogPublicationOperation.ROTATE,
                        INSTALLATION, CANDIDATE, EVIDENCE, Optional.empty()));
        for (String invalidDigest : new String[] {
                "", "1".repeat(63), "1".repeat(65), "A".repeat(64), "g".repeat(64)
        }) {
            assertThrows(IllegalArgumentException.class,
                    () -> intent(RemoteCapabilityCatalogPublicationOperation.ROTATE,
                            INSTALLATION, CANDIDATE, EVIDENCE,
                            Optional.of(invalidDigest)));
        }
    }

    @Test
    void resultsEnforceOperationStatusAndBoundedIdentityShape() {
        RemoteCapabilityCatalogPublicationResult.Succeeded installed = success(
                RemoteCapabilityCatalogPublicationOperation.INSTALL,
                RemoteCapabilityCatalogPublicationStatus.INSTALLED);
        RemoteCapabilityCatalogPublicationResult.Succeeded replay = success(
                RemoteCapabilityCatalogPublicationOperation.ROTATE,
                RemoteCapabilityCatalogPublicationStatus.EXACT_REPLAY);
        RemoteCapabilityCatalogPublicationResult.Refused refused =
                new RemoteCapabilityCatalogPublicationResult.Refused(
                        RemoteCapabilityCatalogPublicationOperation.ROTATE,
                        RemoteCapabilityCatalogPublicationRefusalReason
                                .EXPECTED_CURRENT_MISMATCH,
                        INSTALLATION,
                        EVIDENCE);

        assertEquals(CATALOG_DIGEST, installed.catalogSha256());
        assertEquals(BINDING_DIGEST, installed.bindingSha256());
        assertSame(RemoteCapabilityCatalogPublicationStatus.EXACT_REPLAY,
                replay.status());
        assertSame(RemoteCapabilityCatalogPublicationRefusalReason
                        .EXPECTED_CURRENT_MISMATCH,
                refused.reason());

        assertThrows(IllegalArgumentException.class,
                () -> success(RemoteCapabilityCatalogPublicationOperation.INSTALL,
                        RemoteCapabilityCatalogPublicationStatus.ROTATED));
        assertThrows(IllegalArgumentException.class,
                () -> success(RemoteCapabilityCatalogPublicationOperation.ROTATE,
                        RemoteCapabilityCatalogPublicationStatus.INSTALLED));
        assertThrows(IllegalArgumentException.class,
                () -> new RemoteCapabilityCatalogPublicationResult.Succeeded(
                        RemoteCapabilityCatalogPublicationOperation.INSTALL,
                        RemoteCapabilityCatalogPublicationStatus.INSTALLED,
                        "UPPER", EVIDENCE, CATALOG, CATALOG_DIGEST, BINDING_DIGEST));
        assertThrows(IllegalArgumentException.class,
                () -> new RemoteCapabilityCatalogPublicationResult.Succeeded(
                        RemoteCapabilityCatalogPublicationOperation.INSTALL,
                        RemoteCapabilityCatalogPublicationStatus.INSTALLED,
                        INSTALLATION, EVIDENCE, CATALOG,
                        "f".repeat(63), BINDING_DIGEST));
        assertThrows(NullPointerException.class,
                () -> new RemoteCapabilityCatalogPublicationResult.Refused(
                        RemoteCapabilityCatalogPublicationOperation.INSTALL,
                        null, INSTALLATION, EVIDENCE));
    }

    @Test
    void intentAndResultsRenderWithoutAuthorityBearingOrSensitiveValues() {
        RemoteCapabilityCatalogPublicationIntent rotate = intent(
                RemoteCapabilityCatalogPublicationOperation.ROTATE,
                INSTALLATION, CANDIDATE, EVIDENCE, Optional.of(BINDING_DIGEST));
        RemoteCapabilityCatalogPublicationResult.Succeeded succeeded = success(
                RemoteCapabilityCatalogPublicationOperation.ROTATE,
                RemoteCapabilityCatalogPublicationStatus.ROTATED);

        for (Object value : new Object[] {rotate, succeeded}) {
            String rendered = value.toString();
            assertFalse(rendered.contains(CANDIDATE));
            assertFalse(rendered.contains(CATALOG_DIGEST));
            assertFalse(rendered.contains(BINDING_DIGEST));
            assertFalse(rendered.contains("C:\\protected"));
            assertFalse(rendered.contains("https://provider.invalid"));
            assertFalse(rendered.contains("credential-sentinel"));
        }
        assertTrue(rotate.toString().contains("ROTATE"));
        assertTrue(succeeded.toString().contains("ROTATED"));
    }

    @Test
    void publisherPortIsPackagePrivateAndHasOneClosedMethod() {
        assertTrue(RemoteCapabilityCatalogPublisher.class.isInterface());
        assertFalse(Modifier.isPublic(
                RemoteCapabilityCatalogPublisher.class.getModifiers()));
        assertEquals(1, RemoteCapabilityCatalogPublisher.class.getDeclaredMethods().length);
        assertEquals(RemoteCapabilityCatalogPublicationResult.class,
                RemoteCapabilityCatalogPublisher.class.getDeclaredMethods()[0]
                        .getReturnType());
        assertArrayEquals(
                new Class<?>[] {RemoteCapabilityCatalogPublicationIntent.class},
                RemoteCapabilityCatalogPublisher.class.getDeclaredMethods()[0]
                        .getParameterTypes());
    }

    private static RemoteCapabilityCatalogPublicationIntent intent(
            RemoteCapabilityCatalogPublicationOperation operation,
            String installationIdentity,
            String candidateInputIdentity,
            String evidenceIdentity,
            Optional<String> expectedCurrentBindingSha256) {
        return new RemoteCapabilityCatalogPublicationIntent(
                operation,
                installationIdentity,
                candidateInputIdentity,
                evidenceIdentity,
                expectedCurrentBindingSha256);
    }

    private static RemoteCapabilityCatalogPublicationResult.Succeeded success(
            RemoteCapabilityCatalogPublicationOperation operation,
            RemoteCapabilityCatalogPublicationStatus status) {
        return new RemoteCapabilityCatalogPublicationResult.Succeeded(
                operation,
                status,
                INSTALLATION,
                EVIDENCE,
                CATALOG,
                CATALOG_DIGEST,
                BINDING_DIGEST);
    }
}
