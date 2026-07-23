package com.enhancer.cli;

import com.enhancer.runtime.AgentRunLease;
import com.enhancer.runtime.AgentRunRetryPolicy;
import com.enhancer.runtime.IsolatedWorkerLauncher;
import com.enhancer.runtime.SingleWorkerSchedulerQueue;
import com.enhancer.session.DevelopmentSessionCheckpointState;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

final class CliArguments {
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");
    private static final Set<String> RUN_OPTIONS = Set.of(
            "project-root",
            "task-id",
            "target-path",
            "expected-sha256",
            "evidence-root",
            "run-record-root");
    private static final Set<String> REPLAY_OPTIONS = Set.of("run-record-root", "reference");
    private static final Set<String> RUN_RECORD_LIST_OPTIONS =
            Set.of("run-record-root", "limit");
    private static final Set<String> SCHEDULER_STATUS_OPTIONS =
            Set.of("queue-root", "queue-id", "limit");
    private static final Set<String> SCHEDULER_CYCLE_OPTIONS = Set.of(
            "project-root",
            "queue-root",
            "queue-id",
            "runtime-root",
            "external-effect-root",
            "cycle-checkpoint-root",
            "evidence-root",
            "run-record-root",
            "invocation-root",
            "owner-id",
            "max-attempts",
            "lease-millis",
            "process-timeout-millis");
    private static final Set<String> SCHEDULER_DRAIN_OPTIONS = Set.of(
            "project-root",
            "queue-root",
            "queue-id",
            "runtime-root",
            "external-effect-root",
            "cycle-checkpoint-root",
            "evidence-root",
            "run-record-root",
            "invocation-root",
            "owner-id",
            "max-attempts",
            "lease-millis",
            "process-timeout-millis",
            "max-cycles");
    private static final Set<String> SCHEDULER_SUBMIT_OPTIONS = Set.of(
            "project-root",
            "submission-root",
            "queue-root",
            "task-id",
            "queue-id",
            "max-work-items",
            "required-capability",
            "message-id",
            "correlation-id",
            "logical-run-id",
            "producer",
            "occurred-at",
            "target-path",
            "expected-sha256");
    private static final Set<String> SCHEDULER_SUBMIT_GENERATED_OPTIONS = Set.of(
            "project-root",
            "submission-root",
            "queue-root",
            "task-id",
            "submission-id",
            "max-work-items",
            "required-capability",
            "producer",
            "target-path",
            "expected-sha256");
    private static final Set<String> CHECKPOINT_START_OPTIONS = Set.of(
            "project-root", "step", "next-action");
    private static final Set<String> CHECKPOINT_RECORD_OPTIONS = Set.of(
            "project-root", "run-id", "expected-revision", "state", "step", "next-action");
    private static final Set<String> CHECKPOINT_REPEATABLE_OPTIONS = Set.of(
            "artifact", "evidence");
    private static final Set<String> CHECKPOINT_SHOW_OPTIONS = Set.of("project-root");
    private static final Set<String> CHECKPOINT_CLEAR_OPTIONS = Set.of(
            "project-root", "run-id", "expected-revision");

    private CliArguments() {
    }

    static CliCommand parse(String[] arguments) {
        if (arguments == null || arguments.length == 0) {
            throw new CliUsageException(
                    "command is required: run, replay, run-record-list, scheduler-submit, "
                    + "scheduler-submit-generated, scheduler-status, scheduler-cycle, "
                            + "scheduler-drain, or "
                            + "checkpoint operation");
        }
        String command = arguments[0];
        return switch (command) {
            case "run" -> parseRun(parseOptions(arguments, RUN_OPTIONS));
            case "replay" -> parseReplay(parseOptions(arguments, REPLAY_OPTIONS));
            case "run-record-list" -> parseRunRecordList(
                    parseOptions(arguments, RUN_RECORD_LIST_OPTIONS));
            case "scheduler-status" -> parseSchedulerStatus(
                    parseOptions(arguments, SCHEDULER_STATUS_OPTIONS));
            case "scheduler-cycle" -> parseSchedulerCycle(
                    parseOptions(arguments, SCHEDULER_CYCLE_OPTIONS));
            case "scheduler-drain" -> parseSchedulerDrain(
                    parseOptions(arguments, SCHEDULER_DRAIN_OPTIONS));
            case "scheduler-submit" -> parseSchedulerSubmit(
                    parseOptions(arguments, SCHEDULER_SUBMIT_OPTIONS));
            case "scheduler-submit-generated" -> parseSchedulerSubmitGenerated(
                    parseOptions(arguments, SCHEDULER_SUBMIT_GENERATED_OPTIONS));
            case "checkpoint-start" -> parseCheckpointStart(arguments);
            case "checkpoint-record" -> parseCheckpointRecord(arguments);
            case "checkpoint-show" -> new CheckpointShowCliCommand(
                    path(parseOptions(arguments, CHECKPOINT_SHOW_OPTIONS)
                            .get("project-root"), "project-root"));
            case "checkpoint-clear" -> parseCheckpointClear(
                    parseOptions(arguments, CHECKPOINT_CLEAR_OPTIONS));
            default -> throw new CliUsageException("unknown command: " + command);
        };
    }

