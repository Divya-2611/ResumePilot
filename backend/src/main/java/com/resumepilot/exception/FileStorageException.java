package com.resumepilot.exception;

/**
 * Thrown for file storage / parsing failures (e.g. corrupt upload).
 */
public class FileStorageException extends ApiException {

    public FileStorageException(String message) {
        super(500, message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(500, message + ": " + cause.getMessage());
    }
}
