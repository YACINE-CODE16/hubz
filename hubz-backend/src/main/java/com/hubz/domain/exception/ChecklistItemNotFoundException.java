package com.hubz.domain.exception;

import java.util.UUID;

public class ChecklistItemNotFoundException extends RuntimeException {

    public ChecklistItemNotFoundException(UUID id) {
        super("Checklist item not found with id: " + id);
    }
}
