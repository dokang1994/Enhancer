package com.enhancer.model;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** Acquires remote-model capability facts from one locally pinned catalog file. */
public final class PinnedFileRemoteModelCandidateCapabilityFactSource {
    private static final int MAX_BYTES = 4 * 1024 * 1024;
    private static final String HEADER =
            "enhancer-remote-model-candidate-capability-catalog-v1";
    private static final Pattern LABEL = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");
    private static final Pattern DIGEST = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern INTEGER = Pattern.compile("0|[1-9][0-9]*");
    private static final Pattern CURRENCY = Pattern.compile("[A-Z]{3}");

    private final Path catalogFile;
    private final byte[] expectedDigest;

    public PinnedFileRemoteModelCandidateCapabilityFactSource(
            Path catalogFile, String expectedSha256) {
        Path checked = Objects.requireNonNull(catalogFile, "catalogFile must not be null");
        if (!checked.isAbsolute() || !checked.equals(checked.normalize())) {
            throw new IllegalArgumentException("catalogFile must be absolute and normalized");
        }
        Objects.requireNonNull(expectedSha256, "expectedSha256 must not be null");
        if (!DIGEST.matcher(expectedSha256).matches()) {
            throw new IllegalArgumentException(
                    "expectedSha256 must be a lowercase SHA-256 digest");
        }
        this.catalogFile = checked;
        this.expectedDigest = HexFormat.of().parseHex(expectedSha256);
    }

