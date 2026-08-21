package com.enhancer.bus;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enhancer.kernel.VerificationStatus;
import com.enhancer.model.ModelCostBudget;
import com.enhancer.model.ModelDataClassification;
import com.enhancer.model.ModelExecutionProfile;
import com.enhancer.model.ModelLocalityRequirement;
import com.enhancer.model.ModelReasoningRequirement;
import com.enhancer.model.ModelTokenBudget;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Verifies the wire format without a filesystem. Publication concerns — atomic move, spool
 * capacity, unusable roots — belong to {@link FileSpoolMessageTransportTest}.
 *
 * <p>Every round trip carries a nanosecond-bearing instant and supplementary characters, so each
 * payload kind proves precision and strict UTF-8 rather than leaving both to one dedicated case.
 */
class MessageEnvelopeCodecTest {
    private static final ApprovedTaskRevision TASK_REVISION = new ApprovedTaskRevision(
            "gate-8-file-spool-transport", "CURRENT_TASK.md", "a".repeat(64));
    private static final String SNAPSHOT_ID = "b".repeat(64);
    private static final Instant OCCURRED_AT = Instant.parse("2026-07-20T08:00:00.123456789Z");
    private static final String MESSAGE_ID = "00000000-0000-0000-0000-0000000000a1";
    private static final String CAUSATION_ID = "00000000-0000-0000-0000-0000000000c1";
    private static final String RESULT_TASK_ID = "gate-8-file-spool-transport";
    private static final String RESULT_REFERENCE =
            "run-record/00000000-0000-0000-0000-0000000000d1";

    private static final int FRAME_MAGIC = 0x53504C31;
    private static final String CODEC_VERSION = "transport-spool-v1";
    private static final String MODEL_WORK_CODEC_VERSION = "transport-spool-v2";
    private static final String MODEL_WORK_ENVELOPE_VERSION = "message-envelope-v2";
    private static final String MODEL_WORK_PAYLOAD_VERSION = "model-work-payload-v1";

