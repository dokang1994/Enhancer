package com.enhancer.runtime;

import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.EdECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.NamedParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** Immutable public-only trust snapshot injected into signed cancellation verification. */
public final class CancellationGrantTrustPolicy {
    public static final Duration MAXIMUM_SUPPORTED_GRANT_LIFETIME =
            Duration.ofHours(24);
    public static final Duration MAXIMUM_SUPPORTED_CLOCK_SKEW =
            Duration.ofMinutes(5);
    public static final int MAX_TRUSTED_KEYS = 64;

    private static final Pattern BOUNDED_IDENTITY = Pattern.compile(
            "[A-Za-z0-9][A-Za-z0-9._-]{0,127}");
    private static final Pattern LOWERCASE_SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final int MAX_SUBJECTS_PER_KEY = 256;
    private static final int MAX_PUBLIC_KEY_BYTES = 4 * 1024;

    private final String configurationId;
    private final String configurationRevision;
    private final String audience;
    private final String policyRevision;
    private final Duration maximumGrantLifetime;
    private final Duration clockSkew;
    private final List<TrustedKey> trustedKeys;
    private final Map<KeyIdentity, TrustedKey> keysByIdentity;

    public CancellationGrantTrustPolicy(
            String configurationId,
            String configurationRevision,
            String audience,
            String policyRevision,
            Duration maximumGrantLifetime,
            Duration clockSkew,
            List<TrustedKey> trustedKeys) {
        this.configurationId = boundedIdentity(configurationId, "configurationId");
        this.configurationRevision = boundedIdentity(
                configurationRevision, "configurationRevision");
        this.audience = boundedIdentity(audience, "audience");
        this.policyRevision = boundedIdentity(policyRevision, "policyRevision");
        this.maximumGrantLifetime = positiveBounded(
                maximumGrantLifetime,
                MAXIMUM_SUPPORTED_GRANT_LIFETIME,
                "maximumGrantLifetime");
        this.clockSkew = nonNegativeBounded(
                clockSkew, MAXIMUM_SUPPORTED_CLOCK_SKEW, "clockSkew");
        Objects.requireNonNull(trustedKeys, "trustedKeys must not be null");
        if (trustedKeys.isEmpty() || trustedKeys.size() > MAX_TRUSTED_KEYS) {
            throw new IllegalArgumentException(
                    "trustedKeys must contain between 1 and "
                            + MAX_TRUSTED_KEYS + " entries");
        }
        List<TrustedKey> copied = new ArrayList<>(trustedKeys.size());
        Map<KeyIdentity, TrustedKey> indexed = new HashMap<>();
        for (TrustedKey key : trustedKeys) {
            TrustedKey checked = Objects.requireNonNull(
                    key, "trustedKeys must not contain null");
            KeyIdentity identity = new KeyIdentity(checked.issuerId(), checked.keyId());
            if (indexed.put(identity, checked) != null) {
                throw new IllegalArgumentException(
                        "trusted issuer/key identities must be unique");
            }
            copied.add(checked);
        }
        this.trustedKeys = List.copyOf(copied);
        this.keysByIdentity = Collections.unmodifiableMap(indexed);
    }

    public String configurationId() {
        return configurationId;
    }

    public String configurationRevision() {
        return configurationRevision;
    }

    public String audience() {
        return audience;
    }

    public String policyRevision() {
        return policyRevision;
    }

    public Duration maximumGrantLifetime() {
        return maximumGrantLifetime;
    }

    public Duration clockSkew() {
        return clockSkew;
    }

    public List<TrustedKey> trustedKeys() {
        return trustedKeys;
    }

    Optional<TrustedKey> find(String issuerId, String keyId) {
        return Optional.ofNullable(keysByIdentity.get(new KeyIdentity(
                boundedIdentity(issuerId, "issuerId"),
                boundedIdentity(keyId, "keyId"))));
    }

    /** One exact Ed25519 public verification key and its bounded authorization scope. */
    public static final class TrustedKey {
        private final String issuerId;
        private final String keyId;
        private final Set<String> authorizedSubjects;
        private final byte[] publicKeySubjectPublicKeyInfo;
        private final String publicKeySha256;
        private final Instant validFrom;
        private final Instant validUntil;
        private final Optional<Instant> revokedAt;
        private final EdECPublicKey publicKey;

