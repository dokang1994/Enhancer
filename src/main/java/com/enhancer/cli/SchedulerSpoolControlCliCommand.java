package com.enhancer.cli;

import com.enhancer.bus.ControlSignal;
import java.nio.file.Path;
import java.time.Instant;

record SchedulerSpoolControlCliCommand(
        Path runtimeRoot,
        String goalId,
        Path transportSpoolRoot,
        String destinationName,
        int maxPendingPublications,
        String messageId,
        String producer,
        Instant occurredAt,
        ControlSignal signal,
        String reason) implements CliCommand {
}
