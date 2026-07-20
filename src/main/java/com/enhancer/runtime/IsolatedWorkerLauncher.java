package com.enhancer.runtime;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Runs one worker in a child process and returns its terminal outcome.
 *
 * <p>This is the second external command authority in the codebase and is deliberately narrower
 * than the first. {@code GitWorkspaceCollector} must defend against a poisoned executable lookup
 * because it runs a configured external program; this class has no lookup to poison. The
 * executable is the JVM already running — resolved from {@code java.home}, canonicalized, and
 * required to be a regular file — and the child runs the current classpath. No caller-supplied
 * executable, command name, or shell reaches {@link ProcessBuilder}.
 *
 * <p>The entry point is a {@link Class}, not a command string, so it is necessarily already on
 * the running classpath: a caller chooses which of this project's own entry points to run and
 * cannot name a program.
 *
 * <p>The child is bounded the way the Git adapter is bounded. Its output is discarded by the
 * operating system rather than read, so a chatty child can neither block on a full pipe nor grow
 * this process's memory, and nothing it prints can be mistaken for a result. Its environment is
 * stripped of the variables that would let an inherited setting inject JVM arguments. A watchdog
 * destroys a child that overruns its timeout. Only the exit code and a bounded reason survive.
 */
public final class IsolatedWorkerLauncher implements WorkerProcessLauncher {
    /** Upper bound on any single child's runtime, so a caller cannot disable the watchdog. */
    public static final Duration MAX_TIMEOUT = Duration.ofMinutes(5);

    /** Bound on the argument vector, mirroring the collection bounds used across the runtime. */
    public static final int MAX_ARGUMENTS = 256;

    private static final int MAX_ARGUMENT_CHARACTERS = 4096;
    private static final Duration DESTROY_GRACE = Duration.ofSeconds(5);

    /**
     * Inherited settings that would let the environment inject JVM arguments into the child.
     * Removing them keeps the child's configuration owned by this process.
     */
    private static final List<String> REMOVED_ENVIRONMENT = List.of(
            "JAVA_TOOL_OPTIONS", "_JAVA_OPTIONS", "JDK_JAVA_OPTIONS");

    @Override
    public IsolatedWorkerOutcome run(
            Class<?> entryPoint, List<String> arguments, Duration timeout) {
        Objects.requireNonNull(entryPoint, "entryPoint must not be null");
        Objects.requireNonNull(arguments, "arguments must not be null");
        Objects.requireNonNull(timeout, "timeout must not be null");
        requireBoundedTimeout(timeout);
        List<String> checkedArguments = requireBoundedArguments(arguments);

        Process process;
        try {
            process = start(entryPoint, checkedArguments);
        } catch (IOException unstartable) {
            return IsolatedWorkerOutcome.refused(
                    IsolatedWorkerStatus.START_FAILED, reason(unstartable));
        }

        try {
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                destroy(process);
                return IsolatedWorkerOutcome.refused(
                        IsolatedWorkerStatus.TIMED_OUT,
                        "the isolated worker exceeded its " + timeout + " bound and was destroyed");
            }
            return IsolatedWorkerOutcome.completed(process.exitValue());
        } catch (InterruptedException interrupted) {
            destroy(process);
            Thread.currentThread().interrupt();
            return IsolatedWorkerOutcome.refused(
                    IsolatedWorkerStatus.TIMED_OUT,
                    "the calling thread was interrupted and the isolated worker was destroyed");
        }
    }

    /**
     * Resolves the JVM this process is already running.
     *
     * @throws UncheckedIOException if it cannot be canonicalized or is not a regular file, since
     *     a launcher that cannot identify its own runtime must not fall back to a lookup
     */
    public static Path javaExecutable() {
        String home = System.getProperty("java.home");
        if (home == null || home.isBlank()) {
            throw new UncheckedIOException(new IOException("java.home is not set"));
        }
        Path candidate = Path.of(home)
                .resolve("bin")
                .resolve(isWindows() ? "java.exe" : "java");
        try {
            Path real = candidate.toRealPath();
            if (!Files.isRegularFile(real, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("the JVM executable is not a regular file: " + real);
            }
            return real;
        } catch (IOException unresolvable) {
            throw new UncheckedIOException(unresolvable);
        }
    }

    private Process start(Class<?> entryPoint, List<String> arguments) throws IOException {
        List<String> command = new ArrayList<>();
        command.add(javaExecutable().toString());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(entryPoint.getName());
        command.addAll(arguments);

        ProcessBuilder builder = new ProcessBuilder(command)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD);
        sanitizeEnvironment(builder.environment());
        return builder.start();
    }

    static void sanitizeEnvironment(Map<String, String> environment) {
        environment.keySet().removeIf(key -> REMOVED_ENVIRONMENT.stream()
                .anyMatch(removed -> removed.equalsIgnoreCase(key)));
    }

    private static void destroy(Process process) {
        process.destroyForcibly();
        try {
            process.waitFor(DESTROY_GRACE.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static void requireBoundedTimeout(Duration timeout) {
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        if (timeout.compareTo(MAX_TIMEOUT) > 0) {
            throw new IllegalArgumentException("timeout must not exceed " + MAX_TIMEOUT);
        }
    }

    private static List<String> requireBoundedArguments(List<String> arguments) {
        if (arguments.size() > MAX_ARGUMENTS) {
            throw new IllegalArgumentException(
                    "arguments must not exceed " + MAX_ARGUMENTS + " entries");
        }
        List<String> checked = new ArrayList<>(arguments.size());
        for (String argument : arguments) {
            if (argument == null) {
                throw new IllegalArgumentException("an argument must not be null");
            }
            if (argument.length() > MAX_ARGUMENT_CHARACTERS) {
                throw new IllegalArgumentException(
                        "an argument must not exceed " + MAX_ARGUMENT_CHARACTERS + " characters");
            }
            checked.add(argument);
        }
        return List.copyOf(checked);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .startsWith("windows");
    }

    private static String reason(IOException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
    }
}
