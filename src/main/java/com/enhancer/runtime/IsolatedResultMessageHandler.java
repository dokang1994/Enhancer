package com.enhancer.runtime;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.MessageHandler;
import com.enhancer.bus.MessagePayload;
import com.enhancer.bus.ResultPayload;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.model.ModelInvokeTool;
import com.enhancer.run.RunRecord;
import com.enhancer.run.RunRecordStore;
import com.enhancer.tool.ReadFileTool;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * Validates one isolated-worker Result delivery against the dispatched Work and shared
 * RunRecord. It has no persistence, execution, finalization, or queue authority.
 */
final class IsolatedResultMessageHandler implements MessageHandler {
    private final WorkItem workItem;
    private final RunRecordStore runRecordStore;
    private Optional<String> acceptedReference = Optional.empty();

    IsolatedResultMessageHandler(
            WorkItem workItem,
            RunRecordStore runRecordStore) {
        this.workItem = Objects.requireNonNull(workItem, "workItem must not be null");
        this.runRecordStore = Objects.requireNonNull(
                runRecordStore, "runRecordStore must not be null");
    }

    @Override
    public void handle(MessageEnvelope result) {
        Objects.requireNonNull(result, "result must not be null");
        if (acceptedReference.isPresent()) {
            throw new IllegalStateException(
                    "the isolated result handler accepted more than one delivery");
        }
        try {
            acceptedReference = Optional.of(validate(result));
        } catch (IOException invalid) {
            throw new IllegalArgumentException(invalid.getMessage(), invalid);
        }
    }

    Optional<String> acceptedReference() {
        return acceptedReference;
    }

    private String validate(MessageEnvelope result) throws IOException {
        MessageEnvelope work = workItem.workMessage();
        requireEqual(work.correlationId(), result.correlationId(), "correlation identity");
        requireEqual(work.logicalRunId(), result.logicalRunId(), "logical run identity");
        requireEqual(
                work.messageId(),
                result.causationId().orElseThrow(() -> new IOException(
                        "the result envelope names no causation identity")),
                "causation identity");

        MessagePayload payload = result.payload();
        if (!(payload instanceof ResultPayload resultPayload)) {
            throw new IOException("the result envelope does not carry a ResultPayload");
        }
        requireEqual(
                workItem.taskRevision().taskId(), resultPayload.taskId(), "task identity");

        RunRecord record = runRecordStore
                .resolve(resultPayload.runRecordReference())
                .record();
        requireRunRecordBinding(record, workItem);
        VerificationStatus recorded = record.verification().status();
        if (recorded != resultPayload.verificationStatus()) {
            throw new IOException("the child claimed verification status "
                    + resultPayload.verificationStatus()
                    + " but the resolved RunRecord records " + recorded);
        }
        return resultPayload.runRecordReference();
    }

    static void requireRunRecordBinding(RunRecord record, WorkItem workItem)
            throws IOException {
        try {
            DurableAgentRunFinalizer.requireBinding(record, workItem);
        } catch (IllegalArgumentException mismatch) {
            throw new IOException(mismatch.getMessage(), mismatch);
        }

        ExecutionInput expected = executionInput(workItem);
        boolean readFileScoped = workItem.allowedTools().contains(ReadFileTool.NAME);
        String expectedToolName = readFileScoped
                ? ReadFileTool.NAME
                : ModelInvokeTool.NAME;
        String pathArgument = readFileScoped
                ? ReadFileTool.PATH_ARGUMENT
                : ModelInvokeTool.PROMPT_PATH_ARGUMENT;
        if (!record.toolRequest().toolName().equals(expectedToolName)) {
            throw new IOException(
                    "the RunRecord Tool request does not match isolated execution");
        }
        String target = record.toolRequest()
                .arguments()
                .get(pathArgument);
        if (!expected.targetPath().equals(target)) {
            throw new IOException(
                    "the RunRecord execution target does not match the dispatched work");
        }
        if (record.verification().status() != VerificationStatus.NOT_PERFORMED
                && !record.expectedContentSha256()
                        .equals(Optional.of(expected.expectedContentSha256()))) {
            throw new IOException(
                    "the RunRecord expected digest does not match the dispatched work");
        }
    }

    private static ExecutionInput executionInput(WorkItem workItem) {
        return workItem.executionInput()
                .map(declared -> new ExecutionInput(
                        declared.targetPath(),
                        declared.expectedContentSha256()))
                .orElseGet(() -> new ExecutionInput(
                        workItem.taskRevision().sourceDocument(),
                        workItem.taskRevision().sourceSha256()));
    }

    private static void requireEqual(String expected, String actual, String label)
            throws IOException {
        if (!expected.equals(actual)) {
            throw new IOException("the result envelope's " + label
                    + " does not match the dispatched work");
        }
    }

    private record ExecutionInput(String targetPath, String expectedContentSha256) {
    }
}