    public RemoteModelCandidateCapabilityFactSourceResult acquire(
            String candidateIdentity, String destinationIdentity, Instant evaluationTime) {
        programmingLabel(candidateIdentity, "candidateIdentity");
        programmingLabel(destinationIdentity, "destinationIdentity");
        Objects.requireNonNull(evaluationTime, "evaluationTime must not be null");

        BasicFileAttributes before;
        try {
            Path parent = catalogFile.getParent();
            if (parent == null
                    || !Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)
                    || !parent.toRealPath().equals(parent)
                    || Files.isSymbolicLink(catalogFile)
                    || !Files.isRegularFile(catalogFile, LinkOption.NOFOLLOW_LINKS)
                    || !catalogFile.toRealPath().equals(catalogFile)) {
                return refusal(Reason.UNSAFE);
            }
            before = Files.readAttributes(catalogFile, BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
        } catch (IOException | SecurityException exception) {
            return refusal(Reason.UNSAFE);
        }
        if (before.size() < 1 || before.size() > MAX_BYTES) {
            return refusal(Reason.SIZE);
        }

        byte[] bytes;
        try (FileChannel channel = FileChannel.open(catalogFile,
                StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            long size = channel.size();
            if (size < 1 || size > MAX_BYTES) {
                return refusal(Reason.SIZE);
            }
            if (size != before.size()) {
                return refusal(Reason.CHANGED);
            }
            bytes = readDeclaredBytes(channel, size);
            BasicFileAttributes after = Files.readAttributes(catalogFile,
                    BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (before.size() != after.size()
                    || !Objects.equals(before.fileKey(), after.fileKey())
                    || !before.creationTime().equals(after.creationTime())
                    || !before.lastModifiedTime().equals(after.lastModifiedTime())
                    || Files.isSymbolicLink(catalogFile)
                    || !catalogFile.toRealPath().equals(catalogFile)) {
                return refusal(Reason.CHANGED);
            }
        } catch (IOException | SecurityException exception) {
            return refusal(Reason.CHANGED);
        }
        if (!MessageDigest.isEqual(expectedDigest, sha256(bytes))) {
            return refusal(Reason.PIN);
        }

        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException exception) {
            return refusal(Reason.UTF8);
        }
        Catalog catalog;
        try {
            catalog = parse(text);
        } catch (CatalogFailure failure) {
            return new RemoteModelCandidateCapabilityFactSourceResult.Refused(
                    failure.reason);
        }
        CapabilityRecord selected = catalog.records.stream()
                .filter(record -> record.candidate.equals(candidateIdentity)
                        && record.destination.equals(destinationIdentity))
                .findFirst().orElse(null);
        if (selected == null) {
            return new RemoteModelCandidateCapabilityFactSourceResult.Refused(
                    RemoteModelCandidateCapabilityFactSourceRefusalReason.RECORD_NOT_FOUND);
        }
        if (evaluationTime.isBefore(selected.observed)
                || !evaluationTime.isBefore(selected.expires)) {
            return new RemoteModelCandidateCapabilityFactSourceResult.Refused(
                    RemoteModelCandidateCapabilityFactSourceRefusalReason.RECORD_STALE);
        }
        RemoteModelCandidateCapabilityFacts facts =
                RemoteModelCandidateCapabilityFacts.create(
                        RemoteModelCandidateCapabilityFacts.SCHEMA_VERSION,
                        catalog.identity,
                        HexFormat.of().formatHex(expectedDigest),
                        selected.evidence,
                        selected.observed,
                        selected.expires,
                        selected.candidate,
                        selected.destination,
                        selected.modelClass,
                        selected.capability,
                        "profile-token-budget-v1",
                        selected.schedule,
                        new ModelTokenBudget(selected.basisInput, selected.basisOutput,
                                selected.basisTotal),
                        ModelReasoningRequirement.valueOf(selected.reasoning),
                        selected.context,
                        new ModelTokenBudget(selected.maximumInput,
                                selected.maximumOutput, selected.maximumTotal),
                        Duration.ofMillis(selected.millis),
                        selected.currency,
                        selected.cost,
                        ModelDataClassification.valueOf(selected.classification));
        return new RemoteModelCandidateCapabilityFactSourceResult.Acquired(facts);
    }

    static byte[] readDeclaredBytes(SeekableByteChannel channel, long size)
            throws IOException {
        if (size < 0 || size > Integer.MAX_VALUE) {
            throw new IOException("catalog changed during read");
        }
        byte[] bytes = new byte[(int) size];
        ByteBuffer target = ByteBuffer.wrap(bytes);
        while (target.hasRemaining()) {
            if (channel.read(target) <= 0) {
                throw new IOException("catalog changed during read");
            }
        }
        if (channel.read(ByteBuffer.allocate(1)) >= 0 || channel.size() != size) {
            throw new IOException("catalog changed during read");
        }
        return bytes;
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
            if (header.startsWith("enhancer-remote-model-candidate-capability-catalog-v")) {
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
        return new Catalog(catalogIdentity, List.copyOf(records));
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

    private static void programmingLabel(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.length() > 256 || !LABEL.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    name + " must be a bounded stable lowercase label");
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

    private static byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static RemoteModelCandidateCapabilityFactSourceResult refusal(Reason reason) {
        return new RemoteModelCandidateCapabilityFactSourceResult.Refused(reason.value);
    }

    private static CatalogFailure canonical() {
        return new CatalogFailure(RemoteModelCandidateCapabilityFactSourceRefusalReason
                .CATALOG_NOT_CANONICAL);
    }

    private static CatalogFailure schema() {
        return new CatalogFailure(RemoteModelCandidateCapabilityFactSourceRefusalReason
                .CATALOG_SCHEMA_UNSUPPORTED);
    }

    private static CatalogFailure content() {
        return new CatalogFailure(RemoteModelCandidateCapabilityFactSourceRefusalReason
                .CATALOG_CONTENT_INVALID);
    }

    private record Catalog(String identity, List<CapabilityRecord> records) {
    }

    private record CapabilityRecord(String evidence, Instant observed, Instant expires,
            String candidate, String destination, String modelClass, String capability,
            String schedule, long basisInput, long basisOutput, long basisTotal,
            String reasoning, long context, long maximumInput, long maximumOutput,
            long maximumTotal, long millis, String currency, long cost,
            String classification) implements Comparable<CapabilityRecord> {
        @Override
        public int compareTo(CapabilityRecord other) {
            int compared = candidate.compareTo(other.candidate);
            return compared != 0 ? compared : destination.compareTo(other.destination);
        }
    }

    private static final class Cursor {
        private final List<String> lines;
        private int index;

        private Cursor(List<String> lines) { this.lines = lines; }

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

    private static final class CatalogFailure extends Exception {
        private static final long serialVersionUID = 1L;
        private final RemoteModelCandidateCapabilityFactSourceRefusalReason reason;

        private CatalogFailure(
                RemoteModelCandidateCapabilityFactSourceRefusalReason reason) {
            this.reason = reason;
        }
    }

    private enum Reason {
        UNSAFE(RemoteModelCandidateCapabilityFactSourceRefusalReason
                .CATALOG_UNAVAILABLE_OR_UNSAFE),
        SIZE(RemoteModelCandidateCapabilityFactSourceRefusalReason
                .CATALOG_OUTSIDE_SIZE_BOUND),
        CHANGED(RemoteModelCandidateCapabilityFactSourceRefusalReason
                .CATALOG_CHANGED_DURING_READ),
        PIN(RemoteModelCandidateCapabilityFactSourceRefusalReason.CATALOG_PIN_MISMATCH),
        UTF8(RemoteModelCandidateCapabilityFactSourceRefusalReason.CATALOG_INVALID_UTF8);

        private final RemoteModelCandidateCapabilityFactSourceRefusalReason value;

        Reason(RemoteModelCandidateCapabilityFactSourceRefusalReason value) {
            this.value = value;
        }
    }
}
