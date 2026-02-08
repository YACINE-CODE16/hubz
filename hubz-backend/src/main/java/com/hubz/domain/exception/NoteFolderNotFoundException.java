package com.hubz.domain.exception;

import java.util.UUID;

public class NoteFolderNotFoundException extends RuntimeException {
    public NoteFolderNotFoundException(UUID id) {
        super("Note folder not found with id: " + id);
    }
}
