package com.hubz.domain.exception;

public class TeamMemberAlreadyExistsException extends RuntimeException {
    public TeamMemberAlreadyExistsException(String message) {
        super(message);
    }
}
