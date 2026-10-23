package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PinnedFileRemoteModelCandidateCapabilityFactSourceLookupTest {

    @TempDir
    Path temporaryRoot;

    @Test
    void exactMatchMapsEveryRfc0030FieldAndVerifiedPin() throws Exception {
        byte[] bytes = RemoteModelCapabilityCatalogTestFixture.oneRecord();
        String pin = RemoteModelCapabilityCatalogTestFixture.sha256(bytes);
        RemoteModelCandidateCapabilityFacts facts = acquired(
                source(write(bytes), pin), Instant.parse("2026-10-23T00:01:00Z"));

        assertEquals(RemoteModelCandidateCapabilityFacts.SCHEMA_VERSION,
                facts.schemaVersion());
        assertEquals("local-capability-catalog", facts.factSourceIdentity());
        assertEquals(pin, facts.sourceRevision());
        assertEquals("capacity-evidence-a", facts.capacityEvidenceIdentity());
        assertEquals(Instant.parse("2026-10-23T00:00:00Z"), facts.observedAt());
        assertEquals(Instant.parse("2026-10-23T00:05:00Z"), facts.expiresAt());
        assertEquals("candidate-a", facts.candidateIdentity());
        assertEquals("destination-a", facts.destinationIdentity());
        assertEquals("reasoning-standard", facts.modelClass());
        assertEquals("repository-analysis", facts.requiredCapability());
        assertEquals("profile-token-budget-v1", facts.tokenSemanticsIdentity());
        assertEquals("schedule-a", facts.priceScheduleIdentity());
        assertEquals(new ModelTokenBudget(10, 5, 15), facts.tokenBudgetBasis());
        assertEquals(ModelReasoningRequirement.EXTENDED,
                facts.maximumReasoningRequirement());
        assertEquals(1000, facts.maximumContextTokens());
        assertEquals(new ModelTokenBudget(100, 50, 150), facts.maximumTokenBudget());
        assertEquals(Duration.ofMillis(60000), facts.maximumInvocationTime());
        assertEquals("USD", facts.costCurrencyCode());
        assertEquals(987654321, facts.maximumCostMicrounits());
        assertEquals(ModelDataClassification.RESTRICTED,
                facts.maximumDataClassification());
    }

    @Test
    void lookupIsExactAndMissingPairReturnsRecordNotFound() throws Exception {
        byte[] bytes = RemoteModelCapabilityCatalogTestFixture.oneRecord();
        RemoteModelCandidateCapabilityFactSourceResult.Refused refused = assertInstanceOf(
                RemoteModelCandidateCapabilityFactSourceResult.Refused.class,
                source(write(bytes), RemoteModelCapabilityCatalogTestFixture.sha256(bytes))
                        .acquire("candidate-a", "destination-b",
                                Instant.parse("2026-10-23T00:01:00Z")));
        assertEquals(RemoteModelCandidateCapabilityFactSourceRefusalReason.RECORD_NOT_FOUND,
                refused.reason());
    }

    @Test
    void freshnessIsInclusiveAtObservationAndExclusiveAtExpiry() throws Exception {
        byte[] bytes = RemoteModelCapabilityCatalogTestFixture.oneRecord();
        PinnedFileRemoteModelCandidateCapabilityFactSource source = source(
                write(bytes), RemoteModelCapabilityCatalogTestFixture.sha256(bytes));
        acquired(source, Instant.parse("2026-10-23T00:00:00Z"));
        assertStale(source, Instant.parse("2026-10-22T23:59:59Z"));
        assertStale(source, Instant.parse("2026-10-23T00:05:00Z"));
    }

    @Test
    void everyAcquisitionRereadsAndRotationRequiresANewPinnedSource() throws Exception {
        byte[] original = RemoteModelCapabilityCatalogTestFixture.oneRecord();
        byte[] rotated = RemoteModelCapabilityCatalogTestFixture.replace(original,
                "capacityEvidenceIdentity=capacity-evidence-a",
                "capacityEvidenceIdentity=capacity-evidence-b");
        Path path = write(original);
        PinnedFileRemoteModelCandidateCapabilityFactSource oldSource = source(
                path, RemoteModelCapabilityCatalogTestFixture.sha256(original));
        acquired(oldSource, Instant.parse("2026-10-23T00:01:00Z"));

        Files.write(path, rotated);
        RemoteModelCandidateCapabilityFactSourceResult.Refused mismatch = assertInstanceOf(
                RemoteModelCandidateCapabilityFactSourceResult.Refused.class,
                oldSource.acquire("candidate-a", "destination-a",
                        Instant.parse("2026-10-23T00:01:00Z")));
        assertEquals(RemoteModelCandidateCapabilityFactSourceRefusalReason
                .CATALOG_PIN_MISMATCH, mismatch.reason());

        RemoteModelCandidateCapabilityFacts rotatedFacts = acquired(
                source(path, RemoteModelCapabilityCatalogTestFixture.sha256(rotated)),
                Instant.parse("2026-10-23T00:01:00Z"));
        assertEquals("capacity-evidence-b", rotatedFacts.capacityEvidenceIdentity());
    }

    private RemoteModelCandidateCapabilityFacts acquired(
            PinnedFileRemoteModelCandidateCapabilityFactSource source, Instant time) {
        RemoteModelCandidateCapabilityFactSourceResult.Acquired acquired = assertInstanceOf(
                RemoteModelCandidateCapabilityFactSourceResult.Acquired.class,
                source.acquire("candidate-a", "destination-a", time));
        return acquired.facts();
    }

    private void assertStale(PinnedFileRemoteModelCandidateCapabilityFactSource source,
            Instant time) {
        RemoteModelCandidateCapabilityFactSourceResult.Refused refused = assertInstanceOf(
                RemoteModelCandidateCapabilityFactSourceResult.Refused.class,
                source.acquire("candidate-a", "destination-a", time));
        assertEquals(RemoteModelCandidateCapabilityFactSourceRefusalReason.RECORD_STALE,
                refused.reason());
    }

    private Path write(byte[] bytes) throws Exception {
        Path path = temporaryRoot.resolve("catalog").toAbsolutePath().normalize();
        Files.write(path, bytes);
        return path;
    }

    private static PinnedFileRemoteModelCandidateCapabilityFactSource source(
            Path path, String pin) {
        return new PinnedFileRemoteModelCandidateCapabilityFactSource(path, pin);
    }
}
