package com.enhancer.bus;

/**
 * Typed message payloads. The hierarchy is sealed to exactly five kinds so consumers exhaust
 * them by type; payloads carry bounded identities and references only, never content or
 * authority.
 */
public sealed interface MessagePayload
        permits WorkPayload, ModelWorkPayload, ResultPayload, ControlPayload, HandoffPayload {
}
