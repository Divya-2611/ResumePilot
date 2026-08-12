package com.resumepilot.exception;

/**
 * Base class for all application-level exceptions.
 * The HTTP status drives the global exception handler response.
 */
public abstract class ApiException extends RuntimeException {

    private final int status;

    protected ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
