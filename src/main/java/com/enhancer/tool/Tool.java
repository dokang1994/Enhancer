package com.enhancer.tool;

public interface Tool {
    String name();

    ToolResult execute(ToolRequest request, ExecutionPolicy policy) throws Exception;
}
