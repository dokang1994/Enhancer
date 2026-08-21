package com.enhancer.bus;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.model.ModelCostBudget;
import com.enhancer.model.ModelDataClassification;
import com.enhancer.model.ModelExecutionProfile;
import com.enhancer.model.ModelInvokeTool;
import com.enhancer.model.ModelLocalityRequirement;
import com.enhancer.model.ModelReasoningRequirement;
import com.enhancer.model.ModelTokenBudget;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModelWorkPayloadTest {
    private static final ApprovedTaskRevision TASK_REVISION = new ApprovedTaskRevision(
            "model-work-payload-test",
            "CURRENT_TASK.md",
            "f".repeat(64));
    private static final String SNAPSHOT_ID = "a".repeat(64);
    private static final String RESPONSE_SHA256 = "e".repeat(64);

    @Test
    void retainsOneExactCompleteProfileAsMandatoryImmutableModelWork() {
        ModelExecutionProfile profile = profile("repository-analysis", "reasoning-standard");
        ModelWorkPayload.ModelInvocationExecutionInput input =
                new ModelWorkPayload.ModelInvocationExecutionInput(
                        "docs/prompt.md", RESPONSE_SHA256, profile);
        LinkedHashSet<String> mutableTools = new LinkedHashSet<>(Set.of(
                ModelWorkPayload.MODEL_INVOKE_TOOL_NAME,
                "read-file"));

        ModelWorkPayload payload = new ModelWorkPayload(
                TASK_REVISION, SNAPSHOT_ID, mutableTools, input);
        mutableTools.add("later-tool");

        assertEquals(TASK_REVISION, payload.taskRevision());
        assertEquals(SNAPSHOT_ID, payload.snapshotId());
        assertEquals(Set.of("model-invoke", "read-file"), payload.allowedTools());
        assertEquals(input, payload.executionInput());
        assertEquals("docs/prompt.md", input.targetPath());
        assertEquals(RESPONSE_SHA256, input.expectedResponseSha256());
        assertSame(profile, input.executionProfile());
        assertEquals("repository-analysis", profile.requiredCapability());
        assertEquals("reasoning-standard", profile.modelClass());
        assertNotEquals(profile.requiredCapability(), profile.modelClass());
        assertNotEquals(ModelWorkPayload.MODEL_INVOKE_TOOL_NAME, profile.modelClass());
        assertThrows(
                UnsupportedOperationException.class,
                () -> payload.allowedTools().add("another-tool"));
        assertFalse(payload.allowedTools().contains("later-tool"));
    }

    @Test
    void hasDeterministicNestedValueSemantics() {
        ModelWorkPayload first = payload(profile("capability-one", "model-class"));
        ModelWorkPayload equal = payload(profile("capability-one", "model-class"));
        ModelWorkPayload changed = payload(profile("capability-two", "model-class"));

        assertEquals(first, equal);
        assertEquals(first.hashCode(), equal.hashCode());
        assertNotEquals(first, changed);
    }

    @Test
    void exposesOnlyTheExactClosedDataShape() {
        assertTrue(ModelWorkPayload.class.isRecord());
        assertTrue(Modifier.isPublic(ModelWorkPayload.class.getModifiers()));
        assertTrue(Modifier.isFinal(ModelWorkPayload.class.getModifiers()));
        assertArrayEquals(
                new String[] {"taskRevision", "snapshotId", "allowedTools", "executionInput"},
                componentNames(ModelWorkPayload.class));
        assertArrayEquals(
                new Class<?>[] {
                        ApprovedTaskRevision.class,
                        String.class,
                        Set.class,
                        ModelWorkPayload.ModelInvocationExecutionInput.class
                },
                componentTypes(ModelWorkPayload.class));

        Class<?> inputType = ModelWorkPayload.ModelInvocationExecutionInput.class;
        assertTrue(inputType.isRecord());
        assertTrue(Modifier.isPublic(inputType.getModifiers()));
        assertTrue(Modifier.isStatic(inputType.getModifiers()));
        assertTrue(Modifier.isFinal(inputType.getModifiers()));
        assertArrayEquals(
                new String[] {"targetPath", "expectedResponseSha256", "executionProfile"},
                componentNames(inputType));
        assertArrayEquals(
                new Class<?>[] {String.class, String.class, ModelExecutionProfile.class},
                componentTypes(inputType));

        for (RecordComponent component : ModelWorkPayload.class.getRecordComponents()) {
            assertFalse(component.getGenericType().getTypeName().contains("Optional"));
        }
        for (RecordComponent component : inputType.getRecordComponents()) {
            assertFalse(component.getGenericType().getTypeName().contains("Optional"));
        }
        assertArrayEquals(
                new Class<?>[] {MessagePayload.class},
                ModelWorkPayload.class.getInterfaces());
        assertEquals(ModelInvokeTool.NAME, ModelWorkPayload.MODEL_INVOKE_TOOL_NAME);
    }

    @Test
    void rejectsEveryMissingOuterOrInnerValue() {
        ModelExecutionProfile profile = profile("capability", "model-class");
        ModelWorkPayload.ModelInvocationExecutionInput input = input(profile);

        assertThrows(NullPointerException.class, () -> new ModelWorkPayload(
                null, SNAPSHOT_ID, Set.of("model-invoke"), input));
        assertThrows(NullPointerException.class, () -> new ModelWorkPayload(
                TASK_REVISION, null, Set.of("model-invoke"), input));
        assertThrows(NullPointerException.class, () -> new ModelWorkPayload(
                TASK_REVISION, SNAPSHOT_ID, null, input));
        assertThrows(NullPointerException.class, () -> new ModelWorkPayload(
                TASK_REVISION, SNAPSHOT_ID, Set.of("model-invoke"), null));
        assertThrows(NullPointerException.class, () ->
                new ModelWorkPayload.ModelInvocationExecutionInput(
                        null, RESPONSE_SHA256, profile));
        assertThrows(NullPointerException.class, () ->
                new ModelWorkPayload.ModelInvocationExecutionInput(
                        "docs/prompt.md", null, profile));
        assertThrows(NullPointerException.class, () ->
                new ModelWorkPayload.ModelInvocationExecutionInput(
                        "docs/prompt.md", RESPONSE_SHA256, null));
    }

    @Test
    void requiresAnExactModelInvokeToolScopeAndBoundsEveryTool() {
        ModelWorkPayload.ModelInvocationExecutionInput input = input(
                profile("capability", "model-class"));

        assertThrows(IllegalArgumentException.class, () -> new ModelWorkPayload(
                TASK_REVISION, SNAPSHOT_ID, Set.of(), input));
        assertThrows(IllegalArgumentException.class, () -> new ModelWorkPayload(
                TASK_REVISION, SNAPSHOT_ID, Set.of("read-file"), input));
        assertThrows(IllegalArgumentException.class, () -> new ModelWorkPayload(
                TASK_REVISION, SNAPSHOT_ID, Set.of("MODEL-INVOKE"), input));
        assertThrows(IllegalArgumentException.class, () -> new ModelWorkPayload(
                TASK_REVISION, SNAPSHOT_ID, Set.of("model-invoke", " "), input));
        assertThrows(IllegalArgumentException.class, () -> new ModelWorkPayload(
                TASK_REVISION,
                SNAPSHOT_ID,
                Set.of("model-invoke", "x".repeat(257)),
                input));
        LinkedHashSet<String> withNull = new LinkedHashSet<>(Set.of("model-invoke"));
        withNull.add(null);
        assertThrows(NullPointerException.class, () -> new ModelWorkPayload(
                TASK_REVISION, SNAPSHOT_ID, withNull, input));
    }

    @Test
    void acceptsTheToolCardinalityBoundaryAndRejectsOverflow() {
        ModelWorkPayload.ModelInvocationExecutionInput input = input(
                profile("capability", "model-class"));
        Set<String> atLimit = tools(ModelWorkPayload.MAX_ALLOWED_TOOLS);
        Set<String> oversized = tools(ModelWorkPayload.MAX_ALLOWED_TOOLS + 1);

        assertEquals(256, ModelWorkPayload.MAX_ALLOWED_TOOLS);
        assertEquals(
                ModelWorkPayload.MAX_ALLOWED_TOOLS,
                new ModelWorkPayload(TASK_REVISION, SNAPSHOT_ID, atLimit, input)
                        .allowedTools().size());
        assertThrows(IllegalArgumentException.class, () -> new ModelWorkPayload(
                TASK_REVISION, SNAPSHOT_ID, oversized, input));
    }

    @Test
    void reusesExistingSnapshotTargetAndDigestBounds() {
        ModelExecutionProfile profile = profile("capability", "model-class");

        assertThrows(IllegalArgumentException.class, () -> new ModelWorkPayload(
                TASK_REVISION,
                "not-a-digest",
                Set.of("model-invoke"),
                input(profile)));
        assertThrows(IllegalArgumentException.class, () ->
                new ModelWorkPayload.ModelInvocationExecutionInput(
                        " ", RESPONSE_SHA256, profile));
        assertThrows(IllegalArgumentException.class, () ->
                new ModelWorkPayload.ModelInvocationExecutionInput(
                        "x".repeat(1025), RESPONSE_SHA256, profile));
        assertThrows(IllegalArgumentException.class, () ->
                new ModelWorkPayload.ModelInvocationExecutionInput(
                        "docs/prompt.md", "e".repeat(63), profile));
        assertThrows(IllegalArgumentException.class, () ->
                new ModelWorkPayload.ModelInvocationExecutionInput(
                        "docs/prompt.md", "E".repeat(64), profile));
    }

    private static ModelWorkPayload payload(ModelExecutionProfile profile) {
        return new ModelWorkPayload(
                TASK_REVISION,
                SNAPSHOT_ID,
                Set.of("model-invoke", "read-file"),
                input(profile));
    }

    private static ModelWorkPayload.ModelInvocationExecutionInput input(
            ModelExecutionProfile profile) {
        return new ModelWorkPayload.ModelInvocationExecutionInput(
                "docs/prompt.md", RESPONSE_SHA256, profile);
    }

    private static ModelExecutionProfile profile(String capability, String modelClass) {
        return new ModelExecutionProfile(
                ModelExecutionProfile.SCHEMA_VERSION,
                capability,
                modelClass,
                ModelLocalityRequirement.LOCAL_ONLY,
                ModelReasoningRequirement.STANDARD,
                8192,
                new ModelTokenBudget(2048, 1024, 4096),
                new ModelCostBudget("USD", 10_000L),
                Duration.ofSeconds(30),
                ModelDataClassification.CONFIDENTIAL);
    }

    private static Set<String> tools(int size) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        values.add("model-invoke");
        for (int index = 1; index < size; index++) {
            values.add("tool-" + index);
        }
        return values;
    }

    private static String[] componentNames(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(RecordComponent::getName)
                .toArray(String[]::new);
    }

    private static Class<?>[] componentTypes(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(RecordComponent::getType)
                .toArray(Class<?>[]::new);
    }
}
