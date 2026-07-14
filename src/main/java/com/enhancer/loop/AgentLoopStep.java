package com.enhancer.loop;

@FunctionalInterface
public interface AgentLoopStep {
    AgentLoopState execute(AgentLoopState currentState);
}
