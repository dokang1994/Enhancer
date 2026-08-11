package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.ControlPayload;
import com.enhancer.bus.ControlSignal;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuditBackedSignedCancellationAuthorizerTest {
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-00000000a101";
    private static final String OTHER_GOAL_ID =
            "00000000-0000-0000-0000-00000000a102";
    private static final String CONTROL_MESSAGE_ID =
            "00000000-0000-0000-0000-00000000a103";
    private static final String AUTHORIZATION_ID =
            "00000000-0000-0000-0000-00000000a104";
    private static final String CAUSATION_ID =
            "00000000-0000-0000-0000-00000000a105";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-00000000a106";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-00000000a107";
    private static final String AUDIENCE = "enhancer-local-control";
    private static final String ISSUER_ID = "operations";
    private static final String KEY_ID = "primary-2026";
    private static final String SUBJECT_ID = "operator-17";
    private static final String POLICY_REVISION = "cancel-policy-v1";
    private static final Instant ISSUED_AT = Instant.parse("2026-08-11T10:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-08-11T10:10:00Z");

    @TempDir
    Path temporaryRoot;

    @Test
    void canonicalGrantRoundTripsAndEveryRetainedRequestFactChangesItsDigest()
            throws Exception {
        KeyPair keyPair = ed25519();
        MessageEnvelope request = request("operator requested cancellation", ISSUED_AT);
        DetachedSignedCancellationGrant grant = grant(
                keyPair,
                GOAL_ID,
                request,
                AUTHORIZATION_ID,
                POLICY_REVISION,
                ISSUED_AT,
                EXPIRES_AT);

        DetachedSignedCancellationGrant decoded =
                DetachedSignedCancellationGrant.parse(grant.encoded());

        assertEquals(grant.claims(), decoded.claims());
        assertArrayEquals(grant.signature(), decoded.signature());
        assertArrayEquals(grant.signingBytes(), decoded.signingBytes());
        assertEquals(sha256(grant.encoded()), decoded.proofSha256());

        String original = DetachedSignedCancellationGrant.requestSha256(request);
        assertNotEquals(
                original,
                DetachedSignedCancellationGrant.requestSha256(new MessageEnvelope(
                        request.messageId(),
                        "different-correlation",
                        request.causationId(),
                        request.logicalRunId(),
                        request.producer(),
                        request.occurredAt(),
                        request.payload())));
        assertNotEquals(
                original,
                DetachedSignedCancellationGrant.requestSha256(new MessageEnvelope(
                        request.messageId(),
                        request.correlationId(),
                        Optional.empty(),
                        request.logicalRunId(),
                        request.producer(),
                        request.occurredAt(),
                        request.payload())));
        assertNotEquals(
                original,
                DetachedSignedCancellationGrant.requestSha256(new MessageEnvelope(
                        request.messageId(),
                        request.correlationId(),
                        request.causationId(),
                        request.logicalRunId(),
                        "different-producer",
                        request.occurredAt(),
                        request.payload())));
        assertNotEquals(
                original,
                DetachedSignedCancellationGrant.requestSha256(new MessageEnvelope(
                        request.messageId(),
                        request.correlationId(),
                        request.causationId(),
                        request.logicalRunId(),
                        request.producer(),
                        request.occurredAt().plusNanos(1),
                        new ControlPayload(
                                ControlSignal.CANCEL,
                                "operator requested cancellation"))));
        assertNotEquals(
                original,
                DetachedSignedCancellationGrant.requestSha256(new MessageEnvelope(
                        request.messageId(),
                        request.correlationId(),
                        request.causationId(),
                        request.logicalRunId(),
                        request.producer(),
                        request.occurredAt(),
                        new ControlPayload(ControlSignal.CANCEL, "different reason"))));
        assertThrows(
                IllegalArgumentException.class,
                () -> DetachedSignedCancellationGrant.requestSha256(new MessageEnvelope(
                        request.messageId(),
                        request.correlationId(),
                        request.causationId(),
                        request.logicalRunId(),
                        request.producer(),
                        request.occurredAt(),
                        new ControlPayload(ControlSignal.PAUSE, "pause"))));
    }

    @Test
    void parserRejectsMalformedNonCanonicalAndUnboundedProofs() throws Exception {
        KeyPair keyPair = ed25519();
        DetachedSignedCancellationGrant grant = grant(
                keyPair,
                GOAL_ID,
                request("operator requested cancellation", ISSUED_AT),
                AUTHORIZATION_ID,
                POLICY_REVISION,
                ISSUED_AT,
                EXPIRES_AT);
        byte[] encoded = grant.encoded();

        byte[] badMagic = encoded.clone();
        badMagic[0] ^= 1;
        assertThrows(IOException.class, () ->
                DetachedSignedCancellationGrant.parse(badMagic));
        assertThrows(IOException.class, () ->
                DetachedSignedCancellationGrant.parse(
                        Arrays.copyOf(encoded, encoded.length - 1)));
        assertThrows(IOException.class, () ->
                DetachedSignedCancellationGrant.parse(
                        Arrays.copyOf(encoded, encoded.length + 1)));
        assertThrows(IOException.class, () ->
                DetachedSignedCancellationGrant.parse(
                        new byte[DetachedSignedCancellationGrant.MAX_PROOF_BYTES + 1]));
        assertThrows(IllegalArgumentException.class, () ->
                DetachedSignedCancellationGrant.create(grant.claims(), new byte[63]));
        assertThrows(IllegalArgumentException.class, () ->
                new DetachedSignedCancellationGrant.Claims(
                        AUDIENCE,
                        GOAL_ID.toUpperCase(),
                        CONTROL_MESSAGE_ID,
                        "a".repeat(64),
                        AUTHORIZATION_ID,
                        ISSUER_ID,
                        KEY_ID,
                        SUBJECT_ID,
                        POLICY_REVISION,
                        ISSUED_AT,
                        EXPIRES_AT));
    }

    @Test
    void verifierAcceptsOnlyExactCurrentTrustedEd25519Grant() throws Exception {
        KeyPair keyPair = ed25519();
        MessageEnvelope request = request("operator requested cancellation", ISSUED_AT);
        DetachedSignedCancellationGrant grant = grant(
                keyPair,
                GOAL_ID,
                request,
                AUTHORIZATION_ID,
                POLICY_REVISION,
                ISSUED_AT,
                EXPIRES_AT);
        CancellationGrantTrustPolicy policy = policy(
                keyPair,
                Optional.empty(),
                POLICY_REVISION);
        SignedCancellationGrantVerifier verifier = new SignedCancellationGrantVerifier(
                policy,
                Clock.fixed(ISSUED_AT.plusSeconds(60), ZoneOffset.UTC));

        SignedCancellationGrantVerifier.Verified verified = assertInstanceOf(
                SignedCancellationGrantVerifier.Verified.class,
                verifier.verify(GOAL_ID, request, grant));

        assertEquals(AUTHORIZATION_ID, verified.authorization().authorizationId());
        assertEquals(
                SignedCancellationGrantVerifier.actorId(ISSUER_ID, SUBJECT_ID),
                verified.authorization().actorId());
        assertEquals(sha256(keyPair.getPublic().getEncoded()),
                verified.authorization().publicKeySha256());
        assertEquals("Ed25519", verified.authorization().signatureAlgorithm());

        byte[] badSignature = grant.signature();
        badSignature[0] ^= 1;
        assertDenied(
                SignedCancellationGrantDenial.INVALID_SIGNATURE,
                verifier.verify(
                        GOAL_ID,
                        request,
                        DetachedSignedCancellationGrant.create(grant.claims(), badSignature)));
        assertDenied(
                SignedCancellationGrantDenial.TARGET_MISMATCH,
                verifier.verify(OTHER_GOAL_ID, request, grant));
        assertDenied(
                SignedCancellationGrantDenial.REQUEST_MISMATCH,
                verifier.verify(
                        GOAL_ID,
                        request("changed reason", ISSUED_AT),
                        grant));

        SignedCancellationGrantVerifier expired = new SignedCancellationGrantVerifier(
                policy,
                Clock.fixed(EXPIRES_AT, ZoneOffset.UTC));
        assertDenied(
                SignedCancellationGrantDenial.EXPIRED,
                expired.verify(GOAL_ID, request, grant));

        CancellationGrantTrustPolicy revokedPolicy = policy(
                keyPair,
                Optional.of(ISSUED_AT.plusSeconds(30)),
                POLICY_REVISION);
        SignedCancellationGrantVerifier revoked = new SignedCancellationGrantVerifier(
                revokedPolicy,
                Clock.fixed(ISSUED_AT.plusSeconds(30), ZoneOffset.UTC));
        assertDenied(
                SignedCancellationGrantDenial.KEY_REVOKED,
                revoked.verify(GOAL_ID, request, grant));
    }

    @Test
    void verifierRejectsPolicyTimeLifetimeSubjectAndKeyValidityMismatches()
            throws Exception {
        KeyPair keyPair = ed25519();
        MessageEnvelope request = request("operator requested cancellation", ISSUED_AT);
        CancellationGrantTrustPolicy policy = policy(
                keyPair,
                Optional.empty(),
                POLICY_REVISION);

        DetachedSignedCancellationGrant wrongPolicy = grant(
                keyPair,
                GOAL_ID,
                request,
                AUTHORIZATION_ID,
                "other-policy",
                ISSUED_AT,
                EXPIRES_AT);
        assertDenied(
                SignedCancellationGrantDenial.POLICY_MISMATCH,
                new SignedCancellationGrantVerifier(
                        policy,
                        Clock.fixed(ISSUED_AT.plusSeconds(60), ZoneOffset.UTC))
                        .verify(GOAL_ID, request, wrongPolicy));

        DetachedSignedCancellationGrant future = grant(
                keyPair,
                GOAL_ID,
                request,
                AUTHORIZATION_ID,
                POLICY_REVISION,
                ISSUED_AT.plusSeconds(120),
                EXPIRES_AT);
        assertDenied(
                SignedCancellationGrantDenial.NOT_YET_VALID,
                new SignedCancellationGrantVerifier(
                        policy,
                        Clock.fixed(ISSUED_AT, ZoneOffset.UTC))
                        .verify(GOAL_ID, request, future));

        DetachedSignedCancellationGrant tooLong = grant(
                keyPair,
                GOAL_ID,
                request,
                AUTHORIZATION_ID,
                POLICY_REVISION,
                ISSUED_AT,
                ISSUED_AT.plus(Duration.ofMinutes(16)));
        assertDenied(
                SignedCancellationGrantDenial.LIFETIME_EXCEEDED,
                new SignedCancellationGrantVerifier(
                        policy,
                        Clock.fixed(ISSUED_AT.plusSeconds(60), ZoneOffset.UTC))
                        .verify(GOAL_ID, request, tooLong));

        byte[] publicKey = keyPair.getPublic().getEncoded();
        CancellationGrantTrustPolicy wrongSubjectPolicy =
                new CancellationGrantTrustPolicy(
                        "local-installation",
                        "configuration-v1",
                        AUDIENCE,
                        POLICY_REVISION,
                        Duration.ofMinutes(15),
                        Duration.ofSeconds(30),
                        List.of(new CancellationGrantTrustPolicy.TrustedKey(
                                ISSUER_ID,
                                KEY_ID,
                                Set.of("different-operator"),
                                publicKey,
                                sha256(publicKey),
                                ISSUED_AT.minusSeconds(60),
                                EXPIRES_AT.plusSeconds(60),
                                Optional.empty())));
        DetachedSignedCancellationGrant grant = grant(
                keyPair,
                GOAL_ID,
                request,
                AUTHORIZATION_ID,
                POLICY_REVISION,
                ISSUED_AT,
                EXPIRES_AT);
        assertDenied(
                SignedCancellationGrantDenial.SUBJECT_NOT_AUTHORIZED,
                new SignedCancellationGrantVerifier(
                        wrongSubjectPolicy,
                        Clock.fixed(ISSUED_AT.plusSeconds(60), ZoneOffset.UTC))
                        .verify(GOAL_ID, request, grant));

        CancellationGrantTrustPolicy expiredKeyPolicy =
                new CancellationGrantTrustPolicy(
                        "local-installation",
                        "configuration-v1",
                        AUDIENCE,
                        POLICY_REVISION,
                        Duration.ofMinutes(15),
                        Duration.ofSeconds(30),
                        List.of(new CancellationGrantTrustPolicy.TrustedKey(
                                ISSUER_ID,
                                KEY_ID,
                                Set.of(SUBJECT_ID),
                                publicKey,
                                sha256(publicKey),
                                ISSUED_AT.minusSeconds(60),
                                ISSUED_AT.plusSeconds(30),
                                Optional.empty())));
        assertDenied(
                SignedCancellationGrantDenial.KEY_NOT_VALID,
                new SignedCancellationGrantVerifier(
                        expiredKeyPolicy,
                        Clock.fixed(ISSUED_AT.plusSeconds(60), ZoneOffset.UTC))
                        .verify(GOAL_ID, request, grant));
    }

    @Test
    void policyRejectsNonEd25519KeysAndUnsafeTimeBounds() throws Exception {
        KeyPair rsa = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        assertThrows(IllegalArgumentException.class, () ->
                key(rsa, Optional.empty()));

        KeyPair keyPair = ed25519();
        assertThrows(IllegalArgumentException.class, () ->
                new CancellationGrantTrustPolicy(
                        "local-installation",
                        "configuration-v1",
                        AUDIENCE,
                        POLICY_REVISION,
                        Duration.ofHours(25),
                        Duration.ZERO,
                        List.of(key(keyPair, Optional.empty()))));
        assertThrows(IllegalArgumentException.class, () ->
                new CancellationGrantTrustPolicy(
                        "local-installation",
                        "configuration-v1",
                        AUDIENCE,
                        POLICY_REVISION,
                        Duration.ofMinutes(10),
                        Duration.ofMinutes(6),
                        List.of(key(keyPair, Optional.empty()))));
    }

    @Test
    void authorizerPersistsBeforeApprovalAndExactRetryRevalidatesCurrentState()
            throws Exception {
        KeyPair keyPair = ed25519();
        MessageEnvelope request = request("operator requested cancellation", ISSUED_AT);
        DetachedSignedCancellationGrant grant = grant(
                keyPair,
                GOAL_ID,
                request,
                AUTHORIZATION_ID,
                POLICY_REVISION,
                ISSUED_AT,
                EXPIRES_AT);
        CancellationGrantTrustPolicy policy = policy(
                keyPair,
                Optional.empty(),
                POLICY_REVISION);
        Path auditRoot = temporaryRoot.resolve("authorization-audit");
        FileSystemCancellationAuthorizationAuditStore store =
                new FileSystemCancellationAuthorizationAuditStore(auditRoot);
        AuditBackedSignedCancellationAuthorizer authorizer =
                new AuditBackedSignedCancellationAuthorizer(
                        grant.encoded(),
                        policy,
                        Clock.fixed(ISSUED_AT.plusSeconds(60), ZoneOffset.UTC),
                        store);

        ControlAuthorizationDecision.Approved approved = assertInstanceOf(
                ControlAuthorizationDecision.Approved.class,
                authorizer.authorize(GOAL_ID, request));
        CancellationAuthorizationAuditRecord first =
                store.find(AUTHORIZATION_ID).orElseThrow();
        Path artifact = auditRoot.resolve(
                AUTHORIZATION_ID + ".cancellation-authorization");
        byte[] firstBytes = Files.readAllBytes(artifact);

        assertEquals(first.authorizationId(), approved.authorizationId());
        assertEquals(first.actorId(), approved.actorId());
        assertEquals(first.issuedAt(), approved.authorizedAt());

        AuditBackedSignedCancellationAuthorizer retry =
                new AuditBackedSignedCancellationAuthorizer(
                        grant.encoded(),
                        policy,
                        Clock.fixed(ISSUED_AT.plusSeconds(120), ZoneOffset.UTC),
                        new FileSystemCancellationAuthorizationAuditStore(auditRoot));
        assertInstanceOf(
                ControlAuthorizationDecision.Approved.class,
                retry.authorize(GOAL_ID, request));
        assertEquals(first,
                new FileSystemCancellationAuthorizationAuditStore(auditRoot)
                        .find(AUTHORIZATION_ID)
                        .orElseThrow());
        assertArrayEquals(firstBytes, Files.readAllBytes(artifact));

        AuditBackedSignedCancellationAuthorizer expired =
                new AuditBackedSignedCancellationAuthorizer(
                        grant.encoded(),
                        policy,
                        Clock.fixed(EXPIRES_AT, ZoneOffset.UTC),
                        new FileSystemCancellationAuthorizationAuditStore(auditRoot));
        assertDenied(
                SignedCancellationGrantDenial.EXPIRED,
                expired.authorize(GOAL_ID, request));
        assertArrayEquals(firstBytes, Files.readAllBytes(artifact));
    }

    @Test
    void denialAndAuditFailureCannotProduceApprovalOrAudit() throws Exception {
        KeyPair keyPair = ed25519();
        MessageEnvelope request = request("operator requested cancellation", ISSUED_AT);
        DetachedSignedCancellationGrant grant = grant(
                keyPair,
                GOAL_ID,
                request,
                AUTHORIZATION_ID,
                POLICY_REVISION,
                ISSUED_AT,
                EXPIRES_AT);
        CancellationGrantTrustPolicy policy = policy(
                keyPair,
                Optional.empty(),
                POLICY_REVISION);
        Path deniedRoot = temporaryRoot.resolve("denied-audit");
        AuditBackedSignedCancellationAuthorizer authorizer =
                new AuditBackedSignedCancellationAuthorizer(
                        grant.encoded(),
                        policy,
                        Clock.fixed(ISSUED_AT.plusSeconds(60), ZoneOffset.UTC),
                        new FileSystemCancellationAuthorizationAuditStore(deniedRoot));

        assertDenied(
                SignedCancellationGrantDenial.TARGET_MISMATCH,
                authorizer.authorize(OTHER_GOAL_ID, request));
        assertFalse(Files.exists(deniedRoot));

        AuditBackedSignedCancellationAuthorizer malformed =
                new AuditBackedSignedCancellationAuthorizer(
                        new byte[] {1, 2, 3},
                        policy,
                        Clock.fixed(ISSUED_AT.plusSeconds(60), ZoneOffset.UTC),
                        new FileSystemCancellationAuthorizationAuditStore(deniedRoot));
        assertDenied(
                SignedCancellationGrantDenial.MALFORMED_PROOF,
                malformed.authorize(GOAL_ID, request));
        assertFalse(Files.exists(deniedRoot));

        CancellationAuthorizationAuditStore failingStore =
                new CancellationAuthorizationAuditStore() {
                    @Override
                    public CancellationAuthorizationAuditRecord persist(
                            CancellationAuthorizationAuditRecord audit) throws IOException {
                        throw new IOException("forced audit failure");
                    }

                    @Override
                    public Optional<CancellationAuthorizationAuditRecord> find(
                            String authorizationId) {
                        return Optional.empty();
                    }

                    @Override
                    public CancellationAuthorizationAuditRecord resolve(String reference)
                            throws IOException {
                        throw new IOException("forced audit failure");
                    }
                };
        AuditBackedSignedCancellationAuthorizer failing =
                new AuditBackedSignedCancellationAuthorizer(
                        grant.encoded(),
                        policy,
                        Clock.fixed(ISSUED_AT.plusSeconds(60), ZoneOffset.UTC),
                        failingStore);
        assertThrows(IOException.class, () -> failing.authorize(GOAL_ID, request));
    }

    @Test
    void auditOnlyPrefixRecoversWhileValidAndCannotApplyAtExpiry() throws Exception {
        KeyPair keyPair = ed25519();
        MessageEnvelope request = request("operator requested cancellation", ISSUED_AT);
        DetachedSignedCancellationGrant grant = grant(
                keyPair,
                GOAL_ID,
                request,
                AUTHORIZATION_ID,
                POLICY_REVISION,
                ISSUED_AT,
                EXPIRES_AT);
        CancellationGrantTrustPolicy policy = policy(
                keyPair,
                Optional.empty(),
                POLICY_REVISION);

        Path runtimeRoot = temporaryRoot.resolve("recover-runtime");
        FileSystemAgentRuntimeStateStore durableStore = readyRuntime(
                runtimeRoot, request);
        long sourceRevision = durableStore.resolve(GOAL_ID).revision();
        Path auditRoot = temporaryRoot.resolve("recover-audit");
        AuditBackedSignedCancellationAuthorizer authorizer =
                new AuditBackedSignedCancellationAuthorizer(
                        grant.encoded(),
                        policy,
                        Clock.fixed(ISSUED_AT.plusSeconds(60), ZoneOffset.UTC),
                        new FileSystemCancellationAuthorizationAuditStore(auditRoot));
        AgentRuntimeStateStore failingRuntimeStore = new AgentRuntimeStateStore() {
            @Override
            public void create(AgentRuntimeState initialState) throws IOException {
                durableStore.create(initialState);
            }

            @Override
            public void update(AgentRuntimeState nextState) throws IOException {
                throw new IOException("forced runtime persistence failure");
            }

            @Override
            public AgentRuntimeState resolve(String goalId) throws IOException {
                return durableStore.resolve(goalId);
            }
        };
        Path eventRoot = temporaryRoot.resolve("recover-events");
        AuthenticatedCancellationApplication interrupted =
                new AuthenticatedCancellationApplication(
                        failingRuntimeStore,
                        Clock.fixed(ISSUED_AT.plusSeconds(61), ZoneOffset.UTC),
                        authorizer,
                        new RuntimeEventRecorder(
                                new FileSystemRuntimeEventStore(eventRoot),
                                ignored -> {
                                    throw new AssertionError(
                                            "runtime failure must precede event publication");
                                }));

        assertThrows(IOException.class, () ->
                interrupted.apply(GOAL_ID, CONTROL_MESSAGE_ID));
        assertTrue(new FileSystemCancellationAuthorizationAuditStore(auditRoot)
                .find(AUTHORIZATION_ID)
                .isPresent());
        assertEquals(sourceRevision, durableStore.resolve(GOAL_ID).revision());
        assertFalse(durableStore.resolve(GOAL_ID).cancellationApplication().isPresent());
        assertFalse(Files.exists(eventRoot));

        CancellationApplicationRecord recovered =
                new AuthenticatedCancellationApplication(
                        durableStore,
                        Clock.fixed(ISSUED_AT.plusSeconds(120), ZoneOffset.UTC),
                        new AuditBackedSignedCancellationAuthorizer(
                                grant.encoded(),
                                policy,
                                Clock.fixed(ISSUED_AT.plusSeconds(120), ZoneOffset.UTC),
                                new FileSystemCancellationAuthorizationAuditStore(auditRoot)))
                        .apply(GOAL_ID, CONTROL_MESSAGE_ID);
        assertEquals(AUTHORIZATION_ID, recovered.authorizationId());

        Path expiredRuntimeRoot = temporaryRoot.resolve("expired-runtime");
        FileSystemAgentRuntimeStateStore expiredDurable = readyRuntime(
                expiredRuntimeRoot, request);
        long expiredSourceRevision = expiredDurable.resolve(GOAL_ID).revision();
        Path expiredAuditRoot = temporaryRoot.resolve("expired-audit");
        AuditBackedSignedCancellationAuthorizer initial =
                new AuditBackedSignedCancellationAuthorizer(
                        grant.encoded(),
                        policy,
                        Clock.fixed(ISSUED_AT.plusSeconds(60), ZoneOffset.UTC),
                        new FileSystemCancellationAuthorizationAuditStore(expiredAuditRoot));
        assertInstanceOf(
                ControlAuthorizationDecision.Approved.class,
                initial.authorize(GOAL_ID, request));
        AuthenticatedCancellationApplication expiredApplication =
                new AuthenticatedCancellationApplication(
                        expiredDurable,
                        Clock.fixed(EXPIRES_AT, ZoneOffset.UTC),
                        new AuditBackedSignedCancellationAuthorizer(
                                grant.encoded(),
                                policy,
                                Clock.fixed(EXPIRES_AT, ZoneOffset.UTC),
                                new FileSystemCancellationAuthorizationAuditStore(
                                        expiredAuditRoot)));
        assertThrows(ControlAuthorizationDeniedException.class, () ->
                expiredApplication.apply(GOAL_ID, CONTROL_MESSAGE_ID));
        assertEquals(
                expiredSourceRevision,
                expiredDurable.resolve(GOAL_ID).revision());
        assertFalse(expiredDurable.resolve(GOAL_ID)
                .cancellationApplication()
                .isPresent());
    }

    @Test
    void changedProofCannotReuseAnExistingAuthorizationIdentity() throws Exception {
        KeyPair keyPair = ed25519();
        MessageEnvelope request = request("operator requested cancellation", ISSUED_AT);
        CancellationGrantTrustPolicy policy = policy(
                keyPair,
                Optional.empty(),
                POLICY_REVISION);
        DetachedSignedCancellationGrant first = grant(
                keyPair,
                GOAL_ID,
                request,
                AUTHORIZATION_ID,
                POLICY_REVISION,
                ISSUED_AT,
                EXPIRES_AT);
        Path auditRoot = temporaryRoot.resolve("conflicting-audit");
        assertInstanceOf(
                ControlAuthorizationDecision.Approved.class,
                new AuditBackedSignedCancellationAuthorizer(
                        first.encoded(),
                        policy,
                        Clock.fixed(ISSUED_AT.plusSeconds(60), ZoneOffset.UTC),
                        new FileSystemCancellationAuthorizationAuditStore(auditRoot))
                        .authorize(GOAL_ID, request));
        Path artifact = auditRoot.resolve(
                AUTHORIZATION_ID + ".cancellation-authorization");
        byte[] firstBytes = Files.readAllBytes(artifact);
        DetachedSignedCancellationGrant changed = grant(
                keyPair,
                GOAL_ID,
                request,
                AUTHORIZATION_ID,
                POLICY_REVISION,
                ISSUED_AT,
                EXPIRES_AT.plusSeconds(30));

        assertThrows(IOException.class, () ->
                new AuditBackedSignedCancellationAuthorizer(
                        changed.encoded(),
                        policy,
                        Clock.fixed(ISSUED_AT.plusSeconds(60), ZoneOffset.UTC),
                        new FileSystemCancellationAuthorizationAuditStore(auditRoot))
                        .authorize(GOAL_ID, request));
        assertArrayEquals(firstBytes, Files.readAllBytes(artifact));
    }

    private static void assertDenied(
            SignedCancellationGrantDenial expected,
            Object actual) {
        if (actual instanceof SignedCancellationGrantVerifier.Denied denied) {
            assertEquals(expected, denied.reason());
            return;
        }
        ControlAuthorizationDecision.Denied denied = assertInstanceOf(
                ControlAuthorizationDecision.Denied.class, actual);
        assertEquals(expected.name(), denied.reason());
    }

    private static MessageEnvelope request(String reason, Instant occurredAt) {
        return new MessageEnvelope(
                CONTROL_MESSAGE_ID,
                "correlation-authenticated-cancel",
                Optional.of(CAUSATION_ID),
                "logical-run-authenticated-cancel",
                "control-interface",
                occurredAt,
                new ControlPayload(ControlSignal.CANCEL, reason));
    }

    private static FileSystemAgentRuntimeStateStore readyRuntime(
            Path root, MessageEnvelope controlRequest) throws Exception {
        FileSystemAgentRuntimeStateStore store =
                new FileSystemAgentRuntimeStateStore(root);
        ApprovedTaskRevision revision = new ApprovedTaskRevision(
                "signed-cancellation-core",
                "CURRENT_TASK.md",
                "7b6c9d8e0f11223344556677889900aabbccddeeff00112233445566778899aa");
        MessageEnvelope workMessage = new MessageEnvelope(
                CAUSATION_ID,
                controlRequest.correlationId(),
                Optional.empty(),
                controlRequest.logicalRunId(),
                "signed-cancellation-test",
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

    private static DetachedSignedCancellationGrant grant(
            KeyPair keyPair,
            String goalId,
            MessageEnvelope request,
            String authorizationId,
            String policyRevision,
            Instant issuedAt,
            Instant expiresAt) throws Exception {
        DetachedSignedCancellationGrant.Claims claims =
                new DetachedSignedCancellationGrant.Claims(
                        AUDIENCE,
                        goalId,
                        request.messageId(),
                        DetachedSignedCancellationGrant.requestSha256(request),
                        authorizationId,
                        ISSUER_ID,
                        KEY_ID,
                        SUBJECT_ID,
                        policyRevision,
                        issuedAt,
                        expiresAt);
        return DetachedSignedCancellationGrant.create(
                claims, sign(keyPair.getPrivate(), claims.signingBytes()));
    }

    private static CancellationGrantTrustPolicy policy(
            KeyPair keyPair,
            Optional<Instant> revokedAt,
            String policyRevision) {
        return new CancellationGrantTrustPolicy(
                "local-installation",
                "configuration-v1",
                AUDIENCE,
                policyRevision,
                Duration.ofMinutes(15),
                Duration.ofSeconds(30),
                List.of(key(keyPair, revokedAt)));
    }

    private static CancellationGrantTrustPolicy.TrustedKey key(
            KeyPair keyPair,
            Optional<Instant> revokedAt) {
        byte[] encoded = keyPair.getPublic().getEncoded();
        return new CancellationGrantTrustPolicy.TrustedKey(
                ISSUER_ID,
                KEY_ID,
                Set.of(SUBJECT_ID),
                encoded,
                sha256(encoded),
                ISSUED_AT.minusSeconds(60),
                EXPIRES_AT.plusSeconds(60),
                revokedAt);
    }

    private static KeyPair ed25519() throws Exception {
        return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
    }

    private static byte[] sign(PrivateKey privateKey, byte[] content) throws Exception {
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