    private static final String LEGACY_WORK_FRAME =
            "U1BMMQAAAg51xiHJZRQS8/NVT0aBhJMI6Zlt95qq/jVvWq3ihJbIHAAAABJ0cmFuc3BvcnQtc3Bvb2wtdjEAAAAFUVVFVUUAAAAMd29ya2VyLXRhc2tzAAAAE21lc3NhZ2UtZW52ZWxvcGUtdjEAAAAkMDAwMDAwMDAtMDAwMC0wMDAwLTAwMDAtMDAwMDAwMDAwMGExAAAAE2NvcnJlbGF0aW9uLXNwb29sLTEBAAAAJDAwMDAwMDAwLTAwMDAtMDAwMC0wMDAwLTAwMDAwMDAwMDBjMQAAABNsb2dpY2FsLXJ1bi1zcG9vbC0xAAAAFGFnZW50LWxvb3Atw7DFuMWh4oKsAAAAAGpd1YAHW80VAAAABFdPUksAAAAbZ2F0ZS04LWZpbGUtc3Bvb2wtdHJhbnNwb3J0AAAAD0NVUlJFTlRfVEFTSy5tZAAAAEBhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhAAAAQGJiYmJiYmJiYmJiYmJiYmJiYmJiYmJiYmJiYmJiYmJiYmJiYmJiYmJiYmJiYmJiYmJiYmJiYmJiYmJiYmJiYmIAAAABAAAACXJlYWQtZmlsZQEAAAAJUkVBRE1FLm1kAAAAQGNjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2M=";
    private static final String LEGACY_RESULT_FRAME =
            "U1BMMQAAASvo8lPQev2xjHDs8ZGCWBl1JMZb8/5AMBDeKpBArxID1wAAABJ0cmFuc3Bv"
                    + "cnQtc3Bvb2wtdjEAAAAFVE9QSUMAAAAOcnVudGltZS1ldmVudHMAAAATbWVzc2FnZS1lbnZl"
                    + "bG9wZS12MQAAACQwMDAwMDAwMC0wMDAwLTAwMDAtMDAwMC0wMDAwMDAwMDAwYTEAAAAT"
                    + "Y29ycmVsYXRpb24tc3Bvb2wtMQAAAAATbG9naWNhbC1ydW4tc3Bvb2wtMQAAABRhZ2VudC1s"
                    + "b29wLcOwxbjFoeKCrAAAAABqXdWAB1vNFQAAAAZSRVNVTFQAAAAbZ2F0ZS04LWZpbGUtc3Bv"
                    + "b2wtdHJhbnNwb3J0AAAAL3J1bi1yZWNvcmQvMDAwMDAwMDAtMDAwMC0wMDAwLTAwMDAtMDAw"
                    + "MDAwMDAwMGQxAAAACFZFUklGSUVE";
    private static final String LEGACY_CONTROL_FRAME =
            "U1BMMQAAAQ7ieZ+N/llLY4YVeC6Byku6W3FXB12b9PR5qIyTHR7k/wAAABJ0cmFuc3Bv"
                    + "cnQtc3Bvb2wtdjEAAAAFVE9QSUMAAAAIY29udHJvbHMAAAATbWVzc2FnZS1lbnZlbG9wZS12"
                    + "MQAAACQwMDAwMDAwMC0wMDAwLTAwMDAtMDAwMC0wMDAwMDAwMDAwYTEAAAATY29ycmVs"
                    + "YXRpb24tc3Bvb2wtMQEAAAAkMDAwMDAwMDAtMDAwMC0wMDAwLTAwMDAtMDAwMDAwMDAwMGMx"
                    + "AAAAE2xvZ2ljYWwtcnVuLXNwb29sLTEAAAAUYWdlbnQtbG9vcC3DsMW4xaHigqwAAAAAal3Vg"
                    + "AdbzRUAAAAHQ09OVFJPTAAAAAZDQU5DRUwAAAAQb3BlcmF0b3IgcmVxdWVzdA==";
    private static final String LEGACY_HANDOFF_FRAME =
            "U1BMMQAAAbWHPvosV/R2UmvLOAPDyDVER43SKRPFi/UiSHoI5d0YQAAAABJ0cmFuc3BvcnQtc3Bvb2wtdjEAAAAFUVVFVUUAAAAIcmV2aWV3ZXIAAAATbWVzc2FnZS1lbnZlbG9wZS12MQAAACQwMDAwMDAwMC0wMDAwLTAwMDAtMDAwMC0wMDAwMDAwMDAwYTEAAAATY29ycmVsYXRpb24tc3Bvb2wtMQAAAAATbG9naWNhbC1ydW4tc3Bvb2wtMQAAABRhZ2VudC1sb29wLcOwxbjFoeKCrAAAAABqXdWAB1vNFQAAAAdIQU5ET0ZGAAAAG2dhdGUtOC1maWxlLXNwb29sLXRyYW5zcG9ydAAAAA9DVVJSRU5UX1RBU0subWQAAABAYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYQAAAEBlZWVlZWVlZWVlZWVlZWVlZWVlZWVlZWVlZWVlZWVlZWVlZWVlZWVlZWVlZWVlZWVlZWVlZWVlZWVlZWVlZWVlAAAAL3J1bi1yZWNvcmQvMDAwMDAwMDAtMDAwMC0wMDAwLTAwMDAtMDAwMDAwMDAwMGUx";

    private final MessageEnvelopeCodec codec = new MessageEnvelopeCodec();

    @Test
    void preservesEveryLegacyPayloadFrameByteForByte() {
        assertEquals(LEGACY_WORK_FRAME,
                Base64.getEncoder().encodeToString(codec.encode(legacyWorkMessage())));
        assertEquals(LEGACY_RESULT_FRAME,
                Base64.getEncoder().encodeToString(codec.encode(resultMessage())));
        assertEquals(LEGACY_CONTROL_FRAME,
                Base64.getEncoder().encodeToString(codec.encode(legacyControlMessage())));
        assertEquals(LEGACY_HANDOFF_FRAME,
                Base64.getEncoder().encodeToString(codec.encode(legacyHandoffMessage())));
    }

