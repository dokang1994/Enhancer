package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class DeterministicFakeModelGatewayTest {

    @Test
    void respondsAsAPureFunctionOfTheRequest() throws Exception {
        ModelRequest request = new ModelRequest(
                "correlation-1",
                "Explain the evidence envelope.",
                "reasoning-standard",
                Duration.ofSeconds(2),
                4096);

        ModelResponse response = new DeterministicFakeModelGateway().invoke(request);

        assertTrue(response.text().startsWith("deterministic-fake-v1\n"));
        assertTrue(response.text().contains(
                "model-class=reasoning-standard\n"));
        assertTrue(response.text().contains(
                "prompt-sha256=" + sha256("Explain the evidence envelope.") + "\n"));
        assertTrue(response.text().contains(
                "prompt-length=" + "Explain the evidence envelope.".length() + "\n"));
        assertTrue(response.text().endsWith(
                "echo=Explain the evidence envelope."));
        assertEquals("reasoning-standard", response.modelClass());
        assertEquals("Explain the evidence envelope.".length(), response.usage().inputUnits());
        assertEquals(response.text().length(), response.usage().outputUnits());
    }

    @Test
    void identicalRequestsAlwaysProduceIdenticalResponses() throws Exception {
        ModelRequest request = new ModelRequest(
                "correlation-1",
                "same prompt",
                "model-class",
                Duration.ofSeconds(1),
                2048);

        ModelResponse first = new DeterministicFakeModelGateway().invoke(request);
        ModelResponse second = new DeterministicFakeModelGateway().invoke(request);

        assertEquals(first, second);
    }

    @Test
    void differentPromptsOrModelClassesChangeTheResponse() throws Exception {
        DeterministicFakeModelGateway gateway = new DeterministicFakeModelGateway();
        ModelResponse base = gateway.invoke(new ModelRequest(
                "correlation-1", "prompt-a", "model-class", Duration.ofSeconds(1), 2048));

        assertTrue(!base.equals(gateway.invoke(new ModelRequest(
                "correlation-1", "prompt-b", "model-class", Duration.ofSeconds(1), 2048))));
        assertTrue(!base.equals(gateway.invoke(new ModelRequest(
                "correlation-1", "prompt-a", "other-class", Duration.ofSeconds(1), 2048))));
    }

    @Test
    void refusesAResponseExceedingTheDeclaredLengthBudget() {
        ModelRequest request = new ModelRequest(
                "correlation-1",
                "a prompt whose deterministic response cannot fit",
                "model-class",
                Duration.ofSeconds(1),
                16);

        ModelGatewayException exception = assertThrows(
                ModelGatewayException.class,
                () -> new DeterministicFakeModelGateway().invoke(request));

        assertEquals(ModelFailureCode.BUDGET_EXCEEDED, exception.code());
    }

    @Test
    void rejectsAMissingRequestBeforeInvocation() {
        assertThrows(
                NullPointerException.class,
                () -> new DeterministicFakeModelGateway().invoke(null));
    }

    private String sha256(String content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(content.getBytes(StandardCharsets.UTF_8)));
    }
}
