package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class IsolatedWorkerLauncherTest {
    private static final Duration GENEROUS = Duration.ofSeconds(60);

    private final IsolatedWorkerLauncher launcher = new IsolatedWorkerLauncher();

    @Test
    void runsAChildToCompletionAndKeepsOnlyItsExitCode() {
        IsolatedWorkerOutcome outcome = launcher.run(
                ExitingWorker.class, List.of("7"), GENEROUS);

        assertEquals(IsolatedWorkerStatus.COMPLETED, outcome.status());
        assertEquals(7, outcome.exitCode().orElseThrow());
        assertTrue(outcome.reason().isEmpty(), "a completed outcome carries no reason");
    }

    @Test
    void destroysAChildThatOverrunsItsTimeout() {
        Instant startedAt = Instant.now();

        IsolatedWorkerOutcome outcome = launcher.run(
                SleepingWorker.class, List.of(), Duration.ofSeconds(1));

        assertEquals(IsolatedWorkerStatus.TIMED_OUT, outcome.status());
        assertTrue(outcome.exitCode().isEmpty(),
                "a timed-out child must not present an exit code that could read as clean");
        assertTrue(outcome.reason().isPresent());
        // SleepingWorker would run far past this if it were not forcibly destroyed.
        assertTrue(Duration.between(startedAt, Instant.now()).compareTo(Duration.ofSeconds(30)) < 0,
                "the watchdog must not wait for the child to finish on its own");
    }

    @Test
    void discardsChildOutputRatherThanTreatingItAsAResult() {
        IsolatedWorkerOutcome outcome = launcher.run(
                ChattyWorker.class, List.of(), GENEROUS);

        assertEquals(IsolatedWorkerStatus.COMPLETED, outcome.status());
        assertEquals(0, outcome.exitCode().orElseThrow());
        assertTrue(outcome.reason().isEmpty(),
                "child output is never retained, however much of it there is");
    }

    @Test
    void rejectsInputsThatWouldWidenTheGrantedAuthority() {
        assertThrows(NullPointerException.class,
                () -> launcher.run(null, List.of(), GENEROUS));
        assertThrows(NullPointerException.class,
                () -> launcher.run(ExitingWorker.class, null, GENEROUS));
        assertThrows(NullPointerException.class,
                () -> launcher.run(ExitingWorker.class, List.of(), null));
        assertThrows(IllegalArgumentException.class,
                () -> launcher.run(ExitingWorker.class, List.of(), Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> launcher.run(ExitingWorker.class, List.of(),
                        IsolatedWorkerLauncher.MAX_TIMEOUT.plusSeconds(1)));

        // An argument is data for the child, never a command: it cannot introduce a second
        // executable, and a null entry is refused rather than silently dropped.
        assertThrows(IllegalArgumentException.class,
                () -> launcher.run(ExitingWorker.class, java.util.Arrays.asList("0", null),
                        GENEROUS));
    }

    @Test
    void runsOnlyTheCurrentJvmExecutable() throws IOException {
        Path resolved = IsolatedWorkerLauncher.javaExecutable();

        assertTrue(Files.isRegularFile(resolved), "the executable must exist as a regular file");
        assertEquals(resolved, resolved.toRealPath(), "the executable must be canonical");
        assertTrue(resolved.startsWith(Path.of(System.getProperty("java.home")).toRealPath()),
                "the child must be the JVM this process is already running");

        // Deliberately not asserted: that the executable sits outside the project. That is a
        // GitWorkspaceCollector property, where a repository shipping its own git would poison a
        // PATH lookup. This launcher performs no lookup -- it re-runs the interpreter already
        // running -- so the executable's location is irrelevant to the threat, and this project
        // vendors its own JDK under .tools/ precisely inside the project root.
        ProcessHandle.current().info().command().ifPresent(running ->
                assertEquals(resolved, Path.of(running).toAbsolutePath().normalize(),
                        "the resolved executable must be the one this process is running"));
    }

    /** Exits with the code named by its first argument. */
    public static final class ExitingWorker {
        private ExitingWorker() {
        }

        public static void main(String[] arguments) {
            System.exit(Integer.parseInt(arguments[0]));
        }
    }

    /** Outlives any timeout this suite sets, so only forcible destruction ends it. */
    public static final class SleepingWorker {
        private SleepingWorker() {
        }

        public static void main(String[] arguments) throws InterruptedException {
            Thread.sleep(Duration.ofMinutes(10).toMillis());
        }
    }

    /** Writes far more than the launcher will read, then exits cleanly. */
    public static final class ChattyWorker {
        private ChattyWorker() {
        }

        public static void main(String[] arguments) {
            String line = "x".repeat(1024);
            for (int index = 0; index < 4096; index++) {
                System.out.println(line);
                System.err.println(line);
            }
        }
    }
}
