package com.enhancer.runtime;

import com.enhancer.bus.DeliveryDestination;
import com.enhancer.bus.DeliveryDestinationKind;
import com.enhancer.bus.DeliveryOutcome;
import com.enhancer.bus.InProcessMessageBus;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.workspace.ApprovedTaskRevision;
import com.enhancer.workspace.WorkspaceSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Authority-preserving in-process publication of one approved Workspace snapshot as a Gate 7
 * work message. All task and Tool scope data comes from existing governed inputs.
 */
public final class WorkMessagePublisher {
    private final InProcessMessageBus bus;
    private final DeliveryDestination destination;

    public WorkMessagePublisher(
            InProcessMessageBus bus,
            DeliveryDestination destination) {
        this.bus = Objects.requireNonNull(bus, "bus must not be null");
        this.destination = Objects.requireNonNull(
                destination, "destination must not be null");
        if (destination.kind() != DeliveryDestinationKind.QUEUE) {
            throw new IllegalArgumentException(
                    "work destination must be a queue");
        }
    }

    /**
     * Publishes one work envelope after proving the approved task matches the snapshot revision.
     * Message identities and time are explicit so the composition remains deterministic.
     */
    public List<DeliveryOutcome> publish(
            WorkspaceSnapshot snapshot,
            ApprovedTask approvedTask,
            String messageId,
            String correlationId,
            Optional<String> causationId,
            String logicalRunId,
            String producer,
            Instant occurredAt) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(approvedTask, "approvedTask must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        requireMatchingTask(snapshot.approvedTaskRevision(), approvedTask);
        if (occurredAt.isBefore(snapshot.capturedAt())) {
            throw new IllegalArgumentException(
                    "occurredAt must not be before the Workspace snapshot");
        }

        MessageEnvelope envelope = new MessageEnvelope(
                messageId,
                correlationId,
                causationId,
                logicalRunId,
                producer,
                occurredAt,
                new WorkPayload(
                        snapshot.approvedTaskRevision(),
                        snapshot.snapshotId(),
                        approvedTask.allowedTools()));
        return bus.publish(destination, envelope);
    }

    private static void requireMatchingTask(
            ApprovedTaskRevision revision,
            ApprovedTask approvedTask) {
        if (!revision.taskId().equals(approvedTask.taskId())
                || !revision.sourceDocument().equals(approvedTask.sourceDocument())) {
            throw new IllegalArgumentException(
                    "approved task must match the Workspace snapshot revision");
        }
    }
}
