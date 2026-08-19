package com.enhancer.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * The only executed gateway in the RFC-0013 minimum slice.
 *
 * <p>The response is a pure function of the request: no clock, randomness,
 * credential, network, or filesystem state participates, so persisted evidence
 * digests remain reproducible across hosts and runs.
 */
public final class DeterministicFakeModelGateway implements ModelGateway {

    @Override
    public ModelResponse invoke(ModelRequest request) throws ModelGatewayException {
        Objects.requireNonNull(request, "request must not be null");

        String text = "deterministic-fake-v1\n"
                + "model-class=" + request.modelClass() + "\n"
                + "prompt-sha256=" + sha256(request.prompt()) + "\n"
                + "prompt-length=" + request.prompt().length() + "\n"
                + "echo=" + request.prompt();
        if (text.length() > request.maxResponseLength()) {
            throw new ModelGatewayException(
                    ModelFailureCode.BUDGET_EXCEEDED,
                    "deterministic response length " + text.length()
                            + " exceeds the declared budget " + request.maxResponseLength());
        }
        return new ModelResponse(
                text,
                request.modelClass(),
                new ModelUsage(request.prompt().length(), text.length()));
    }

    private static String sha256(String content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
