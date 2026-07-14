package com.enhancer.tool;

import java.util.Objects;
import java.util.Optional;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.regex.Pattern;

public record VerificationEvidence(
        String summary,
        String outputTail,
        int originalOutputLength,
        boolean truncated,
        Optional<String> fullOutputReference,
        Optional<String> contentSha256) {

    public static final int MAX_SUMMARY_CHARACTERS = 512;
    public static final int MAX_OUTPUT_TAIL_CHARACTERS = 4096;
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    public VerificationEvidence {
        Objects.requireNonNull(summary, "summary must not be null");
        Objects.requireNonNull(outputTail, "outputTail must not be null");
        Objects.requireNonNull(fullOutputReference, "fullOutputReference must not be null");
        Objects.requireNonNull(contentSha256, "contentSha256 must not be null");

        if (summary.isBlank()) {
            throw new IllegalArgumentException("summary must not be blank");
        }
        if (summary.length() > MAX_SUMMARY_CHARACTERS) {
            throw new IllegalArgumentException(
                    "summary must not exceed " + MAX_SUMMARY_CHARACTERS + " characters");
        }
        if (outputTail.length() > MAX_OUTPUT_TAIL_CHARACTERS) {
            throw new IllegalArgumentException(
                    "outputTail must not exceed " + MAX_OUTPUT_TAIL_CHARACTERS + " characters");
        }
        if (originalOutputLength < outputTail.length()) {
            throw new IllegalArgumentException(
                    "originalOutputLength must not be shorter than outputTail");
        }

        boolean expectedTruncation = originalOutputLength > outputTail.length();
        if (truncated != expectedTruncation) {
            throw new IllegalArgumentException(
                    "truncated must match original output and tail lengths");
        }
        if (truncated && fullOutputReference.isEmpty()) {
            throw new IllegalArgumentException(
                    "truncated output requires a complete-output reference");
        }
        if (fullOutputReference.isPresent() && fullOutputReference.orElseThrow().isBlank()) {
            throw new IllegalArgumentException("fullOutputReference must not be blank");
        }
        if (contentSha256.isPresent()
                && !SHA_256.matcher(contentSha256.orElseThrow()).matches()) {
            throw new IllegalArgumentException(
                    "contentSha256 must be 64 lowercase hexadecimal characters");
        }
    }

    public VerificationEvidence(
            String summary,
            String outputTail,
            int originalOutputLength,
            boolean truncated,
            Optional<String> fullOutputReference) {
        this(
                summary,
                outputTail,
                originalOutputLength,
                truncated,
                fullOutputReference,
                Optional.empty());
    }

    public static VerificationEvidence capture(
            String summary,
            String fullOutput,
            Optional<String> fullOutputReference) {
        Objects.requireNonNull(fullOutput, "fullOutput must not be null");

        boolean truncated = fullOutput.length() > MAX_OUTPUT_TAIL_CHARACTERS;
        String outputTail = truncated
                ? fullOutput.substring(fullOutput.length() - MAX_OUTPUT_TAIL_CHARACTERS)
                : fullOutput;

        return new VerificationEvidence(
                summary,
                outputTail,
                fullOutput.length(),
                truncated,
                fullOutputReference,
                Optional.of(sha256(fullOutput)));
    }

    private static String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
