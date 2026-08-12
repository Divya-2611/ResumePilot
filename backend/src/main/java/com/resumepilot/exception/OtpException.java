package com.resumepilot.exception;

/**
 * Thrown for OTP-related failures: invalid, expired or max attempts reached.
 */
public class OtpException extends ApiException {

    public OtpException(String message) {
        super(400, message);
    }
}
