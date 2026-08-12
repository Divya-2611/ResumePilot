package com.resumepilot.exception;

/**
 * Thrown when the configured AI provider cannot be reached or returns garbage.
 * Callers should fall back to the local heuristic engine.
 */
public class AiServiceException extends ApiException {

    public AiServiceException(String message) {
        super(502, message);
    }
}
