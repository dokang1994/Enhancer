package com.enhancer.runtime;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** Loads one exact, independently pinned, public-only cancellation trust snapshot. */
public final class PinnedFileCancellationGrantTrustPolicyLoader {
    public static final int MAX_POLICY_BYTES = 4 * 1024 * 1024;

    private static final String HEADER =
            "enhancer-cancellation-grant-trust-policy-v1";
    private static final int MAX_SUBJECTS_PER_KEY = 256;
    private static final Pattern LOWERCASE_SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final Comparator<CancellationGrantTrustPolicy.TrustedKey> KEY_ORDER =
            Comparator.comparing(CancellationGrantTrustPolicy.TrustedKey::issuerId)
                    .thenComparing(CancellationGrantTrustPolicy.TrustedKey::keyId);

    private final Path policyFile;
    private final String expectedSha256;
    private final byte[] expectedDigest;

    public PinnedFileCancellationGrantTrustPolicyLoader(
            Path policyFile, String expectedSha256) {
        Path checkedPath = Objects.requireNonNull(
                policyFile, "policyFile must not be null");
        if (!checkedPath.isAbsolute() || !checkedPath.equals(checkedPath.normalize())) {
            throw new IllegalArgumentException(
                    "policyFile must be an absolute normalized path");
        }
        this.policyFile = checkedPath;
        this.expectedSha256 = lowercaseSha256(expectedSha256);
        this.expectedDigest = HexFormat.of().parseHex(this.expectedSha256);
    }

    public CancellationGrantTrustPolicy load() throws IOException {
        CanonicalSnapshot snapshot = readCanonicalSnapshot(policyFile);
        byte[] actualDigest = HexFormat.of().parseHex(snapshot.sha256());
        if (!MessageDigest.isEqual(expectedDigest, actualDigest)) {
            throw new IOException("cancellation trust policy does not match its pin");
        }
        return snapshot.policy();
    }

    /** Reads and validates one exact canonical public-only policy snapshot. */
    public static CanonicalSnapshot readCanonicalSnapshot(Path policyFile)
            throws IOException {
        Path checkedPath = Objects.requireNonNull(
                policyFile, "policyFile must not be null");
        if (!checkedPath.isAbsolute() || !checkedPath.equals(checkedPath.normalize())) {
            throw new IllegalArgumentException(
                    "policyFile must be an absolute normalized path");
        }
        validateExactRegularFile(checkedPath);
        byte[] content = readOneBoundedSnapshot(checkedPath);
        byte[] actualDigest = sha256Bytes(content);
        String actualSha256 = HexFormat.of().formatHex(actualDigest);
        try {
            CancellationGrantTrustPolicy policy = parse(content, actualSha256);
            byte[] canonical = encodeCanonical(policy);
            if (!MessageDigest.isEqual(content, canonical)) {
                throw new IOException(
                        "cancellation trust policy is not in canonical form");
            }
            return new CanonicalSnapshot(policy, actualSha256, content);
        } catch (IllegalArgumentException exception) {
            throw new IOException("invalid cancellation trust policy", exception);
        }
    }

    private static void validateExactRegularFile(Path policyFile) throws IOException {
        Path parent = policyFile.getParent();
        if (parent == null
                || !Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)
                || !parent.toRealPath().equals(parent)) {
            throw new IOException(
                    "cancellation trust policy parent must be an exact real directory");
        }
        if (!Files.exists(policyFile, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(policyFile)
                || !Files.isRegularFile(policyFile, LinkOption.NOFOLLOW_LINKS)
                || !policyFile.toRealPath().equals(policyFile)) {
            throw new IOException(
                    "cancellation trust policy must be an exact real regular file");
        }
    }

