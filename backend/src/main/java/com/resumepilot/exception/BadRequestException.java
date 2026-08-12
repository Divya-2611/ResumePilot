package com.resumepilot.exception;

/**
 * Thrown on invalid input / business rule violations (400).
 */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(400, message);
    }
}
