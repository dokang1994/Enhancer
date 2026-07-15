package com.enhancer.cli;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
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

    private CliArguments() {
    }

    static CliCommand parse(String[] arguments) {
        if (arguments == null || arguments.length == 0) {
            throw new CliUsageException("command is required: run or replay");
        }
        String command = arguments[0];
        return switch (command) {
            case "run" -> parseRun(parseOptions(arguments, RUN_OPTIONS));
            case "replay" -> parseReplay(parseOptions(arguments, REPLAY_OPTIONS));
            default -> throw new CliUsageException("unknown command: " + command);
        };
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

    private static String nonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new CliUsageException(name + " must not be blank");
        }
        return value;
    }

    private static Path path(String value, String name) {
        try {
            return Path.of(nonBlank(value, name)).toAbsolutePath().normalize();
        } catch (InvalidPathException exception) {
            throw new CliUsageException(name + " is not a valid path", exception);
        }
    }
}
