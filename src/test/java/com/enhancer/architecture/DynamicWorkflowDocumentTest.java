package com.enhancer.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Keeps the optional dynamic increment queue bounded by the single active task. */
class DynamicWorkflowDocumentTest {
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("user.dir"));
    private static final Pattern IDENTIFIER =
            Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");
    private static final Pattern INCREMENT_HEADING = Pattern.compile(
            "(?m)^### Increment (\\d+) - ([a-z0-9]+(?:-[a-z0-9]+)*)\\s*$");
    private static final Set<String> STATES =
            Set.of("Pending", "In Progress", "Completed", "Blocked");

    @Test
    void liveDynamicWorkflowIsBoundedSequentialAndDependencySafe() throws IOException {
        String task = read("CURRENT_TASK.md");
        String workflow = section(task, "Dynamic Workflow");
        int firstIncrement = workflow.indexOf("### Increment ");
        assertTrue(firstIncrement >= 0, "dynamic workflow must declare increments");
        String metadata = workflow.substring(0, firstIncrement);

        String workflowId = value(metadata, "Workflow ID");
        assertTrue(IDENTIFIER.matcher(workflowId).matches(), "workflow ID must be stable");
        assertEquals("Sequential", value(metadata, "Mode"));
        int limit = Integer.parseInt(value(metadata, "Increment Limit"));
        assertTrue(limit >= 2 && limit <= 16, "increment limit must be between 2 and 16");
        assertTrue(value(metadata, "Selection Rule")
                .startsWith("Select the first dependency-ready Pending increment"));
        assertFalse(value(metadata, "Stop Conditions").isBlank());

        List<Increment> increments = increments(workflow);
        assertEquals(limit, increments.size(), "declared limit must equal the increment count");

        Set<String> seen = new HashSet<>();
        Map<String, String> states = new HashMap<>();
        int inProgress = 0;
        int activeIndex = -1;
        for (int index = 0; index < increments.size(); index++) {
            Increment increment = increments.get(index);
            assertEquals(index + 1, increment.number(), "increment numbers must be ordered");
            assertTrue(seen.add(increment.id()), "increment IDs must be unique");
            assertTrue(STATES.contains(increment.state()), "increment state is invalid");

            for (String field : List.of(
                    "Scope", "Exit Criteria", "Verification", "Next Action")) {
                assertFalse(value(increment.body(), field).isBlank(),
                        () -> increment.id() + " is missing " + field);
            }

            for (String dependency : increment.dependencies()) {
                assertTrue(states.containsKey(dependency),
                        () -> increment.id() + " dependency must reference an earlier increment");
                if (increment.state().equals("In Progress")
                        || increment.state().equals("Completed")) {
                    assertEquals("Completed", states.get(dependency),
                            () -> increment.id() + " started before its dependency completed");
                }
            }
            if (increment.state().equals("In Progress")) {
                inProgress++;
                activeIndex = index;
            }
            states.put(increment.id(), increment.state());
        }
        assertTrue(inProgress <= 1, "at most one increment may be In Progress");
        if (activeIndex >= 0) {
            for (int index = 0; index < activeIndex; index++) {
                Increment earlier = increments.get(index);
                boolean dependencyReady = earlier.dependencies().stream()
                        .allMatch(dependency -> states.get(dependency).equals("Completed"));
                assertFalse(earlier.state().equals("Pending") && dependencyReady,
                        () -> earlier.id() + " was ready before the selected increment");
            }
        }

        String taskStatus = section(task, "Status").strip();
        if (taskStatus.equals("In Progress")) {
            assertEquals(1, inProgress, "an active dynamic task needs one current increment");
        } else if (taskStatus.equals("Completed")) {
            assertTrue(increments.stream().allMatch(i -> i.state().equals("Completed")),
                    "a completed parent task cannot retain unfinished increments");
        }
    }

    @Test
    void governedInstructionSurfacesConnectTheDynamicWorkflow() throws IOException {
        Map<String, String> requiredConnections = Map.of(
                "AGENTS.md", "## Dynamic Workflow Rules",
                ".ai/workflow.md", "### Dynamic Increment Workflow",
                "ARCHITECTURE.md", "### Document-Driven Dynamic Increment Workflow Boundary",
                ".ai/architecture.md", "## Dynamic Workflow",
                "README.md", "## Document-Driven Dynamic Increment Workflows",
                "prompts/IMPLEMENT_TASK.md", "## Dynamic Workflow",
                "prompts/SESSION_START.md", "## Dynamic Workflow",
                "prompts/SESSION_CLOSE.md", "For a dynamic workflow");

        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, String> connection : requiredConnections.entrySet()) {
            if (!read(connection.getKey()).contains(connection.getValue())) {
                missing.add(connection.getKey() + " -> " + connection.getValue());
            }
        }
        assertTrue(missing.isEmpty(), () -> "dynamic workflow connections are missing: " + missing);
    }

    @Test
    void adaptiveDevelopmentSubagentPolicyIsBoundedAndConnected() throws IOException {
        Map<String, String> requiredConnections = Map.of(
                "AGENTS.md", "## Adaptive Development Subagent Delegation",
                ".ai/workflow.md", "### Adaptive Development Subagent Delegation",
                "ARCHITECTURE.md", "### Development-Time Adaptive Subagent Delegation",
                ".ai/architecture.md", "Adaptive development subagents",
                "README.md", "## Adaptive Development Subagents",
                "prompts/IMPLEMENT_TASK.md", "## Adaptive Subagent Delegation",
                "prompts/SESSION_START.md", "## Adaptive Subagent Delegation",
                "prompts/SESSION_CLOSE.md", "Adaptive subagent delegation");

        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, String> connection : requiredConnections.entrySet()) {
            if (!read(connection.getKey()).contains(connection.getValue())) {
                missing.add(connection.getKey() + " -> " + connection.getValue());
            }
        }
        assertTrue(missing.isEmpty(),
                () -> "adaptive subagent connections are missing: " + missing);

        String policy = section(read("AGENTS.md"), "Adaptive Development Subagent Delegation");
        String compactPolicy = policy.replaceAll("\\s+", " ");
        String normalized = compactPolicy.toLowerCase(Locale.ROOT);
        assertTrue(compactPolicy.contains(
                "The user supplies the task authority; the primary Agent selects the execution topology."));
        assertTrue(compactPolicy.contains("at least two independent bounded subtasks"));
        assertTrue(compactPolicy.contains("coordination cost"));
        assertTrue(compactPolicy.contains("small, sequential, tightly coupled"));
        assertTrue(compactPolicy.contains("at most three concurrent subagents"));
        assertTrue(compactPolicy.contains("one delegation level"));
        assertTrue(compactPolicy.contains("three dispatches per increment"));
        assertTrue(compactPolicy.contains("six dispatches per Active Task"));
        assertTrue(compactPolicy.contains("read-only"));
        assertTrue(compactPolicy.contains("primary Agent alone"));
        assertTrue(compactPolicy.contains("reports are recommendations, never verification evidence"));
        assertTrue(compactPolicy.contains("increment selection remains sequential"));
        assertTrue(compactPolicy.contains("Gate 13"));
        assertFalse(normalized.contains("explicit user approval"),
                "subagent topology selection must not add a per-use approval condition");
    }

    private static List<Increment> increments(String workflow) {
        Matcher matcher = INCREMENT_HEADING.matcher(workflow);
        List<Heading> headings = new ArrayList<>();
        while (matcher.find()) {
            headings.add(new Heading(
                    Integer.parseInt(matcher.group(1)),
                    matcher.group(2),
                    matcher.end()));
        }

        List<Increment> increments = new ArrayList<>();
        for (int index = 0; index < headings.size(); index++) {
            Heading heading = headings.get(index);
            int bodyEnd = index + 1 < headings.size()
                    ? workflow.lastIndexOf("### Increment ", headings.get(index + 1).bodyStart())
                    : workflow.length();
            String body = workflow.substring(heading.bodyStart(), bodyEnd);
            String dependencies = value(body, "Depends On");
            List<String> dependencyIds = dependencies.equals("none")
                    ? List.of()
                    : List.of(dependencies.split("\\s*,\\s*"));
            increments.add(new Increment(
                    heading.number(),
                    heading.id(),
                    value(body, "State"),
                    dependencyIds,
                    body));
        }
        return increments;
    }

    private static String section(String markdown, String heading) {
        Pattern pattern = Pattern.compile(
                "(?ms)^## " + Pattern.quote(heading) + "\\s*$\\R(.*?)(?=^## |\\z)");
        Matcher matcher = pattern.matcher(markdown);
        assertTrue(matcher.find(), () -> "missing section: " + heading);
        return matcher.group(1);
    }

    private static String value(String content, String name) {
        Pattern pattern = Pattern.compile(
                "(?m)^" + Pattern.quote(name) + ":\\s*(\\S.*)$");
        Matcher matcher = pattern.matcher(content);
        assertTrue(matcher.find(), () -> "missing or blank field: " + name);
        return matcher.group(1).strip();
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(PROJECT_ROOT.resolve(relativePath), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
    }

    private record Heading(int number, String id, int bodyStart) {}

    private record Increment(
            int number,
            String id,
            String state,
            List<String> dependencies,
            String body) {}
}
