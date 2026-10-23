package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PinnedFileRemoteModelCandidateCapabilityFactSourceSnapshotTest {

    private static final Instant NOW = Instant.parse("2026-10-23T00:01:00Z");

    @TempDir
    Path temporaryRoot;

    @Test
    void rejectsUnsafeMissingDirectoryAndSymbolicSourcesFirst() throws Exception {
        Path missing = temporaryRoot.resolve("missing");
        assertEquals(reason(missing, "0".repeat(64)),
                RemoteModelCandidateCapabilityFactSourceRefusalReason
                        .CATALOG_UNAVAILABLE_OR_UNSAFE);

        Path directory = Files.createDirectory(temporaryRoot.resolve("directory"));
        assertEquals(reason(directory, "0".repeat(64)),
                RemoteModelCandidateCapabilityFactSourceRefusalReason
                        .CATALOG_UNAVAILABLE_OR_UNSAFE);

        byte[] content = RemoteModelCapabilityCatalogTestFixture.zeroRecords();
        Path target = write("target", content);
        Path link = temporaryRoot.resolve("link");
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            Assumptions.assumeTrue(false, "symbolic links unavailable");
        }
        assertEquals(reason(link, RemoteModelCapabilityCatalogTestFixture.sha256(content)),
                RemoteModelCandidateCapabilityFactSourceRefusalReason
                        .CATALOG_UNAVAILABLE_OR_UNSAFE);
    }

    @Test
    void rejectsEmptyAndOversizeBeforePinAndAcceptsExactMaximumSizeStage()
            throws Exception {
        byte[] empty = new byte[0];
        assertEquals(reason(write("empty", empty), "0".repeat(64)),
                RemoteModelCandidateCapabilityFactSourceRefusalReason
                        .CATALOG_OUTSIDE_SIZE_BOUND);

        byte[] oversized = new byte[4 * 1024 * 1024 + 1];
        assertEquals(reason(write("oversized", oversized), "0".repeat(64)),
                RemoteModelCandidateCapabilityFactSourceRefusalReason
                        .CATALOG_OUTSIDE_SIZE_BOUND);

        byte[] maximum = new byte[4 * 1024 * 1024];
        java.util.Arrays.fill(maximum, (byte) 'x');
        assertNotEquals(
                RemoteModelCandidateCapabilityFactSourceRefusalReason
                        .CATALOG_OUTSIDE_SIZE_BOUND,
                reason(write("maximum", maximum),
                        RemoteModelCapabilityCatalogTestFixture.sha256(maximum)));
    }

    @Test
    void pinPrecedesUtf8AndStrictUtf8PrecedesParsing() throws Exception {
        byte[] malformed = {(byte) 0xC3, (byte) 0x28};
        Path path = write("malformed", malformed);
        assertEquals(reason(path, "0".repeat(64)),
                RemoteModelCandidateCapabilityFactSourceRefusalReason
                        .CATALOG_PIN_MISMATCH);
        assertEquals(reason(path, RemoteModelCapabilityCatalogTestFixture.sha256(malformed)),
                RemoteModelCandidateCapabilityFactSourceRefusalReason
                        .CATALOG_INVALID_UTF8);
    }

    @Test
    void exactReadHelperDetectsEarlyEofExtraBytesAndSizeDrift() throws Exception {
        assertArrayEquals(new byte[] {1, 2, 3},
                PinnedFileRemoteModelCandidateCapabilityFactSource.readDeclaredBytes(
                        new FakeChannel(new byte[] {1, 2, 3}, 3), 3));
        assertThrows(IOException.class, () ->
                PinnedFileRemoteModelCandidateCapabilityFactSource.readDeclaredBytes(
                        new FakeChannel(new byte[] {1, 2}, 2), 3));
        assertThrows(IOException.class, () ->
                PinnedFileRemoteModelCandidateCapabilityFactSource.readDeclaredBytes(
                        new FakeChannel(new byte[] {1, 2, 3, 4}, 4), 3));
        assertThrows(IOException.class, () ->
                PinnedFileRemoteModelCandidateCapabilityFactSource.readDeclaredBytes(
                        new FakeChannel(new byte[] {1, 2, 3}, 4), 3));
    }

    private RemoteModelCandidateCapabilityFactSourceRefusalReason reason(
            Path path, String pin) {
        RemoteModelCandidateCapabilityFactSourceResult.Refused refused =
                (RemoteModelCandidateCapabilityFactSourceResult.Refused)
                        new PinnedFileRemoteModelCandidateCapabilityFactSource(
                                path.toAbsolutePath().normalize(), pin)
                                .acquire("candidate-a", "destination-a", NOW);
        return refused.reason();
    }

    private Path write(String name, byte[] content) throws IOException {
        Path path = temporaryRoot.resolve(name).toAbsolutePath().normalize();
        Files.write(path, content, new java.nio.file.OpenOption[0]);
        return path;
    }

    private static final class FakeChannel implements SeekableByteChannel {
        private final byte[] bytes;
        private final long reportedSize;
        private int position;
        private boolean open = true;

        private FakeChannel(byte[] bytes, long reportedSize) {
            this.bytes = bytes.clone();
            this.reportedSize = reportedSize;
        }

        @Override
        public int read(ByteBuffer destination) {
            if (position == bytes.length) {
                return -1;
            }
            int count = Math.min(destination.remaining(), bytes.length - position);
            destination.put(bytes, position, count);
            position += count;
            return count;
        }

        @Override public int write(ByteBuffer source) { throw new UnsupportedOperationException(); }
        @Override public long position() { return position; }
        @Override public SeekableByteChannel position(long value) { throw new UnsupportedOperationException(); }
        @Override public long size() { return reportedSize; }
        @Override public SeekableByteChannel truncate(long size) { throw new UnsupportedOperationException(); }
        @Override public boolean isOpen() { return open; }
        @Override public void close() { open = false; }
    }
}
