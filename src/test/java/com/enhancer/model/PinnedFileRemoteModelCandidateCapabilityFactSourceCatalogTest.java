package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PinnedFileRemoteModelCandidateCapabilityFactSourceCatalogTest {

    private static final Instant NOW = Instant.parse("2026-10-23T00:01:00Z");

    @TempDir
    Path temporaryRoot;

    @Test
    void zeroAndMaximumRecordCatalogsPassTheFirstEightStages() throws Exception {
        assertReason(RemoteModelCapabilityCatalogTestFixture.zeroRecords(),
                RemoteModelCandidateCapabilityFactSourceRefusalReason.RECORD_NOT_FOUND);
        assertReason(RemoteModelCapabilityCatalogTestFixture.maximumRecords(),
                RemoteModelCandidateCapabilityFactSourceRefusalReason.RECORD_NOT_FOUND);
    }

    @Test
    void unsupportedCatalogAndRecordSchemasReturnReasonSix() throws Exception {
        byte[] one = RemoteModelCapabilityCatalogTestFixture.oneRecord();
        assertReason(RemoteModelCapabilityCatalogTestFixture.replace(one,
                        "capability-catalog-v1", "capability-catalog-v2"),
                RemoteModelCandidateCapabilityFactSourceRefusalReason
                        .CATALOG_SCHEMA_UNSUPPORTED);
        assertReason(RemoteModelCapabilityCatalogTestFixture.replace(one,
                        "capability-facts-v1", "capability-facts-v2"),
                RemoteModelCandidateCapabilityFactSourceRefusalReason
                        .CATALOG_SCHEMA_UNSUPPORTED);
    }

    @Test
    void noncanonicalStructuresAndLexemesReturnReasonSeven() throws Exception {
        byte[] one = RemoteModelCapabilityCatalogTestFixture.oneRecord();
        String text = new String(one, StandardCharsets.UTF_8);
        for (byte[] invalid : List.of(
                ("\uFEFF" + text).getBytes(StandardCharsets.UTF_8),
                text.replace("\n", "\r\n").getBytes(StandardCharsets.UTF_8),
                text.substring(0, text.length() - 1).getBytes(StandardCharsets.UTF_8),
                (text + "\n").getBytes(StandardCharsets.UTF_8),
                text.replace("recordCount=1", "recordCount=01")
                        .getBytes(StandardCharsets.UTF_8),
                text.replace("maxInputTokens=10", "maxInputTokens=+10")
                        .getBytes(StandardCharsets.UTF_8),
                text.replace("2026-10-23T00:00:00Z", "2026-10-23T00:00:00.000Z")
                        .getBytes(StandardCharsets.UTF_8),
                text.replace("record.0.capacityEvidenceIdentity=",
                        "unknown=value\nrecord.0.capacityEvidenceIdentity=")
                        .getBytes(StandardCharsets.UTF_8))) {
            assertReason(invalid,
                    RemoteModelCandidateCapabilityFactSourceRefusalReason
                            .CATALOG_NOT_CANONICAL);
        }
    }

    @Test
    void semanticViolationsReturnReasonEight() throws Exception {
        byte[] one = RemoteModelCapabilityCatalogTestFixture.oneRecord();
        for (byte[] invalid : List.of(
                RemoteModelCapabilityCatalogTestFixture.replace(one,
                        "catalogIdentity=local-capability-catalog",
                        "catalogIdentity=INVALID"),
                RemoteModelCapabilityCatalogTestFixture.replace(one,
                        "modelClass=reasoning-standard",
                        "modelClass=" + "a".repeat(65)),
                RemoteModelCapabilityCatalogTestFixture.replace(one,
                        "tokenSemanticsIdentity=profile-token-budget-v1",
                        "tokenSemanticsIdentity=other"),
                RemoteModelCapabilityCatalogTestFixture.replace(one,
                        "maximumReasoningRequirement=EXTENDED",
                        "maximumReasoningRequirement=UNKNOWN"),
                RemoteModelCapabilityCatalogTestFixture.replace(one,
                        "maximumContextTokens=1000", "maximumContextTokens=0"),
                RemoteModelCapabilityCatalogTestFixture.replace(one,
                        "maximumInvocationTimeMillis=60000",
                        "maximumInvocationTimeMillis=300001"),
                RemoteModelCapabilityCatalogTestFixture.replace(one,
                        "costCurrencyCode=USD", "costCurrencyCode=usd"),
                RemoteModelCapabilityCatalogTestFixture.replace(one,
                        "observedAt=2026-10-23T00:00:00Z",
                        "observedAt=2026-10-23T00:05:00Z"))) {
            assertReason(invalid,
                    RemoteModelCandidateCapabilityFactSourceRefusalReason
                            .CATALOG_CONTENT_INVALID);
        }
    }

    @Test
    void duplicateUnsortedAndInvalidUnselectedRecordsInvalidateWholeCatalog()
            throws Exception {
        assertReason(RemoteModelCapabilityCatalogTestFixture.twoRecords(
                        "candidate-b", "destination-a",
                        "candidate-a", "destination-a"),
                RemoteModelCandidateCapabilityFactSourceRefusalReason
                        .CATALOG_CONTENT_INVALID);
        assertReason(RemoteModelCapabilityCatalogTestFixture.twoRecords(
                        "candidate-a", "destination-a",
                        "candidate-a", "destination-a"),
                RemoteModelCandidateCapabilityFactSourceRefusalReason
                        .CATALOG_CONTENT_INVALID);

        byte[] two = RemoteModelCapabilityCatalogTestFixture.twoRecords(
                "candidate-a", "destination-a", "candidate-b", "destination-a");
        assertReason(RemoteModelCapabilityCatalogTestFixture.replace(two,
                        "record.1.maximumContextTokens=1000",
                        "record.1.maximumContextTokens=0"),
                RemoteModelCandidateCapabilityFactSourceRefusalReason
                        .CATALOG_CONTENT_INVALID);
    }

    private void assertReason(byte[] content,
            RemoteModelCandidateCapabilityFactSourceRefusalReason expected)
            throws IOException {
        Path path = temporaryRoot.resolve("catalog-"
                + Math.abs(java.util.Arrays.hashCode(content)))
                .toAbsolutePath().normalize();
        Files.write(path, content);
        RemoteModelCandidateCapabilityFactSourceResult.Refused refused =
                (RemoteModelCandidateCapabilityFactSourceResult.Refused)
                        new PinnedFileRemoteModelCandidateCapabilityFactSource(
                                path,
                                RemoteModelCapabilityCatalogTestFixture.sha256(content))
                                .acquire("missing-candidate", "missing-destination", NOW);
        assertEquals(expected, refused.reason());
    }
}
