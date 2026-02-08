package com.hubz.domain.exception;

public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException() {
        super("Le token est invalide ou a expiré");
    }

    public InvalidTokenException(String message) {
        super(message);
    }
}