    private static CheckpointStartCliCommand parseCheckpointStart(String[] arguments) {
        RepeatableOptions options = parseRepeatableOptions(
                arguments,
                CHECKPOINT_START_OPTIONS,
                Set.of("artifact"));
        return new CheckpointStartCliCommand(
                path(options.single().get("project-root"), "project-root"),
                nonBlank(options.single().get("step"), "step"),
                nonBlank(options.single().get("next-action"), "next-action"),
                options.repeated().getOrDefault("artifact", List.of()));
    }

    private static CheckpointRecordCliCommand parseCheckpointRecord(String[] arguments) {
        RepeatableOptions options = parseRepeatableOptions(
                arguments,
                CHECKPOINT_RECORD_OPTIONS,
                CHECKPOINT_REPEATABLE_OPTIONS);
        DevelopmentSessionCheckpointState state;
        try {
            state = DevelopmentSessionCheckpointState.valueOf(
                    options.single().get("state"));
        } catch (IllegalArgumentException exception) {
            throw new CliUsageException("state is invalid", exception);
        }
        return new CheckpointRecordCliCommand(
                path(options.single().get("project-root"), "project-root"),
                nonBlank(options.single().get("run-id"), "run-id"),
                positiveLong(options.single().get("expected-revision"), "expected-revision"),
                state,
                nonBlank(options.single().get("step"), "step"),
                nonBlank(options.single().get("next-action"), "next-action"),
                options.repeated().getOrDefault("artifact", List.of()),
                options.repeated().getOrDefault("evidence", List.of()));
    }

    private static CheckpointClearCliCommand parseCheckpointClear(
            Map<String, String> options) {
        return new CheckpointClearCliCommand(
                path(options.get("project-root"), "project-root"),
                nonBlank(options.get("run-id"), "run-id"),
                positiveLong(options.get("expected-revision"), "expected-revision"));
    }

    private static RunCliCommand parseRun(Map<String, String> options) {
        String digest = options.get("expected-sha256");
        if (!SHA_256.matcher(digest).matches()) {
            throw new CliUsageException(
                    "expected-sha256 must be 64 lowercase hexadecimal characters");
        }
        return new RunCliCommand(
                path(options.get("project-root"), "project-root"),
                nonBlank(options.get("task-id"), "task-id"),
                nonBlank(options.get("target-path"), "target-path"),
                digest,
                path(options.get("evidence-root"), "evidence-root"),
                path(options.get("run-record-root"), "run-record-root"));
    }

    private static ReplayCliCommand parseReplay(Map<String, String> options) {
        return new ReplayCliCommand(
                path(options.get("run-record-root"), "run-record-root"),
                nonBlank(options.get("reference"), "reference"));
    }

    private static RunRecordListCliCommand parseRunRecordList(
            Map<String, String> options) {
        long limit = positiveLong(options.get("limit"), "limit");
        if (limit > RunRecordListCliCommand.MAX_REFERENCES) {
            throw new CliUsageException(
                    "limit must not exceed "
                            + RunRecordListCliCommand.MAX_REFERENCES);
        }
        return new RunRecordListCliCommand(
                path(options.get("run-record-root"), "run-record-root"),
                (int) limit);
    }

    private static SchedulerStatusCliCommand parseSchedulerStatus(
            Map<String, String> options) {
        long limit = positiveLong(options.get("limit"), "limit");
        if (limit > SchedulerStatusCliCommand.MAX_LISTED_WORK_ITEMS) {
            throw new CliUsageException(
                    "limit must not exceed "
                            + SchedulerStatusCliCommand.MAX_LISTED_WORK_ITEMS);
        }
        return new SchedulerStatusCliCommand(
                path(options.get("queue-root"), "queue-root"),
                canonicalUuid(options.get("queue-id"), "queue-id"),
                (int) limit);
    }

