package com.hubz.domain.exception;

import java.util.UUID;

public class MemberNotFoundException extends RuntimeException {

    public MemberNotFoundException(UUID organizationId, UUID userId) {
        super("Member with user ID " + userId + " not found in organization " + organizationId);
    }
}
