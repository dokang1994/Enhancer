package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PinnedFileCancellationGrantTrustPolicyLoaderTest {
    private static final Instant VALID_FROM =
            Instant.parse("2026-08-11T09:00:00Z");
    private static final Instant VALID_UNTIL =
            Instant.parse("2027-08-11T09:00:00Z");

    @TempDir
    Path temporaryRoot;

    @Test
    void canonicalSnapshotDefensivelyOwnsItsBytesAndDigest() throws Exception {
        KeyPair keyPair = ed25519();
        byte[] content = canonicalPolicy(keyPair, "-");
        Path path = write("snapshot.conf", content);

        PinnedFileCancellationGrantTrustPolicyLoader.CanonicalSnapshot snapshot =
                PinnedFileCancellationGrantTrustPolicyLoader.readCanonicalSnapshot(path);
        byte[] exposed = snapshot.bytes();
        exposed[0] ^= 1;

        assertEquals(sha256(content), snapshot.sha256());
        assertArrayEquals(content, snapshot.bytes());
        assertEquals(snapshot.sha256(), snapshot.policy().configurationRevision());
    }

    @Test
    void loadsCanonicalPinnedPublicPolicyAndDerivesConfigurationRevision()
            throws Exception {
        KeyPair keyPair = ed25519();
        byte[] encoded = canonicalPolicy(keyPair, "-");
        Path policyFile = write("policy.conf", encoded);
        String pin = sha256(encoded);

        CancellationGrantTrustPolicy policy =
                new PinnedFileCancellationGrantTrustPolicyLoader(policyFile, pin)
                        .load();

        assertEquals("local-installation", policy.configurationId());
        assertEquals(pin, policy.configurationRevision());
        assertEquals("enhancer-local-control", policy.audience());
        assertEquals("cancel-policy-v1", policy.policyRevision());
        assertEquals(Duration.ofMinutes(15), policy.maximumGrantLifetime());
        assertEquals(Duration.ofSeconds(30), policy.clockSkew());
        assertEquals(1, policy.trustedKeys().size());
        CancellationGrantTrustPolicy.TrustedKey key = policy.trustedKeys().get(0);
        assertEquals("operations", key.issuerId());
        assertEquals("primary-2026", key.keyId());
        assertEquals(List.of("operator-17", "operator-18"),
                new ArrayList<>(key.authorizedSubjects()));
        assertArrayEquals(keyPair.getPublic().getEncoded(),
                key.publicKeySubjectPublicKeyInfo());
        assertEquals(sha256(keyPair.getPublic().getEncoded()), key.publicKeySha256());
        assertEquals(VALID_FROM, key.validFrom());
        assertEquals(VALID_UNTIL, key.validUntil());
        assertTrue(key.revokedAt().isEmpty());
    }

    @Test
    void requiresAnAbsoluteNormalizedPathAndCanonicalLowercasePin() throws Exception {
        Path absolute = temporaryRoot.resolve("policy.conf").toAbsolutePath();
        Path nonNormalized = absolute.getParent().resolve("child").resolve("..")
                .resolve(absolute.getFileName());

        assertThrows(IllegalArgumentException.class, () ->
                new PinnedFileCancellationGrantTrustPolicyLoader(
                        Path.of("policy.conf"), "a".repeat(64)));
        assertThrows(IllegalArgumentException.class, () ->
                new PinnedFileCancellationGrantTrustPolicyLoader(
                        nonNormalized, "a".repeat(64)));
        assertThrows(IllegalArgumentException.class, () ->
                new PinnedFileCancellationGrantTrustPolicyLoader(
                        absolute, "A".repeat(64)));
        assertThrows(IllegalArgumentException.class, () ->
                new PinnedFileCancellationGrantTrustPolicyLoader(
                        absolute, "a".repeat(63)));
    }

    @Test
    void rejectsMissingNonregularAndSymbolicSources() throws Exception {
        Path missing = temporaryRoot.resolve("missing.conf").toAbsolutePath();
        assertThrows(IOException.class, () ->
                new PinnedFileCancellationGrantTrustPolicyLoader(
                        missing, "a".repeat(64)).load());

        Path directory = Files.createDirectory(
                temporaryRoot.resolve("directory.conf")).toAbsolutePath();
        assertThrows(IOException.class, () ->
                new PinnedFileCancellationGrantTrustPolicyLoader(
                        directory, "a".repeat(64)).load());

        byte[] encoded = canonicalPolicy(ed25519(), "-");
        Path target = write("target.conf", encoded);
        Path link = temporaryRoot.resolve("linked.conf").toAbsolutePath();
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            Assumptions.assumeTrue(false,
                    "symbolic links are unavailable: " + exception.getMessage());
        }
        assertThrows(IOException.class, () ->
                new PinnedFileCancellationGrantTrustPolicyLoader(
                        link, sha256(encoded)).load());
    }

    @Test
    void rejectsPinMismatchOversizeAndEmptyFiles() throws Exception {
        byte[] encoded = canonicalPolicy(ed25519(), "-");
        Path policyFile = write("mismatch.conf", encoded);
        assertThrows(IOException.class, () ->
                new PinnedFileCancellationGrantTrustPolicyLoader(
                        policyFile, "0".repeat(64)).load());

        Path oversized = temporaryRoot.resolve("oversized.conf").toAbsolutePath();
        Files.write(oversized,
                new byte[PinnedFileCancellationGrantTrustPolicyLoader.MAX_POLICY_BYTES + 1]);
        assertThrows(IOException.class, () ->
                new PinnedFileCancellationGrantTrustPolicyLoader(
                        oversized, "0".repeat(64)).load());

        Path empty = write("empty.conf", new byte[0]);
        assertThrows(IOException.class, () ->
                new PinnedFileCancellationGrantTrustPolicyLoader(
                        empty, sha256(new byte[0])).load());
    }

    @Test
    void rejectsMalformedTextAndNoncanonicalStructure() throws Exception {
        byte[] canonical = canonicalPolicy(ed25519(), "-");
        String text = new String(canonical, StandardCharsets.UTF_8);

        assertRejected(text.replace("\n", "\r\n").getBytes(StandardCharsets.UTF_8));
        assertRejected(text.replace("audience=", "\n#a comment\naudience=")
                .getBytes(StandardCharsets.UTF_8));
        assertRejected(text.replace("audience=", "unknown=value\naudience=")
                .getBytes(StandardCharsets.UTF_8));
        assertRejected((text + "trailing=value\n").getBytes(StandardCharsets.UTF_8));
        assertRejected(text.substring(0, text.length() - 1)
                .getBytes(StandardCharsets.UTF_8));
        assertRejected(text.replace("maximumGrantLifetimeSeconds=900",
                "maximumGrantLifetimeSeconds=0900").getBytes(StandardCharsets.UTF_8));
        assertRejected(text.replace("2026-08-11T09:00:00Z",
                "2026-08-11T09:00:00.000Z").getBytes(StandardCharsets.UTF_8));
        assertRejected(text.replace("subject.0=operator-17\n",
                "subject.0=operator-18\n").getBytes(StandardCharsets.UTF_8));
        assertRejected(text.replace("subject.0=operator-17\ntrustedKey.0.subject.1=operator-18",
                "subject.0=operator-18\ntrustedKey.0.subject.1=operator-17")
                .getBytes(StandardCharsets.UTF_8));

        byte[] malformedUtf8 = canonical.clone();
        malformedUtf8[malformedUtf8.length / 2] = (byte) 0xC3;
        malformedUtf8[malformedUtf8.length / 2 + 1] = (byte) 0x28;
        assertRejected(malformedUtf8);
    }

    @Test
    void rejectsNoncanonicalKeyMaterialFingerprintAndUnsupportedAlgorithm()
            throws Exception {
        KeyPair ed25519 = ed25519();
        String text = new String(canonicalPolicy(ed25519, "-"),
                StandardCharsets.UTF_8);
        String publicKey = Base64.getEncoder().encodeToString(
                ed25519.getPublic().getEncoded());
        String fingerprint = sha256(ed25519.getPublic().getEncoded());

        assertRejected(text.replace(publicKey, publicKey.substring(0,
                publicKey.length() - 1)).getBytes(StandardCharsets.UTF_8));
        assertRejected(text.replace(fingerprint, "f".repeat(64))
                .getBytes(StandardCharsets.UTF_8));
        assertRejected(text.replace(fingerprint, fingerprint.toUpperCase())
                .getBytes(StandardCharsets.UTF_8));

        byte[] privateKey = ed25519.getPrivate().getEncoded();
        assertRejected(replaceKey(text, privateKey).getBytes(StandardCharsets.UTF_8));

        KeyPairGenerator rsaGenerator = KeyPairGenerator.getInstance("RSA");
        rsaGenerator.initialize(2048);
        byte[] rsaPublic = rsaGenerator.generateKeyPair().getPublic().getEncoded();
        assertRejected(replaceKey(text, rsaPublic).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void policyRotationAndRollbackRequireTheMatchingIndependentlySuppliedPin()
            throws Exception {
        KeyPair keyPair = ed25519();
        byte[] original = canonicalPolicy(keyPair, "-");
        byte[] revoked = canonicalPolicy(keyPair, "2026-08-11T10:00:00Z");
        Path policyFile = write("rotation.conf", original);
        String originalPin = sha256(original);
        String revokedPin = sha256(revoked);

        assertTrue(new PinnedFileCancellationGrantTrustPolicyLoader(
                policyFile, originalPin).load().trustedKeys().get(0)
                .revokedAt().isEmpty());

        Files.write(policyFile, revoked);
        assertThrows(IOException.class, () ->
                new PinnedFileCancellationGrantTrustPolicyLoader(
                        policyFile, originalPin).load());
        assertEquals(Instant.parse("2026-08-11T10:00:00Z"),
                new PinnedFileCancellationGrantTrustPolicyLoader(
                        policyFile, revokedPin).load().trustedKeys().get(0)
                        .revokedAt().orElseThrow());

        Files.write(policyFile, original);
        assertThrows(IOException.class, () ->
                new PinnedFileCancellationGrantTrustPolicyLoader(
                        policyFile, revokedPin).load());
    }

    private void assertRejected(byte[] content) throws Exception {
        Path path = write("rejected-" + Math.abs(java.util.Arrays.hashCode(content))
                + ".conf", content);
        assertThrows(IOException.class, () ->
                new PinnedFileCancellationGrantTrustPolicyLoader(
                        path, sha256(content)).load());
    }

    private Path write(String name, byte[] content) throws IOException {
        Path path = temporaryRoot.resolve(name).toAbsolutePath().normalize();
        Files.write(path, content);
        return path;
    }

    private static byte[] canonicalPolicy(KeyPair keyPair, String revokedAt) {
        byte[] publicKey = keyPair.getPublic().getEncoded();
        String content = String.join("\n",
                "enhancer-cancellation-grant-trust-policy-v1",
                "configurationId=local-installation",
                "audience=enhancer-local-control",
                "policyRevision=cancel-policy-v1",
                "maximumGrantLifetimeSeconds=900",
                "clockSkewSeconds=30",
                "trustedKeyCount=1",
                "trustedKey.0.issuerId=operations",
                "trustedKey.0.keyId=primary-2026",
                "trustedKey.0.subjectCount=2",
                "trustedKey.0.subject.0=operator-17",
                "trustedKey.0.subject.1=operator-18",
                "trustedKey.0.publicKeySubjectPublicKeyInfo="
                        + Base64.getEncoder().encodeToString(publicKey),
                "trustedKey.0.publicKeySha256=" + sha256(publicKey),
                "trustedKey.0.validFrom=" + VALID_FROM,
                "trustedKey.0.validUntil=" + VALID_UNTIL,
                "trustedKey.0.revokedAt=" + revokedAt,
                "");
        return content.getBytes(StandardCharsets.UTF_8);
    }

    private static String replaceKey(String policy, byte[] replacement) {
        String encoded = Base64.getEncoder().encodeToString(replacement);
        String fingerprint = sha256(replacement);
        String withKey = policy.replaceFirst(
                "(?m)^trustedKey\\.0\\.publicKeySubjectPublicKeyInfo=.*$",
                "trustedKey.0.publicKeySubjectPublicKeyInfo=" + encoded);
        return withKey.replaceFirst(
                "(?m)^trustedKey\\.0\\.publicKeySha256=.*$",
                "trustedKey.0.publicKeySha256=" + fingerprint);
    }

    private static KeyPair ed25519() throws Exception {
        return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
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