    @Test
    void roundTripsOneExactCompleteModelWorkProfileWithCanonicalBytes() throws Exception {
        TransportMessage message = modelWorkMessage(Set.of("read-file", "model-invoke"));
        byte[] expected = frame(modelWorkBody(
                MODEL_WORK_CODEC_VERSION,
                MODEL_WORK_ENVELOPE_VERSION,
                "MODEL_WORK",
                MODEL_WORK_PAYLOAD_VERSION,
                new String[] {"model-invoke", "read-file"},
                ModelExecutionProfile.SCHEMA_VERSION,
                ModelLocalityRequirement.LOCAL_ONLY.name(),
                0));

        assertArrayEquals(expected, codec.encode(message));
        assertEquals(message, codec.decode(expected));
        assertEquals(OCCURRED_AT, codec.decode(expected).envelope().occurredAt());

        LinkedHashSet<String> reversed = new LinkedHashSet<>();
        reversed.add("read-file");
        reversed.add("model-invoke");
        assertArrayEquals(codec.encode(message), codec.encode(modelWorkMessage(reversed)));
    }

    @Test
    void rejectsEveryCrossFamilyOrNonCanonicalModelWorkBody() throws Exception {
        assertThrows(CorruptedSpooledMessageException.class, () -> codec.decode(frame(
                modelWorkBody(CODEC_VERSION, MessageEnvelope.ENVELOPE_VERSION,
                        "MODEL_WORK", MODEL_WORK_PAYLOAD_VERSION,
                        new String[] {"model-invoke"}, ModelExecutionProfile.SCHEMA_VERSION,
                        ModelLocalityRequirement.LOCAL_ONLY.name(), 0))));
        assertThrows(CorruptedSpooledMessageException.class, () -> codec.decode(frame(
                resultBody(MODEL_WORK_CODEC_VERSION, MODEL_WORK_ENVELOPE_VERSION,
                        "TOPIC", "RESULT", "VERIFIED"))));
        assertThrows(CorruptedSpooledMessageException.class, () -> codec.decode(frame(
                modelWorkBody(MODEL_WORK_CODEC_VERSION, MessageEnvelope.ENVELOPE_VERSION,
                        "MODEL_WORK", MODEL_WORK_PAYLOAD_VERSION,
                        new String[] {"model-invoke"}, ModelExecutionProfile.SCHEMA_VERSION,
                        ModelLocalityRequirement.LOCAL_ONLY.name(), 0))));
        assertThrows(CorruptedSpooledMessageException.class, () -> codec.decode(frame(
                modelWorkBody(CODEC_VERSION, MODEL_WORK_ENVELOPE_VERSION,
                        "MODEL_WORK", MODEL_WORK_PAYLOAD_VERSION,
                        new String[] {"model-invoke"}, ModelExecutionProfile.SCHEMA_VERSION,
                        ModelLocalityRequirement.LOCAL_ONLY.name(), 0))));
        assertThrows(CorruptedSpooledMessageException.class, () -> codec.decode(frame(
                modelWorkBody(MODEL_WORK_CODEC_VERSION, MODEL_WORK_ENVELOPE_VERSION,
                        "MODEL_WORK", MODEL_WORK_PAYLOAD_VERSION,
                        new String[] {"read-file", "model-invoke"},
                        ModelExecutionProfile.SCHEMA_VERSION,
                        ModelLocalityRequirement.LOCAL_ONLY.name(), 0))));
        assertThrows(CorruptedSpooledMessageException.class, () -> codec.decode(frame(
                modelWorkBody(MODEL_WORK_CODEC_VERSION, MODEL_WORK_ENVELOPE_VERSION,
                        "MODEL_WORK", MODEL_WORK_PAYLOAD_VERSION,
                        new String[] {"model-invoke", "model-invoke"},
                        ModelExecutionProfile.SCHEMA_VERSION,
                        ModelLocalityRequirement.LOCAL_ONLY.name(), 0))));
    }

