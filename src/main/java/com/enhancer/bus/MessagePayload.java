package com.enhancer.bus;

/**
 * Typed message payloads. The hierarchy is sealed to exactly four kinds so consumers exhaust
 * them by type; payloads carry bounded identities and references only, never content or
 * authority.
 */
public sealed interface MessagePayload
        permits WorkPayload, ResultPayload, ControlPayload, HandoffPayload {
}
