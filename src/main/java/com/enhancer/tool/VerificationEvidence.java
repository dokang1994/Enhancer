package com.enhancer.tool;

import java.util.Objects;
import java.util.Optional;

public record VerificationEvidence(
        String summary,
        String outputTail,
        int originalOutputLength,
        boolean truncated,
        Optional<String> fullOutputReference) {

    public static final int MAX_SUMMARY_CHARACTERS = 512;
    public static final int MAX_OUTPUT_TAIL_CHARACTERS = 4096;

    public VerificationEvidence {
        Objects.requireNonNull(summary, "summary must not be null");
        Objects.requireNonNull(outputTail, "outputTail must not be null");
        Objects.requireNonNull(fullOutputReference, "fullOutputReference must not be null");

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
                fullOutputReference);
    }
}
