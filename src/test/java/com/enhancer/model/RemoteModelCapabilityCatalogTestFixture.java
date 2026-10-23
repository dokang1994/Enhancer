package com.enhancer.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

final class RemoteModelCapabilityCatalogTestFixture {

    private RemoteModelCapabilityCatalogTestFixture() {
    }

    static byte[] zeroRecords() {
        return text(List.of()).getBytes(StandardCharsets.UTF_8);
    }

    static byte[] oneRecord() {
        return text(List.of(new Pair("candidate-a", "destination-a")))
                .getBytes(StandardCharsets.UTF_8);
    }

    static byte[] twoRecords(String firstCandidate, String firstDestination,
            String secondCandidate, String secondDestination) {
        return text(List.of(
                new Pair(firstCandidate, firstDestination),
                new Pair(secondCandidate, secondDestination)))
                .getBytes(StandardCharsets.UTF_8);
    }

    static byte[] maximumRecords() {
        List<Pair> pairs = new ArrayList<>();
        for (int index = 0; index < 256; index++) {
            pairs.add(new Pair("candidate-" + String.format("%03d", index),
                    "destination-a"));
        }
        return text(pairs).getBytes(StandardCharsets.UTF_8);
    }

    static byte[] replace(byte[] source, String target, String replacement) {
        return new String(source, StandardCharsets.UTF_8)
                .replace(target, replacement)
                .getBytes(StandardCharsets.UTF_8);
    }

    static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private static String text(List<Pair> pairs) {
        StringBuilder output = new StringBuilder();
        append(output, "enhancer-remote-model-candidate-capability-catalog-v1");
        append(output, "catalogIdentity=local-capability-catalog");
        append(output, "recordCount=" + pairs.size());
        for (int index = 0; index < pairs.size(); index++) {
            Pair pair = pairs.get(index);
            String prefix = "record." + index + ".";
            append(output, prefix + "schemaVersion="
                    + RemoteModelCandidateCapabilityFacts.SCHEMA_VERSION);
            append(output, prefix + "capacityEvidenceIdentity=capacity-evidence-a");
            append(output, prefix + "observedAt=2026-10-23T00:00:00Z");
            append(output, prefix + "expiresAt=2026-10-23T00:05:00Z");
            append(output, prefix + "candidateIdentity=" + pair.candidate());
            append(output, prefix + "destinationIdentity=" + pair.destination());
            append(output, prefix + "modelClass=reasoning-standard");
            append(output, prefix + "requiredCapability=repository-analysis");
            append(output, prefix + "tokenSemanticsIdentity=profile-token-budget-v1");
            append(output, prefix + "priceScheduleIdentity=schedule-a");
            append(output, prefix + "tokenBudgetBasis.maxInputTokens=10");
            append(output, prefix + "tokenBudgetBasis.maxOutputTokens=5");
            append(output, prefix + "tokenBudgetBasis.maxTotalTokens=15");
            append(output, prefix + "maximumReasoningRequirement=EXTENDED");
            append(output, prefix + "maximumContextTokens=1000");
            append(output, prefix + "maximumTokenBudget.maxInputTokens=100");
            append(output, prefix + "maximumTokenBudget.maxOutputTokens=50");
            append(output, prefix + "maximumTokenBudget.maxTotalTokens=150");
            append(output, prefix + "maximumInvocationTimeMillis=60000");
            append(output, prefix + "costCurrencyCode=USD");
            append(output, prefix + "maximumCostMicrounits=987654321");
            append(output, prefix + "maximumDataClassification=RESTRICTED");
        }
        return output.toString();
    }

    private static void append(StringBuilder output, String line) {
        output.append(line).append('\n');
    }

    private record Pair(String candidate, String destination) {
    }
}
