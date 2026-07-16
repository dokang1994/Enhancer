package com.enhancer.bus;

/**
 * Provider-neutral boundary for carrying one routed envelope toward another process or transport
 * peer. A configured implementation owns its endpoint and protocol details; those concerns do
 * not enter this contract.
 *
 * <p>The returned outcome describes only whether this transport accepted responsibility for the
 * attempted hop. It is never evidence that a receiving Message Bus admitted, journaled,
 * dispatched, or delivered the envelope.
 */
@FunctionalInterface
public interface MessageTransport {
    TransportOutcome send(TransportMessage message);
}
