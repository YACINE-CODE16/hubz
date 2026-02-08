package com.hubz.domain.exception;

import java.util.UUID;

public class DirectMessageNotFoundException extends RuntimeException {
    public DirectMessageNotFoundException(UUID id) {
        super("Direct message not found: " + id);
    }
}
