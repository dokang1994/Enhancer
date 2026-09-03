package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.enhancer.model.DeterministicFakeExactRequestInvocationResult;
import com.enhancer.model.DeterministicFakeExactRequestInvoker;
import com.enhancer.model.DeterministicFakeModelGateway;
import com.enhancer.model.ModelFailureCode;
import com.enhancer.model.ModelResponse;
import com.enhancer.model.ModelUsage;
import com.enhancer.tool.EvidenceRunNamespaceStore;
import com.enhancer.tool.EvidenceStoragePolicy;
import com.enhancer.tool.ExecutionPolicy;
import com.enhancer.tool.FileSystemEvidenceStore;
import com.enhancer.tool.ToolFailureCode;
import com.enhancer.tool.ToolRequest;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.ToolResultStatus;
import com.enhancer.tool.VerificationEvidence;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeterministicFakeReturnedOutcomeToolTest {
    @TempDir
    Path temporaryRoot;

    @Test
    void mapsEveryGatewayFailureToCodeOnlyEvidenceWithoutStorage() throws Exception {
        Map<ModelFailureCode, ToolFailureCode> expected = new EnumMap<>(ModelFailureCode.class);
        expected.put(ModelFailureCode.TIMED_OUT, ToolFailureCode.TIMED_OUT);
        expected.put(ModelFailureCode.PROVIDER_UNAVAILABLE, ToolFailureCode.TEMPORARY_FAILURE);
        expected.put(ModelFailureCode.RESPONSE_INVALID, ToolFailureCode.INVALID_RESULT);
        expected.put(ModelFailureCode.BUDGET_EXCEEDED, ToolFailureCode.TOOL_REPORTED_FAILURE);

        for (var mapping : expected.entrySet()) {
            DeterministicFakeModelGateway gateway = mock(DeterministicFakeModelGateway.class);
            ModelAttemptTestFixture.Fixture fixture = fixture("prompt", gateway);
            doThrow(new com.enhancer.model.ModelGatewayException(
                    mapping.getKey(), "secret gateway diagnostic"))
                    .when(gateway)
                    .invoke(same(fixture.preparation().profiledRequest().request()));
            DeterministicFakeExactRequestInvocationResult returned =
                    new DeterministicFakeExactRequestInvoker()
                            .invoke(ModelAttemptTestFixture.requireReady(fixture));
            EvidenceRunNamespaceStore store = mock(EvidenceRunNamespaceStore.class);
            DeterministicFakeReturnedOutcomeTool tool =
                    new DeterministicFakeReturnedOutcomeTool(
                            returned, ModelAttemptTestFixture.TARGET_PATH, store);

            ToolResult result = tool.execute(
                    tool.request(), fixture.preparation().executionPolicy());

            assertEquals(ToolResultStatus.FAILURE, result.status());
            assertEquals(mapping.getValue(), result.failureCode().orElseThrow());
            assertEquals("tool-failure-code=" + mapping.getValue().name(),
                    result.evidence().outputTail());
            assertFalse(result.evidence().outputTail().contains("secret"));
            verifyNoInteractions(store);
        }
    }

    @Test
    void validatesAllResponseStructureBeforeEvidenceActivity() throws Exception {
        String prompt = "structure";
        ModelResponse expected = realResponse(prompt);
        ModelResponse[] invalid = {
            new ModelResponse(expected.text(), "different-model", expected.usage()),
            new ModelResponse(expected.text() + "x", expected.modelClass(),
                    new ModelUsage(prompt.length(), expected.text().length() + 1L)),
            new ModelResponse(expected.text(), expected.modelClass(),
                    new ModelUsage(prompt.length() + 1L, expected.text().length())),
            new ModelResponse(expected.text(), expected.modelClass(),
                    new ModelUsage(prompt.length(), expected.text().length() - 1L))
        };

        for (ModelResponse response : invalid) {
            EvidenceRunNamespaceStore store = mock(EvidenceRunNamespaceStore.class);
            Returned returned = succeeded(prompt, response);
            DeterministicFakeReturnedOutcomeTool tool =
                    new DeterministicFakeReturnedOutcomeTool(
                            returned.result(), ModelAttemptTestFixture.TARGET_PATH, store);

            ToolResult result = tool.execute(tool.request(), returned.policy());

            assertEquals(ToolFailureCode.INVALID_RESULT,
                    result.failureCode().orElseThrow());
            assertEquals("tool-failure-code=INVALID_RESULT", result.evidence().outputTail());
            assertFalse(result.evidence().outputTail().contains(response.text()));
            verifyNoInteractions(store);
        }
    }

    @Test
    void keepsShortEvidenceInlineAndLazilyPersistsCompleteLongEvidence() throws Exception {
        Path shortRoot = temporaryRoot.resolve("short");
        FileSystemEvidenceStore shortStore = store(shortRoot);
        Returned shortReturned = succeeded("short", realResponse("short"));
        DeterministicFakeReturnedOutcomeTool shortTool =
                new DeterministicFakeReturnedOutcomeTool(
                        shortReturned.result(), ModelAttemptTestFixture.TARGET_PATH, shortStore);

        ToolResult shortResult = shortTool.execute(shortTool.request(), shortReturned.policy());

        assertEquals(ToolResultStatus.SUCCESS, shortResult.status());
        assertFalse(shortResult.evidence().truncated());
        assertFalse(Files.exists(shortRoot));

        String longPrompt = "x".repeat(5_000);
        String longText = realResponse(longPrompt).text();
        Path longRoot = temporaryRoot.resolve("long");
        FileSystemEvidenceStore longStore = store(longRoot);
        Returned longReturned = succeeded(longPrompt, realResponse(longPrompt));
        DeterministicFakeReturnedOutcomeTool longTool =
                new DeterministicFakeReturnedOutcomeTool(
                        longReturned.result(), ModelAttemptTestFixture.TARGET_PATH, longStore);

        ToolResult longResult = longTool.execute(longTool.request(), longReturned.policy());

        assertTrue(longResult.evidence().truncated());
        assertEquals(
                longText,
                longStore.resolve(
                        longResult.evidence().fullOutputReference().orElseThrow()).content());
    }

    @Test
    void requiresExactRequestPolicyAndOneShotUseWithoutCallingGatewayAgain() throws Exception {
        DeterministicFakeModelGateway gateway = mock(DeterministicFakeModelGateway.class);
        ModelResponse response = realResponse("identity");
        ModelAttemptTestFixture.Fixture fixture = fixture("identity", gateway);
        when(gateway.invoke(same(fixture.preparation().profiledRequest().request())))
                .thenReturn(response);
        DeterministicFakeExactRequestInvocationResult returned =
                new DeterministicFakeExactRequestInvoker()
                        .invoke(ModelAttemptTestFixture.requireReady(fixture));
        reset(gateway);
        DeterministicFakeReturnedOutcomeTool differentRequestTool =
                new DeterministicFakeReturnedOutcomeTool(
                        returned, ModelAttemptTestFixture.TARGET_PATH,
                        mock(EvidenceRunNamespaceStore.class));
        ToolRequest equalRequest = new ToolRequest(
                differentRequestTool.request().toolName(),
                differentRequestTool.request().correlationId(),
                differentRequestTool.request().arguments());
        assertThrows(IllegalArgumentException.class, () -> differentRequestTool.execute(
                equalRequest, fixture.preparation().executionPolicy()));

        DeterministicFakeReturnedOutcomeTool differentPolicyTool =
                new DeterministicFakeReturnedOutcomeTool(
                        returned, ModelAttemptTestFixture.TARGET_PATH,
                        mock(EvidenceRunNamespaceStore.class));
        ExecutionPolicy equalPolicy = new ExecutionPolicy(
                fixture.preparation().executionPolicy().projectRoot(),
                fixture.preparation().executionPolicy().allowedTools(),
                fixture.preparation().executionPolicy().deniedTools(),
                fixture.preparation().executionPolicy().maxReadBytes(),
                fixture.preparation().executionPolicy().timeout(),
                fixture.preparation().executionPolicy().cancellationToken());
        assertThrows(IllegalArgumentException.class, () -> differentPolicyTool.execute(
                differentPolicyTool.request(), equalPolicy));

        DeterministicFakeReturnedOutcomeTool oneShot =
                new DeterministicFakeReturnedOutcomeTool(
                        returned, ModelAttemptTestFixture.TARGET_PATH,
                        mock(EvidenceRunNamespaceStore.class));
        ToolResult first = oneShot.execute(
                oneShot.request(), fixture.preparation().executionPolicy());
        assertSame(ToolResultStatus.SUCCESS, first.status());
        assertThrows(IllegalStateException.class, () -> oneShot.execute(
                oneShot.request(), fixture.preparation().executionPolicy()));
        verifyNoInteractions(gateway);
    }

    @Test
    void sanitizerPreservesEveryClosedCodeAndRemovesDiagnostics() {
        for (ToolFailureCode code : ToolFailureCode.values()) {
            ToolResult raw = new ToolResult(
                    "model-invoke",
                    ToolResultStatus.FAILURE,
                    OptionalInt.empty(),
                    Optional.of(code),
                    VerificationEvidence.capture(
                            "secret summary", "secret path prompt response", Optional.empty()));

            ToolResult sanitized = DeterministicFakeReturnedOutcomeTool.sanitize(raw);

            assertEquals(code, sanitized.failureCode().orElseThrow());
            assertEquals("tool-failure-code=" + code.name(), sanitized.evidence().outputTail());
            assertFalse(sanitized.toString().contains("secret"));
        }
    }

    private ModelAttemptTestFixture.Fixture fixture(
            String prompt, DeterministicFakeModelGateway gateway) {
        return ModelAttemptTestFixture.admitted(
                temporaryRoot,
                prompt,
                ModelAttemptTestFixture.sha256(
                        ModelAttemptTestFixture.deterministicResponse(prompt)),
                gateway);
    }

    private Returned succeeded(String prompt, ModelResponse response) throws Exception {
        DeterministicFakeModelGateway gateway = mock(DeterministicFakeModelGateway.class);
        ModelAttemptTestFixture.Fixture fixture = fixture(prompt, gateway);
        when(gateway.invoke(same(fixture.preparation().profiledRequest().request())))
                .thenReturn(response);
        DeterministicFakeExactRequestInvocationResult result =
                new DeterministicFakeExactRequestInvoker()
                        .invoke(ModelAttemptTestFixture.requireReady(fixture));
        reset(gateway);
        verifyNoInteractions(gateway);
        return new Returned(result, fixture.preparation().executionPolicy());
    }

    private ModelResponse realResponse(String prompt) throws Exception {
        return new DeterministicFakeModelGateway().invoke(new com.enhancer.model.ModelRequest(
                "response-fixture", prompt, "deterministic-fake",
                java.time.Duration.ofSeconds(1), 20_000));
    }

    private FileSystemEvidenceStore store(Path root) {
        return new FileSystemEvidenceStore(
                root, new EvidenceStoragePolicy(EvidenceStoragePolicy.MAX_SUPPORTED_CONTENT_BYTES));
    }

    private record Returned(
            DeterministicFakeExactRequestInvocationResult result,
            ExecutionPolicy policy) {}
}