    @Test
    void rejectsUnknownOrInvalidModelWorkVersionsAndProfileValues() throws Exception {
        assertRejectsModelBody("MODEL_WORK", "model-work-payload-v2",
                ModelExecutionProfile.SCHEMA_VERSION,
                ModelLocalityRequirement.LOCAL_ONLY.name(), 0);
        assertRejectsModelBody("MODEL_TELEMETRY", MODEL_WORK_PAYLOAD_VERSION,
                ModelExecutionProfile.SCHEMA_VERSION,
                ModelLocalityRequirement.LOCAL_ONLY.name(), 0);
        assertRejectsModelBody("MODEL_WORK", MODEL_WORK_PAYLOAD_VERSION,
                "model-execution-profile-v2",
                ModelLocalityRequirement.LOCAL_ONLY.name(), 0);
        assertRejectsModelBody("MODEL_WORK", MODEL_WORK_PAYLOAD_VERSION,
                ModelExecutionProfile.SCHEMA_VERSION, "REMOTE_ALLOWED", 0);
        assertRejectsModelBody("MODEL_WORK", MODEL_WORK_PAYLOAD_VERSION,
                ModelExecutionProfile.SCHEMA_VERSION,
                ModelLocalityRequirement.LOCAL_ONLY.name(), 1_000_000_000);
        assertRejectsModelBody("MODEL_WORK", MODEL_WORK_PAYLOAD_VERSION,
                ModelExecutionProfile.SCHEMA_VERSION,
                ModelLocalityRequirement.LOCAL_ONLY.name(), 1_500_000);
    }

    @Test
    void roundTripsAWorkMessageWithExecutionInputAndCausation() throws Exception {
        assertRoundTrip(
                DeliveryDestination.queue("worker-tasks"),
                Optional.of(CAUSATION_ID),
                new WorkPayload(TASK_REVISION, SNAPSHOT_ID, Set.of("read-file", "verify"),
                        Optional.of(new WorkPayload.ExecutionInput(
                                "docs/target-🚀.md", "c".repeat(64)))));
    }

    @Test
    void roundTripsAWorkMessageWithoutExecutionInputOrCausation() throws Exception {
        assertRoundTrip(
                DeliveryDestination.queue("worker-tasks"),
                Optional.empty(),
                new WorkPayload(TASK_REVISION, SNAPSHOT_ID, Set.of("read-file")));
    }

    @Test
    void roundTripsAWorkMessageAtMaximumToolCardinality() throws Exception {
        Set<String> tools = new LinkedHashSet<>();
        for (int index = 0; index < WorkPayload.MAX_ALLOWED_TOOLS; index++) {
            tools.add("tool-" + index);
        }

        assertRoundTrip(
                DeliveryDestination.queue("worker-tasks"),
                Optional.empty(),
                new WorkPayload(TASK_REVISION, SNAPSHOT_ID, tools));
    }

    @Test
    void roundTripsAResultMessage() throws Exception {
        assertRoundTrip(
                DeliveryDestination.topic("runtime-events"),
                Optional.empty(),
                new ResultPayload("gate-8-file-spool-transport",
                        "run-record/00000000-0000-0000-0000-0000000000d1",
                        VerificationStatus.VERIFIED));
    }

    @Test
    void roundTripsAControlMessage() throws Exception {
        assertRoundTrip(
                DeliveryDestination.topic("controls"),
                Optional.of(CAUSATION_ID),
                new ControlPayload(ControlSignal.CANCEL, "operator request 🚀"));
    }

    @Test
    void roundTripsAHandoffMessage() throws Exception {
        assertRoundTrip(
                DeliveryDestination.queue("reviewer"),
                Optional.empty(),
                new HandoffPayload(TASK_REVISION, "e".repeat(64),
                        "run-record/00000000-0000-0000-0000-0000000000e1"));
    }

    @Test
    void encodesDeterministically() {
        // No wall-clock or random state enters the frame, so a peer may deduplicate on content.
        assertArrayEquals(codec.encode(workMessage()), codec.encode(workMessage()));
    }

