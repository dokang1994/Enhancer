package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

class RemoteCapabilityCatalogCandidateSnapshotTest {

    @Test
    void snapshotDerivesDigestAndCompleteCatalogFromTheSameDefensiveBytes()
            throws Exception {
        byte[] bytes = RemoteModelCapabilityCatalogTestFixture.oneRecord();
        RemoteCapabilityCatalogCandidateSnapshot snapshot =
                RemoteCapabilityCatalogCandidateSnapshot.capture(bytes);
        bytes[0] ^= 1;

        assertEquals("local-capability-catalog", snapshot.catalogIdentity());
        assertEquals(RemoteModelCapabilityCatalogTestFixture.sha256(
                RemoteModelCapabilityCatalogTestFixture.oneRecord()), snapshot.sha256());
        assertEquals(1, snapshot.catalog().records().size());
        byte[] exposed = snapshot.bytes();
        exposed[0] ^= 1;
        assertFalse(java.util.Arrays.equals(exposed, snapshot.bytes()));
        assertFalse(snapshot.toString().contains(snapshot.sha256()));
        assertFalse(snapshot.toString().contains("987654321"));
    }

    @Test
    void snapshotSeparatesSizeFromCompleteCatalogFailure() {
        assertReason(new byte[0],
                RemoteCapabilityCatalogCandidateSnapshot.Failure.OUTSIDE_SIZE_BOUND);
        assertReason(new byte[4 * 1024 * 1024 + 1],
                RemoteCapabilityCatalogCandidateSnapshot.Failure.OUTSIDE_SIZE_BOUND);
        assertReason(new byte[] {'x'},
                RemoteCapabilityCatalogCandidateSnapshot.Failure.CATALOG_INVALID);
        assertReason(new byte[4 * 1024 * 1024],
                RemoteCapabilityCatalogCandidateSnapshot.Failure.CATALOG_INVALID);
    }

    @Test
    void evidenceVerifierContractBindsExactSnapshotAndHasNoImplementation() throws Exception {
        RemoteCapabilityCatalogCandidateSnapshot snapshot =
                RemoteCapabilityCatalogCandidateSnapshot.capture(
                        RemoteModelCapabilityCatalogTestFixture.oneRecord());
        RemoteCapabilityCatalogEvidenceVerificationRequest request =
                new RemoteCapabilityCatalogEvidenceVerificationRequest(
                        "installation-a", "candidate-input-a", "evidence-a", snapshot);

        assertSame(snapshot, request.candidateSnapshot());
        assertFalse(Modifier.isPublic(
                RemoteCapabilityCatalogEvidenceVerifier.class.getModifiers()));
        assertTrue(RemoteCapabilityCatalogEvidenceVerifier.class.isInterface());
        assertEquals(1,
                RemoteCapabilityCatalogEvidenceVerifier.class.getDeclaredMethods().length);
        assertEquals(RemoteCapabilityCatalogEvidenceDecision.class,
                RemoteCapabilityCatalogEvidenceVerifier.class.getDeclaredMethods()[0]
                        .getReturnType());
        assertFalse(request.toString().contains(snapshot.sha256()));
        assertThrows(IllegalArgumentException.class,
                () -> new RemoteCapabilityCatalogEvidenceVerificationRequest(
                        "UPPER", "candidate-input-a", "evidence-a", snapshot));
    }

    private static void assertReason(
            byte[] bytes, RemoteCapabilityCatalogCandidateSnapshot.Failure expected) {
        RemoteCapabilityCatalogCandidateSnapshot.Exception failure = assertThrows(
                RemoteCapabilityCatalogCandidateSnapshot.Exception.class,
                () -> RemoteCapabilityCatalogCandidateSnapshot.capture(bytes));
        assertEquals(expected, failure.failure());
    }
}
