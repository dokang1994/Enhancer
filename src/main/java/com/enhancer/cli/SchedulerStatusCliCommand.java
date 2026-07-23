package com.enhancer.cli;

import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

record SchedulerStatusCliCommand(
        Path queueRoot,
        String queueId,
        int limit) implements CliCommand {
    static final int MAX_LISTED_WORK_ITEMS = 48;

    SchedulerStatusCliCommand {
        Objects.requireNonNull(queueRoot, "queueRoot must not be null");
        queueRoot = queueRoot.toAbsolutePath().normalize();
        Objects.requireNonNull(queueId, "queueId must not be null");
        try {
            if (!UUID.fromString(queueId).toString().equals(queueId)) {
                throw new IllegalArgumentException(
                        "queueId must be a canonical UUID");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "queueId must be a canonical UUID",
                    exception);
        }
        if (limit < 1 || limit > MAX_LISTED_WORK_ITEMS) {
            throw new IllegalArgumentException(
                    "limit must be between 1 and "
                            + MAX_LISTED_WORK_ITEMS);
        }
    }
}
