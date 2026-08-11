package com.enhancer.runtime;

import com.enhancer.bus.ControlPayload;
import com.enhancer.bus.ControlSignal;
import com.enhancer.bus.MessageEnvelope;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/** Strict transient representation of one detached signed exact-request cancellation grant. */
final class DetachedSignedCancellationGrant {
    static final int MAX_PROOF_BYTES = 16 * 1024;
    static final int SIGNATURE_BYTES = 64;

    private static final int FRAME_MAGIC = 0x45434731;
    private static final int MAX_FIELD_BYTES = 4 * 1024;
    private static final String SIGNING_DOMAIN =
            "enhancer:detached-cancellation-grant";
    private static final String GRANT_VERSION = "grant-v1";
    private static final String REQUEST_DOMAIN = "enhancer:cancellation-request:v1";
    private static final Pattern BOUNDED_IDENTITY = Pattern.compile(
            "[A-Za-z0-9][A-Za-z0-9._-]{0,127}");
    private static final Pattern LOWERCASE_SHA256 = Pattern.compile("[0-9a-f]{64}");

    private final Claims claims;
    private final byte[] signature;

    private DetachedSignedCancellationGrant(Claims claims, byte[] signature) {
        this.claims = Objects.requireNonNull(claims, "claims must not be null");
        this.signature = Objects.requireNonNull(
                signature, "signature must not be null").clone();
        if (this.signature.length != SIGNATURE_BYTES) {
            throw new IllegalArgumentException(
                    "Ed25519 signature must contain exactly 64 bytes");
        }
    }

    static DetachedSignedCancellationGrant create(Claims claims, byte[] signature) {
        return new DetachedSignedCancellationGrant(claims, signature);
    }

    static DetachedSignedCancellationGrant parse(byte[] proof) throws IOException {
        byte[] checked = Objects.requireNonNull(proof, "proof must not be null").clone();
        if (checked.length > MAX_PROOF_BYTES) {
            throw new IOException("signed cancellation proof exceeds the supported bound");
        }
        try (DataInputStream input = new DataInputStream(
                new ByteArrayInputStream(checked))) {
            if (input.readInt() != FRAME_MAGIC) {
                throw new IOException("signed cancellation proof magic is invalid");
            }
            int claimsLength = input.readInt();
            if (claimsLength < 0
                    || claimsLength > MAX_PROOF_BYTES
                    || claimsLength > input.available() - Integer.BYTES) {
                throw new IOException("signed cancellation claims length is invalid");
            }
            byte[] claimsBytes = new byte[claimsLength];
            input.readFully(claimsBytes);
            int signatureLength = input.readInt();
            if (signatureLength != SIGNATURE_BYTES
                    || signatureLength > input.available()) {
                throw new IOException("signed cancellation signature length is invalid");
            }
            byte[] signature = new byte[signatureLength];
            input.readFully(signature);
            if (input.read() != -1) {
                throw new IOException("signed cancellation proof contains trailing bytes");
            }
            Claims claims = Claims.decode(claimsBytes);
            if (!MessageDigest.isEqual(claimsBytes, claims.signingBytes())) {
                throw new IOException("signed cancellation claims are not canonical");
            }
            return new DetachedSignedCancellationGrant(claims, signature);
        } catch (EOFException exception) {
            throw new IOException("signed cancellation proof is truncated", exception);
        }
    }

    Claims claims() {
        return claims;
    }

    byte[] signature() {
        return signature.clone();
    }

    byte[] signingBytes() {
        return claims.signingBytes();
    }

