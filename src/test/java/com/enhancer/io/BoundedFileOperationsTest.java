package com.enhancer.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.InputStream;
import java.security.MessageDigest;
import org.junit.jupiter.api.Test;

class BoundedFileOperationsTest {
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
