package com.enhancer.maintenance.installation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InstallationPhaseEvidencePointStoreContractTest {
    private static final UUID TRANSACTION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000123");

    @Test
    void createsReadsAndExactReplaysOneImmutableSemanticPoint() throws Exception {
        InMemoryStore store = new InMemoryStore();
        InstallationPhaseEvidence evidence = evidence("1".repeat(64));

        InstallationPhaseEvidencePointStore.Mutation created = store.create(evidence);
        InstallationPhaseEvidencePointStore.Mutation replayed = store.create(evidence);

        assertEquals(InstallationPhaseEvidencePointStore.MutationDisposition.CREATED,
                created.disposition());
        assertEquals(InstallationPhaseEvidencePointStore.MutationDisposition.EXACT_REPLAY,
                replayed.disposition());
        assertEquals(evidence, created.evidence());
        assertEquals(evidence, replayed.evidence());
        assertEquals(Optional.of(evidence), store.read(point()));
        assertEquals(1, store.mutations);
    }

    @Test
    void changedReuseConflictsAndPreservesTheFirstValue() throws Exception {
        InMemoryStore store = new InMemoryStore();
        InstallationPhaseEvidence first = evidence("1".repeat(64));
        InstallationPhaseEvidence changed = evidence("2".repeat(64));
        store.create(first);

        InstallationPhaseEvidenceStoreException failure = assertThrows(
                InstallationPhaseEvidenceStoreException.class,
                () -> store.create(changed));

        assertEquals(InstallationPhaseEvidenceStoreException.Reason.EVIDENCE_CONFLICT,
                failure.reason());
        assertEquals(point(), failure.point());
        assertEquals(Optional.of(first), store.read(point()));
        assertEquals(1, store.mutations);
    }

    @Test
    void exactReadReturnsAbsenceWithoutCreatingOrScanning() throws Exception {
        InMemoryStore store = new InMemoryStore();

        assertTrue(store.read(point()).isEmpty());
        assertFalse(InstallationPhaseEvidenceResolver.class.isAssignableFrom(
                InstallationPhaseEvidencePointStore.class));
        assertEquals(0, store.mutations);
    }

    @Test
    void storeFailureTaxonomyIsFiniteAndPointBound() {
        assertEquals(6, InstallationPhaseEvidenceStoreException.Reason.values().length);
        InstallationPhaseEvidenceStoreException failure =
                new InstallationPhaseEvidenceStoreException(
                        InstallationPhaseEvidenceStoreException.Reason.CORRUPT_EVIDENCE,
                        point(),
                        "stored semantic evidence is corrupt");

        assertEquals(InstallationPhaseEvidenceStoreException.Reason.CORRUPT_EVIDENCE,
                failure.reason());
        assertEquals(point(), failure.point());
    }

    private static InstallationPhaseEvidence evidence(String digest) {
        return new InstallationPhaseEvidence(
                InstallationPhaseEvidence.SCHEMA_VERSION,
                TRANSACTION_ID,
                InstallationPhase.RESOLVE_PRINCIPALS,
                0L,
                digest,
                Optional.empty());
    }

    private static InstallationPhaseEvidencePoint point() {
        return new InstallationPhaseEvidencePoint(
                TRANSACTION_ID, InstallationPhase.RESOLVE_PRINCIPALS, 0L);
    }

    private static final class InMemoryStore implements InstallationPhaseEvidencePointStore {
        private final Map<InstallationPhaseEvidencePoint, InstallationPhaseEvidence> values =
                new HashMap<>();
        private int mutations;

        @Override
        public Mutation create(InstallationPhaseEvidence evidence)
                throws InstallationPhaseEvidenceStoreException {
            InstallationPhaseEvidencePoint point = new InstallationPhaseEvidencePoint(
                    evidence.transactionId(), evidence.phase(), evidence.pendingRevision());
            InstallationPhaseEvidence existing = values.get(point);
            if (existing == null) {
                values.put(point, evidence);
                mutations++;
                return new Mutation(evidence, MutationDisposition.CREATED);
            }
            if (existing.equals(evidence)) {
                return new Mutation(existing, MutationDisposition.EXACT_REPLAY);
            }
            throw new InstallationPhaseEvidenceStoreException(
                    InstallationPhaseEvidenceStoreException.Reason.EVIDENCE_CONFLICT,
                    point,
                    "evidence point is already bound");
        }

        @Override
        public Optional<InstallationPhaseEvidence> read(InstallationPhaseEvidencePoint point) {
            return Optional.ofNullable(values.get(point));
        }
    }
}
