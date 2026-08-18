package com.enhancer.maintenance.installation;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Deterministic bounded byte format for the complete installation transaction cursor. */
final class InstallationTransactionFileFormat {
    private static final int MAGIC = 0x49545331;
    private static final String PAYLOAD_KIND = "installation-transaction-state";
    static final int MAX_BODY_BYTES = 256 * 1024;
    private static final int MAX_EVIDENCE_COUNT = 11;

    private InstallationTransactionFileFormat() {}

    static byte[] encode(InstallationTransactionState state)
            throws InstallationRecordFormatException {
        InstallationTransactionState checked = Objects.requireNonNull(
                state, "state must not be null");
        InstallationIntegrityEnvelope.Writer writer =
                new InstallationIntegrityEnvelope.Writer();
        writer.writeString(PAYLOAD_KIND);
        writer.writeInt(checked.schemaVersion());
        writePlan(writer, checked.plan());
        writeEnvironment(writer, checked.environment());
        writer.writeString(checked.sourceReleaseVersion());
        writer.writeString(checked.permissionPolicySha256());
        writer.writeOptionalString(checked.expectedCurrentActivationIdentity());
        writer.writeString(checked.requestedActivationIdentity());
        writer.writeInt(checked.succeededPhaseEvidencePrefix().size());
        for (InstallationPhaseEvidence evidence : checked.succeededPhaseEvidencePrefix()) {
            InstallationPhaseEvidenceFileFormat.writeValue(writer, evidence);
        }
        writer.writeLong(checked.revision());
        writer.writeEnum(checked.phase());
        writer.writeEnum(checked.stepStatus());
        return InstallationIntegrityEnvelope.encode(MAGIC, MAX_BODY_BYTES, writer.toByteArray());
    }

    static InstallationTransactionState decode(byte[] envelope)
            throws InstallationRecordFormatException {
        byte[] body = InstallationIntegrityEnvelope.decode(MAGIC, MAX_BODY_BYTES, envelope);
        InstallationIntegrityEnvelope.Reader reader =
                new InstallationIntegrityEnvelope.Reader(body);
        if (!PAYLOAD_KIND.equals(reader.readString())) {
            throw InstallationIntegrityEnvelope.corrupt("transaction payload kind is invalid");
        }
        int schema = reader.readInt();
        if (schema != InstallationTransactionState.SCHEMA_VERSION) {
            throw InstallationIntegrityEnvelope.failure(
                    InstallationRecordFormatException.Reason.UNSUPPORTED_SCHEMA,
                    "transaction schema is unsupported");
        }
        try {
            CancellationTrustInstallationPlan plan = readPlan(reader);
            InstallationEnvironmentEvidence environment = readEnvironment(reader);
            String sourceReleaseVersion = reader.readString();
            String permissionPolicySha256 = reader.readString();
            Optional<String> expectedActivation = reader.readOptionalString();
            String requestedActivation = reader.readString();
            int evidenceCount = reader.readInt();
            if (evidenceCount < 0 || evidenceCount > MAX_EVIDENCE_COUNT) {
                throw InstallationIntegrityEnvelope.corrupt(
                        "transaction evidence count is invalid");
            }
            List<InstallationPhaseEvidence> evidence = new ArrayList<>(evidenceCount);
            for (int index = 0; index < evidenceCount; index++) {
                evidence.add(InstallationPhaseEvidenceFileFormat.readValue(reader));
            }
            long revision = reader.readLong();
            InstallationPhase phase = reader.readEnum(InstallationPhase.class);
            InstallationTransactionState.StepStatus status = reader.readEnum(
                    InstallationTransactionState.StepStatus.class);
            reader.requireFinished();
            InstallationTransactionState decoded = new InstallationTransactionState(
                    schema,
                    plan,
                    environment,
                    sourceReleaseVersion,
                    permissionPolicySha256,
                    expectedActivation,
                    requestedActivation,
                    evidence,
                    revision,
                    phase,
                    status);
            if (!Arrays.equals(envelope, encode(decoded))) {
                throw InstallationIntegrityEnvelope.failure(
                        InstallationRecordFormatException.Reason.NON_CANONICAL_RECORD,
                        "transaction record is not canonical");
            }
            return decoded;
        } catch (InstallationRecordFormatException exception) {
            throw exception;
        } catch (IllegalArgumentException exception) {
            throw InstallationIntegrityEnvelope.corrupt(
                    "transaction fields violate the domain contract");
        }
    }

