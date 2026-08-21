package com.enhancer.bus;

import com.enhancer.kernel.VerificationStatus;
import com.enhancer.model.ModelCostBudget;
import com.enhancer.model.ModelDataClassification;
import com.enhancer.model.ModelExecutionProfile;
import com.enhancer.model.ModelLocalityRequirement;
import com.enhancer.model.ModelReasoningRequirement;
import com.enhancer.model.ModelTokenBudget;
import com.enhancer.workspace.ApprovedTaskRevision;
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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Deterministic, integrity-checked codec between one {@link TransportMessage} — its destination
 * plus one typed {@link MessageEnvelope} — and a self-describing byte frame.
 *
 * <p>The frame is {@code [magic][bodyLength][sha-256 of body][body]}. Decoding validates the
 * magic, length, digest, strict UTF-8, exact body consumption, and every envelope invariant,
 * failing closed on any deviation.
 *
 * <p>Encoding is total for a valid message and depends on no wall-clock or random state, so the
 * same message always yields the same bytes and a peer may deduplicate on content. Occurrence
 * time is carried as epoch-second plus nanosecond: rounding an {@link Instant} to milliseconds
 * would silently rewrite provenance the receiver is meant to trust.
 *
 * <p>This is separate from any transport so the wire format can be verified without a
 * filesystem, and so a second adapter can reuse it unchanged. Legacy payloads retain their
 * v1 family; only {@link ModelWorkPayload} uses the v2 family.
 */
final class MessageEnvelopeCodec {
    static final String CODEC_VERSION = "transport-spool-v1";
    static final String MODEL_WORK_CODEC_VERSION = "transport-spool-v2";
    static final String MODEL_WORK_ENVELOPE_VERSION = "message-envelope-v2";
    static final String MODEL_WORK_PAYLOAD_VERSION = "model-work-payload-v1";
    static final int MAX_MESSAGE_BYTES = 1024 * 1024;

    private static final int FRAME_MAGIC = 0x53504C31;
    private static final int DIGEST_BYTES = 32;
    private static final int MAX_STRING_BYTES = 64 * 1024;
    private static final String KIND_WORK = "WORK";
    private static final String KIND_RESULT = "RESULT";
    private static final String KIND_CONTROL = "CONTROL";
    private static final String KIND_HANDOFF = "HANDOFF";
    private static final String KIND_MODEL_WORK = "MODEL_WORK";

    byte[] encode(TransportMessage message) {
        byte[] body = encodeBody(message);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(
                Integer.BYTES + Integer.BYTES + DIGEST_BYTES + body.length);
        try (DataOutputStream output = new DataOutputStream(buffer)) {
            output.writeInt(FRAME_MAGIC);
            output.writeInt(body.length);
            output.write(sha256(body));
            output.write(body);
        } catch (IOException impossible) {
            throw new UncheckedIOException(
                    "encoding a transport message must not fail", impossible);
        }
        return buffer.toByteArray();
    }

