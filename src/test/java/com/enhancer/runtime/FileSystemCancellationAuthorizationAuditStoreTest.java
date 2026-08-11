package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.ControlSignal;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemCancellationAuthorizationAuditStoreTest {
    private static final String AUTHORIZATION_ID =
            "00000000-0000-0000-0000-00000000b101";
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-00000000b102";
    private static final String CONTROL_MESSAGE_ID =
            "00000000-0000-0000-0000-00000000b103";

    @TempDir
    Path temporaryRoot;

    @Test
    void persistsResolvesAndExactReplaysWithoutRewrite() throws Exception {
        Path root = existingParent().resolve("authorization-audit");
        FileSystemCancellationAuthorizationAuditStore store =
                new FileSystemCancellationAuthorizationAuditStore(root);
        CancellationAuthorizationAuditRecord audit = audit("a".repeat(64));

        assertEquals(audit, store.persist(audit));
        Path artifact = root.resolve(
                AUTHORIZATION_ID + ".cancellation-authorization");
        byte[] firstBytes = Files.readAllBytes(artifact);
        assertEquals(Optional.of(audit), store.find(AUTHORIZATION_ID));
        assertEquals(audit, store.resolve(audit.reference()));

        assertEquals(audit, store.persist(audit));
        assertArrayEquals(firstBytes, Files.readAllBytes(artifact));
    }

    @Test
    void changedReuseCorruptionTrailingAndOversizedArtifactsFailClosed()
            throws Exception {
        Path root = existingParent().resolve("invalid-audit");
        FileSystemCancellationAuthorizationAuditStore store =
                new FileSystemCancellationAuthorizationAuditStore(root);
        CancellationAuthorizationAuditRecord original = audit("a".repeat(64));
        store.persist(original);
        Path artifact = root.resolve(
                AUTHORIZATION_ID + ".cancellation-authorization");
        byte[] firstBytes = Files.readAllBytes(artifact);

        assertThrows(IOException.class, () -> store.persist(audit("b".repeat(64))));
        assertArrayEquals(firstBytes, Files.readAllBytes(artifact));

        byte[] corrupt = firstBytes.clone();
        corrupt[corrupt.length - 1] ^= 1;
        Files.write(artifact, corrupt);
        assertThrows(IOException.class, () -> store.resolve(original.reference()));

        byte[] unsupported = firstBytes.clone();
        ByteBuffer.wrap(unsupported).putInt(
                FileSystemCancellationAuthorizationAuditStore.HEADER_BYTES,
                2);
        replaceDigest(unsupported);
        Files.write(artifact, unsupported);
        IOException versionFailure = assertThrows(
                IOException.class, () -> store.resolve(original.reference()));
        assertTrue(versionFailure.getMessage().contains("version"));

        byte[] malformedUtf8 = firstBytes.clone();
        int firstStringOffset =
                FileSystemCancellationAuthorizationAuditStore.HEADER_BYTES
                        + Integer.BYTES
                        + Integer.BYTES;
        malformedUtf8[firstStringOffset] = (byte) 0xC3;
        malformedUtf8[firstStringOffset + 1] = (byte) 0x28;
        replaceDigest(malformedUtf8);
        Files.write(artifact, malformedUtf8);
        assertThrows(IOException.class, () -> store.resolve(original.reference()));

        Files.write(
                artifact,
                ByteBuffer.allocate(firstBytes.length + 1)
                        .put(firstBytes)
                        .put((byte) 1)
                        .array());
        assertThrows(IOException.class, () -> store.resolve(original.reference()));

        try (FileChannel channel = FileChannel.open(
                artifact,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            channel.position(
                    FileSystemCancellationAuthorizationAuditStore.HEADER_BYTES
                            + FileSystemCancellationAuthorizationAuditStore.MAX_STATE_BYTES);
            channel.write(ByteBuffer.wrap(new byte[] {1}));
        }
        assertThrows(IOException.class, () -> store.resolve(original.reference()));
    }

    @Test
    void missingUnsafeReferenceAndSymbolicRootsFailWithoutCreatingAuthority()
            throws Exception {
        Path parent = existingParent();
        Path root = parent.resolve("missing-audit");
        FileSystemCancellationAuthorizationAuditStore store =
                new FileSystemCancellationAuthorizationAuditStore(root);
        assertFalse(store.find(AUTHORIZATION_ID).isPresent());
        assertFalse(Files.exists(root));
        assertThrows(IOException.class, () -> store.resolve(
                "cancellation-authorization/../../outside"));

        Path target = parent.resolve("audit-target");
        Path link = parent.resolve("audit-link");
        Files.createDirectory(target);
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException exception) {
            Assumptions.assumeTrue(
                    false,
                    "symbolic links are unavailable: " + exception.getMessage());
        }
        FileSystemCancellationAuthorizationAuditStore linked =
                new FileSystemCancellationAuthorizationAuditStore(link);
        IOException failure = assertThrows(
                IOException.class, () -> linked.persist(audit("a".repeat(64))));
        assertTrue(failure.getMessage().contains("symbolic"), failure.getMessage());
    }

    private Path existingParent() throws IOException {
        Path parent = temporaryRoot.resolve("operator-owned-parent");
        Files.createDirectories(parent);
        return parent;
    }

    private static CancellationAuthorizationAuditRecord audit(String proofSha256) {
        return CancellationAuthorizationAuditRecord.create(
                AUTHORIZATION_ID,
                "enhancer-local-control",
                "operations",
                "operator-17",
                "signed-cancellation-actor-v1:" + "c".repeat(64),
                GOAL_ID,
                CONTROL_MESSAGE_ID,
                ControlSignal.CANCEL,
                "d".repeat(64),
                proofSha256,
                "primary-2026",
                "e".repeat(64),
                "Ed25519",
                "local-installation",
                "configuration-v1",
                "cancel-policy-v1",
                Instant.parse("2026-08-11T10:00:00Z"),
                Instant.parse("2026-08-11T10:10:00Z"),
                Instant.parse("2026-08-11T10:01:00Z"),
                Optional.empty(),
                SignedCancellationGrantVerifier.VERIFIER_VERSION);
    }

    private static void replaceDigest(byte[] envelope) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(envelope);
        int payloadLength = buffer.getInt(Integer.BYTES);
        byte[] payload = new byte[payloadLength];
        System.arraycopy(
                envelope,
                FileSystemCancellationAuthorizationAuditStore.HEADER_BYTES,
                payload,
                0,
                payloadLength);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(payload);
        System.arraycopy(
                digest,
                0,
                envelope,
                Integer.BYTES + Integer.BYTES,
                digest.length);
    }
}
