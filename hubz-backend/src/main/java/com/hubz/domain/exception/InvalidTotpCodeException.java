package com.hubz.domain.exception;

/**
 * Exception thrown when an invalid TOTP code is provided.
 */
public class InvalidTotpCodeException extends RuntimeException {

    public InvalidTotpCodeException() {
        super("Invalid TOTP code");
    }

    public InvalidTotpCodeException(String message) {
        super(message);
    }
}
