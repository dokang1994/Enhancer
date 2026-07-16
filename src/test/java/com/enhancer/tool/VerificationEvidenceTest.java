package com.enhancer.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class VerificationEvidenceTest {
    @Test
    void preservesShortOutputWithoutAReference() {
        String output = "compile passed\n17 tests passed";

        VerificationEvidence evidence = VerificationEvidence.capture(
                "Build verification passed",
                output,
                Optional.empty());

        assertEquals("Build verification passed", evidence.summary());
        assertEquals(output, evidence.outputTail());
        assertEquals(output.length(), evidence.originalOutputLength());
        assertFalse(evidence.truncated());
        assertTrue(evidence.fullOutputReference().isEmpty());
        assertEquals(64, evidence.contentSha256().orElseThrow().length());
    }

    @Test
    void retainsTheFinalOutputTailAndCompleteOutputReference() {
        String expectedTail = "x".repeat(VerificationEvidence.MAX_OUTPUT_TAIL_CHARACTERS);
        String output = "discarded-prefix" + expectedTail;

        VerificationEvidence evidence = VerificationEvidence.capture(
                "Build failed",
                output,
                Optional.of("evidence/build-123.log"));

        assertEquals(expectedTail, evidence.outputTail());
        assertEquals(output.length(), evidence.originalOutputLength());
        assertTrue(evidence.truncated());
        assertEquals("evidence/build-123.log", evidence.fullOutputReference().orElseThrow());
    }

    @Test
    void doesNotSplitASupplementaryCharacterAtTheTailBoundary()
            throws Exception {
        String output = "\uD83D\uDE80"
                + "x".repeat(
                        VerificationEvidence.MAX_OUTPUT_TAIL_CHARACTERS - 1);

        VerificationEvidence evidence = VerificationEvidence.capture(
                "Build output",
                output,
                Optional.of("evidence/run/unicode"));

        assertEquals(
                VerificationEvidence.MAX_OUTPUT_TAIL_CHARACTERS - 1,
                evidence.outputTail().length());
        assertTrue(evidence.outputTail().chars().allMatch(value -> value == 'x'));
        StandardCharsets.UTF_8
                .newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .encode(java.nio.CharBuffer.wrap(evidence.outputTail()));
    }

    @Test
    void contentIdentityDoesNotDependOnItsStorageReference() {
        String output = "same-content".repeat(500);

        VerificationEvidence first = VerificationEvidence.capture(
                "first summary",
                output,
                Optional.of("evidence/run/first"));
        VerificationEvidence second = VerificationEvidence.capture(
                "second summary",
                output,
                Optional.of("evidence/run/second"));

        assertEquals(first.contentSha256(), second.contentSha256());
    }

    @Test
    void rejectsTruncatedOutputWithoutACompleteOutputReference() {
        String output = "x".repeat(VerificationEvidence.MAX_OUTPUT_TAIL_CHARACTERS + 1);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> VerificationEvidence.capture("Build failed", output, Optional.empty()));

        assertTrue(exception.getMessage().contains("reference"));
        assertThrows(
                IllegalArgumentException.class,
                () -> VerificationEvidence.capture("Build failed", output, Optional.of(" ")));
    }

    @Test
    void rejectsUnboundedOrContradictoryEvidence() {
        assertThrows(
                IllegalArgumentException.class,
                () -> VerificationEvidence.capture(" ", "output", Optional.empty()));
        assertThrows(
                IllegalArgumentException.class,
                () -> VerificationEvidence.capture(
                        "s".repeat(VerificationEvidence.MAX_SUMMARY_CHARACTERS + 1),
                        "output",
                        Optional.empty()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VerificationEvidence(
                        "summary",
                        "tail",
                        10,
                        false,
                        Optional.empty()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VerificationEvidence(
                        "summary",
                        "x".repeat(VerificationEvidence.MAX_OUTPUT_TAIL_CHARACTERS + 1),
                        VerificationEvidence.MAX_OUTPUT_TAIL_CHARACTERS + 1,
                        false,
                        Optional.empty()));
    }
}
