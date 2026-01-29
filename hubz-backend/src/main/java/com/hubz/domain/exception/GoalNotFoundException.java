package com.hubz.domain.exception;

import java.util.UUID;

public class GoalNotFoundException extends RuntimeException {
    public GoalNotFoundException(UUID id) {
        super("Goal not found with id: " + id);
    }
}
