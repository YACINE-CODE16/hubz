package com.hubz.domain.exception;

public class CannotChangeOwnerRoleException extends RuntimeException {

    public CannotChangeOwnerRoleException() {
        super("Cannot change the role of the organization owner. Transfer ownership first.");
    }
}