    @Test
    void failsClosedOnEveryFrameCorruption() {
        byte[] frame = codec.encode(workMessage());

        byte[] badMagic = frame.clone();
        badMagic[0] ^= 0x7f;
        assertThrows(CorruptedSpooledMessageException.class, () -> codec.decode(badMagic));

        byte[] digestMismatch = frame.clone();
        digestMismatch[digestMismatch.length - 1] ^= 0x01;
        assertThrows(CorruptedSpooledMessageException.class, () -> codec.decode(digestMismatch));

        byte[] truncated = java.util.Arrays.copyOf(frame, frame.length - 1);
        assertThrows(CorruptedSpooledMessageException.class, () -> codec.decode(truncated));

        byte[] trailing = java.util.Arrays.copyOf(frame, frame.length + 1);
        assertThrows(CorruptedSpooledMessageException.class, () -> codec.decode(trailing));

        assertThrows(CorruptedSpooledMessageException.class, () -> codec.decode(new byte[0]));
        assertThrows(CorruptedSpooledMessageException.class,
                () -> codec.decode(new byte[] {0, 1, 2, 3}));
    }

    @Test
    void acceptsTheHandCraftedBaselineFrame() throws Exception {
        // Proves the four rejection cases below differ from a decodable frame in exactly one
        // field. Without this, a malformed body would make them pass by truncation rather than by
        // the guard under test -- which is how an earlier version of this test passed against a
        // codec whose version guard had been removed.
        assertEquals(
                codec.decode(codec.encode(resultMessage())),
                codec.decode(frame(resultBody(CODEC_VERSION, "TOPIC", "RESULT", "VERIFIED"))));
    }

    @Test
    void failsClosedOnAFrameFromAnIncompatiblePeer() throws IOException {
        // A peer running a newer format must be refused, not partially decoded. Every frame here
        // is internally consistent -- correct magic, length, digest, and a complete body -- so
        // only the version, kind, or enum guard can reject it.
        assertThrows(CorruptedSpooledMessageException.class,
                () -> codec.decode(frame(
                        resultBody("transport-spool-v2", "TOPIC", "RESULT", "VERIFIED"))),
                "an unsupported codec version must be refused");

        assertThrows(CorruptedSpooledMessageException.class,
                () -> codec.decode(frame(
                        resultBody("transport-spool-v3", "TOPIC", "RESULT", "VERIFIED"))),
                "an unknown codec version must be refused");

        assertThrows(CorruptedSpooledMessageException.class,
                () -> codec.decode(frame(
                        resultBody(CODEC_VERSION, "MULTICAST", "RESULT", "VERIFIED"))),
                "an unknown destination kind must be refused");

        assertThrows(CorruptedSpooledMessageException.class,
                () -> codec.decode(frame(
                        resultBody(CODEC_VERSION, "TOPIC", "TELEMETRY", "VERIFIED"))),
                "an unknown payload kind must be refused");

        assertThrows(CorruptedSpooledMessageException.class,
                () -> codec.decode(frame(
                        resultBody(CODEC_VERSION, "TOPIC", "RESULT", "PARTIALLY_VERIFIED"))),
                "an unknown verification status must be refused");

        assertThrows(CorruptedSpooledMessageException.class,
                () -> codec.decode(frame(resultBody(
                        CODEC_VERSION, "message-envelope-v2", "TOPIC", "RESULT", "VERIFIED"))),
                "an unsupported envelope version must be refused");
    }

    @Test
    void failsClosedOnACollectionLargerThanTheContractAllows() throws IOException {
        // The cardinality ceiling is a decode-side guard, not merely an encode-side one: a peer
        // that ignores it must not be able to make this process allocate an unbounded set.
        byte[] body = body(output -> {
            writeString(output, CODEC_VERSION);
            writeString(output, "QUEUE");
            writeString(output, "worker-tasks");
            writeString(output, MessageEnvelope.ENVELOPE_VERSION);
            writeString(output, MESSAGE_ID);
            writeString(output, "correlation-spool-1");
            output.writeBoolean(false);
            writeString(output, "logical-run-spool-1");
            writeString(output, "agent-loop");
            output.writeLong(OCCURRED_AT.getEpochSecond());
            output.writeInt(OCCURRED_AT.getNano());
            writeString(output, "WORK");
            writeString(output, TASK_REVISION.taskId());
            writeString(output, TASK_REVISION.sourceDocument());
            writeString(output, TASK_REVISION.sourceSha256());
            writeString(output, SNAPSHOT_ID);
            output.writeInt(WorkPayload.MAX_ALLOWED_TOOLS + 1);
            for (int index = 0; index <= WorkPayload.MAX_ALLOWED_TOOLS; index++) {
                writeString(output, "tool-" + index);
            }
            output.writeBoolean(false);
        });

        assertThrows(CorruptedSpooledMessageException.class, () -> codec.decode(frame(body)));
    }

