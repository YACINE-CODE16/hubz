package com.hubz.domain.exception;

/**
 * Exception thrown when 2FA is required but TOTP code was not provided.
 */
public class TwoFactorRequiredException extends RuntimeException {

    public TwoFactorRequiredException() {
        super("Two-factor authentication is required");
    }

    public TwoFactorRequiredException(String message) {
        super(message);
    }
}
