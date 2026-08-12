package com.enhancer.maintenance;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;

/** Dedicated operator process entry point; never part of the runtime CLI. */
public final class CancellationTrustMaintenanceOperator {
    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_CONFIGURATION = 2;
    private static final int EXIT_REFUSAL = 20;
    private static final int EXIT_DURABILITY = 70;
    private static final int MAX_PATH_CHARACTERS = 4096;
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private final CancellationTrustMaintenance maintenance;

    public CancellationTrustMaintenanceOperator() {
        this(new CancellationTrustMaintenance());
    }

    CancellationTrustMaintenanceOperator(CancellationTrustMaintenance maintenance) {
        this.maintenance = Objects.requireNonNull(
                maintenance, "maintenance must not be null");
    }

    public static void main(String[] arguments) {
        int exitCode = new CancellationTrustMaintenanceOperator().execute(
                arguments, System.out, System.err);
        System.exit(exitCode);
    }

    public int execute(String[] arguments, PrintStream stdout, PrintStream stderr) {
        Objects.requireNonNull(arguments, "arguments must not be null");
        Objects.requireNonNull(stdout, "stdout must not be null");
        Objects.requireNonNull(stderr, "stderr must not be null");
        try {
            Parsed parsed = parse(arguments);
            CancellationTrustMaintenanceResult result = parsed.operation()
                    == Operation.INSTALL
                    ? maintenance.install(parsed.applicationJar(), parsed.candidatePolicy())
                    : maintenance.rotate(
                            parsed.applicationJar(),
                            parsed.candidatePolicy(),
                            parsed.expectedCurrentMetadataSha256());
            stdout.print("status=" + result.status() + "\n"
                    + "policyFile=" + result.policyFile() + "\n"
                    + "policySha256=" + result.policySha256() + "\n"
                    + "metadataSha256=" + result.metadataSha256() + "\n");
            stdout.flush();
            return stdout.checkError() ? emitUnexpected(stderr) : EXIT_SUCCESS;
        } catch (OperatorUsageException exception) {
            return emitFailure(
                    stderr,
                    EXIT_CONFIGURATION,
                    CancellationTrustMaintenanceFailureCategory.CONFIGURATION,
                    "INVALID_ARGUMENTS");
        } catch (CancellationTrustMaintenanceException exception) {
            int exitCode = switch (exception.category()) {
                case CONFIGURATION -> EXIT_CONFIGURATION;
                case REFUSAL -> EXIT_REFUSAL;
                case DURABILITY -> EXIT_DURABILITY;
            };
            return emitFailure(
                    stderr,
                    exitCode,
                    exception.category(),
                    exception.reason().name());
        } catch (IOException | RuntimeException exception) {
            return emitUnexpected(stderr);
        }
    }

    private static int emitUnexpected(PrintStream stderr) {
        return emitFailure(
                stderr,
                EXIT_DURABILITY,
                CancellationTrustMaintenanceFailureCategory.DURABILITY,
                "UNEXPECTED_FAILURE");
    }

    private static int emitFailure(
            PrintStream stderr,
            int exitCode,
            CancellationTrustMaintenanceFailureCategory category,
            String reason) {
        stderr.print("status=ERROR\n"
                + "exitCode=" + exitCode + "\n"
                + "category=" + category + "\n"
                + "reason=" + reason + "\n");
        stderr.flush();
        return exitCode;
    }

    private static Parsed parse(String[] arguments) throws OperatorUsageException {
        if (arguments.length != 5 && arguments.length != 7) {
            throw new OperatorUsageException();
        }
        Operation operation = switch (arguments[0]) {
            case "install" -> Operation.INSTALL;
            case "rotate" -> Operation.ROTATE;
            default -> throw new OperatorUsageException();
        };
        if (!arguments[1].equals("--application-jar")
                || !arguments[3].equals("--candidate-policy")) {
            throw new OperatorUsageException();
        }
        Path applicationJar = exactPath(arguments[2]);
        Path candidatePolicy = exactPath(arguments[4]);
        if (operation == Operation.INSTALL) {
            if (arguments.length != 5) {
                throw new OperatorUsageException();
            }
            return new Parsed(operation, applicationJar, candidatePolicy, null);
        }
        if (arguments.length != 7
                || !arguments[5].equals("--expected-current-metadata-sha256")
                || !SHA256.matcher(arguments[6]).matches()) {
            throw new OperatorUsageException();
        }
        return new Parsed(operation, applicationJar, candidatePolicy, arguments[6]);
    }

    private static Path exactPath(String value) throws OperatorUsageException {
        if (value.isEmpty()
                || value.length() > MAX_PATH_CHARACTERS
                || value.codePoints().anyMatch(character ->
                        Character.isISOControl(character))) {
            throw new OperatorUsageException();
        }
        try {
            Path path = Path.of(value);
            if (!path.isAbsolute() || !path.equals(path.normalize())) {
                throw new OperatorUsageException();
            }
            return path;
        } catch (InvalidPathException exception) {
            throw new OperatorUsageException();
        }
    }

    private enum Operation { INSTALL, ROTATE }

    private record Parsed(
            Operation operation,
            Path applicationJar,
            Path candidatePolicy,
            String expectedCurrentMetadataSha256) { }

    private static final class OperatorUsageException extends Exception {
        private static final long serialVersionUID = 1L;
    }
}
