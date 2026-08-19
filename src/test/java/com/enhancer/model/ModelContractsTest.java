package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ModelContractsTest {

    @Test
    void requestRetainsItsExactBoundedFields() {
        ModelRequest request = new ModelRequest(
                "correlation-1",
                "Summarize the repository constitution.",
                "reasoning-standard",
                Duration.ofSeconds(2),
                4096);

        assertEquals("correlation-1", request.correlationId());
        assertEquals("Summarize the repository constitution.", request.prompt());
        assertEquals("reasoning-standard", request.modelClass());
        assertEquals(Duration.ofSeconds(2), request.timeout());
        assertEquals(4096, request.maxResponseLength());
    }

    @Test
    void requestRejectsMissingOrBlankFields() {
        assertThrows(NullPointerException.class, () -> new ModelRequest(
                null, "prompt", "model-class", Duration.ofSeconds(1), 16));
        assertThrows(IllegalArgumentException.class, () -> new ModelRequest(
                " ", "prompt", "model-class", Duration.ofSeconds(1), 16));
        assertThrows(NullPointerException.class, () -> new ModelRequest(
                "correlation-1", null, "model-class", Duration.ofSeconds(1), 16));
        assertThrows(IllegalArgumentException.class, () -> new ModelRequest(
                "correlation-1", " ", "model-class", Duration.ofSeconds(1), 16));
        assertThrows(NullPointerException.class, () -> new ModelRequest(
                "correlation-1", "prompt", null, Duration.ofSeconds(1), 16));
        assertThrows(NullPointerException.class, () -> new ModelRequest(
                "correlation-1", "prompt", "model-class", null, 16));
    }

    @Test
    void requestRejectsUnstableModelClassLabels() {
        for (String modelClass : new String[] {
                "Claude-Model", "claude_model", "claude model", "-leading",
                "trailing-", "provider/model", "a".repeat(
                        ModelRequest.MAX_MODEL_CLASS_CHARACTERS + 1)}) {
            assertThrows(IllegalArgumentException.class, () -> new ModelRequest(
                    "correlation-1", "prompt", modelClass, Duration.ofSeconds(1), 16));
        }
    }

    @Test
    void requestBoundsPromptCorrelationTimeoutAndResponseLength() {
        assertThrows(IllegalArgumentException.class, () -> new ModelRequest(
                "c".repeat(ModelRequest.MAX_CORRELATION_ID_CHARACTERS + 1),
                "prompt", "model-class", Duration.ofSeconds(1), 16));
        assertThrows(IllegalArgumentException.class, () -> new ModelRequest(
                "correlation-1",
                "p".repeat(ModelRequest.MAX_PROMPT_CHARACTERS + 1),
                "model-class", Duration.ofSeconds(1), 16));
        assertThrows(IllegalArgumentException.class, () -> new ModelRequest(
                "correlation-1", "prompt", "model-class", Duration.ZERO, 16));
        assertThrows(IllegalArgumentException.class, () -> new ModelRequest(
                "correlation-1", "prompt", "model-class", Duration.ofSeconds(-1), 16));
        assertThrows(IllegalArgumentException.class, () -> new ModelRequest(
                "correlation-1", "prompt", "model-class",
                ModelRequest.MAX_TIMEOUT.plusMillis(1), 16));
        assertThrows(IllegalArgumentException.class, () -> new ModelRequest(
                "correlation-1", "prompt", "model-class", Duration.ofSeconds(1), 0));
        assertThrows(IllegalArgumentException.class, () -> new ModelRequest(
                "correlation-1", "prompt", "model-class", Duration.ofSeconds(1),
                ModelRequest.MAX_RESPONSE_LENGTH + 1));
    }

    @Test
    void responseRetainsBoundedTextModelClassAndUsage() {
        ModelResponse response = new ModelResponse(
                "bounded response",
                "reasoning-standard",
                new ModelUsage(10, 16));

        assertEquals("bounded response", response.text());
        assertEquals("reasoning-standard", response.modelClass());
        assertEquals(10, response.usage().inputUnits());
        assertEquals(16, response.usage().outputUnits());
    }

    @Test
    void responseRejectsMissingOversizedOrUnstableFields() {
        ModelUsage usage = new ModelUsage(1, 1);
        assertThrows(NullPointerException.class, () -> new ModelResponse(
                null, "model-class", usage));
        assertThrows(IllegalArgumentException.class, () -> new ModelResponse(
                "t".repeat(ModelResponse.MAX_TEXT_CHARACTERS + 1), "model-class", usage));
        assertThrows(NullPointerException.class, () -> new ModelResponse(
                "text", null, usage));
        assertThrows(IllegalArgumentException.class, () -> new ModelResponse(
                "text", "Unstable Label", usage));
        assertThrows(NullPointerException.class, () -> new ModelResponse(
                "text", "model-class", null));
    }

    @Test
    void usageRejectsNegativeAndOversizedUnitCounts() {
        assertThrows(IllegalArgumentException.class, () -> new ModelUsage(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new ModelUsage(0, -1));
        assertThrows(IllegalArgumentException.class, () -> new ModelUsage(
                ModelUsage.MAX_UNITS + 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new ModelUsage(
                0, ModelUsage.MAX_UNITS + 1));
        assertEquals(0, new ModelUsage(0, 0).inputUnits());
    }

    @Test
    void gatewayExceptionCarriesOneTypedCodeAndBoundedReason() {
        ModelGatewayException exception = new ModelGatewayException(
                ModelFailureCode.BUDGET_EXCEEDED,
                "response exceeds the declared budget");

        assertEquals(ModelFailureCode.BUDGET_EXCEEDED, exception.code());
        assertEquals("response exceeds the declared budget", exception.getMessage());
        assertThrows(NullPointerException.class, () -> new ModelGatewayException(
                null, "reason"));
        assertThrows(NullPointerException.class, () -> new ModelGatewayException(
                ModelFailureCode.TIMED_OUT, null));
        assertThrows(IllegalArgumentException.class, () -> new ModelGatewayException(
                ModelFailureCode.TIMED_OUT, " "));
        assertThrows(IllegalArgumentException.class, () -> new ModelGatewayException(
                ModelFailureCode.TIMED_OUT,
                "r".repeat(ModelGatewayException.MAX_REASON_CHARACTERS + 1)));
    }

    @Test
    void failureCodesStayExactlyTheFourSpecifiedConditions() {
        assertEquals(4, ModelFailureCode.values().length);
        for (String name : new String[] {
                "PROVIDER_UNAVAILABLE", "RESPONSE_INVALID", "BUDGET_EXCEEDED", "TIMED_OUT"}) {
            assertEquals(name, ModelFailureCode.valueOf(name).name());
        }
    }
}
