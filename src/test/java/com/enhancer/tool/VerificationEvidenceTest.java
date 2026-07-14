package com.enhancer.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
