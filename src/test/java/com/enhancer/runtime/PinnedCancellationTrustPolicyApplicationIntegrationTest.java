package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enhancer.bus.ControlPayload;
import com.enhancer.bus.ControlSignal;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PinnedCancellationTrustPolicyApplicationIntegrationTest {
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-00000000c101";
    private static final String CONTROL_MESSAGE_ID =
            "00000000-0000-0000-0000-00000000c102";
    private static final String AUTHORIZATION_ID =
            "00000000-0000-0000-0000-00000000c103";
    private static final String CAUSATION_ID =
            "00000000-0000-0000-0000-00000000c104";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-00000000c105";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-00000000c106";
    private static final Instant ISSUED_AT =
            Instant.parse("2026-08-11T10:00:00Z");
    private static final Instant EXPIRES_AT =
            Instant.parse("2026-08-11T10:10:00Z");

    @TempDir
    Path temporaryRoot;

    @Test
    void loadedPinnedSnapshotAuthorizesAndAuditsItsExactFileRevision()
            throws Exception {
        KeyPair keyPair = ed25519();
        MessageEnvelope request = request();
        DetachedSignedCancellationGrant grant = grant(keyPair, request);
        byte[] policyBytes = canonicalPolicy(keyPair, "-");
        Path policyFile = writePolicy(policyBytes);
        String pin = sha256(policyBytes);
        CancellationGrantTrustPolicy policy =
                new PinnedFileCancellationGrantTrustPolicyLoader(policyFile, pin)
                        .load();
        Path auditRoot = temporaryRoot.resolve("successful-audit");

        ControlAuthorizationDecision.Approved approved = assertInstanceOf(
                ControlAuthorizationDecision.Approved.class,
                new AuditBackedSignedCancellationAuthorizer(
                        grant.encoded(),
                        policy,
                        Clock.fixed(ISSUED_AT.plusSeconds(60), ZoneOffset.UTC),
                        new FileSystemCancellationAuthorizationAuditStore(auditRoot))
                        .authorize(GOAL_ID, request));

        assertEquals(AUTHORIZATION_ID, approved.authorizationId());
        CancellationAuthorizationAuditRecord audit =
                new FileSystemCancellationAuthorizationAuditStore(auditRoot)
                        .find(AUTHORIZATION_ID)
                        .orElseThrow();
        assertEquals(pin, audit.trustConfigurationRevision());
        assertEquals("local-installation", audit.trustConfigurationId());
    }

    @Test
    void authorizationIdentityCannotReplayAcrossPinnedConfigurationRevision()
            throws Exception {
        KeyPair keyPair = ed25519();
        MessageEnvelope request = request();
        DetachedSignedCancellationGrant grant = grant(keyPair, request);
        byte[] original = canonicalPolicy(keyPair, "-");
        Path policyFile = writePolicy(original);
        Path auditRoot = temporaryRoot.resolve("rotated-audit");
        FileSystemCancellationAuthorizationAuditStore auditStore =
                new FileSystemCancellationAuthorizationAuditStore(auditRoot);
        Clock clock = Clock.fixed(ISSUED_AT.plusSeconds(60), ZoneOffset.UTC);

        new AuditBackedSignedCancellationAuthorizer(
                grant.encoded(),
                new PinnedFileCancellationGrantTrustPolicyLoader(
                        policyFile, sha256(original)).load(),
                clock,
                auditStore)
                .authorize(GOAL_ID, request);
        Path auditFile = auditRoot.resolve(
                AUTHORIZATION_ID + ".cancellation-authorization");
        byte[] firstAudit = Files.readAllBytes(auditFile);
        byte[] rotated = new String(original, StandardCharsets.UTF_8)
                .replace("maximumGrantLifetimeSeconds=900",
                        "maximumGrantLifetimeSeconds=901")
                .getBytes(StandardCharsets.UTF_8);
        Files.write(policyFile, rotated);

        assertThrows(java.io.IOException.class, () ->
                new AuditBackedSignedCancellationAuthorizer(
                        grant.encoded(),
                        new PinnedFileCancellationGrantTrustPolicyLoader(
                                policyFile, sha256(rotated)).load(),
                        clock,
                        auditStore)
                        .authorize(GOAL_ID, request));
        assertArrayEquals(firstAudit, Files.readAllBytes(auditFile));
    }

    @Test
    void changedOrCurrentlyRevokedSnapshotsCannotMutateAuditRuntimeOrEvents()
            throws Exception {
        KeyPair keyPair = ed25519();
        MessageEnvelope request = request();
        DetachedSignedCancellationGrant grant = grant(keyPair, request);
        byte[] original = canonicalPolicy(keyPair, "-");
        byte[] revoked = canonicalPolicy(keyPair, "2026-08-11T10:00:30Z");
        Path policyFile = writePolicy(original);
        String originalPin = sha256(original);

        Path runtimeRoot = temporaryRoot.resolve("runtime");
        FileSystemAgentRuntimeStateStore runtimeStore =
                readyRuntime(runtimeRoot, request);
        long sourceRevision = runtimeStore.resolve(GOAL_ID).revision();
        Path auditRoot = temporaryRoot.resolve("denied-audit");
        Path eventRoot = temporaryRoot.resolve("denied-events");

        Files.write(policyFile, revoked);
        assertThrows(java.io.IOException.class, () ->
                new PinnedFileCancellationGrantTrustPolicyLoader(
                        policyFile, originalPin).load());
        assertUnchanged(runtimeStore, sourceRevision, auditRoot, eventRoot);

        CancellationGrantTrustPolicy revokedPolicy =
                new PinnedFileCancellationGrantTrustPolicyLoader(
                        policyFile, sha256(revoked)).load();
        AuthenticatedCancellationApplication application =
                new AuthenticatedCancellationApplication(
                        runtimeStore,
                        Clock.fixed(ISSUED_AT.plusSeconds(60), ZoneOffset.UTC),
                        new AuditBackedSignedCancellationAuthorizer(
                                grant.encoded(),
                                revokedPolicy,
                                Clock.fixed(
                                        ISSUED_AT.plusSeconds(60),
                                        ZoneOffset.UTC),
                                new FileSystemCancellationAuthorizationAuditStore(
                                        auditRoot)),
                        new RuntimeEventRecorder(
                                new FileSystemRuntimeEventStore(eventRoot),
                                ignored -> {
                                    throw new AssertionError(
                                            "denial must precede event publication");
                                }));

        ControlAuthorizationDeniedException denied = assertThrows(
                ControlAuthorizationDeniedException.class,
                () -> application.apply(GOAL_ID, CONTROL_MESSAGE_ID));

        assertEquals(
                "control authorization was denied: "
                        + SignedCancellationGrantDenial.KEY_REVOKED.name(),
                denied.getMessage());
        assertUnchanged(runtimeStore, sourceRevision, auditRoot, eventRoot);
    }

    private static void assertUnchanged(
            FileSystemAgentRuntimeStateStore runtimeStore,
            long sourceRevision,
            Path auditRoot,
            Path eventRoot) throws Exception {
        AgentRuntimeState state = runtimeStore.resolve(GOAL_ID);
        assertEquals(sourceRevision, state.revision());
        assertFalse(state.cancellationApplication().isPresent());
        assertFalse(Files.exists(auditRoot));
        assertFalse(Files.exists(eventRoot));
    }

    private Path writePolicy(byte[] content) throws Exception {
        Path path = temporaryRoot.resolve("cancellation-trust-policy.conf")
                .toAbsolutePath()
                .normalize();
        Files.write(path, content);
        return path;
    }

    private static FileSystemAgentRuntimeStateStore readyRuntime(
            Path root, MessageEnvelope controlRequest) throws Exception {
        FileSystemAgentRuntimeStateStore store =
                new FileSystemAgentRuntimeStateStore(root);
        ApprovedTaskRevision revision = new ApprovedTaskRevision(
                "pinned-cancellation-policy-integration",
                "CURRENT_TASK.md",
                "7b6c9d8e0f11223344556677889900aabbccddeeff00112233445566778899aa");
        MessageEnvelope workMessage = new MessageEnvelope(
                CAUSATION_ID,
                controlRequest.correlationId(),
                Optional.empty(),
                controlRequest.logicalRunId(),
                "pinned-policy-test",
                ISSUED_AT.minusSeconds(120),
                new WorkPayload(
                        revision,
                        "6a5b4c3d2e1f00112233445566778899aabbccddeeff00112233445566778899",
                        Set.of("read-file")));
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID,
                new WorkItem(WORK_ITEM_ID, "read-file-worker", workMessage),
                store,
                Clock.fixed(ISSUED_AT.minusSeconds(120), ZoneOffset.UTC));
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);
        runtime.recordControlRequest(controlRequest);
        return store;
    }

    private static MessageEnvelope request() {
        return new MessageEnvelope(
                CONTROL_MESSAGE_ID,
                "correlation-pinned-cancel",
                Optional.of(CAUSATION_ID),
                "logical-run-pinned-cancel",
                "control-interface",
                ISSUED_AT,
                new ControlPayload(
                        ControlSignal.CANCEL,
                        "operator requested cancellation"));
    }

    private static DetachedSignedCancellationGrant grant(
            KeyPair keyPair, MessageEnvelope request) throws Exception {
        DetachedSignedCancellationGrant.Claims claims =
                new DetachedSignedCancellationGrant.Claims(
                        "enhancer-local-control",
                        GOAL_ID,
                        request.messageId(),
                        DetachedSignedCancellationGrant.requestSha256(request),
                        AUTHORIZATION_ID,
                        "operations",
                        "primary-2026",
                        "operator-17",
                        "cancel-policy-v1",
                        ISSUED_AT,
                        EXPIRES_AT);
        return DetachedSignedCancellationGrant.create(
                claims, sign(keyPair.getPrivate(), claims.signingBytes()));
    }

    private static byte[] canonicalPolicy(KeyPair keyPair, String revokedAt) {
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
                "trustedKey.0.validFrom=2026-08-11T09:00:00Z",
                "trustedKey.0.validUntil=2027-08-11T09:00:00Z",
                "trustedKey.0.revokedAt=" + revokedAt,
                "").getBytes(StandardCharsets.UTF_8);
    }

    private static KeyPair ed25519() throws Exception {
        return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
    }

    private static byte[] sign(PrivateKey privateKey, byte[] content)
            throws Exception {
        Signature signature = Signature.getInstance("Ed25519");
        signature.initSign(privateKey);
        signature.update(content);
        return signature.sign();
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