    private void assertRejectsModelBody(
            String kind,
            String payloadVersion,
            String profileVersion,
            String locality,
            int durationNanos) throws Exception {
        byte[] body = modelWorkBody(
                MODEL_WORK_CODEC_VERSION,
                MODEL_WORK_ENVELOPE_VERSION,
                kind,
                payloadVersion,
                new String[] {"model-invoke"},
                profileVersion,
                locality,
                durationNanos);
        assertThrows(CorruptedSpooledMessageException.class, () -> codec.decode(frame(body)));
    }

    private static byte[] modelWorkBody(
            String codecVersion,
            String envelopeVersion,
            String payloadKind,
            String payloadVersion,
            String[] tools,
            String profileVersion,
            String locality,
            int durationNanos) throws IOException {
        return body(output -> {
            writeString(output, codecVersion);
            writeString(output, "QUEUE");
            writeString(output, "model-work");
            writeString(output, envelopeVersion);
            writeString(output, MESSAGE_ID);
            writeString(output, "correlation-spool-1");
            output.writeBoolean(false);
            writeString(output, "logical-run-spool-1");
            writeString(output, "agent-loop");
            output.writeLong(OCCURRED_AT.getEpochSecond());
            output.writeInt(OCCURRED_AT.getNano());
            writeString(output, payloadKind);
            writeString(output, payloadVersion);
            writeString(output, TASK_REVISION.taskId());
            writeString(output, TASK_REVISION.sourceDocument());
            writeString(output, TASK_REVISION.sourceSha256());
            writeString(output, SNAPSHOT_ID);
            output.writeInt(tools.length);
            for (String tool : tools) {
                writeString(output, tool);
            }
            writeString(output, "docs/prompt.md");
            writeString(output, "d".repeat(64));
            writeString(output, profileVersion);
            writeString(output, "repository-analysis");
            writeString(output, "reasoning-standard");
            writeString(output, locality);
            writeString(output, ModelReasoningRequirement.STANDARD.name());
            output.writeLong(8192);
            output.writeLong(2048);
            output.writeLong(1024);
            output.writeLong(4096);
            writeString(output, "USD");
            output.writeLong(10_000);
            output.writeLong(30);
            output.writeInt(durationNanos);
            writeString(output, ModelDataClassification.CONFIDENTIAL.name());
        });
    }

    private static TransportMessage modelWorkMessage(Set<String> tools) {
        return new TransportMessage(
                DeliveryDestination.queue("model-work"),
                new MessageEnvelope(
                        MESSAGE_ID,
                        "correlation-spool-1",
                        Optional.empty(),
                        "logical-run-spool-1",
                        "agent-loop",
                        OCCURRED_AT,
                        new ModelWorkPayload(
                                TASK_REVISION,
                                SNAPSHOT_ID,
                                tools,
                                new ModelWorkPayload.ModelInvocationExecutionInput(
                                        "docs/prompt.md", "d".repeat(64), modelProfile()))));
    }

    private static ModelExecutionProfile modelProfile() {
        return new ModelExecutionProfile(
                ModelExecutionProfile.SCHEMA_VERSION,
                "repository-analysis",
                "reasoning-standard",
                ModelLocalityRequirement.LOCAL_ONLY,
                ModelReasoningRequirement.STANDARD,
                8192,
                new ModelTokenBudget(2048, 1024, 4096),
                new ModelCostBudget("USD", 10_000),
                Duration.ofSeconds(30),
                ModelDataClassification.CONFIDENTIAL);
    }

    private static TransportMessage legacyWorkMessage() {
        return new TransportMessage(
                DeliveryDestination.queue("worker-tasks"),
                envelope(Optional.of(CAUSATION_ID), new WorkPayload(
                        TASK_REVISION,
                        SNAPSHOT_ID,
                        Set.of("read-file"),
                        Optional.of(new WorkPayload.ExecutionInput(
                                "README.md", "c".repeat(64))))));
    }

