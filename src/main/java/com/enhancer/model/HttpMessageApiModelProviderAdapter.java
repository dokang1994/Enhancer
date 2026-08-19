package com.enhancer.model;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * The one concrete provider adapter shape RFC-0013 bounds but never executes.
 *
 * <p>It maps a {@link ModelRequest} to one remote HTTP message API and maps the
 * provider reply back into the bounded {@link ModelResponse}, so provider wire
 * vocabulary exists only inside this package-private class. No test, build, or
 * continuous-integration step constructs or invokes it; executing a real provider
 * requires its own explicit user authorization and accepted decision.
 */
final class HttpMessageApiModelProviderAdapter implements ModelGateway {
    private final URI endpoint;
    private final String providerModelName;
    private final ModelCredentialSupplier credentialSupplier;
    private final HttpClient httpClient;

    HttpMessageApiModelProviderAdapter(
            URI endpoint,
            String providerModelName,
            ModelCredentialSupplier credentialSupplier,
            HttpClient httpClient) {
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint must not be null");
        this.providerModelName = Objects.requireNonNull(
                providerModelName, "providerModelName must not be null");
        this.credentialSupplier = Objects.requireNonNull(
                credentialSupplier, "credentialSupplier must not be null");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        if (providerModelName.isBlank()) {
            throw new IllegalArgumentException("providerModelName must not be blank");
        }
    }

    @Override
    public ModelResponse invoke(ModelRequest request) throws ModelGatewayException {
        Objects.requireNonNull(request, "request must not be null");
        HttpRequest wireRequest = HttpRequest.newBuilder(endpoint)
                .timeout(request.timeout())
                .header("content-type", "application/json")
                .header("authorization", "Bearer " + credentialSupplier.credential())
                .POST(HttpRequest.BodyPublishers.ofString(
                        encodeRequestBody(request),
                        StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> wireResponse;
        try {
            wireResponse = httpClient.send(
                    wireRequest,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException exception) {
            throw new ModelGatewayException(
                    ModelFailureCode.TIMED_OUT,
                    "provider did not respond within " + request.timeout());
        } catch (IOException exception) {
            throw new ModelGatewayException(
                    ModelFailureCode.PROVIDER_UNAVAILABLE,
                    "provider connection failed");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ModelGatewayException(
                    ModelFailureCode.PROVIDER_UNAVAILABLE,
                    "provider invocation was interrupted");
        }
        if (wireResponse.statusCode() != 200) {
            throw new ModelGatewayException(
                    ModelFailureCode.PROVIDER_UNAVAILABLE,
                    "provider returned status " + wireResponse.statusCode());
        }
        return decodeResponseBody(wireResponse.body(), request);
    }

    /** Encodes the bounded request as the remote message API's JSON body. */
    String encodeRequestBody(ModelRequest request) {
        return "{\"model\":\"" + escapeJson(providerModelName)
                + "\",\"max_output_length\":" + request.maxResponseLength()
                + ",\"messages\":[{\"role\":\"user\",\"content\":\""
                + escapeJson(request.prompt()) + "\"}]}";
    }

    /**
     * Decodes the provider reply back into the bounded neutral response. The
     * request's model class, not the provider model name, labels the result, so
     * wire vocabulary stays inside this adapter.
     */
    ModelResponse decodeResponseBody(String body, ModelRequest request)
            throws ModelGatewayException {
        String text = unescapeJson(stringField(body, "content"));
        if (text.length() > request.maxResponseLength()) {
            throw new ModelGatewayException(
                    ModelFailureCode.BUDGET_EXCEEDED,
                    "provider response length " + text.length()
                            + " exceeds the declared budget " + request.maxResponseLength());
        }
        try {
            return new ModelResponse(
                    text,
                    request.modelClass(),
                    new ModelUsage(
                            numberField(body, "input_units"),
                            numberField(body, "output_units")));
        } catch (IllegalArgumentException exception) {
            throw new ModelGatewayException(
                    ModelFailureCode.RESPONSE_INVALID,
                    "provider response violates the bounded response contract");
        }
    }

    private static String stringField(String body, String name)
            throws ModelGatewayException {
        String marker = "\"" + name + "\":\"";
        int start = body.indexOf(marker);
        if (start < 0) {
            throw invalidResponse(name);
        }
        StringBuilder raw = new StringBuilder();
        boolean escaped = false;
        for (int index = start + marker.length(); index < body.length(); index++) {
            char character = body.charAt(index);
            if (escaped) {
                raw.append('\\').append(character);
                escaped = false;
            } else if (character == '\\') {
                escaped = true;
            } else if (character == '"') {
                return raw.toString();
            } else {
                raw.append(character);
            }
        }
        throw invalidResponse(name);
    }

    private static long numberField(String body, String name)
            throws ModelGatewayException {
        String marker = "\"" + name + "\":";
        int start = body.indexOf(marker);
        if (start < 0) {
            throw invalidResponse(name);
        }
        int index = start + marker.length();
        int end = index;
        while (end < body.length() && Character.isDigit(body.charAt(end))) {
            end++;
        }
        if (end == index) {
            throw invalidResponse(name);
        }
        try {
            return Long.parseLong(body.substring(index, end));
        } catch (NumberFormatException exception) {
            throw invalidResponse(name);
        }
    }

    private static ModelGatewayException invalidResponse(String field) {
        return new ModelGatewayException(
                ModelFailureCode.RESPONSE_INVALID,
                "provider response omits or malforms the required field: " + field);
    }

    private static String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private static String unescapeJson(String value) throws ModelGatewayException {
        StringBuilder text = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character != '\\') {
                text.append(character);
                continue;
            }
            index++;
            if (index >= value.length()) {
                throw invalidResponse("content");
            }
            char control = value.charAt(index);
            switch (control) {
                case '"' -> text.append('"');
                case '\\' -> text.append('\\');
                case '/' -> text.append('/');
                case 'n' -> text.append('\n');
                case 'r' -> text.append('\r');
                case 't' -> text.append('\t');
                case 'u' -> {
                    if (index + 4 >= value.length()) {
                        throw invalidResponse("content");
                    }
                    try {
                        text.append((char) Integer.parseInt(
                                value.substring(index + 1, index + 5), 16));
                    } catch (NumberFormatException exception) {
                        throw invalidResponse("content");
                    }
                    index += 4;
                }
                default -> throw invalidResponse("content");
            }
        }
        return text.toString();
    }
}
