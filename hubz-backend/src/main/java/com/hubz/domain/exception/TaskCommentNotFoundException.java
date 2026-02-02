package com.hubz.domain.exception;

import java.util.UUID;

public class TaskCommentNotFoundException extends RuntimeException {
    public TaskCommentNotFoundException(UUID id) {
        super("Task comment not found: " + id);
    }
}
