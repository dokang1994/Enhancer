package com.enhancer.runtime;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import java.util.Objects;

/**
 * Immutable end-user intent for creating one bounded Scheduler queue and admitting one exact
 * dependency-free work message. The manifest grants no execution authority.
 */
public record DurableSubmissionManifest(
        String queueId,
        int maxWorkItems,
        String requiredCapability,
        MessageEnvelope workMessage) {

    public DurableSubmissionManifest {
        Objects.requireNonNull(workMessage, "workMessage must not be null");
        if (!(workMessage.payload() instanceof WorkPayload)) {
            throw new IllegalArgumentException(
                    "submission workMessage must carry WorkPayload");
        }
        SchedulerQueueState.initial(queueId, maxWorkItems);
        queueId = SchedulerQueueState.requireCanonicalQueueId(queueId);
        WorkItem validated = new WorkItem(
                DurableWorkItemAdmissionHandler.workItemIdFor(workMessage.messageId()),
                requiredCapability,
                workMessage);
        requiredCapability = validated.requiredCapability();
    }

    public String submissionId() {
        return workMessage.messageId();
    }
}