    private static byte[] readOneBoundedSnapshot(Path policyFile) throws IOException {
        try (FileChannel channel = FileChannel.open(
                policyFile, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            long declaredSize = channel.size();
            if (declaredSize <= 0 || declaredSize > MAX_POLICY_BYTES) {
                throw new IOException(
                        "cancellation trust policy is outside supported size bounds");
            }
            byte[] content = new byte[(int) declaredSize];
            ByteBuffer target = ByteBuffer.wrap(content);
            while (target.hasRemaining()) {
                int read = channel.read(target);
                if (read < 0) {
                    throw new IOException(
                            "cancellation trust policy changed while reading");
                }
            }
            ByteBuffer extra = ByteBuffer.allocate(1);
            if (channel.read(extra) >= 0 || channel.size() != declaredSize) {
                throw new IOException(
                        "cancellation trust policy changed while reading");
            }
            return content;
        }
    }

    private static CancellationGrantTrustPolicy parse(
            byte[] content, String configurationRevision) throws IOException {
        String text = decodeUtf8(content);
        if (!text.endsWith("\n") || text.indexOf('\r') >= 0) {
            throw new IOException(
                    "cancellation trust policy must use canonical LF text");
        }
        String[] split = text.split("\n", -1);
        if (!split[split.length - 1].isEmpty()) {
            throw new IOException(
                    "cancellation trust policy must end with LF");
        }
        List<String> lines = Arrays.asList(split).subList(0, split.length - 1);
        if (lines.stream().anyMatch(String::isEmpty)) {
            throw new IOException(
                    "cancellation trust policy must not contain blank lines");
        }

        Cursor cursor = new Cursor(lines);
        cursor.expect(HEADER);
        String configurationId = cursor.value("configurationId");
        String audience = cursor.value("audience");
        String policyRevision = cursor.value("policyRevision");
        long lifetimeSeconds = canonicalLong(
                cursor.value("maximumGrantLifetimeSeconds"),
                "maximumGrantLifetimeSeconds");
        long skewSeconds = canonicalLong(
                cursor.value("clockSkewSeconds"), "clockSkewSeconds");
        int keyCount = canonicalCount(
                cursor.value("trustedKeyCount"),
                "trustedKeyCount",
                CancellationGrantTrustPolicy.MAX_TRUSTED_KEYS);
        List<CancellationGrantTrustPolicy.TrustedKey> keys =
                new ArrayList<>(keyCount);
        for (int keyIndex = 0; keyIndex < keyCount; keyIndex++) {
            String prefix = "trustedKey." + keyIndex + ".";
            String issuerId = cursor.value(prefix + "issuerId");
            String keyId = cursor.value(prefix + "keyId");
            int subjectCount = canonicalCount(
                    cursor.value(prefix + "subjectCount"),
                    prefix + "subjectCount",
                    MAX_SUBJECTS_PER_KEY);
            Set<String> subjects = new LinkedHashSet<>();
            for (int subjectIndex = 0;
                    subjectIndex < subjectCount;
                    subjectIndex++) {
                String subject = cursor.value(
                        prefix + "subject." + subjectIndex);
                if (!subjects.add(subject)) {
                    throw new IOException(
                            "cancellation trust policy contains duplicate subjects");
                }
            }
            byte[] publicKey = canonicalBase64(cursor.value(
                    prefix + "publicKeySubjectPublicKeyInfo"));
            String fingerprint = cursor.value(prefix + "publicKeySha256");
            Instant validFrom = canonicalInstant(
                    cursor.value(prefix + "validFrom"), prefix + "validFrom");
            Instant validUntil = canonicalInstant(
                    cursor.value(prefix + "validUntil"), prefix + "validUntil");
            String revokedValue = cursor.value(prefix + "revokedAt");
            Optional<Instant> revokedAt = revokedValue.equals("-")
                    ? Optional.empty()
                    : Optional.of(canonicalInstant(
                            revokedValue, prefix + "revokedAt"));
            keys.add(new CancellationGrantTrustPolicy.TrustedKey(
                    issuerId,
                    keyId,
                    subjects,
                    publicKey,
                    fingerprint,
                    validFrom,
                    validUntil,
                    revokedAt));
        }
        cursor.requireEnd();
        return new CancellationGrantTrustPolicy(
                configurationId,
                configurationRevision,
                audience,
                policyRevision,
                Duration.ofSeconds(lifetimeSeconds),
                Duration.ofSeconds(skewSeconds),
                keys);
    }

    private static byte[] encodeCanonical(CancellationGrantTrustPolicy policy) {
        StringBuilder output = new StringBuilder();
        append(output, HEADER);
        append(output, "configurationId=" + policy.configurationId());
        append(output, "audience=" + policy.audience());
        append(output, "policyRevision=" + policy.policyRevision());
        append(output, "maximumGrantLifetimeSeconds="
                + policy.maximumGrantLifetime().getSeconds());
        append(output, "clockSkewSeconds=" + policy.clockSkew().getSeconds());
        List<CancellationGrantTrustPolicy.TrustedKey> keys = policy.trustedKeys()
                .stream()
                .sorted(KEY_ORDER)
                .toList();
        append(output, "trustedKeyCount=" + keys.size());
        for (int keyIndex = 0; keyIndex < keys.size(); keyIndex++) {
            CancellationGrantTrustPolicy.TrustedKey key = keys.get(keyIndex);
            String prefix = "trustedKey." + keyIndex + ".";
            append(output, prefix + "issuerId=" + key.issuerId());
            append(output, prefix + "keyId=" + key.keyId());
            List<String> subjects = key.authorizedSubjects().stream().sorted().toList();
            append(output, prefix + "subjectCount=" + subjects.size());
            for (int subjectIndex = 0;
                    subjectIndex < subjects.size();
                    subjectIndex++) {
                append(output, prefix + "subject." + subjectIndex + "="
                        + subjects.get(subjectIndex));
            }
            append(output, prefix + "publicKeySubjectPublicKeyInfo="
                    + Base64.getEncoder().encodeToString(
                            key.publicKeySubjectPublicKeyInfo()));
            append(output, prefix + "publicKeySha256=" + key.publicKeySha256());
            append(output, prefix + "validFrom=" + key.validFrom());
            append(output, prefix + "validUntil=" + key.validUntil());
            append(output, prefix + "revokedAt="
                    + key.revokedAt().map(Instant::toString).orElse("-"));
        }
        return output.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void append(StringBuilder output, String line) {
        output.append(line).append('\n');
    }

    private static String decodeUtf8(byte[] content) throws IOException {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new IOException(
                    "cancellation trust policy is not valid UTF-8", exception);
        }
    }

    private static long canonicalLong(String value, String field) throws IOException {
        try {
            long parsed = Long.parseLong(value);
            if (!Long.toString(parsed).equals(value)) {
                throw new IOException(field + " is not a canonical integer");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IOException(field + " is not a canonical integer", exception);
        }
    }

    private static int canonicalCount(
            String value, String field, int maximum) throws IOException {
        long parsed = canonicalLong(value, field);
        if (parsed < 1 || parsed > maximum) {
            throw new IOException(field + " is outside supported bounds");
        }
        return (int) parsed;
    }

    private static Instant canonicalInstant(String value, String field)
            throws IOException {
        try {
            Instant parsed = Instant.parse(value);
            if (!parsed.toString().equals(value)) {
                throw new IOException(field + " is not a canonical instant");
            }
            return parsed;
        } catch (DateTimeParseException exception) {
            throw new IOException(field + " is not a canonical instant", exception);
        }
    }

    private static byte[] canonicalBase64(String value) throws IOException {
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            if (!Base64.getEncoder().encodeToString(decoded).equals(value)) {
                throw new IOException("public key is not canonical Base64");
            }
            return decoded;
        } catch (IllegalArgumentException exception) {
            throw new IOException("public key is not canonical Base64", exception);
        }
    }

    private static String lowercaseSha256(String value) {
        Objects.requireNonNull(value, "expectedSha256 must not be null");
        if (!LOWERCASE_SHA256.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "expectedSha256 must be lowercase SHA-256");
        }
        return value;
    }

