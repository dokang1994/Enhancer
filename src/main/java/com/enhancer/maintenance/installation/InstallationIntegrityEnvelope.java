package com.enhancer.maintenance.installation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Pure domain-separated integrity envelope and strict bounded binary field helpers. */
final class InstallationIntegrityEnvelope {
    private static final int ENVELOPE_SCHEMA_VERSION = 1;
    private static final int DIGEST_BYTES = 32;
    static final int HEADER_BYTES = Integer.BYTES * 3 + DIGEST_BYTES;
    private static final int MAX_STRING_BYTES = 16 * 1024;

    private InstallationIntegrityEnvelope() {}

    static byte[] encode(int magic, int maximumBodyBytes, byte[] body)
            throws InstallationRecordFormatException {
        byte[] checked = Objects.requireNonNull(body, "body must not be null").clone();
        if (checked.length > maximumBodyBytes) {
            throw failure(
                    InstallationRecordFormatException.Reason.SIZE_LIMIT_EXCEEDED,
                    "record body exceeds the supported size limit");
        }
        byte[] digest = digest(magic, ENVELOPE_SCHEMA_VERSION, checked);
        return ByteBuffer.allocate(HEADER_BYTES + checked.length)
                .putInt(magic)
                .putInt(ENVELOPE_SCHEMA_VERSION)
                .putInt(checked.length)
                .put(digest)
                .put(checked)
                .array();
    }

    static byte[] decode(int expectedMagic, int maximumBodyBytes, byte[] envelope)
            throws InstallationRecordFormatException {
        byte[] checked = Objects.requireNonNull(
                envelope, "envelope must not be null").clone();
        if (checked.length < HEADER_BYTES) {
            throw corrupt("record envelope is truncated");
        }
        ByteBuffer buffer = ByteBuffer.wrap(checked);
        if (buffer.getInt() != expectedMagic) {
            throw corrupt("record domain magic is invalid");
        }
        int envelopeSchema = buffer.getInt();
        if (envelopeSchema != ENVELOPE_SCHEMA_VERSION) {
            throw failure(
                    InstallationRecordFormatException.Reason.UNSUPPORTED_SCHEMA,
                    "record envelope schema is unsupported");
        }
        int declaredLength = buffer.getInt();
        if (declaredLength > maximumBodyBytes) {
            throw failure(
                    InstallationRecordFormatException.Reason.SIZE_LIMIT_EXCEEDED,
                    "record body exceeds the supported size limit");
        }
        if (declaredLength < 0
                || checked.length != HEADER_BYTES + declaredLength) {
            throw corrupt("record body length does not match its envelope");
        }
        byte[] declaredDigest = new byte[DIGEST_BYTES];
        buffer.get(declaredDigest);
        byte[] body = new byte[declaredLength];
        buffer.get(body);
        byte[] actualDigest = digest(expectedMagic, envelopeSchema, body);
        if (!MessageDigest.isEqual(declaredDigest, actualDigest)) {
            throw corrupt("record envelope digest does not match");
        }
        return body;
    }

