package com.enhancer.maintenance.installation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InstallationIntegrityFileFormatTest {
    private static final String PERMISSION_POLICY_SHA256 = "f".repeat(64);

    @Test
    void transactionFormatDeterministicallyRoundTripsInitialMidAndTerminalStates()
            throws Exception {
        InstallationTransactionState initial = initialState();
        InstallationTransactionState middle = pendingAt(InstallationPhase.PUBLISH_METADATA)
                .markSucceeded(evidence(pendingAt(InstallationPhase.PUBLISH_METADATA)));
        InstallationTransactionState terminal = terminalState();

        for (InstallationTransactionState state : List.of(initial, middle, terminal)) {
            byte[] first = InstallationTransactionFileFormat.encode(state);
            byte[] second = InstallationTransactionFileFormat.encode(state);
            InstallationTransactionState decoded =
                    InstallationTransactionFileFormat.decode(first);

            assertArrayEquals(first, second);
            assertEquals(state, decoded);
            assertArrayEquals(first, InstallationTransactionFileFormat.encode(decoded));
        }
    }

    @Test
    void evidenceFormatRoundTripsNormalAndActivationOnlyAtTheExactExpectedPoint()
            throws Exception {
        InstallationTransactionState normal = initialState();
        InstallationTransactionState activation = pendingAt(InstallationPhase.ACTIVATE);

        for (InstallationTransactionState pending : List.of(normal, activation)) {
            InstallationPhaseEvidence expected = evidence(pending);
            InstallationPhaseEvidencePoint point =
                    InstallationPhaseEvidencePoint.fromPending(pending);
            byte[] first = InstallationPhaseEvidenceFileFormat.encode(expected);
            byte[] second = InstallationPhaseEvidenceFileFormat.encode(expected);

            assertArrayEquals(first, second);
            assertEquals(expected,
                    InstallationPhaseEvidenceFileFormat.decode(first, point));
        }
    }

    @Test
    void filenamesAreCanonicalBoundedAndBindEveryPointField() {
        InstallationTransactionState pending = pendingAt(InstallationPhase.PUBLISH_METADATA);
        InstallationPhaseEvidencePoint point =
                InstallationPhaseEvidencePoint.fromPending(pending);

        String transactionName = InstallationRecordFileNames.transaction(
                pending.plan().transactionId());
        String evidenceName = InstallationRecordFileNames.evidence(point);

        assertEquals("00000000-0000-0000-0000-000000000123.installation-transaction-v1",
                transactionName);
        assertEquals("00000000-0000-0000-0000-000000000123.12.publish-metadata"
                + ".installation-phase-evidence-v1", evidenceName);
        assertFalse(transactionName.contains("/") || transactionName.contains("\\"));
        assertFalse(evidenceName.contains("/") || evidenceName.contains("\\"));
    }

    @Test
    void corruptionWrongDomainTruncationAndTrailingBytesFailClosed() throws Exception {
        byte[] transaction = InstallationTransactionFileFormat.encode(initialState());
        byte[] evidence = InstallationPhaseEvidenceFileFormat.encode(evidence(initialState()));

        byte[] corrupt = transaction.clone();
        corrupt[corrupt.length - 1] ^= 1;
        assertReason(InstallationRecordFormatException.Reason.CORRUPT_RECORD,
                () -> InstallationTransactionFileFormat.decode(corrupt));
        assertReason(InstallationRecordFormatException.Reason.CORRUPT_RECORD,
                () -> InstallationPhaseEvidenceFileFormat.decode(
                        transaction,
                        InstallationPhaseEvidencePoint.fromPending(initialState())));
        assertReason(InstallationRecordFormatException.Reason.CORRUPT_RECORD,
                () -> InstallationTransactionFileFormat.decode(evidence));
        assertReason(InstallationRecordFormatException.Reason.CORRUPT_RECORD,
                () -> InstallationTransactionFileFormat.decode(
                        Arrays.copyOf(transaction, transaction.length - 1)));
        assertReason(InstallationRecordFormatException.Reason.CORRUPT_RECORD,
                () -> InstallationTransactionFileFormat.decode(
                        Arrays.copyOf(transaction, transaction.length + 1)));
    }

    @Test
    void unsupportedEnvelopeAndDomainSchemasAreTypedRefusals() throws Exception {
        byte[] envelopeSchema = InstallationTransactionFileFormat.encode(initialState());
        ByteBuffer.wrap(envelopeSchema).putInt(Integer.BYTES, 99);
        replaceDigest(envelopeSchema);
        assertReason(InstallationRecordFormatException.Reason.UNSUPPORTED_SCHEMA,
                () -> InstallationTransactionFileFormat.decode(envelopeSchema));

        byte[] domainSchema = InstallationTransactionFileFormat.encode(initialState());
        ByteBuffer buffer = ByteBuffer.wrap(domainSchema);
        int bodyStart = InstallationIntegrityEnvelope.HEADER_BYTES;
        int kindLength = buffer.getInt(bodyStart);
        buffer.putInt(bodyStart + Integer.BYTES + kindLength, 99);
        replaceDigest(domainSchema);
        assertReason(InstallationRecordFormatException.Reason.UNSUPPORTED_SCHEMA,
                () -> InstallationTransactionFileFormat.decode(domainSchema));
    }

    @Test
    void validForeignEvidenceIsRejectedAgainstTheExpectedPoint() throws Exception {
        InstallationTransactionState expected = initialState();
        InstallationPhaseEvidence foreign = new InstallationPhaseEvidence(
                InstallationPhaseEvidence.SCHEMA_VERSION,
                UUID.fromString("00000000-0000-0000-0000-000000000999"),
                expected.phase(),
                expected.revision(),
                "a".repeat(64),
                Optional.empty());
        byte[] validForeignEnvelope = InstallationPhaseEvidenceFileFormat.encode(foreign);

        assertReason(InstallationRecordFormatException.Reason.FOREIGN_RECORD,
                () -> InstallationPhaseEvidenceFileFormat.decode(
                        validForeignEnvelope,
                        InstallationPhaseEvidencePoint.fromPending(expected)));
    }

    @Test
    void malformedUtf8AndOversizedDeclaredLengthFailClosed() throws Exception {
        byte[] malformed = InstallationTransactionFileFormat.encode(initialState());
        int firstString = InstallationIntegrityEnvelope.HEADER_BYTES + Integer.BYTES;
        malformed[firstString] = (byte) 0xc3;
        malformed[firstString + 1] = (byte) 0x28;
        replaceDigest(malformed);
        assertReason(InstallationRecordFormatException.Reason.CORRUPT_RECORD,
                () -> InstallationTransactionFileFormat.decode(malformed));

        byte[] oversized = InstallationTransactionFileFormat.encode(initialState());
        ByteBuffer.wrap(oversized).putInt(
                Integer.BYTES * 2,
                InstallationTransactionFileFormat.MAX_BODY_BYTES + 1);
        assertReason(InstallationRecordFormatException.Reason.SIZE_LIMIT_EXCEEDED,
                () -> InstallationTransactionFileFormat.decode(oversized));
    }

    @Test
    void foreignPathDialectAndNonCanonicalUnicodeFailClosed() throws Exception {
        byte[] foreignDialect = InstallationTransactionFileFormat.encode(initialState());
        String current = File.separatorChar == '\\' ? "windows" : "posix";
        String replacement = File.separatorChar == '\\' ? "foreign" : "other";
        byte[] currentBytes = current.getBytes(StandardCharsets.UTF_8);
        int dialectOffset = indexOf(foreignDialect, currentBytes);
        System.arraycopy(
                replacement.getBytes(StandardCharsets.UTF_8),
                0,
                foreignDialect,
                dialectOffset,
                currentBytes.length);
        replaceDigest(foreignDialect);
        assertReason(InstallationRecordFormatException.Reason.FOREIGN_RECORD,
                () -> InstallationTransactionFileFormat.decode(foreignDialect));

        assertReason(InstallationRecordFormatException.Reason.NON_CANONICAL_RECORD,
                () -> InstallationTransactionFileFormat.encode(
                        stateWithInstallerIdentity("\ud800")));
    }

    private static InstallationTransactionState initialState() {
        CancellationTrustInstallationPlan plan = CancellationTrustInstallationPlanTest.validPlan();
        InstallationEnvironmentEvidence environment = new InstallationEnvironmentEvidence(
                plan.transactionId(), "fake-adapter", "fake-v1", plan.principals(),
                "fake-filesystem", true, true);
        return InstallationTransactionState.start(
                plan, environment, "release-v1", PERMISSION_POLICY_SHA256,
                Optional.of("activation-old"), "activation-new");
    }

    private static InstallationTransactionState stateWithInstallerIdentity(String identity) {
        CancellationTrustInstallationPlan source =
                CancellationTrustInstallationPlanTest.validPlan();
        InstallationPrincipalSet principals = new InstallationPrincipalSet(
                new InstallationPrincipal(
                        InstallationPrincipalRole.INSTALLER_PUBLISHER, identity),
                source.principals().operator(),
                source.principals().runtime());
        CancellationTrustInstallationPlan plan = new CancellationTrustInstallationPlan(
                source.transactionId(),
                source.operation(),
                principals,
                source.installationRoot(),
                source.applicationJar(),
                source.runtimeDistributionRoot(),
                source.operatorDistributionRoot(),
                source.operatorCandidateInbox(),
                source.activationPoint(),
                source.auditRoot(),
                source.sourceManifestSha256(),
                source.applicationJarSha256(),
                source.runtimeDistributionSha256(),
                source.operatorDistributionSha256(),
                source.permissionPolicyRevision(),
                source.policySha256(),
                source.requestedMetadataSha256(),
                source.expectedCurrentMetadataSha256());
        InstallationEnvironmentEvidence environment = new InstallationEnvironmentEvidence(
                plan.transactionId(), "fake-adapter", "fake-v1", principals,
                "fake-filesystem", true, true);
        return InstallationTransactionState.start(
                plan, environment, "release-v1", PERMISSION_POLICY_SHA256,
                Optional.of("activation-old"), "activation-new");
    }

    private static InstallationTransactionState pendingAt(InstallationPhase target) {
        InstallationTransactionState state = initialState();
        while (state.phase() != target) {
            state = state.markSucceeded(evidence(state)).beginNext();
        }
        return state;
    }

    private static InstallationTransactionState terminalState() {
        InstallationTransactionState state = initialState();
        while (true) {
            state = state.markSucceeded(evidence(state));
            if (state.isTerminalRecord()) {
                return state;
            }
            state = state.beginNext();
        }
    }

    private static InstallationPhaseEvidence evidence(InstallationTransactionState pending) {
        int phaseIndex = InstallationPhase.requiredOrder().indexOf(pending.phase());
        Optional<String> activation = pending.phase() == InstallationPhase.ACTIVATE
                ? Optional.of(pending.requestedActivationIdentity())
                : Optional.empty();
        return new InstallationPhaseEvidence(
                InstallationPhaseEvidence.SCHEMA_VERSION,
                pending.plan().transactionId(),
                pending.phase(),
                pending.revision(),
                String.format("%064x", phaseIndex + 1),
                activation);
    }

    private static void replaceDigest(byte[] envelope) throws Exception {
        ByteBuffer source = ByteBuffer.wrap(envelope);
        int magic = source.getInt(0);
        int schema = source.getInt(Integer.BYTES);
        int length = source.getInt(Integer.BYTES * 2);
        byte[] body = Arrays.copyOfRange(
                envelope,
                InstallationIntegrityEnvelope.HEADER_BYTES,
                InstallationIntegrityEnvelope.HEADER_BYTES + length);
        ByteBuffer authenticated = ByteBuffer.allocate(Integer.BYTES * 3 + body.length)
                .putInt(magic)
                .putInt(schema)
                .putInt(length)
                .put(body);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(authenticated.array());
        System.arraycopy(
                digest,
                0,
                envelope,
                Integer.BYTES * 3,
                digest.length);
    }

    private static int indexOf(byte[] source, byte[] target) {
        for (int offset = 0; offset <= source.length - target.length; offset++) {
            boolean match = true;
            for (int index = 0; index < target.length; index++) {
                if (source[offset + index] != target[index]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return offset;
            }
        }
        throw new AssertionError("expected encoded token was not found");
    }

    private static void assertReason(
            InstallationRecordFormatException.Reason reason,
            Throwing action) {
        InstallationRecordFormatException failure = assertThrows(
                InstallationRecordFormatException.class,
                action::run);
        assertEquals(reason, failure.reason());
    }

    private interface Throwing {
        void run() throws Exception;
    }
}
