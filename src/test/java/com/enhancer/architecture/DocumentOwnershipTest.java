package com.enhancer.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Makes Constitution Section 4 executable.
 *
 * <p>Every project fact has exactly one owning document. Stating the rule in prose
 * was not enough: {@code docs/11-Architecture.md} drifted to claim Gate 7 was
 * {@code Specified - Next} long after Gate 8 had taken that marker, because nothing
 * checked it. These assertions are the check.
 */
class DocumentOwnershipTest {
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("user.dir"));

    /**
     * A gate maturity verdict: "Gate 7 is Contract Verified", "Delivery Gate 6 ... is
     * Integrated". Markdown emphasis is stripped before matching, so the bold form used
     * by the {@code docs/} specifications is caught too.
     */
    private static final Pattern GATE_MATURITY_CLAIM = Pattern.compile(
            "\\bGate \\d+\\b[^.\\n]{0,120}?\\b(?:is|are|remains|remain)\\b[^.\\n]{0,60}?"
                    + "\\b(?:Contract Verified|Integrated|Operational|Specified - Next)\\b");

    /**
     * Documents that legitimately record maturity.
     *
     * <p>{@code PROJECT_STATE.md} owns it. The rest are append-only historical records
     * whose entries were true when written and are never revised, plus {@code ROADMAP.md},
     * which owns the {@code Status: Specified - Next} gate grammar that
     * {@code RepositoryTaskPlanner} parses.
     */
    private static final Set<String> MATURITY_EXEMPT = Set.of(
            "PROJECT_STATE.md",
            "ROADMAP.md",
            "DECISION_LOG.md",
            "CHANGELOG.md",
            "docs/verification-log.md");

    private static final Set<String> EXEMPT_DIRECTORIES = Set.of("docs/superpowers");

    @Test
    void onlyProjectStateClaimsGateMaturity() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path document : markdownDocuments()) {
            String relative = relativePath(document);
            if (MATURITY_EXEMPT.contains(relative) || isExemptDirectory(relative)) {
                continue;
            }
            String[] lines = stripEmphasis(read(document)).split("\n", -1);
            for (int index = 0; index < lines.length; index++) {
                Matcher matcher = GATE_MATURITY_CLAIM.matcher(lines[index]);
                if (matcher.find()) {
                    violations.add(relative + ":" + (index + 1) + " -> \"" + matcher.group() + "\"");
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "capability maturity is owned by PROJECT_STATE.md alone "
                        + "(Constitution Section 4); remove these claims rather than "
                        + "updating them: " + violations);
    }

    @Test
    void onlyCurrentTaskDeclaresTheNextTask() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path document : markdownDocuments()) {
            String relative = relativePath(document);
            if (relative.equals("CURRENT_TASK.md") || isExemptDirectory(relative)) {
                continue;
            }
            String[] lines = read(document).split("\n", -1);
            for (int index = 0; index < lines.length; index++) {
                if (lines[index].strip().equals("## Next Task")) {
                    violations.add(relative + ":" + (index + 1));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "the next task is owned by CURRENT_TASK.md alone "
                        + "(Constitution Section 4): " + violations);
    }

    private static List<Path> markdownDocuments() throws IOException {
        try (var paths = Files.walk(PROJECT_ROOT)) {
            return paths
                    .filter(path -> path.toString().endsWith(".md"))
                    .filter(DocumentOwnershipTest::isProjectDocument)
                    .sorted()
                    .toList();
        }
    }

    private static boolean isProjectDocument(Path path) {
        String relative = relativePath(path);
        return !relative.startsWith("build/")
                && !relative.startsWith(".git/")
                && !relative.startsWith(".gradle/")
                && !relative.startsWith(".tools/")
                && !relative.startsWith(".enhancer/");
    }

    private static boolean isExemptDirectory(String relative) {
        return EXEMPT_DIRECTORIES.stream().anyMatch(relative::startsWith);
    }

    private static String relativePath(Path path) {
        return PROJECT_ROOT.relativize(path).toString().replace('\\', '/');
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static String stripEmphasis(String markdown) {
        return markdown.replace("*", "").replace("`", "");
    }
}