    byte[] encoded() {
        byte[] claimsBytes = claims.signingBytes();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(
                Integer.BYTES * 3 + claimsBytes.length + signature.length);
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(FRAME_MAGIC);
            output.writeInt(claimsBytes.length);
            output.write(claimsBytes);
            output.writeInt(signature.length);
            output.write(signature);
        } catch (IOException impossible) {
            throw new UncheckedIOException(
                    "encoding an in-memory signed cancellation grant must not fail",
                    impossible);
        }
        byte[] encoded = bytes.toByteArray();
        if (encoded.length > MAX_PROOF_BYTES) {
            throw new IllegalArgumentException(
                    "signed cancellation proof exceeds the supported bound");
        }
        return encoded;
    }

    String proofSha256() {
        return sha256(encoded());
    }

    static String requestSha256(MessageEnvelope retainedRequest) {
        MessageEnvelope request = Objects.requireNonNull(
                retainedRequest, "retainedRequest must not be null");
        if (!(request.payload() instanceof ControlPayload control)
                || control.signal() != ControlSignal.CANCEL) {
            throw new IllegalArgumentException(
                    "retained request digest requires exact CANCEL Control payload");
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            writeStringFrame(output, REQUEST_DOMAIN);
            writeStringFrame(output, MessageEnvelope.ENVELOPE_VERSION);
            writeStringFrame(output, request.messageId());
            writeStringFrame(output, request.correlationId());
            writeOptionalUuidFrame(output, request.causationId());
            writeStringFrame(output, request.logicalRunId());
            writeStringFrame(output, request.producer());
            writeInstantFrame(output, request.occurredAt());
            writeStringFrame(output, "CONTROL");
            writeStringFrame(output, ControlSignal.CANCEL.name());
            writeStringFrame(output, control.reason());
        } catch (IOException impossible) {
            throw new UncheckedIOException(
                    "encoding an in-memory retained request must not fail", impossible);
        }
        return sha256(bytes.toByteArray());
    }

    record Claims(
            String audience,
            String goalId,
            String controlMessageId,
            String requestSha256,
            String authorizationId,
            String issuerId,
            String keyId,
            String subjectId,
            String policyRevision,
            Instant issuedAt,
            Instant expiresAt) {
        Claims {
            audience = boundedIdentity(audience, "audience");
            goalId = RuntimeIdentity.canonicalUuid(goalId, "goalId");
            controlMessageId = RuntimeIdentity.canonicalUuid(
                    controlMessageId, "controlMessageId");
            requestSha256 = lowercaseSha256(requestSha256, "requestSha256");
            authorizationId = RuntimeIdentity.canonicalUuid(
                    authorizationId, "authorizationId");
            issuerId = boundedIdentity(issuerId, "issuerId");
            keyId = boundedIdentity(keyId, "keyId");
            subjectId = boundedIdentity(subjectId, "subjectId");
            policyRevision = boundedIdentity(policyRevision, "policyRevision");
            Objects.requireNonNull(issuedAt, "issuedAt must not be null");
            Objects.requireNonNull(expiresAt, "expiresAt must not be null");
            if (!expiresAt.isAfter(issuedAt)) {
                throw new IllegalArgumentException("expiresAt must be after issuedAt");
            }
        }

        byte[] signingBytes() {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                writeStringFrame(output, SIGNING_DOMAIN);
                writeStringFrame(output, GRANT_VERSION);
                writeStringFrame(output, audience);
                writeStringFrame(output, goalId);
                writeStringFrame(output, controlMessageId);
                writeStringFrame(output, requestSha256);
                writeStringFrame(output, ControlSignal.CANCEL.name());
                writeStringFrame(output, authorizationId);
                writeStringFrame(output, issuerId);
                writeStringFrame(output, keyId);
                writeStringFrame(output, subjectId);
                writeStringFrame(output, policyRevision);
                writeInstantFrame(output, issuedAt);
                writeInstantFrame(output, expiresAt);
            } catch (IOException impossible) {
                throw new UncheckedIOException(
                        "encoding signed cancellation claims must not fail", impossible);
            }
            byte[] encoded = bytes.toByteArray();
            if (encoded.length > MAX_PROOF_BYTES) {
                throw new IllegalArgumentException(
                        "signed cancellation claims exceed the supported bound");
            }
            return encoded;
        }

        private static Claims decode(byte[] encoded) throws IOException {
            try (DataInputStream input = new DataInputStream(
                    new ByteArrayInputStream(encoded))) {
                requireEquals(readStringFrame(input), SIGNING_DOMAIN, "domain");
                requireEquals(readStringFrame(input), GRANT_VERSION, "version");
                String audience = readStringFrame(input);
                String goalId = readStringFrame(input);
                String controlMessageId = readStringFrame(input);
                String requestSha256 = readStringFrame(input);
                requireEquals(
                        readStringFrame(input), ControlSignal.CANCEL.name(), "signal");
                String authorizationId = readStringFrame(input);
                String issuerId = readStringFrame(input);
                String keyId = readStringFrame(input);
                String subjectId = readStringFrame(input);
                String policyRevision = readStringFrame(input);
                Instant issuedAt = readInstantFrame(input);
                Instant expiresAt = readInstantFrame(input);
                if (input.read() != -1) {
                    throw new IOException(
                            "signed cancellation claims contain trailing bytes");
                }
                return new Claims(
                        audience,
                        goalId,
                        controlMessageId,
                        requestSha256,
                        authorizationId,
                        issuerId,
                        keyId,
                        subjectId,
                        policyRevision,
                        issuedAt,
                        expiresAt);
            } catch (EOFException exception) {
                throw new IOException("signed cancellation claims are truncated", exception);
            } catch (IllegalArgumentException | NullPointerException exception) {
                throw new IOException(
                        "signed cancellation claims violate the contract", exception);
            }
        }
    }

    private static void writeOptionalUuidFrame(
            DataOutputStream output, Optional<String> value) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream optional = new DataOutputStream(bytes)) {
            optional.writeByte(value.isPresent() ? 1 : 0);
            if (value.isPresent()) {
                writeStringFrame(optional, value.orElseThrow());
            }
        }
        writeFrame(output, bytes.toByteArray());
    }

    private static void writeInstantFrame(DataOutputStream output, Instant value)
            throws IOException {
        Instant checked = Objects.requireNonNull(value, "instant must not be null");
        ByteBuffer bytes = ByteBuffer.allocate(Long.BYTES + Integer.BYTES)
                .putLong(checked.getEpochSecond())
                .putInt(checked.getNano());
        writeFrame(output, bytes.array());
    }

    private static Instant readInstantFrame(DataInputStream input) throws IOException {
        byte[] frame = readFrame(input);
        if (frame.length != Long.BYTES + Integer.BYTES) {
            throw new IOException("signed cancellation instant frame length is invalid");
        }
        ByteBuffer bytes = ByteBuffer.wrap(frame);
        try {
            return Instant.ofEpochSecond(bytes.getLong(), bytes.getInt());
        } catch (RuntimeException exception) {
            throw new IOException("signed cancellation instant is invalid", exception);
        }
    }

    private static void writeStringFrame(DataOutputStream output, String value)
            throws IOException {
        writeFrame(output, strictUtf8(value));
    }

    private static String readStringFrame(DataInputStream input) throws IOException {
        byte[] frame = readFrame(input);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(frame))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new IOException(
                    "signed cancellation text is not valid UTF-8", exception);
        }
    }

    private static void writeFrame(DataOutputStream output, byte[] value)
            throws IOException {
        if (value.length > MAX_FIELD_BYTES) {
            throw new IOException("signed cancellation field exceeds supported bound");
        }
        output.writeInt(value.length);
        output.write(value);
    }

    private static byte[] readFrame(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length < 0 || length > MAX_FIELD_BYTES || length > input.available()) {
            throw new IOException("signed cancellation field length is invalid");
        }
        byte[] value = new byte[length];
        input.readFully(value);
        return value;
    }

    private static byte[] strictUtf8(String value) {
        try {
            ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(Objects.requireNonNull(
                            value, "text must not be null")));
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            return bytes;
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException(
                    "signed cancellation text is not valid Unicode", exception);
        }
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
            throw new IllegalArgumentException(
                    field + " must be lowercase SHA-256");
        }
        return value;
    }

    private static void requireEquals(String actual, String expected, String field)
            throws IOException {
        if (!expected.equals(actual)) {
            throw new IOException("signed cancellation " + field + " is unsupported");
        }
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