    private static byte[] sha256Bytes(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    /** Canonical public policy and digest derived from one defensive byte snapshot. */
    public static final class CanonicalSnapshot {
        private final CancellationGrantTrustPolicy policy;
        private final String sha256;
        private final byte[] bytes;

        private CanonicalSnapshot(
                CancellationGrantTrustPolicy policy, String sha256, byte[] bytes) {
            this.policy = Objects.requireNonNull(policy, "policy must not be null");
            this.sha256 = lowercaseSha256(sha256);
            this.bytes = Objects.requireNonNull(bytes, "bytes must not be null").clone();
        }

        public CancellationGrantTrustPolicy policy() { return policy; }

        public String sha256() { return sha256; }

        public byte[] bytes() { return bytes.clone(); }
    }

    private static final class Cursor {
        private final List<String> lines;
        private int index;

        private Cursor(List<String> lines) {
            this.lines = lines;
        }

        private void expect(String expected) throws IOException {
            if (index >= lines.size() || !lines.get(index).equals(expected)) {
                throw new IOException(
                        "cancellation trust policy has unexpected content at line "
                                + (index + 1));
            }
            index++;
        }

        private String value(String field) throws IOException {
            String prefix = field + "=";
            if (index >= lines.size() || !lines.get(index).startsWith(prefix)) {
                throw new IOException(
                        "cancellation trust policy expected " + field
                                + " at line " + (index + 1));
            }
            String value = lines.get(index).substring(prefix.length());
            if (value.isEmpty()) {
                throw new IOException(
                        "cancellation trust policy field " + field
                                + " must not be empty");
            }
            index++;
            return value;
        }

        private void requireEnd() throws IOException {
            if (index != lines.size()) {
                throw new IOException(
                        "cancellation trust policy contains trailing content");
            }
        }
    }
}
