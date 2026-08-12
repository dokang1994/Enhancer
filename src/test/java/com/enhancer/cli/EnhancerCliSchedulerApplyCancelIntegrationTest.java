package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.ControlPayload;
import com.enhancer.bus.ControlSignal;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.runtime.DurableAgentRuntime;
import com.enhancer.runtime.FileSystemAgentRuntimeStateStore;
import com.enhancer.runtime.InstalledCancellationTrustMetadata;
import com.enhancer.runtime.WorkItem;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnhancerCliSchedulerApplyCancelIntegrationTest {
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-00000000cb01";
    private static final String CONTROL_ID =
            "00000000-0000-0000-0000-00000000cb02";
    private static final String AUTHORIZATION_ID =
            "00000000-0000-0000-0000-00000000cb03";
    private static final String CAUSATION_ID =
            "00000000-0000-0000-0000-00000000cb04";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-00000000cb05";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-00000000cb06";
    private static final Instant ISSUED_AT = Instant.parse("2026-08-12T06:00:00Z");
    private static final Clock CLOCK = Clock.fixed(
            ISSUED_AT.plusSeconds(60), ZoneOffset.UTC);

    @TempDir
    Path temporaryRoot;

    @Test
    void appliesSignedCancellationAndDurableReplayRepairsEventsWithoutTrust()
            throws Exception {
        Layout layout = prepare();
        InstalledCancellationTrustMetadata metadata = new InstalledCancellationTrustMetadata(
                layout.policyFile(), sha256(Files.readAllBytes(layout.policyFile())));
        Execution applied = execute(new EnhancerCli(
                input -> null, () -> metadata, CLOCK), arguments(layout, false));

        assertEquals(0, applied.exitCode());
        assertTrue(applied.stdout().contains("status=CANCELLATION_APPLIED"));
        assertTrue(applied.stdout().contains("authorizationId=" + AUTHORIZATION_ID));
        assertFalse(applied.stdout().contains(metadata.expectedSha256()));
        assertTrue(Files.exists(layout.auditRoot()));
        assertFalse(Files.exists(layout.eventRoot()));
        long revision = new FileSystemAgentRuntimeStateStore(layout.runtimeRoot())
                .resolve(GOAL_ID).revision();

        Files.delete(layout.proofFile());
        Files.delete(layout.policyFile());
        Execution replay = execute(new EnhancerCli(
                input -> null,
                () -> { throw new java.io.IOException("must not load trust on replay"); },
                CLOCK), arguments(layout, true));

        assertEquals(0, replay.exitCode());
        assertTrue(replay.stdout().contains("status=CANCELLATION_APPLIED"));
        assertEquals(revision, new FileSystemAgentRuntimeStateStore(layout.runtimeRoot())
                .resolve(GOAL_ID).revision());
        try (Stream<Path> points = Files.list(layout.publicationRoot())) {
            assertEquals(1, points.count());
        }
    }

    @Test
    void mapsMalformedProofToPolicyDenialWithoutDurableMutation() throws Exception {
        Layout layout = prepare();
        Files.write(layout.proofFile(), new byte[] {1, 2, 3});
        InstalledCancellationTrustMetadata metadata = new InstalledCancellationTrustMetadata(
                layout.policyFile(), sha256(Files.readAllBytes(layout.policyFile())));
        long revision = new FileSystemAgentRuntimeStateStore(layout.runtimeRoot())
                .resolve(GOAL_ID).revision();

        Execution denied = execute(new EnhancerCli(
                input -> null, () -> metadata, CLOCK), arguments(layout, false));

        assertEquals(20, denied.exitCode());
        assertTrue(denied.stderr().contains("exitCode=20"));
        assertEquals(revision, new FileSystemAgentRuntimeStateStore(layout.runtimeRoot())
                .resolve(GOAL_ID).revision());
        assertFalse(Files.exists(layout.auditRoot()));
        assertFalse(Files.exists(layout.eventRoot()));
    }

    @Test
    void mapsUnavailableInstalledTrustToConfigurationFailure() throws Exception {
        Layout layout = prepare();
        long revision = new FileSystemAgentRuntimeStateStore(layout.runtimeRoot())
                .resolve(GOAL_ID).revision();

        Execution failed = execute(new EnhancerCli(
                input -> null,
                () -> { throw new java.io.IOException("installed trust missing"); },
                CLOCK), arguments(layout, false));

        assertEquals(2, failed.exitCode());
        assertTrue(failed.stderr().contains("exitCode=2"));
        assertEquals(revision, new FileSystemAgentRuntimeStateStore(layout.runtimeRoot())
                .resolve(GOAL_ID).revision());
        assertFalse(Files.exists(layout.auditRoot()));
    }

    private Layout prepare() throws Exception {
        Path runtimeRoot = absolute("runtime");
        Path auditRoot = absolute("audit");
        Path eventRoot = absolute("events");
        Path publicationRoot = absolute("points");
        Path proofFile = absolute("proof.bin");
        Path policyFile = absolute("policy.conf");
        KeyPair keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        MessageEnvelope control = controlRequest();
        readyRuntime(runtimeRoot, control);
        byte[] policy = canonicalPolicy(keyPair);
        Files.write(policyFile, policy);
        Files.write(proofFile, signedProof(keyPair, control));
        return new Layout(
                runtimeRoot, auditRoot, eventRoot, publicationRoot, proofFile, policyFile);
    }

    private static void readyRuntime(Path root, MessageEnvelope control) throws Exception {
        FileSystemAgentRuntimeStateStore store = new FileSystemAgentRuntimeStateStore(root);
        ApprovedTaskRevision revision = new ApprovedTaskRevision(
                "scheduler-apply-cancel-test",
                "CURRENT_TASK.md",
                "7b6c9d8e0f11223344556677889900aabbccddeeff00112233445566778899aa");
        MessageEnvelope work = new MessageEnvelope(
                CAUSATION_ID,
                control.correlationId(),
                Optional.empty(),
                control.logicalRunId(),
                "scheduler-apply-cancel-test",
                ISSUED_AT.minusSeconds(120),
                new WorkPayload(
                        revision,
                        "6a5b4c3d2e1f00112233445566778899aabbccddeeff00112233445566778899",
                        Set.of("read-file")));
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID,
                new WorkItem(WORK_ITEM_ID, "read-file-worker", work),
                store,
                Clock.fixed(ISSUED_AT.minusSeconds(120), ZoneOffset.UTC));
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);
        runtime.recordControlRequest(control);
    }

    private static MessageEnvelope controlRequest() {
        return new MessageEnvelope(
                CONTROL_ID,
                "cancel-correlation",
                Optional.of(CAUSATION_ID),
                "cancel-logical-run",
                "control-interface",
                ISSUED_AT,
                new ControlPayload(ControlSignal.CANCEL, "operator requested cancellation"));
    }

    private static byte[] signedProof(KeyPair keyPair, MessageEnvelope request)
            throws Exception {
        byte[] requestProjection = framedRequest(request);
        ByteArrayOutputStream claimsBytes = new ByteArrayOutputStream();
        try (DataOutputStream claims = new DataOutputStream(claimsBytes)) {
            frame(claims, "enhancer:detached-cancellation-grant");
            frame(claims, "grant-v1");
            frame(claims, "enhancer-local-control");
            frame(claims, GOAL_ID);
            frame(claims, CONTROL_ID);
            frame(claims, sha256(requestProjection));
            frame(claims, "CANCEL");
            frame(claims, AUTHORIZATION_ID);
            frame(claims, "operations");
            frame(claims, "primary-2026");
            frame(claims, "operator-17");
            frame(claims, "cancel-policy-v1");
            instantFrame(claims, ISSUED_AT);
            instantFrame(claims, ISSUED_AT.plusSeconds(600));
        }
        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(keyPair.getPrivate());
        signer.update(claimsBytes.toByteArray());
        byte[] signature = signer.sign();
        ByteArrayOutputStream proof = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(proof)) {
            output.writeInt(0x45434731);
            output.writeInt(claimsBytes.size());
            output.write(claimsBytes.toByteArray());
            output.writeInt(signature.length);
            output.write(signature);
        }
        return proof.toByteArray();
    }

    private static byte[] framedRequest(MessageEnvelope request) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            frame(output, "enhancer:cancellation-request:v1");
            frame(output, MessageEnvelope.ENVELOPE_VERSION);
            frame(output, request.messageId());
            frame(output, request.correlationId());
            ByteArrayOutputStream optional = new ByteArrayOutputStream();
            try (DataOutputStream value = new DataOutputStream(optional)) {
                value.writeBoolean(true);
                frame(value, request.causationId().orElseThrow());
            }
            byteFrame(output, optional.toByteArray());
            frame(output, request.logicalRunId());
            frame(output, request.producer());
            instantFrame(output, request.occurredAt());
            frame(output, "CONTROL");
            frame(output, "CANCEL");
            frame(output, ((ControlPayload) request.payload()).reason());
        }
        return bytes.toByteArray();
    }

    private static void frame(DataOutputStream output, String value) throws Exception {
        byteFrame(output, value.getBytes(StandardCharsets.UTF_8));
    }

    private static void instantFrame(DataOutputStream output, Instant value)
            throws Exception {
        byteFrame(output, ByteBuffer.allocate(12)
                .putLong(value.getEpochSecond()).putInt(value.getNano()).array());
    }

    private static void byteFrame(DataOutputStream output, byte[] value) throws Exception {
        output.writeInt(value.length);
        output.write(value);
    }

    private static byte[] canonicalPolicy(KeyPair keyPair) {
        byte[] publicKey = keyPair.getPublic().getEncoded();
        return String.join("\n",
                "enhancer-cancellation-grant-trust-policy-v1",
                "configurationId=local-installation",
                "audience=enhancer-local-control",
                "policyRevision=cancel-policy-v1",
                "maximumGrantLifetimeSeconds=900",
                "clockSkewSeconds=30",
                "trustedKeyCount=1",
                "trustedKey.0.issuerId=operations",
                "trustedKey.0.keyId=primary-2026",
                "trustedKey.0.subjectCount=1",
                "trustedKey.0.subject.0=operator-17",
                "trustedKey.0.publicKeySubjectPublicKeyInfo="
                        + Base64.getEncoder().encodeToString(publicKey),
                "trustedKey.0.publicKeySha256=" + sha256(publicKey),
                "trustedKey.0.validFrom=2026-08-12T05:00:00Z",
                "trustedKey.0.validUntil=2027-08-12T05:00:00Z",
                "trustedKey.0.revokedAt=-",
                "").getBytes(StandardCharsets.UTF_8);
    }

    private Execution execute(EnhancerCli cli, String[] arguments) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = cli.execute(
                arguments,
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new Execution(
                exitCode,
                stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8));
    }

    private String[] arguments(Layout layout, boolean events) {
        java.util.List<String> values = new java.util.ArrayList<>(java.util.List.of(
                "scheduler-apply-cancel",
                "--runtime-root", layout.runtimeRoot().toString(),
                "--goal-id", GOAL_ID,
                "--control-message-id", CONTROL_ID,
                "--proof-file", layout.proofFile().toString(),
                "--authorization-audit-root", layout.auditRoot().toString()));
        if (events) {
            values.addAll(java.util.List.of(
                    "--runtime-event-root", layout.eventRoot().toString(),
                    "--runtime-event-publication-root", layout.publicationRoot().toString(),
                    "--max-pending-runtime-event-publications", "8"));
        }
        return values.toArray(String[]::new);
    }

    private Path absolute(String name) {
        return temporaryRoot.resolve(name).toAbsolutePath().normalize();
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private record Layout(
            Path runtimeRoot,
            Path auditRoot,
            Path eventRoot,
            Path publicationRoot,
            Path proofFile,
            Path policyFile) {
    }

    private record Execution(int exitCode, String stdout, String stderr) {
    }
}
