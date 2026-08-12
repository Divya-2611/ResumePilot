package com.resumepilot.exception;

/**
 * Thrown on authentication failures (401).
 */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(401, message);
    }
}