        public TrustedKey(
                String issuerId,
                String keyId,
                Set<String> authorizedSubjects,
                byte[] publicKeySubjectPublicKeyInfo,
                String expectedPublicKeySha256,
                Instant validFrom,
                Instant validUntil,
                Optional<Instant> revokedAt) {
            this.issuerId = boundedIdentity(issuerId, "issuerId");
            this.keyId = boundedIdentity(keyId, "keyId");
            Objects.requireNonNull(
                    authorizedSubjects, "authorizedSubjects must not be null");
            if (authorizedSubjects.isEmpty()
                    || authorizedSubjects.size() > MAX_SUBJECTS_PER_KEY) {
                throw new IllegalArgumentException(
                        "authorizedSubjects must contain between 1 and "
                                + MAX_SUBJECTS_PER_KEY + " entries");
            }
            LinkedHashSet<String> subjects = new LinkedHashSet<>();
            for (String subject : authorizedSubjects) {
                if (!subjects.add(boundedIdentity(subject, "subjectId"))) {
                    throw new IllegalArgumentException(
                            "authorizedSubjects must not contain duplicates");
                }
            }
            this.authorizedSubjects = Collections.unmodifiableSet(subjects);
            this.publicKeySubjectPublicKeyInfo = Objects.requireNonNull(
                    publicKeySubjectPublicKeyInfo,
                    "publicKeySubjectPublicKeyInfo must not be null").clone();
            if (this.publicKeySubjectPublicKeyInfo.length == 0
                    || this.publicKeySubjectPublicKeyInfo.length > MAX_PUBLIC_KEY_BYTES) {
                throw new IllegalArgumentException(
                        "public key encoding is outside supported bounds");
            }
            this.publicKeySha256 = lowercaseSha256(
                    expectedPublicKeySha256, "expectedPublicKeySha256");
            String actualFingerprint = sha256(this.publicKeySubjectPublicKeyInfo);
            if (!MessageDigest.isEqual(
                    HexFormat.of().parseHex(this.publicKeySha256),
                    HexFormat.of().parseHex(actualFingerprint))) {
                throw new IllegalArgumentException(
                        "public key fingerprint does not match its encoding");
            }
            this.publicKey = decodeEd25519(this.publicKeySubjectPublicKeyInfo);
            this.validFrom = Objects.requireNonNull(
                    validFrom, "validFrom must not be null");
            this.validUntil = Objects.requireNonNull(
                    validUntil, "validUntil must not be null");
            if (!this.validUntil.isAfter(this.validFrom)) {
                throw new IllegalArgumentException("validUntil must be after validFrom");
            }
            this.revokedAt = Objects.requireNonNull(
                    revokedAt, "revokedAt must not be null");
            if (this.revokedAt.isPresent()
                    && this.revokedAt.orElseThrow().isBefore(this.validFrom)) {
                throw new IllegalArgumentException(
                        "revokedAt must not precede validFrom");
            }
        }

        public String issuerId() {
            return issuerId;
        }

        public String keyId() {
            return keyId;
        }

        public Set<String> authorizedSubjects() {
            return authorizedSubjects;
        }

        public byte[] publicKeySubjectPublicKeyInfo() {
            return publicKeySubjectPublicKeyInfo.clone();
        }

        public String publicKeySha256() {
            return publicKeySha256;
        }

        public Instant validFrom() {
            return validFrom;
        }

        public Instant validUntil() {
            return validUntil;
        }

        public Optional<Instant> revokedAt() {
            return revokedAt;
        }

        PublicKey publicKey() {
            return publicKey;
        }

        boolean authorizes(String subjectId) {
            return authorizedSubjects.contains(subjectId);
        }
    }

    private static EdECPublicKey decodeEd25519(byte[] encoded) {
        try {
            PublicKey decoded = KeyFactory.getInstance("Ed25519")
                    .generatePublic(new X509EncodedKeySpec(encoded));
            if (!(decoded instanceof EdECPublicKey edEcPublicKey)
                    || !NamedParameterSpec.ED25519.getName().equals(
                            edEcPublicKey.getParams().getName())) {
                throw new IllegalArgumentException(
                        "public key must be an Ed25519 X.509 key");
            }
            return edEcPublicKey;
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Ed25519 is unavailable", exception);
        } catch (InvalidKeySpecException exception) {
            throw new IllegalArgumentException(
                    "public key must be an Ed25519 X.509 key", exception);
        }
    }

    private static Duration positiveBounded(
            Duration value, Duration maximum, String field) {
        Duration checked = Objects.requireNonNull(value, field + " must not be null");
        if (checked.isZero() || checked.isNegative() || checked.compareTo(maximum) > 0) {
            throw new IllegalArgumentException(
                    field + " must be positive and at most " + maximum);
        }
        return checked;
    }

    private static Duration nonNegativeBounded(
            Duration value, Duration maximum, String field) {
        Duration checked = Objects.requireNonNull(value, field + " must not be null");
        if (checked.isNegative() || checked.compareTo(maximum) > 0) {
            throw new IllegalArgumentException(
                    field + " must be non-negative and at most " + maximum);
        }
        return checked;
    }

    private static String boundedIdentity(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (!BOUNDED_IDENTITY.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    field + " must be a canonical bounded identity");
        }
        return value;
    }

    private static String lowercaseSha256(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (!LOWERCASE_SHA256.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " must be lowercase SHA-256");
        }
        return value;
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private record KeyIdentity(String issuerId, String keyId) {
    }
}