    private static TransportMessage legacyControlMessage() {
        return new TransportMessage(
                DeliveryDestination.topic("controls"),
                envelope(Optional.of(CAUSATION_ID),
                        new ControlPayload(ControlSignal.CANCEL, "operator request")));
    }

    private static TransportMessage legacyHandoffMessage() {
        return new TransportMessage(
                DeliveryDestination.queue("reviewer"),
                envelope(Optional.empty(), new HandoffPayload(
                        TASK_REVISION,
                        "e".repeat(64),
                        "run-record/00000000-0000-0000-0000-0000000000e1")));
    }

    private static TransportMessage resultMessage() {
        return new TransportMessage(
                DeliveryDestination.topic("runtime-events"),
                envelope(Optional.empty(),
                        new ResultPayload(RESULT_TASK_ID, RESULT_REFERENCE,
                                VerificationStatus.VERIFIED)));
    }

    /** A complete result-message body, parameterized on exactly the four guarded fields. */
    private static byte[] resultBody(
            String codecVersion,
            String destinationKind,
            String payloadKind,
            String verificationStatus) throws IOException {
        return resultBody(codecVersion, MessageEnvelope.ENVELOPE_VERSION,
                destinationKind, payloadKind, verificationStatus);
    }

    private static byte[] resultBody(
            String codecVersion,
            String envelopeVersion,
            String destinationKind,
            String payloadKind,
            String verificationStatus) throws IOException {
        return body(output -> {
            writeString(output, codecVersion);
            writeString(output, destinationKind);
            writeString(output, "runtime-events");
            writeString(output, envelopeVersion);
            writeString(output, MESSAGE_ID);
            writeString(output, "correlation-spool-1");
            output.writeBoolean(false);
            writeString(output, "logical-run-spool-1");
            writeString(output, "agent-loop-🚀");
            output.writeLong(OCCURRED_AT.getEpochSecond());
            output.writeInt(OCCURRED_AT.getNano());
            writeString(output, payloadKind);
            writeString(output, RESULT_TASK_ID);
            writeString(output, RESULT_REFERENCE);
            writeString(output, verificationStatus);
        });
    }

    private void assertRoundTrip(
            DeliveryDestination destination,
            Optional<String> causationId,
            MessagePayload payload) throws Exception {
        TransportMessage message =
                new TransportMessage(destination, envelope(causationId, payload));

        TransportMessage decoded = codec.decode(codec.encode(message));

        assertEquals(message, decoded);
        assertEquals(OCCURRED_AT, decoded.envelope().occurredAt(),
                "occurrence time must survive to the nanosecond");
    }

    private static TransportMessage workMessage() {
        return new TransportMessage(
                DeliveryDestination.queue("worker-tasks"),
                envelope(Optional.empty(),
                        new WorkPayload(TASK_REVISION, SNAPSHOT_ID, Set.of("read-file"))));
    }

    private static MessageEnvelope envelope(
            Optional<String> causationId, MessagePayload payload) {
        return new MessageEnvelope(
                MESSAGE_ID,
                "correlation-spool-1",
                causationId,
                "logical-run-spool-1",
                "agent-loop-🚀",
                OCCURRED_AT,
                payload);
    }

    @FunctionalInterface
    private interface BodyWriter {
        void write(DataOutputStream output) throws IOException;
    }

    private static byte[] body(BodyWriter writer) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(buffer)) {
            writer.write(output);
        }
        return buffer.toByteArray();
    }

    /** Wraps a body in a well-formed frame, so only the body's content can be rejected. */
    private static byte[] frame(byte[] body) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(buffer)) {
            output.writeInt(FRAME_MAGIC);
            output.writeInt(body.length);
            output.write(sha256(body));
            output.write(body);
        }
        return buffer.toByteArray();
    }

    private static void writeString(DataOutputStream output, String value) throws IOException {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(encoded.length);
        output.write(encoded);
    }

    private static byte[] sha256(byte[] content) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(content);
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 must be available", unavailable);
        }
    }
}
