package com.enhancer.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BoundedFileOperationsTest {
    @TempDir
    Path temporaryRoot;

    @Test
    void readsTheExactBoundaryWithoutAnExtraAllocation() throws Exception {
        CountingInputStream input = new CountingInputStream(8);

        byte[] bytes = BoundedFileOperations.readAllBytes(input, 8);

        assertArrayEquals(new byte[8], bytes);
        assertEquals(8, input.bytesReturned());
    }

    @Test
    void detectsReadOverflowAfterOnlyOneAdditionalByte() {
        CountingInputStream input = new CountingInputStream(1_000_000);

        assertThrows(
                FileSizeLimitExceededException.class,
                () -> BoundedFileOperations.readAllBytes(input, 8));
        assertEquals(9, input.bytesReturned());
    }

    @Test
    void detectsDigestOverflowAfterOnlyOneAdditionalByte() {
        CountingInputStream input = new CountingInputStream(1_000_000);

        assertThrows(
                FileSizeLimitExceededException.class,
                () -> BoundedFileOperations.sha256(input, 8));
        assertEquals(9, input.bytesReturned());
    }

    @Test
    void hashesTheExactBoundary() throws Exception {
        byte[] expected = MessageDigest.getInstance("SHA-256")
                .digest(new byte[8]);

        assertArrayEquals(
                expected,
                BoundedFileOperations.sha256(
                        new CountingInputStream(8),
                        8));
    }

    @Test
    void noFollowReadRejectsAFinalSymbolicLink() throws Exception {
        Path target = Files.write(temporaryRoot.resolve("target"), new byte[] {1});
        Path link = temporaryRoot.resolve("link");
        try {
            Files.createSymbolicLink(link, target.getFileName());
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            assumeTrue(false, "symbolic-link creation is unavailable: " + exception);
        }

        assertThrows(IOException.class,
                () -> BoundedFileOperations.readAllBytesNoFollow(link, 1));
    }

    private static final class CountingInputStream extends InputStream {
        private final int totalBytes;
        private int bytesReturned;

        private CountingInputStream(int totalBytes) {
            this.totalBytes = totalBytes;
        }

        @Override
        public int read() {
            if (bytesReturned >= totalBytes) {
                return -1;
            }
            bytesReturned++;
            return 0;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) {
            if (bytesReturned >= totalBytes) {
                return -1;
            }
            int read = Math.min(length, totalBytes - bytesReturned);
            java.util.Arrays.fill(bytes, offset, offset + read, (byte) 0);
            bytesReturned += read;
            return read;
        }

        int bytesReturned() {
            return bytesReturned;
        }
    }
}
