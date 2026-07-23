package com.enhancer.runtime;

/** Checked external-effect adapter failure that leaves durable intent prepared. */
public final class ExternalEffectAdapterException extends Exception {
    private static final long serialVersionUID = 1L;

    public ExternalEffectAdapterException(String message) {
        super(message);
    }

    public ExternalEffectAdapterException(String message, Throwable cause) {
        super(message, cause);
    }
}
