package com.enhancer.loop;

import com.enhancer.tool.ToolRequest;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.ToolFailureCode;
import com.enhancer.tool.ToolResultStatus;

@FunctionalInterface
public interface ToolFailureClassifier {
    ToolFailureDisposition classify(ToolRequest request, ToolResult result);

    static ToolFailureClassifier terminalByDefault() {
        return (request, result) -> ToolFailureDisposition.TERMINAL;
    }

    static ToolFailureClassifier standard() {
        return (request, result) -> {
            if (result.status() != ToolResultStatus.FAILURE) {
                throw new IllegalArgumentException("only failed Tool results can be classified");
            }
            ToolFailureCode code = result.failureCode().orElseThrow(
                    () -> new IllegalArgumentException("failed Tool result has no failure code"));
            return code == ToolFailureCode.TIMED_OUT
                            || code == ToolFailureCode.TEMPORARY_FAILURE
                    ? ToolFailureDisposition.RETRYABLE
                    : ToolFailureDisposition.TERMINAL;
        };
    }
}