    private static SchedulerCycleCliCommand parseSchedulerCycle(
            Map<String, String> options) {
        long maxAttempts = positiveLong(options.get("max-attempts"), "max-attempts");
        if (maxAttempts > AgentRunRetryPolicy.MAX_ATTEMPTS) {
            throw new CliUsageException(
                    "max-attempts must not exceed " + AgentRunRetryPolicy.MAX_ATTEMPTS);
        }
        Duration leaseDuration = boundedDuration(
                options.get("lease-millis"),
                "lease-millis",
                AgentRunLease.MAX_DURATION);
        Duration processTimeout = boundedDuration(
                options.get("process-timeout-millis"),
                "process-timeout-millis",
                IsolatedWorkerLauncher.MAX_TIMEOUT);
        String ownerId = nonBlank(options.get("owner-id"), "owner-id");
        if (ownerId.length() > AgentRunLease.MAX_OWNER_CHARACTERS) {
            throw new CliUsageException(
                    "owner-id must not exceed "
                            + AgentRunLease.MAX_OWNER_CHARACTERS
                            + " characters");
        }
        return new SchedulerCycleCliCommand(
                path(options.get("project-root"), "project-root"),
                path(options.get("queue-root"), "queue-root"),
                nonBlank(options.get("queue-id"), "queue-id"),
                path(options.get("runtime-root"), "runtime-root"),
                path(options.get("external-effect-root"), "external-effect-root"),
                path(options.get("cycle-checkpoint-root"), "cycle-checkpoint-root"),
                path(options.get("evidence-root"), "evidence-root"),
                path(options.get("run-record-root"), "run-record-root"),
                path(options.get("invocation-root"), "invocation-root"),
                ownerId,
                (int) maxAttempts,
                leaseDuration,
                processTimeout);
    }

    private static SchedulerDrainCliCommand parseSchedulerDrain(
            Map<String, String> options) {
        SchedulerCycleCliCommand cycle = parseSchedulerCycle(options);
        long maxCycles = positiveLong(
                options.get("max-cycles"), "max-cycles");
        if (maxCycles > SingleWorkerSchedulerQueue.MAX_WORK_ITEMS) {
            throw new CliUsageException(
                    "max-cycles must not exceed "
                            + SingleWorkerSchedulerQueue.MAX_WORK_ITEMS);
        }
        return new SchedulerDrainCliCommand(
                cycle.projectRoot(),
                cycle.queueRoot(),
                cycle.queueId(),
                cycle.runtimeRoot(),
                cycle.externalEffectRoot(),
                cycle.cycleCheckpointRoot(),
                cycle.evidenceRoot(),
                cycle.runRecordRoot(),
                cycle.invocationRoot(),
                cycle.ownerId(),
                cycle.maxAttempts(),
                cycle.leaseDuration(),
                cycle.processTimeout(),
                (int) maxCycles);
    }

    private static SchedulerSubmitCliCommand parseSchedulerSubmit(
            Map<String, String> options) {
        long maxWorkItems = positiveLong(
                options.get("max-work-items"), "max-work-items");
        if (maxWorkItems > SingleWorkerSchedulerQueue.MAX_WORK_ITEMS) {
            throw new CliUsageException(
                    "max-work-items must not exceed "
                            + SingleWorkerSchedulerQueue.MAX_WORK_ITEMS);
        }
        String digest = options.get("expected-sha256");
        if (!SHA_256.matcher(digest).matches()) {
            throw new CliUsageException(
                    "expected-sha256 must be 64 lowercase hexadecimal characters");
        }
        return new SchedulerSubmitCliCommand(
                path(options.get("project-root"), "project-root"),
                path(options.get("submission-root"), "submission-root"),
                path(options.get("queue-root"), "queue-root"),
                nonBlank(options.get("task-id"), "task-id"),
                canonicalUuid(options.get("queue-id"), "queue-id"),
                (int) maxWorkItems,
                nonBlank(options.get("required-capability"), "required-capability"),
                canonicalUuid(options.get("message-id"), "message-id"),
                nonBlank(options.get("correlation-id"), "correlation-id"),
                nonBlank(options.get("logical-run-id"), "logical-run-id"),
                nonBlank(options.get("producer"), "producer"),
                instant(options.get("occurred-at"), "occurred-at"),
                nonBlank(options.get("target-path"), "target-path"),
                digest);
    }

    private static GeneratedSubmitCliCommand parseSchedulerSubmitGenerated(
            Map<String, String> options) {
        long maxWorkItems = positiveLong(
                options.get("max-work-items"), "max-work-items");
        if (maxWorkItems > SingleWorkerSchedulerQueue.MAX_WORK_ITEMS) {
            throw new CliUsageException(
                    "max-work-items must not exceed "
                            + SingleWorkerSchedulerQueue.MAX_WORK_ITEMS);
        }
        String digest = options.get("expected-sha256");
        if (!SHA_256.matcher(digest).matches()) {
            throw new CliUsageException(
                    "expected-sha256 must be 64 lowercase hexadecimal characters");
        }
        return new GeneratedSubmitCliCommand(
                path(options.get("project-root"), "project-root"),
                path(options.get("submission-root"), "submission-root"),
                path(options.get("queue-root"), "queue-root"),
                nonBlank(options.get("task-id"), "task-id"),
                canonicalUuid(options.get("submission-id"), "submission-id"),
                (int) maxWorkItems,
                nonBlank(options.get("required-capability"), "required-capability"),
                nonBlank(options.get("producer"), "producer"),
                nonBlank(options.get("target-path"), "target-path"),
                digest);
    }

