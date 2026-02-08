package com.hubz.domain.exception;

/**
 * Exception thrown when 2FA operations fail.
 */
public class TwoFactorAuthException extends RuntimeException {

    public TwoFactorAuthException(String message) {
        super(message);
    }

    public TwoFactorAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