    TransportMessage decode(byte[] frame) throws CorruptedSpooledMessageException {
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(frame));
        byte[] body;
        byte[] digest = new byte[DIGEST_BYTES];
        try {
            if (input.readInt() != FRAME_MAGIC) {
                throw new CorruptedSpooledMessageException(
                        "spooled message frame magic is invalid");
            }
            int bodyLength = input.readInt();
            if (bodyLength < 0 || bodyLength > MAX_MESSAGE_BYTES) {
                throw new CorruptedSpooledMessageException(
                        "spooled message body length is invalid");
            }
            input.readFully(digest);
            body = new byte[bodyLength];
            input.readFully(body);
            if (input.read() != -1) {
                throw new CorruptedSpooledMessageException(
                        "spooled message frame has trailing bytes");
            }
        } catch (EOFException truncated) {
            throw new CorruptedSpooledMessageException(
                    "spooled message frame is truncated", truncated);
        } catch (CorruptedSpooledMessageException corrupt) {
            throw corrupt;
        } catch (IOException unreadable) {
            throw new CorruptedSpooledMessageException(
                    "spooled message frame is unreadable", unreadable);
        }
        if (!MessageDigest.isEqual(digest, sha256(body))) {
            throw new CorruptedSpooledMessageException(
                    "spooled message digest does not match its body");
        }
        return decodeBody(body);
    }

    private byte[] encodeBody(TransportMessage message) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(buffer)) {
            MessageEnvelope envelope = message.envelope();
            WireFamily family = WireFamily.forPayload(envelope.payload());
            writeString(output, family.codecVersion());
            writeString(output, message.destination().kind().name());
            writeString(output, message.destination().name());

            writeString(output, family.envelopeVersion());
            writeString(output, envelope.messageId());
            writeString(output, envelope.correlationId());
            writeOptionalString(output, envelope.causationId());
            writeString(output, envelope.logicalRunId());
            writeString(output, envelope.producer());
            output.writeLong(envelope.occurredAt().getEpochSecond());
            output.writeInt(envelope.occurredAt().getNano());
            writePayload(output, envelope.payload(), family);
        } catch (IOException impossible) {
            throw new UncheckedIOException(
                    "encoding a transport message must not fail", impossible);
        }
        return buffer.toByteArray();
    }

    private void writePayload(
            DataOutputStream output,
            MessagePayload payload,
            WireFamily family) throws IOException {
        if (family == WireFamily.MODEL_WORK_V2) {
            if (!(payload instanceof ModelWorkPayload modelWork)) {
                throw new IOException("model-work wire family requires model work");
            }
            writeModelWorkPayload(output, modelWork);
        } else if (payload instanceof WorkPayload work) {
            writeString(output, KIND_WORK);
            writeRevision(output, work.taskRevision());
            writeString(output, work.snapshotId());
            writeStringSet(output, work.allowedTools());
            writeExecutionInput(output, work.executionInput());
        } else if (payload instanceof ResultPayload result) {
            writeString(output, KIND_RESULT);
            writeString(output, result.taskId());
            writeString(output, result.runRecordReference());
            writeString(output, result.verificationStatus().name());
        } else if (payload instanceof ControlPayload control) {
            writeString(output, KIND_CONTROL);
            writeString(output, control.signal().name());
            writeString(output, control.reason());
        } else if (payload instanceof HandoffPayload handoff) {
            writeString(output, KIND_HANDOFF);
            writeRevision(output, handoff.taskRevision());
            writeString(output, handoff.snapshotId());
            writeString(output, handoff.runRecordReference());
        } else {
            throw new IOException("unsupported payload kind");
        }
    }

    private void writeModelWorkPayload(
            DataOutputStream output,
            ModelWorkPayload payload) throws IOException {
        writeString(output, KIND_MODEL_WORK);
        writeString(output, MODEL_WORK_PAYLOAD_VERSION);
        writeRevision(output, payload.taskRevision());
        writeString(output, payload.snapshotId());
        writeCanonicalStringSet(output, payload.allowedTools());
        ModelWorkPayload.ModelInvocationExecutionInput input = payload.executionInput();
        writeString(output, input.targetPath());
        writeString(output, input.expectedResponseSha256());
        writeProfile(output, input.executionProfile());
    }

    private TransportMessage decodeBody(byte[] body) throws CorruptedSpooledMessageException {
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(body));
        try {
            WireFamily family = WireFamily.fromCodecVersion(readString(input));
            DeliveryDestination destination = new DeliveryDestination(
                    readEnum(input, DeliveryDestinationKind.class, "destination kind"),
                    readString(input));

            requireEquals(readString(input), family.envelopeVersion(), "envelope version");
            String messageId = readString(input);
            String correlationId = readString(input);
            Optional<String> causationId = readOptionalString(input);
            String logicalRunId = readString(input);
            String producer = readString(input);
            Instant occurredAt = Instant.ofEpochSecond(input.readLong(), input.readInt());
            MessagePayload payload = readPayload(input, family);

            if (input.read() != -1) {
                throw new CorruptedSpooledMessageException(
                        "spooled message body has trailing bytes");
            }
            TransportMessage message = new TransportMessage(destination, new MessageEnvelope(
                    messageId, correlationId, causationId, logicalRunId, producer,
                    occurredAt, payload));
            if (family == WireFamily.MODEL_WORK_V2
                    && !MessageDigest.isEqual(body, encodeBody(message))) {
                throw new CorruptedSpooledMessageException(
                        "spooled model-work message body is not canonical");
            }
            return message;
        } catch (EOFException truncated) {
            throw new CorruptedSpooledMessageException(
                    "spooled message body is truncated", truncated);
        } catch (CorruptedSpooledMessageException corrupt) {
            throw corrupt;
        } catch (IOException unreadable) {
            throw new CorruptedSpooledMessageException(
                    "spooled message body is unreadable", unreadable);
        } catch (IllegalArgumentException | NullPointerException invalid) {
            throw new CorruptedSpooledMessageException(
                    "spooled message body violates an envelope invariant", invalid);
        }
    }

    private MessagePayload readPayload(DataInputStream input, WireFamily family)
            throws IOException {
        String kind = readString(input);
        if (family == WireFamily.MODEL_WORK_V2) {
            if (!KIND_MODEL_WORK.equals(kind)) {
                throw new CorruptedSpooledMessageException(
                        "spooled message payload kind is unsupported");
            }
            return readModelWorkPayload(input);
        }
        return switch (kind) {
            case KIND_WORK -> new WorkPayload(
                    readRevision(input),
                    readString(input),
                    readStringSet(input),
                    readExecutionInput(input));
            case KIND_RESULT -> new ResultPayload(
                    readString(input),
                    readString(input),
                    readEnum(input, VerificationStatus.class, "verification status"));
            case KIND_CONTROL -> new ControlPayload(
                    readEnum(input, ControlSignal.class, "control signal"),
                    readString(input));
            case KIND_HANDOFF -> new HandoffPayload(
                    readRevision(input), readString(input), readString(input));
            default -> throw new CorruptedSpooledMessageException(
                    "spooled message payload kind is unsupported");
        };
    }

    private ModelWorkPayload readModelWorkPayload(DataInputStream input) throws IOException {
        requireEquals(
                readString(input), MODEL_WORK_PAYLOAD_VERSION, "model-work payload version");
        ApprovedTaskRevision revision = readRevision(input);
        String snapshotId = readString(input);
        Set<String> allowedTools = readCanonicalStringSet(input);
        String targetPath = readString(input);
        String expectedResponseSha256 = readString(input);
        ModelExecutionProfile profile = readProfile(input);
        return new ModelWorkPayload(
                revision,
                snapshotId,
                allowedTools,
                new ModelWorkPayload.ModelInvocationExecutionInput(
                        targetPath, expectedResponseSha256, profile));
    }

    private void writeRevision(DataOutputStream output, ApprovedTaskRevision revision)
            throws IOException {
        writeString(output, revision.taskId());
        writeString(output, revision.sourceDocument());
        writeString(output, revision.sourceSha256());
    }

    private ApprovedTaskRevision readRevision(DataInputStream input) throws IOException {
        return new ApprovedTaskRevision(readString(input), readString(input), readString(input));
    }

    private void writeExecutionInput(
            DataOutputStream output,
            Optional<WorkPayload.ExecutionInput> executionInput) throws IOException {
        output.writeBoolean(executionInput.isPresent());
        if (executionInput.isPresent()) {
            WorkPayload.ExecutionInput input = executionInput.orElseThrow();
            writeString(output, input.targetPath());
            writeString(output, input.expectedContentSha256());
        }
    }

    private Optional<WorkPayload.ExecutionInput> readExecutionInput(DataInputStream input)
            throws IOException {
        if (!input.readBoolean()) {
            return Optional.empty();
        }
        return Optional.of(new WorkPayload.ExecutionInput(readString(input), readString(input)));
    }

    private void writeStringSet(DataOutputStream output, Set<String> values) throws IOException {
        output.writeInt(values.size());
        for (String value : values) {
            writeString(output, value);
        }
    }

    private Set<String> readStringSet(DataInputStream input) throws IOException {
        int size = input.readInt();
        if (size < 0 || size > WorkPayload.MAX_ALLOWED_TOOLS) {
            throw new CorruptedSpooledMessageException(
                    "spooled message collection size is invalid");
        }
        Set<String> values = new LinkedHashSet<>();
        for (int index = 0; index < size; index++) {
            values.add(readString(input));
        }
        return values;
    }

    private void writeCanonicalStringSet(DataOutputStream output, Set<String> values)
            throws IOException {
        List<String> canonical = new ArrayList<>(values);
        Collections.sort(canonical);
        output.writeInt(canonical.size());
        for (String value : canonical) {
            writeString(output, value);
        }
    }

    private Set<String> readCanonicalStringSet(DataInputStream input) throws IOException {
        int size = input.readInt();
        if (size <= 0 || size > ModelWorkPayload.MAX_ALLOWED_TOOLS) {
            throw new CorruptedSpooledMessageException(
                    "spooled model-work collection size is invalid");
        }
        Set<String> values = new LinkedHashSet<>();
        for (int index = 0; index < size; index++) {
            if (!values.add(readString(input))) {
                throw new CorruptedSpooledMessageException(
                        "spooled model-work collection contains a duplicate");
            }
        }
        return values;
    }

    private void writeProfile(DataOutputStream output, ModelExecutionProfile profile)
            throws IOException {
        writeString(output, profile.schemaVersion());
        writeString(output, profile.requiredCapability());
        writeString(output, profile.modelClass());
        writeString(output, profile.localityRequirement().name());
        writeString(output, profile.reasoningRequirement().name());
        output.writeLong(profile.minimumContextTokens());
        output.writeLong(profile.tokenBudget().maxInputTokens());
        output.writeLong(profile.tokenBudget().maxOutputTokens());
        output.writeLong(profile.tokenBudget().maxTotalTokens());
        writeString(output, profile.costBudget().currencyCode());
        output.writeLong(profile.costBudget().maxMicrounits());
        output.writeLong(profile.maximumInvocationTime().getSeconds());
        output.writeInt(profile.maximumInvocationTime().getNano());
        writeString(output, profile.dataClassification().name());
    }

    private ModelExecutionProfile readProfile(DataInputStream input) throws IOException {
        String schemaVersion = readString(input);
        String requiredCapability = readString(input);
        String modelClass = readString(input);
        ModelLocalityRequirement locality = readEnum(
                input, ModelLocalityRequirement.class, "model locality requirement");
        ModelReasoningRequirement reasoning = readEnum(
                input, ModelReasoningRequirement.class, "model reasoning requirement");
        long minimumContextTokens = input.readLong();
        ModelTokenBudget tokenBudget = new ModelTokenBudget(
                input.readLong(), input.readLong(), input.readLong());
        ModelCostBudget costBudget = new ModelCostBudget(
                readString(input), input.readLong());
        long durationSeconds = input.readLong();
        int durationNanos = input.readInt();
        if (durationNanos < 0 || durationNanos > 999_999_999) {
            throw new CorruptedSpooledMessageException(
                    "spooled model-work duration nanoseconds are invalid");
        }
        Duration maximumInvocationTime = Duration.ofSeconds(
                durationSeconds, durationNanos);
        ModelDataClassification classification = readEnum(
                input, ModelDataClassification.class, "model data classification");
        return new ModelExecutionProfile(
                schemaVersion,
                requiredCapability,
                modelClass,
                locality,
                reasoning,
                minimumContextTokens,
                tokenBudget,
                costBudget,
                maximumInvocationTime,
                classification);
    }

    private void writeOptionalString(DataOutputStream output, Optional<String> value)
            throws IOException {
        output.writeBoolean(value.isPresent());
        if (value.isPresent()) {
            writeString(output, value.orElseThrow());
        }
    }

    private Optional<String> readOptionalString(DataInputStream input) throws IOException {
        return input.readBoolean() ? Optional.of(readString(input)) : Optional.empty();
    }

    private void writeString(DataOutputStream output, String value) throws IOException {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        if (encoded.length > MAX_STRING_BYTES) {
            throw new IOException("spooled message string exceeds supported bounds");
        }
        output.writeInt(encoded.length);
        output.write(encoded);
    }

    private String readString(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length < 0 || length > MAX_STRING_BYTES) {
            throw new CorruptedSpooledMessageException(
                    "spooled message string length is invalid");
        }
        byte[] encoded = new byte[length];
        input.readFully(encoded);
        try {
            CharBuffer decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(encoded));
            return decoded.toString();
        } catch (CharacterCodingException malformed) {
            throw new CorruptedSpooledMessageException(
                    "spooled message string is not valid UTF-8", malformed);
        }
    }

    private <E extends Enum<E>> E readEnum(DataInputStream input, Class<E> type, String label)
            throws IOException {
        String name = readString(input);
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException unsupported) {
            throw new CorruptedSpooledMessageException(
                    "spooled message " + label + " is unsupported", unsupported);
        }
    }

    private static void requireEquals(String actual, String expected, String label)
            throws CorruptedSpooledMessageException {
        if (!expected.equals(actual)) {
            throw new CorruptedSpooledMessageException(
                    "spooled message " + label + " is unsupported");
        }
    }

    private enum WireFamily {
        LEGACY_V1(CODEC_VERSION, MessageEnvelope.ENVELOPE_VERSION),
        MODEL_WORK_V2(MODEL_WORK_CODEC_VERSION, MODEL_WORK_ENVELOPE_VERSION);

        private final String codecVersion;
        private final String envelopeVersion;

        WireFamily(String codecVersion, String envelopeVersion) {
            this.codecVersion = codecVersion;
            this.envelopeVersion = envelopeVersion;
        }

        String codecVersion() {
            return codecVersion;
        }

        String envelopeVersion() {
            return envelopeVersion;
        }

        static WireFamily forPayload(MessagePayload payload) {
            return payload instanceof ModelWorkPayload ? MODEL_WORK_V2 : LEGACY_V1;
        }

        static WireFamily fromCodecVersion(String version)
                throws CorruptedSpooledMessageException {
            if (CODEC_VERSION.equals(version)) {
                return LEGACY_V1;
            }
            if (MODEL_WORK_CODEC_VERSION.equals(version)) {
                return MODEL_WORK_V2;
            }
            throw new CorruptedSpooledMessageException(
                    "spooled message codec version is unsupported");
        }
    }

    private static byte[] sha256(byte[] content) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(content);
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 must be available", unavailable);
        }
    }
}
