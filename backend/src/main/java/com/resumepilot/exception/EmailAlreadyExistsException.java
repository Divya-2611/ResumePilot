package com.resumepilot.exception;

/**
 * Thrown when an email is already registered (409).
 */
public class EmailAlreadyExistsException extends ApiException {

    public EmailAlreadyExistsException(String message) {
        super(409, message);
    }
}
