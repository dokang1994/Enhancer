package com.enhancer.bus;

/**
 * Receives a delivered envelope and performs a side effect. The bus passes the whole envelope
 * unchanged, so a handler must validate authority against repository state rather than trust the
 * envelope's contents.
 */
@FunctionalInterface
public interface MessageHandler {
    void handle(MessageEnvelope envelope);
}
