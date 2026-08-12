package com.enhancer.runtime;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.util.regex.Pattern;

/** Reads the sole cancellation trust binding beside a protected application JAR. */
public final class InstalledCancellationTrustMetadataLoader {
    public static final String METADATA_FILE_NAME =
            "enhancer-cancellation-trust-metadata-v1";
    public static final int MAX_METADATA_BYTES = 4 * 1024;

    private static final String HEADER =
            "enhancer-installed-cancellation-trust-v1";
    private static final Pattern LOWERCASE_SHA256 = Pattern.compile("[0-9a-f]{64}");

    private final Path metadataFile;

    public InstalledCancellationTrustMetadataLoader(Path applicationJar) {
        Path checked = Objects.requireNonNull(
                applicationJar, "applicationJar must not be null");
        if (!checked.isAbsolute() || !checked.equals(checked.normalize())) {
            throw new IllegalArgumentException(
                    "applicationJar must be an absolute normalized path");
        }
        if (!checked.getFileName().toString().endsWith(".jar")) {
            throw new IllegalArgumentException("applicationJar must have a .jar name");
        }
        try {
            requireExactRegularFile(checked, "applicationJar");
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                    "applicationJar must be an exact real regular file", exception);
        }
        this.metadataFile = checked.resolveSibling(METADATA_FILE_NAME);
    }

    public InstalledCancellationTrustMetadata load() throws IOException {
        return loadCanonicalSnapshot().metadata();
    }

    /** Reads the fixed metadata as one exact canonical snapshot. */
    public CanonicalSnapshot loadCanonicalSnapshot() throws IOException {
        requireExactRegularFile(metadataFile, "installed cancellation trust metadata");
        byte[] snapshot = readBoundedSnapshot(
                metadataFile, MAX_METADATA_BYTES, "installed cancellation trust metadata");
        InstalledCancellationTrustMetadata metadata = parseCanonical(snapshot);
        return new CanonicalSnapshot(metadata, sha256(snapshot), snapshot);
    }

    /** Parses exact canonical metadata bytes without selecting a path. */
    public static InstalledCancellationTrustMetadata parseCanonical(byte[] snapshot)
            throws IOException {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        if (snapshot.length <= 0 || snapshot.length > MAX_METADATA_BYTES) {
            throw new IOException(
                    "installed cancellation trust metadata is outside supported size bounds");
        }
        String text = decodeUtf8(snapshot);
        if (!text.endsWith("\n") || text.indexOf('\r') >= 0) {
            throw new IOException("installed cancellation trust metadata must use canonical LF text");
        }
        String[] lines = text.split("\n", -1);
        if (lines.length != 4
                || !lines[0].equals(HEADER)
                || !lines[1].startsWith("policyPath=")
                || !lines[2].startsWith("policySha256=")
                || !lines[3].isEmpty()) {
            throw new IOException("installed cancellation trust metadata has unexpected content");
        }
        String rawPath = nonEmpty(lines[1].substring("policyPath=".length()), "policyPath");
        String pin = nonEmpty(lines[2].substring("policySha256=".length()), "policySha256");
        final InstalledCancellationTrustMetadata metadata;
        try {
            metadata = new InstalledCancellationTrustMetadata(Path.of(rawPath), pin);
        } catch (IllegalArgumentException exception) {
            throw new IOException("invalid installed cancellation trust metadata", exception);
        }
        if (!Arrays.equals(snapshot, encodeCanonical(metadata))) {
            throw new IOException("installed cancellation trust metadata is not canonical");
        }
        return metadata;
    }

    /** Encodes the unchanged canonical metadata-v1 representation. */
    public static byte[] encodeCanonical(InstalledCancellationTrustMetadata metadata) {
        InstalledCancellationTrustMetadata checked = Objects.requireNonNull(
                metadata, "metadata must not be null");
        return (HEADER + "\npolicyPath=" + checked.policyFile()
                + "\npolicySha256=" + checked.expectedSha256() + "\n")
                .getBytes(StandardCharsets.UTF_8);
    }

    private static String nonEmpty(String value, String field) throws IOException {
        if (value.isEmpty()) {
            throw new IOException(field + " must not be empty");
        }
        return value;
    }

    static void requireExactRegularFile(Path file, String label) throws IOException {
        Path parent = file.getParent();
        if (parent == null
                || !Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)
                || !parent.toRealPath().equals(parent)) {
            throw new IOException(label + " parent must be an exact real directory");
        }
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(file)
                || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                || !file.toRealPath().equals(file)) {
            throw new IOException(label + " must be an exact real regular file");
        }
    }

    static byte[] readBoundedSnapshot(Path file, int maximum, String label)
            throws IOException {
        try (FileChannel channel = FileChannel.open(
                file, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            long size = channel.size();
            if (size <= 0 || size > maximum) {
                throw new IOException(label + " is outside supported size bounds");
            }
            byte[] content = new byte[(int) size];
            ByteBuffer target = ByteBuffer.wrap(content);
            while (target.hasRemaining()) {
                if (channel.read(target) < 0) {
                    throw new IOException(label + " changed while reading");
                }
            }
            if (channel.read(ByteBuffer.allocate(1)) >= 0 || channel.size() != size) {
                throw new IOException(label + " changed while reading");
            }
            return content;
        }
    }

    private static String decodeUtf8(byte[] content) throws IOException {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new IOException(
                    "installed cancellation trust metadata is not valid UTF-8", exception);
        }
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    /** Installed metadata and digest derived from one defensive byte snapshot. */
    public static final class CanonicalSnapshot {
        private final InstalledCancellationTrustMetadata metadata;
        private final String sha256;
        private final byte[] bytes;

        private CanonicalSnapshot(
                InstalledCancellationTrustMetadata metadata,
                String sha256,
                byte[] bytes) {
            this.metadata = Objects.requireNonNull(metadata, "metadata must not be null");
            this.sha256 = Objects.requireNonNull(sha256, "sha256 must not be null");
            if (!LOWERCASE_SHA256.matcher(this.sha256).matches()) {
                throw new IllegalArgumentException("sha256 must be lowercase SHA-256");
            }
            this.bytes = Objects.requireNonNull(bytes, "bytes must not be null").clone();
        }

        public InstalledCancellationTrustMetadata metadata() { return metadata; }

        public String sha256() { return sha256; }

        public byte[] bytes() { return bytes.clone(); }
    }
}
