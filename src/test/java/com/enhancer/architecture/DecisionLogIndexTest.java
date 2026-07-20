package com.enhancer.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Keeps the decision index and the per-decision files from drifting apart.
 *
 * <p>{@code DECISION_LOG.md} is an index: it carries each accepted decision's heading and
 * status, and the reasoning lives in {@code docs/decisions/}. The heading text is the
 * decision's identity — {@code AcceptedDecisionProjector} uses it as the graph node id and
 * {@code TaskJustificationProjector} resolves {@code CURRENT_TASK.md}'s {@code ## Justified
 * By} bullets against it by exact string, failing closed on an unknown reference. An index
 * entry without its file, or a heading edited on one side only, would therefore break the
 * Project Brain rather than merely read badly.
 */
class DecisionLogIndexTest {
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("user.dir"));
    private static final Path INDEX = PROJECT_ROOT.resolve("DECISION_LOG.md");
    private static final Path DECISIONS = PROJECT_ROOT.resolve("docs").resolve("decisions");

    private static final String INDEX_HEADING = "### ";
    private static final String FILE_HEADING = "# ";
    private static final String ACCEPTED = "Status: Accepted Decision";

    /** Keeps the deepest decision path clear of the Windows MAX_PATH ceiling. */
    private static final int MAX_ABSOLUTE_PATH_CHARACTERS = 240;

    @Test
    void everyIndexEntryHasExactlyOneMatchingDecisionFile() throws IOException {
        Map<String, String> index = indexEntries();
        Map<String, Path> files = decisionFiles();

        List<String> violations = new ArrayList<>();
        for (String heading : index.keySet()) {
            if (!files.containsKey(heading)) {
                violations.add("index entry has no decision file: \"" + heading + "\"");
            }
        }
        for (Map.Entry<String, Path> file : files.entrySet()) {
            if (!index.containsKey(file.getKey())) {
                violations.add("decision file is not in the index: "
                        + PROJECT_ROOT.relativize(file.getValue())
                        + " -> \"" + file.getKey() + "\"");
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "DECISION_LOG.md and docs/decisions/ have drifted. The heading text is the "
                        + "decision's identity and is resolved by exact string, so both sides must "
                        + "carry it identically: " + violations);
    }

    @Test
    void everyDecisionRecordsAcceptedStatusOnBothSides() throws IOException {
        List<String> violations = new ArrayList<>();

        for (Map.Entry<String, String> entry : indexEntries().entrySet()) {
            if (!entry.getValue().contains(ACCEPTED)) {
                violations.add("index entry omits \"" + ACCEPTED + "\": \"" + entry.getKey() + "\"");
            }
        }
        for (Map.Entry<String, Path> file : decisionFiles().entrySet()) {
            String body = read(file.getValue());
            if (!body.contains(ACCEPTED)) {
                violations.add("decision file omits \"" + ACCEPTED + "\": "
                        + PROJECT_ROOT.relativize(file.getValue()));
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "AcceptedDecisionProjector treats a section as accepted only when its status "
                        + "line reads exactly \"" + ACCEPTED + "\": " + violations);
    }

    @Test
    void decisionFilesDoNotShadowTheIndexHeadingLevel() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Map.Entry<String, Path> file : decisionFiles().entrySet()) {
            for (String line : read(file.getValue()).split("\n", -1)) {
                if (line.startsWith(INDEX_HEADING)) {
                    violations.add(PROJECT_ROOT.relativize(file.getValue()) + " -> " + line.strip());
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "a decision file must not use the level-3 heading the index reserves for "
                        + "decision identity, or a projector reading it would emit a duplicate "
                        + "node: " + violations);
    }

    @Test
    void decisionPathsStayClearOfTheWindowsPathCeiling() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : decisionFiles().values()) {
            int length = file.toAbsolutePath().toString().length();
            if (length > MAX_ABSOLUTE_PATH_CHARACTERS) {
                violations.add(PROJECT_ROOT.relativize(file) + " (" + length + " characters)");
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "decision file paths must stay under " + MAX_ABSOLUTE_PATH_CHARACTERS
                        + " characters so a deeper checkout does not hit the Windows MAX_PATH "
                        + "ceiling: " + violations);
    }

    @Test
    void justifiedByReferencesResolveAgainstTheIndex() throws IOException {
        Path currentTask = PROJECT_ROOT.resolve("CURRENT_TASK.md");
        Set<String> accepted = indexEntries().keySet();

        List<String> violations = new ArrayList<>();
        boolean inSection = false;
        for (String line : read(currentTask).split("\n", -1)) {
            if (line.startsWith("## ")) {
                inSection = line.strip().equals("## Justified By");
                continue;
            }
            if (inSection && line.startsWith("- ")) {
                String reference = line.substring(2).strip();
                if (!accepted.contains(reference)) {
                    violations.add(reference);
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "TaskJustificationProjector rejects an unresolved reference rather than "
                        + "skipping it, so every CURRENT_TASK.md \"Justified By\" bullet must name "
                        + "an accepted decision heading exactly: " + violations);
    }

    /** Index heading text to the entry body that follows it, in document order. */
    private static Map<String, String> indexEntries() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        Set<String> duplicates = new LinkedHashSet<>();
        String heading = null;
        StringBuilder body = new StringBuilder();

        for (String line : read(INDEX).split("\n", -1)) {
            if (line.startsWith(INDEX_HEADING)) {
                if (heading != null && entries.put(heading, body.toString()) != null) {
                    duplicates.add(heading);
                }
                heading = line.substring(INDEX_HEADING.length()).strip();
                body = new StringBuilder();
            } else if (heading != null && line.startsWith("## ")) {
                if (entries.put(heading, body.toString()) != null) {
                    duplicates.add(heading);
                }
                heading = null;
            } else if (heading != null) {
                body.append(line).append('\n');
            }
        }
        if (heading != null && entries.put(heading, body.toString()) != null) {
            duplicates.add(heading);
        }

        assertTrue(
                duplicates.isEmpty(),
                () -> "AcceptedDecisionProjector rejects a duplicate heading: " + duplicates);
        return entries;
    }

    /** Decision file H1 heading text to its path. */
    private static Map<String, Path> decisionFiles() throws IOException {
        assertTrue(
                Files.isDirectory(DECISIONS),
                () -> "missing decision directory: " + DECISIONS);

        Map<String, Path> files = new LinkedHashMap<>();
        List<String> malformed = new ArrayList<>();
        try (var paths = Files.list(DECISIONS)) {
            for (Path path : paths
                    .filter(candidate -> candidate.toString().endsWith(".md"))
                    .sorted()
                    .toList()) {
                String first = read(path).split("\n", -1)[0];
                if (!first.startsWith(FILE_HEADING) || first.startsWith(INDEX_HEADING)) {
                    malformed.add(PROJECT_ROOT.relativize(path).toString());
                    continue;
                }
                files.put(first.substring(FILE_HEADING.length()).strip(), path);
            }
        }

        assertTrue(
                malformed.isEmpty(),
                () -> "every decision file must open with its heading as a level-1 title: "
                        + malformed);
        return files;
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}