    private static byte[] digest(int magic, int schema, byte[] body) {
        ByteBuffer authenticated = ByteBuffer.allocate(Integer.BYTES * 3 + body.length)
                .putInt(magic)
                .putInt(schema)
                .putInt(body.length)
                .put(body);
        try {
            return MessageDigest.getInstance("SHA-256").digest(authenticated.array());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    static InstallationRecordFormatException corrupt(String detail) {
        return failure(InstallationRecordFormatException.Reason.CORRUPT_RECORD, detail);
    }

    static InstallationRecordFormatException failure(
            InstallationRecordFormatException.Reason reason,
            String detail) {
        return new InstallationRecordFormatException(reason, detail);
    }

    static final class Writer {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private final DataOutputStream output = new DataOutputStream(bytes);

        void writeInt(int value) {
            try {
                output.writeInt(value);
            } catch (IOException exception) {
                throw impossible(exception);
            }
        }

        void writeLong(long value) {
            try {
                output.writeLong(value);
            } catch (IOException exception) {
                throw impossible(exception);
            }
        }

        void writeBoolean(boolean value) {
            try {
                output.writeByte(value ? 1 : 0);
            } catch (IOException exception) {
                throw impossible(exception);
            }
        }

        void writeUuid(UUID value) {
            UUID checked = Objects.requireNonNull(value, "UUID must not be null");
            writeLong(checked.getMostSignificantBits());
            writeLong(checked.getLeastSignificantBits());
        }

        void writeString(String value) throws InstallationRecordFormatException {
            byte[] encoded = strictUtf8(Objects.requireNonNull(
                    value, "string must not be null"));
            if (encoded.length > MAX_STRING_BYTES) {
                throw failure(
                        InstallationRecordFormatException.Reason.SIZE_LIMIT_EXCEEDED,
                        "record string exceeds the supported size limit");
            }
            writeInt(encoded.length);
            try {
                output.write(encoded);
            } catch (IOException exception) {
                throw impossible(exception);
            }
        }

        void writeOptionalString(Optional<String> value)
                throws InstallationRecordFormatException {
            Optional<String> checked = Objects.requireNonNull(
                    value, "optional string must not be null");
            writeBoolean(checked.isPresent());
            if (checked.isPresent()) {
                writeString(checked.orElseThrow());
            }
        }

        void writeEnum(Enum<?> value) throws InstallationRecordFormatException {
            writeString(Objects.requireNonNull(value, "enum must not be null").name());
        }

        byte[] toByteArray() {
            try {
                output.flush();
            } catch (IOException exception) {
                throw impossible(exception);
            }
            return bytes.toByteArray();
        }

        private static byte[] strictUtf8(String value)
                throws InstallationRecordFormatException {
            try {
                ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .encode(CharBuffer.wrap(value));
                byte[] result = new byte[encoded.remaining()];
                encoded.get(result);
                return result;
            } catch (CharacterCodingException exception) {
                throw failure(
                        InstallationRecordFormatException.Reason.NON_CANONICAL_RECORD,
                        "record string is not valid Unicode text");
            }
        }

        private static IllegalStateException impossible(IOException exception) {
            return new IllegalStateException("in-memory encoding failed", exception);
        }
    }

    static final class Reader {
        private final DataInputStream input;

        Reader(byte[] body) {
            input = new DataInputStream(new ByteArrayInputStream(
                    Objects.requireNonNull(body, "body must not be null")));
        }

        int readInt() throws InstallationRecordFormatException {
            try {
                return input.readInt();
            } catch (EOFException exception) {
                throw corrupt("record ended before an integer was read");
            } catch (IOException exception) {
                throw corrupt("record integer could not be read");
            }
        }

        long readLong() throws InstallationRecordFormatException {
            try {
                return input.readLong();
            } catch (EOFException exception) {
                throw corrupt("record ended before a long was read");
            } catch (IOException exception) {
                throw corrupt("record long could not be read");
            }
        }

        boolean readBoolean() throws InstallationRecordFormatException {
            int value;
            try {
                value = input.readUnsignedByte();
            } catch (EOFException exception) {
                throw corrupt("record ended before a boolean was read");
            } catch (IOException exception) {
                throw corrupt("record boolean could not be read");
            }
            if (value != 0 && value != 1) {
                throw corrupt("record boolean is not canonical");
            }
            return value == 1;
        }

        UUID readUuid() throws InstallationRecordFormatException {
            return new UUID(readLong(), readLong());
        }

        String readString() throws InstallationRecordFormatException {
            int length = readInt();
            if (length < 0 || length > MAX_STRING_BYTES || length > available()) {
                throw corrupt("record string length is invalid");
            }
            byte[] encoded = new byte[length];
            try {
                input.readFully(encoded);
                return StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(encoded))
                        .toString();
            } catch (CharacterCodingException exception) {
                throw corrupt("record string is not strict UTF-8");
            } catch (IOException exception) {
                throw corrupt("record string could not be read");
            }
        }

        Optional<String> readOptionalString() throws InstallationRecordFormatException {
            return readBoolean() ? Optional.of(readString()) : Optional.empty();
        }

        <E extends Enum<E>> E readEnum(Class<E> type)
                throws InstallationRecordFormatException {
            String name = readString();
            try {
                return Enum.valueOf(Objects.requireNonNull(type, "type must not be null"), name);
            } catch (IllegalArgumentException exception) {
                throw corrupt("record enum value is invalid");
            }
        }

        void requireFinished() throws InstallationRecordFormatException {
            if (available() != 0) {
                throw corrupt("record body contains trailing bytes");
            }
        }

        private int available() throws InstallationRecordFormatException {
            try {
                return input.available();
            } catch (IOException exception) {
                throw corrupt("record body availability could not be read");
            }
        }
    }
}
