package com.enhancer.model;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** Pure complete codec for the canonical RFC-0031 capability catalog. */
final class RemoteModelCapabilityCatalogCodec {
    private static final String HEADER =
            "enhancer-remote-model-candidate-capability-catalog-v1";
    private static final Pattern LABEL = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");
    private static final Pattern INTEGER = Pattern.compile("0|[1-9][0-9]*");
    private static final Pattern CURRENCY = Pattern.compile("[A-Z]{3}");

    private RemoteModelCapabilityCatalogCodec() {
    }

    static DecodeResult decode(byte[] input) {
        Objects.requireNonNull(input, "input must not be null");
        byte[] bytes = input.clone();
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException exception) {
            return new Refused(Failure.INVALID_UTF8);
        }
        try {
            Catalog catalog = parse(text);
            if (!Arrays.equals(bytes, encode(catalog))) {
                return new Refused(Failure.NOT_CANONICAL);
            }
            return new Decoded(catalog);
        } catch (CatalogFailure failure) {
            return new Refused(failure.failure);
        }
    }

    static byte[] encode(Catalog catalog) {
        Objects.requireNonNull(catalog, "catalog must not be null");
        StringBuilder output = new StringBuilder();
        append(output, HEADER);
        append(output, "catalogIdentity=" + catalog.identity());
        append(output, "recordCount=" + catalog.records().size());
        for (int index = 0; index < catalog.records().size(); index++) {
            CapabilityRecord record = catalog.records().get(index);
            String prefix = "record." + index + ".";
            append(output, prefix + "schemaVersion="
                    + RemoteModelCandidateCapabilityFacts.SCHEMA_VERSION);
            append(output, prefix + "capacityEvidenceIdentity=" + record.evidence());
            append(output, prefix + "observedAt=" + record.observed());
            append(output, prefix + "expiresAt=" + record.expires());
            append(output, prefix + "candidateIdentity=" + record.candidate());
            append(output, prefix + "destinationIdentity=" + record.destination());
            append(output, prefix + "modelClass=" + record.modelClass());
            append(output, prefix + "requiredCapability=" + record.capability());
            append(output, prefix + "tokenSemanticsIdentity=profile-token-budget-v1");
            append(output, prefix + "priceScheduleIdentity=" + record.schedule());
            append(output, prefix + "tokenBudgetBasis.maxInputTokens="
                    + record.basisInput());
            append(output, prefix + "tokenBudgetBasis.maxOutputTokens="
                    + record.basisOutput());
            append(output, prefix + "tokenBudgetBasis.maxTotalTokens="
                    + record.basisTotal());
            append(output, prefix + "maximumReasoningRequirement=" + record.reasoning());
            append(output, prefix + "maximumContextTokens=" + record.context());
            append(output, prefix + "maximumTokenBudget.maxInputTokens="
                    + record.maximumInput());
            append(output, prefix + "maximumTokenBudget.maxOutputTokens="
                    + record.maximumOutput());
            append(output, prefix + "maximumTokenBudget.maxTotalTokens="
                    + record.maximumTotal());
            append(output, prefix + "maximumInvocationTimeMillis=" + record.millis());
            append(output, prefix + "costCurrencyCode=" + record.currency());
            append(output, prefix + "maximumCostMicrounits=" + record.cost());
            append(output, prefix + "maximumDataClassification="
                    + record.classification());
        }
        return output.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static Catalog parse(String text) throws CatalogFailure {
        if (!text.endsWith("\n") || text.endsWith("\n\n")
                || text.indexOf('\r') >= 0 || text.indexOf('\0') >= 0
                || text.startsWith("\uFEFF")) {
            throw canonical();
        }
        String[] split = text.split("\n", -1);
        List<String> lines = Arrays.asList(split).subList(0, split.length - 1);
        if (lines.isEmpty() || lines.stream().anyMatch(String::isEmpty)) {
            throw canonical();
        }
        String header = lines.get(0);
        if (!HEADER.equals(header)) {
            if (header.startsWith(
                    "enhancer-remote-model-candidate-capability-catalog-v")) {
                throw schema();
            }
            throw canonical();
        }
        Cursor cursor = new Cursor(lines);
        cursor.expect(HEADER);
        String catalogIdentity = cursor.value("catalogIdentity");
        contentLabel(catalogIdentity, 256);
        BigInteger countValue = integer(cursor.value("recordCount"));
        if (countValue.compareTo(BigInteger.valueOf(256)) > 0) {
            throw content();
        }
        int count = countValue.intValue();
        List<CapabilityRecord> records = new ArrayList<>(count);
        CapabilityRecord previous = null;
        for (int index = 0; index < count; index++) {
            CapabilityRecord current = record(cursor, index);
            if (previous != null && previous.compareTo(current) >= 0) {
                throw content();
            }
            records.add(current);
            previous = current;
        }
        cursor.end();
        return new Catalog(catalogIdentity, records);
    }

    private static CapabilityRecord record(Cursor cursor, int index)
            throws CatalogFailure {
        String prefix = "record." + index + ".";
        if (!RemoteModelCandidateCapabilityFacts.SCHEMA_VERSION.equals(
                cursor.value(prefix + "schemaVersion"))) {
            throw schema();
        }
        String evidence = cursor.value(prefix + "capacityEvidenceIdentity");
        contentLabel(evidence, 256);
        Instant observed = instant(cursor.value(prefix + "observedAt"));
        Instant expires = instant(cursor.value(prefix + "expiresAt"));
        String candidate = cursor.value(prefix + "candidateIdentity");
        String destination = cursor.value(prefix + "destinationIdentity");
        contentLabel(candidate, 256);
        contentLabel(destination, 256);
        String modelClass = cursor.value(prefix + "modelClass");
        String capability = cursor.value(prefix + "requiredCapability");
        contentLabel(modelClass, 64);
        contentLabel(capability, 256);
        if (!"profile-token-budget-v1".equals(
                cursor.value(prefix + "tokenSemanticsIdentity"))) {
            throw content();
        }
        String schedule = cursor.value(prefix + "priceScheduleIdentity");
        contentLabel(schedule, 256);
        long basisInput = longValue(cursor.value(
                prefix + "tokenBudgetBasis.maxInputTokens"));
        long basisOutput = longValue(cursor.value(
                prefix + "tokenBudgetBasis.maxOutputTokens"));
        long basisTotal = longValue(cursor.value(
                prefix + "tokenBudgetBasis.maxTotalTokens"));
        String reasoning = cursor.value(prefix + "maximumReasoningRequirement");
        long context = longValue(cursor.value(prefix + "maximumContextTokens"));
        long maximumInput = longValue(cursor.value(
                prefix + "maximumTokenBudget.maxInputTokens"));
        long maximumOutput = longValue(cursor.value(
                prefix + "maximumTokenBudget.maxOutputTokens"));
        long maximumTotal = longValue(cursor.value(
                prefix + "maximumTokenBudget.maxTotalTokens"));
        long millis = longValue(cursor.value(prefix + "maximumInvocationTimeMillis"));
        String currency = cursor.value(prefix + "costCurrencyCode");
        long cost = longValue(cursor.value(prefix + "maximumCostMicrounits"));
        String classification = cursor.value(prefix + "maximumDataClassification");

        positive(basisInput, 1_000_000_000L);
        positive(basisOutput, 1_000_000_000L);
        positive(basisTotal, 1_000_000_000L);
        positive(context, 1_000_000_000L);
        positive(maximumInput, 1_000_000_000L);
        positive(maximumOutput, 1_000_000_000L);
        positive(maximumTotal, 1_000_000_000L);
        positive(millis, 300_000L);
        if (!observed.isBefore(expires)
                || !enumValue(ModelReasoningRequirement.class, reasoning)
                || !enumValue(ModelDataClassification.class, classification)
                || !CURRENCY.matcher(currency).matches()
                || cost < 0 || cost > 1_000_000_000_000_000L
                || basisInput > basisTotal - basisOutput
                || maximumInput > maximumTotal - maximumOutput
                || basisTotal > context || maximumTotal > context
                || basisInput > maximumInput || basisOutput > maximumOutput
                || basisTotal > maximumTotal) {
            throw content();
        }
        return new CapabilityRecord(evidence, observed, expires, candidate,
                destination, modelClass, capability, schedule, basisInput,
                basisOutput, basisTotal, reasoning, context, maximumInput,
                maximumOutput, maximumTotal, millis, currency, cost, classification);
    }

    private static BigInteger integer(String value) throws CatalogFailure {
        if (!INTEGER.matcher(value).matches()) {
            throw canonical();
        }
        return new BigInteger(value);
    }

    private static long longValue(String value) throws CatalogFailure {
        BigInteger parsed = integer(value);
        if (parsed.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
            throw content();
        }
        return parsed.longValue();
    }

    private static Instant instant(String value) throws CatalogFailure {
        try {
            Instant parsed = Instant.parse(value);
            if (!parsed.toString().equals(value)) {
                throw canonical();
            }
            return parsed;
        } catch (DateTimeParseException exception) {
            throw canonical();
        }
    }

    private static void contentLabel(String value, int maximum) throws CatalogFailure {
        if (value.length() > maximum || !LABEL.matcher(value).matches()) {
            throw content();
        }
    }

    private static void positive(long value, long maximum) throws CatalogFailure {
        if (value < 1 || value > maximum) {
            throw content();
        }
    }

    private static <E extends Enum<E>> boolean enumValue(Class<E> type, String value) {
        try {
            Enum.valueOf(type, value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static void append(StringBuilder output, String line) {
        output.append(line).append('\n');
    }

    private static CatalogFailure canonical() {
        return new CatalogFailure(Failure.NOT_CANONICAL);
    }

    private static CatalogFailure schema() {
        return new CatalogFailure(Failure.SCHEMA_UNSUPPORTED);
    }

    private static CatalogFailure content() {
        return new CatalogFailure(Failure.CONTENT_INVALID);
    }

    sealed interface DecodeResult permits Decoded, Refused {
    }

    record Decoded(Catalog catalog) implements DecodeResult {
        Decoded {
            Objects.requireNonNull(catalog, "catalog must not be null");
        }
    }

    record Refused(Failure failure) implements DecodeResult {
        Refused {
            Objects.requireNonNull(failure, "failure must not be null");
        }
    }

    enum Failure {
        INVALID_UTF8,
        SCHEMA_UNSUPPORTED,
        NOT_CANONICAL,
        CONTENT_INVALID
    }

    record Catalog(String identity, List<CapabilityRecord> records) {
        Catalog {
            Objects.requireNonNull(identity, "identity must not be null");
            records = List.copyOf(records);
        }

        @Override
        public String toString() {
            return "Catalog[identity=" + identity + ", records=<redacted>]";
        }
    }

    record CapabilityRecord(
            String evidence,
            Instant observed,
            Instant expires,
            String candidate,
            String destination,
            String modelClass,
            String capability,
            String schedule,
            long basisInput,
            long basisOutput,
            long basisTotal,
            String reasoning,
            long context,
            long maximumInput,
            long maximumOutput,
            long maximumTotal,
            long millis,
            String currency,
            long cost,
            String classification) implements Comparable<CapabilityRecord> {
        @Override
        public int compareTo(CapabilityRecord other) {
            int compared = candidate.compareTo(other.candidate);
            return compared != 0 ? compared : destination.compareTo(other.destination);
        }

        @Override
        public String toString() {
            return "CapabilityRecord[candidate=" + candidate
                    + ", destination=" + destination + ", values=<redacted>]";
        }
    }

    private static final class Cursor {
        private final List<String> lines;
        private int index;

        private Cursor(List<String> lines) {
            this.lines = lines;
        }

        private void expect(String expected) throws CatalogFailure {
            if (index >= lines.size() || !lines.get(index++).equals(expected)) {
                throw canonical();
            }
        }

        private String value(String field) throws CatalogFailure {
            String prefix = field + "=";
            if (index >= lines.size() || !lines.get(index).startsWith(prefix)) {
                throw canonical();
            }
            String value = lines.get(index++).substring(prefix.length());
            if (value.isEmpty()) {
                throw canonical();
            }
            return value;
        }

        private void end() throws CatalogFailure {
            if (index != lines.size()) {
                throw canonical();
            }
        }
    }

    private static final class CatalogFailure extends java.lang.Exception {
        private static final long serialVersionUID = 1L;
        private final Failure failure;

        private CatalogFailure(Failure failure) {
            this.failure = failure;
        }
    }
}