    private static void writePlan(
            InstallationIntegrityEnvelope.Writer writer,
            CancellationTrustInstallationPlan plan)
            throws InstallationRecordFormatException {
        writer.writeUuid(plan.transactionId());
        writer.writeEnum(plan.operation());
        writePrincipalSet(writer, plan.principals());
        writer.writeString(FileSystems.getDefault().provider().getScheme());
        writer.writeString(pathDialect());
        for (Path path : List.of(
                plan.installationRoot(),
                plan.applicationJar(),
                plan.runtimeDistributionRoot(),
                plan.operatorDistributionRoot(),
                plan.operatorCandidateInbox(),
                plan.activationPoint(),
                plan.auditRoot())) {
            writer.writeString(path.toString());
        }
        writer.writeString(plan.sourceManifestSha256());
        writer.writeString(plan.applicationJarSha256());
        writer.writeString(plan.runtimeDistributionSha256());
        writer.writeString(plan.operatorDistributionSha256());
        writer.writeString(plan.permissionPolicyRevision());
        writer.writeString(plan.policySha256());
        writer.writeString(plan.requestedMetadataSha256());
        writer.writeOptionalString(plan.expectedCurrentMetadataSha256());
    }

    private static CancellationTrustInstallationPlan readPlan(
            InstallationIntegrityEnvelope.Reader reader)
            throws InstallationRecordFormatException {
        var transactionId = reader.readUuid();
        InstallationOperation operation = reader.readEnum(InstallationOperation.class);
        InstallationPrincipalSet principals = readPrincipalSet(reader);
        String provider = reader.readString();
        String dialect = reader.readString();
        if (!FileSystems.getDefault().provider().getScheme().equals(provider)
                || !pathDialect().equals(dialect)) {
            throw InstallationIntegrityEnvelope.failure(
                    InstallationRecordFormatException.Reason.FOREIGN_RECORD,
                    "transaction paths use a foreign filesystem dialect");
        }
        Path installationRoot = Path.of(reader.readString());
        Path applicationJar = Path.of(reader.readString());
        Path runtimeRoot = Path.of(reader.readString());
        Path operatorRoot = Path.of(reader.readString());
        Path operatorInbox = Path.of(reader.readString());
        Path activationPoint = Path.of(reader.readString());
        Path auditRoot = Path.of(reader.readString());
        return new CancellationTrustInstallationPlan(
                transactionId,
                operation,
                principals,
                installationRoot,
                applicationJar,
                runtimeRoot,
                operatorRoot,
                operatorInbox,
                activationPoint,
                auditRoot,
                reader.readString(),
                reader.readString(),
                reader.readString(),
                reader.readString(),
                reader.readString(),
                reader.readString(),
                reader.readString(),
                reader.readOptionalString());
    }

    private static void writeEnvironment(
            InstallationIntegrityEnvelope.Writer writer,
            InstallationEnvironmentEvidence environment)
            throws InstallationRecordFormatException {
        writer.writeUuid(environment.transactionId());
        writer.writeString(environment.adapterId());
        writer.writeString(environment.adapterVersion());
        writePrincipalSet(writer, environment.resolvedPrincipals());
        writer.writeString(environment.filesystemIdentity());
        writer.writeBoolean(environment.sameFilesystem());
        writer.writeBoolean(environment.linksAbsent());
    }

    private static InstallationEnvironmentEvidence readEnvironment(
            InstallationIntegrityEnvelope.Reader reader)
            throws InstallationRecordFormatException {
        return new InstallationEnvironmentEvidence(
                reader.readUuid(),
                reader.readString(),
                reader.readString(),
                readPrincipalSet(reader),
                reader.readString(),
                reader.readBoolean(),
                reader.readBoolean());
    }

    private static void writePrincipalSet(
            InstallationIntegrityEnvelope.Writer writer,
            InstallationPrincipalSet principals)
            throws InstallationRecordFormatException {
        for (InstallationPrincipal principal : List.of(
                principals.installerPublisher(),
                principals.operator(),
                principals.runtime())) {
            writer.writeEnum(principal.role());
            writer.writeString(principal.stableOperatingSystemIdentity());
        }
    }

    private static InstallationPrincipalSet readPrincipalSet(
            InstallationIntegrityEnvelope.Reader reader)
            throws InstallationRecordFormatException {
        return new InstallationPrincipalSet(
                readPrincipal(reader),
                readPrincipal(reader),
                readPrincipal(reader));
    }

    private static InstallationPrincipal readPrincipal(
            InstallationIntegrityEnvelope.Reader reader)
            throws InstallationRecordFormatException {
        return new InstallationPrincipal(
                reader.readEnum(InstallationPrincipalRole.class),
                reader.readString());
    }

    private static String pathDialect() {
        return File.separatorChar == '\\' ? "windows" : "posix";
    }
}
