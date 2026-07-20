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
     * A gate maturity verdict is any sentence naming both a numbered gate and a maturity level.
     *
     * <p>An earlier version matched only the "Gate 7 is Contract Verified" word order and let
     * "retains Gate 7 at Contract Verified" through, which is how one claim survived in
     * {@code ARCHITECTURE.md}. Rather than chase verbs and connectors, this treats co-occurrence
     * within one sentence as the violation: a document that does not own maturity has no reason
     * to name a gate and a maturity level in the same breath. Markdown emphasis is stripped
     * first, so the bold form used by the {@code docs/} specifications is caught too.
     */
    private static final String MATURITY =
            "(?:Contract Verified|Integrated|Operational|Specified - Next)";

    /** "Gate 7 is Contract Verified", "Delivery Gate 6 ... remains Integrated". */
    private static final Pattern SUBJECT_FIRST = Pattern.compile(
            "\\bGate \\d+\\b[^.\\n]{0,120}?\\b(?:is|are|was|were|remains?|stays?|becomes?)\\b"
                    + "[^.\\n]{0,60}?\\b" + MATURITY + "\\b");

    /**
     * "retains Gate 7 at Contract Verified", "promotes Gate 6 to Integrated".
     *
     * <p>This order is why one claim survived the first version of this test: it matched only the
     * subject-first form. Co-occurrence alone was tried instead and rejected — it also flags
     * forward-looking conditions ("require an Operational single-agent baseline") and the
     * commentary explaining why a claim was removed, neither of which is a verdict.
     */
    private static final Pattern VERB_FIRST = Pattern.compile(
            "\\b(?:retains?|promotes?|keeps?|classifies|classify|leaves?|holds?|places?)\\b"
                    + "[^.\\n]{0,60}?\\bGate \\d+\\b[^.\\n]{0,40}?\\b(?:at|to|as)\\b"
                    + "[^.\\n]{0,40}?\\b" + MATURITY + "\\b");

    /** A parenthetical verdict beside a gate, as used in the connection-sequence table. */
    private static final Pattern PARENTHETICAL = Pattern.compile(
            "\\(" + MATURITY + "\\)[^|\\n]*\\|[^|\\n]*\\bGate \\d+\\b"
                    + "|\\bGate \\d+\\b[^|\\n]*\\|[^|\\n]*\\(" + MATURITY + "\\)");

    private static final List<Pattern> MATURITY_CLAIMS =
            List.of(SUBJECT_FIRST, VERB_FIRST, PARENTHETICAL);

    private static final Pattern NEXT_TASK_HEADING =
            Pattern.compile("^##\\s+Next(?:\\s+Task)?\\s*$", Pattern.CASE_INSENSITIVE);

    private static final Pattern NEXT_TASK_DECLARATION = Pattern.compile(
            "^\\s*(?:-\\s*)?(?:the\\s+)?next\\s+(?:task|increment)\\s+"
                    + "(?::|is\\s+(?!owned\\b)).*$",
            Pattern.CASE_INSENSITIVE);

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

    /**
     * {@code docs/decisions} holds the per-decision files split out of {@code DECISION_LOG.md}.
     * They are the same append-only records that document was exempt for: each states the
     * maturity that was true when the decision was accepted and is never revised afterwards.
     */
    private static final Set<String> EXEMPT_DIRECTORIES = Set.of(
            "docs/superpowers",
            "docs/decisions");

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
                for (Pattern claim : MATURITY_CLAIMS) {
                    Matcher matcher = claim.matcher(lines[index]);
                    if (matcher.find()) {
                        violations.add(relative + ":" + (index + 1)
                                + " -> \"" + matcher.group().strip() + "\"");
                        break;
                    }
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
                String line = lines[index];
                if (NEXT_TASK_HEADING.matcher(line).matches()
                        || NEXT_TASK_DECLARATION.matcher(line).matches()) {
                    violations.add(relative + ":" + (index + 1)
                            + " -> \"" + line.strip() + "\"");
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
