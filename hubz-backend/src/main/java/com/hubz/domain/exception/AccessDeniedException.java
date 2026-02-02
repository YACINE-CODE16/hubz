package com.hubz.domain.exception;

public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }

    public static AccessDeniedException notMember() {
        return new AccessDeniedException("You are not a member of this organization");
    }

    public static AccessDeniedException notAdmin() {
        return new AccessDeniedException("You must be an admin or owner to perform this action");
    }

    public static AccessDeniedException notAuthor() {
        return new AccessDeniedException("You can only modify your own content");
    }
}
