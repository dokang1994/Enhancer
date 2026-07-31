package com.enhancer.runtime;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

final class RuntimeEventContractSupport {
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    private RuntimeEventContractSupport() {
    }

    static String bounded(String value, String name, int maximumCharacters) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        if (value.length() > maximumCharacters) {
            throw new IllegalArgumentException(
                    name + " must not exceed " + maximumCharacters + " characters");
        }
        try {
            StandardCharsets.UTF_8
                    .newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(java.nio.CharBuffer.wrap(value));
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException(
                    name + " must be valid Unicode text", exception);
        }
        return value;
    }

    static String sha256(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (!SHA_256.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    name + " must be 64 lowercase hexadecimal characters");
        }
        return value;
    }

    static Optional<String> optionalSha256(
            Optional<String> value,
            String name) {
        Objects.requireNonNull(value, name + " must not be null");
        return value.map(item -> sha256(item, name));
    }

    static void putFramed(
            java.security.MessageDigest digest,
            String value) {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES)
                .putInt(encoded.length)
                .array());
        digest.update(encoded);
    }
}
