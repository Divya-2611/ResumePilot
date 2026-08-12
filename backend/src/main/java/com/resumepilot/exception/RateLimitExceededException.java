package com.resumepilot.exception;

/**
 * Thrown when a client exceeds an allowed rate (e.g. too many OTP requests).
 */
public class RateLimitExceededException extends ApiException {

    public RateLimitExceededException(String message) {
        super(429, message);
    }
}
