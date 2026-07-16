package com.enhancer.runtime;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.MessageHandler;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Gate 7-to-Gate 8 adapter that admits a delivered work envelope without storing, scheduling, or
 * executing it. The downstream sink owns any later queue semantics.
 */
public final class WorkItemAdmissionHandler implements MessageHandler {
    private final String requiredCapability;
    private final Supplier<String> workItemIds;
    private final Consumer<WorkItem> admittedWork;

    public WorkItemAdmissionHandler(
            String requiredCapability,
            Supplier<String> workItemIds,
            Consumer<WorkItem> admittedWork) {
        this.requiredCapability = Objects.requireNonNull(
                requiredCapability, "requiredCapability must not be null");
        this.workItemIds = Objects.requireNonNull(
                workItemIds, "workItemIds must not be null");
        this.admittedWork = Objects.requireNonNull(
                admittedWork, "admittedWork must not be null");
    }

    @Override
    public void handle(MessageEnvelope envelope) {
        admittedWork.accept(new WorkItem(
                workItemIds.get(),
                requiredCapability,
                envelope));
    }
}
