package com.enhancer.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * File operations whose byte ceiling is enforced while consuming the stream. Overflow detection
 * reads at most one byte beyond the accepted limit.
 */
public final class BoundedFileOperations {
    private static final int BUFFER_BYTES = 8192;

    private BoundedFileOperations() {
    }

    public static byte[] readAllBytes(Path path, long maximumBytes)
            throws IOException {
        Objects.requireNonNull(path, "path must not be null");
        try (InputStream input = Files.newInputStream(path)) {
            return readAllBytes(input, maximumBytes);
        }
    }

    public static byte[] sha256(Path path, long maximumBytes)
            throws IOException {
        Objects.requireNonNull(path, "path must not be null");
        try (InputStream input = Files.newInputStream(path)) {
            return sha256(input, maximumBytes);
        }
    }

    static byte[] readAllBytes(InputStream input, long maximumBytes)
            throws IOException {
        Objects.requireNonNull(input, "input must not be null");
        requireArrayLimit(maximumBytes);
        ByteArrayOutputStream output = new ByteArrayOutputStream(
                (int) Math.min(maximumBytes, BUFFER_BYTES));
        consume(input, maximumBytes, output, null);
        return output.toByteArray();
    }

    static byte[] sha256(InputStream input, long maximumBytes)
            throws IOException {
        Objects.requireNonNull(input, "input must not be null");
        requireLimit(maximumBytes);
        MessageDigest digest = sha256();
        consume(input, maximumBytes, null, digest);
        return digest.digest();
    }

    private static void consume(
            InputStream input,
            long maximumBytes,
            ByteArrayOutputStream output,
            MessageDigest digest) throws IOException {
        byte[] buffer = new byte[BUFFER_BYTES];
        long total = 0;
        while (total < maximumBytes) {
            int requested = (int) Math.min(
                    buffer.length,
                    maximumBytes - total);
            int read = input.read(buffer, 0, requested);
            if (read < 0) {
                return;
            }
            if (read == 0) {
                int single = input.read();
                if (single < 0) {
                    return;
                }
                buffer[0] = (byte) single;
                read = 1;
            }
            if (output != null) {
                output.write(buffer, 0, read);
            } else {
                digest.update(buffer, 0, read);
            }
            total += read;
        }
        if (input.read() >= 0) {
            throw new FileSizeLimitExceededException(maximumBytes);
        }
    }

    private static void requireArrayLimit(long maximumBytes) {
        requireLimit(maximumBytes);
        if (maximumBytes > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "maximumBytes exceeds the supported byte-array size");
        }
    }

    private static void requireLimit(long maximumBytes) {
        if (maximumBytes < 0) {
            throw new IllegalArgumentException(
                    "maximumBytes must not be negative");
        }
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception);
        }
    }
}
