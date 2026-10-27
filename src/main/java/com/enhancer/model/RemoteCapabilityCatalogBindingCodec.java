package com.enhancer.model;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/** Pure strict codec for RFC-0032's fixed path-and-pin binding. */
final class RemoteCapabilityCatalogBindingCodec {
    static final String BINDING_NAME =
            "enhancer-remote-capability-catalog-binding-v1";
    static final String CATALOG_DIRECTORY_NAME =
            "enhancer-remote-capability-catalogs-v1";
    static final String LOCK_NAME =
            "enhancer-remote-capability-catalog-maintenance-v1.lock";
    private static final String HEADER =
            "enhancer-installed-remote-capability-catalog-v1";
    private static final int MAX_BYTES = 4 * 1024;

    private RemoteCapabilityCatalogBindingCodec() {
    }

    static RemoteCapabilityCatalogBinding create(Path catalogDirectory, String digest) {
        Path directory = directory(catalogDirectory);
        String checkedDigest = RemoteCapabilityCatalogPublicationValues.sha256(
                digest, "catalogSha256");
        Path catalogPath = directory.resolve(catalogName(checkedDigest));
        byte[] bytes = bytes(catalogPath, checkedDigest);
        return new RemoteCapabilityCatalogBinding(
                catalogPath, checkedDigest, bytes, sha256(bytes));
    }

    static DecodeResult decode(Path catalogDirectory, byte[] input) {
        Path directory;
        try {
            directory = directory(catalogDirectory);
        } catch (RuntimeException exception) {
            return new Refused();
        }
        Objects.requireNonNull(input, "input must not be null");
        byte[] bytes = input.clone();
        if (bytes.length < 1 || bytes.length > MAX_BYTES) {
            return new Refused();
        }
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException exception) {
            return new Refused();
        }
        if (!text.endsWith("\n") || text.endsWith("\n\n")
                || text.indexOf('\r') >= 0 || text.indexOf('\0') >= 0
                || text.startsWith("\uFEFF")) {
            return new Refused();
        }
        String[] lines = text.split("\n", -1);
        if (lines.length != 4 || !HEADER.equals(lines[0])
                || !lines[1].startsWith("catalogPath=")
                || !lines[2].startsWith("catalogSha256=")
                || !lines[3].isEmpty()) {
            return new Refused();
        }
        String pathText = lines[1].substring("catalogPath=".length());
        String digest = lines[2].substring("catalogSha256=".length());
        if (pathText.isEmpty() || digest.isEmpty()) {
            return new Refused();
        }
        Path catalogPath;
        try {
            catalogPath = Path.of(pathText);
        } catch (InvalidPathException exception) {
            return new Refused();
        }
        if (!catalogPath.isAbsolute() || !catalogPath.equals(catalogPath.normalize())
                || !directory.equals(catalogPath.getParent())
                || !catalogName(digest).equals(catalogPath.getFileName().toString())) {
            return new Refused();
        }
        try {
            RemoteCapabilityCatalogPublicationValues.sha256(digest, "catalogSha256");
        } catch (RuntimeException exception) {
            return new Refused();
        }
        byte[] reencoded = bytes(catalogPath, digest);
        if (!Arrays.equals(bytes, reencoded)) {
            return new Refused();
        }
        return new Decoded(new RemoteCapabilityCatalogBinding(
                catalogPath, digest, bytes, sha256(bytes)));
    }

    private static Path directory(Path value) {
        Path directory = Objects.requireNonNull(value,
                "catalogDirectory must not be null");
        if (!directory.isAbsolute() || !directory.equals(directory.normalize())) {
            throw new IllegalArgumentException(
                    "catalogDirectory must be absolute and normalized");
        }
        return directory;
    }

    private static String catalogName(String digest) {
        return "enhancer-remote-capability-catalog-" + digest + ".conf";
    }

    private static byte[] bytes(Path catalogPath, String digest) {
        return (HEADER + "\n"
                + "catalogPath=" + catalogPath + "\n"
                + "catalogSha256=" + digest + "\n")
                .getBytes(StandardCharsets.UTF_8);
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    sealed interface DecodeResult permits Decoded, Refused {
    }

    record Decoded(RemoteCapabilityCatalogBinding binding) implements DecodeResult {
        Decoded {
            Objects.requireNonNull(binding, "binding must not be null");
        }
    }

    record Refused() implements DecodeResult {
    }
}
