package com.hubz.domain.exception;

import java.util.UUID;

public class TaskAttachmentNotFoundException extends RuntimeException {

    public TaskAttachmentNotFoundException(UUID id) {
        super("Task attachment not found with id: " + id);
    }
}
