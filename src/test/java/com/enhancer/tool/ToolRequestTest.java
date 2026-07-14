package com.enhancer.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ToolRequestTest {
    @Test
    void preservesValidatedIdentityAndAnImmutableArgumentSnapshot() {
        Map<String, String> source = new HashMap<>();
        source.put("path", "docs/readme.md");

        ToolRequest request = new ToolRequest("read-file", "run-1", source);
        source.put("path", "changed.md");

        assertEquals("read-file", request.toolName());
        assertEquals("run-1", request.correlationId());
        assertEquals("docs/readme.md", request.arguments().get("path"));
        assertThrows(
                UnsupportedOperationException.class,
                () -> request.arguments().put("other", "value"));
    }

    @Test
    void rejectsMissingIdentityOrInvalidArguments() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ToolRequest(" ", "run-1", Map.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ToolRequest("read-file", " ", Map.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ToolRequest("read-file", "run-1", Map.of(" ", "value")));
        assertThrows(
                NullPointerException.class,
                () -> new ToolRequest("read-file", "run-1", null));
    }
}