    private static Map<String, String> parseOptions(
            String[] arguments,
            Set<String> expectedOptions) {
        if ((arguments.length - 1) % 2 != 0) {
            throw new CliUsageException("every option requires exactly one value");
        }
        Map<String, String> options = new LinkedHashMap<>();
        for (int index = 1; index < arguments.length; index += 2) {
            String token = arguments[index];
            if (token == null || !token.startsWith("--") || token.length() == 2) {
                throw new CliUsageException("invalid option: " + token);
            }
            String name = token.substring(2);
            if (!expectedOptions.contains(name)) {
                throw new CliUsageException("unknown option: --" + name);
            }
            String value = arguments[index + 1];
            if (options.putIfAbsent(name, nonBlank(value, name)) != null) {
                throw new CliUsageException("duplicate option: --" + name);
            }
        }
        for (String option : expectedOptions) {
            if (!options.containsKey(option)) {
                throw new CliUsageException("missing required option: --" + option);
            }
        }
        return Map.copyOf(options);
    }

    private static RepeatableOptions parseRepeatableOptions(
            String[] arguments,
            Set<String> requiredSingles,
            Set<String> repeatable) {
        if ((arguments.length - 1) % 2 != 0) {
            throw new CliUsageException("every option requires exactly one value");
        }
        Map<String, String> singles = new LinkedHashMap<>();
        Map<String, List<String>> repeated = new LinkedHashMap<>();
        for (int index = 1; index < arguments.length; index += 2) {
            String token = arguments[index];
            if (token == null || !token.startsWith("--") || token.length() == 2) {
                throw new CliUsageException("invalid option: " + token);
            }
            String name = token.substring(2);
            String value = nonBlank(arguments[index + 1], name);
            if (requiredSingles.contains(name)) {
                if (singles.putIfAbsent(name, value) != null) {
                    throw new CliUsageException("duplicate option: --" + name);
                }
            } else if (repeatable.contains(name)) {
                repeated.computeIfAbsent(name, ignored -> new ArrayList<>()).add(value);
            } else {
                throw new CliUsageException("unknown option: --" + name);
            }
        }
        for (String option : requiredSingles) {
            if (!singles.containsKey(option)) {
                throw new CliUsageException("missing required option: --" + option);
            }
        }
        Map<String, List<String>> copied = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : repeated.entrySet()) {
            copied.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return new RepeatableOptions(Map.copyOf(singles), Map.copyOf(copied));
    }

    private static long positiveLong(String value, String name) {
        try {
            long parsed = Long.parseLong(nonBlank(value, name));
            if (parsed < 1) {
                throw new NumberFormatException("not positive");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new CliUsageException(name + " must be a positive integer", exception);
        }
    }

    private static Duration boundedDuration(
            String value,
            String name,
            Duration maximum) {
        long millis = positiveLong(value, name);
        Duration duration = Duration.ofMillis(millis);
        if (duration.compareTo(maximum) > 0) {
            throw new CliUsageException(name + " must not exceed " + maximum.toMillis());
        }
        return duration;
    }

    private static String nonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new CliUsageException(name + " must not be blank");
        }
        return value;
    }

    private static String canonicalUuid(String value, String name) {
        String candidate = nonBlank(value, name);
        try {
            if (!UUID.fromString(candidate).toString().equals(candidate)) {
                throw new IllegalArgumentException("not canonical");
            }
            return candidate;
        } catch (IllegalArgumentException exception) {
            throw new CliUsageException(name + " must be a canonical UUID", exception);
        }
    }

    private static Instant instant(String value, String name) {
        try {
            return Instant.parse(nonBlank(value, name));
        } catch (DateTimeParseException exception) {
            throw new CliUsageException(name + " must be an ISO-8601 instant", exception);
        }
    }

    private static Path path(String value, String name) {
        try {
            return Path.of(nonBlank(value, name)).toAbsolutePath().normalize();
        } catch (InvalidPathException exception) {
            throw new CliUsageException(name + " is not a valid path", exception);
        }
    }

    private record RepeatableOptions(
            Map<String, String> single,
            Map<String, List<String>> repeated) {
    }
}
