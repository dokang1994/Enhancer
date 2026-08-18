package com.enhancer.maintenance.installation;

import java.util.Arrays;
import java.util.Objects;

/** Deterministic bounded byte format for one semantic phase-evidence point value. */
final class InstallationPhaseEvidenceFileFormat {
    private static final int MAGIC = 0x49504531;
    private static final String PAYLOAD_KIND = "installation-phase-evidence";
    private static final int MAX_BODY_BYTES = 16 * 1024;

    private InstallationPhaseEvidenceFileFormat() {}

    static byte[] encode(InstallationPhaseEvidence evidence)
            throws InstallationRecordFormatException {
        InstallationPhaseEvidence checked = Objects.requireNonNull(
                evidence, "evidence must not be null");
        InstallationIntegrityEnvelope.Writer writer =
                new InstallationIntegrityEnvelope.Writer();
        writer.writeString(PAYLOAD_KIND);
        writeValue(writer, checked);
        return InstallationIntegrityEnvelope.encode(MAGIC, MAX_BODY_BYTES, writer.toByteArray());
    }

    static InstallationPhaseEvidence decode(
            byte[] envelope,
            InstallationPhaseEvidencePoint expectedPoint)
            throws InstallationRecordFormatException {
        byte[] body = InstallationIntegrityEnvelope.decode(MAGIC, MAX_BODY_BYTES, envelope);
        InstallationIntegrityEnvelope.Reader reader =
                new InstallationIntegrityEnvelope.Reader(body);
        if (!PAYLOAD_KIND.equals(reader.readString())) {
            throw InstallationIntegrityEnvelope.corrupt("evidence payload kind is invalid");
        }
        InstallationPhaseEvidence evidence = readValue(reader);
        reader.requireFinished();
        if (!Arrays.equals(envelope, encode(evidence))) {
            throw InstallationIntegrityEnvelope.failure(
                    InstallationRecordFormatException.Reason.NON_CANONICAL_RECORD,
                    "evidence record is not canonical");
        }
        InstallationPhaseEvidencePoint point = Objects.requireNonNull(
                expectedPoint, "expectedPoint must not be null");
        if (!point.transactionId().equals(evidence.transactionId())
                || point.phase() != evidence.phase()
                || point.pendingRevision() != evidence.pendingRevision()) {
            throw InstallationIntegrityEnvelope.failure(
                    InstallationRecordFormatException.Reason.FOREIGN_RECORD,
                    "evidence record does not bind the expected point");
        }
        return evidence;
    }

    static void writeValue(
            InstallationIntegrityEnvelope.Writer writer,
            InstallationPhaseEvidence evidence)
            throws InstallationRecordFormatException {
        writer.writeInt(evidence.schemaVersion());
        writer.writeUuid(evidence.transactionId());
        writer.writeEnum(evidence.phase());
        writer.writeLong(evidence.pendingRevision());
        writer.writeString(evidence.semanticEvidenceSha256());
        writer.writeOptionalString(evidence.observedActivationIdentity());
    }

    static InstallationPhaseEvidence readValue(InstallationIntegrityEnvelope.Reader reader)
            throws InstallationRecordFormatException {
        int schema = reader.readInt();
        if (schema != InstallationPhaseEvidence.SCHEMA_VERSION) {
            throw InstallationIntegrityEnvelope.failure(
                    InstallationRecordFormatException.Reason.UNSUPPORTED_SCHEMA,
                    "evidence schema is unsupported");
        }
        try {
            return new InstallationPhaseEvidence(
                    schema,
                    reader.readUuid(),
                    reader.readEnum(InstallationPhase.class),
                    reader.readLong(),
                    reader.readString(),
                    reader.readOptionalString());
        } catch (IllegalArgumentException exception) {
            throw InstallationIntegrityEnvelope.corrupt(
                    "evidence fields violate the domain contract");
        }
    }
}
