package com.hubz.domain.exception;

import java.util.UUID;

public class NoteVersionNotFoundException extends RuntimeException {
    public NoteVersionNotFoundException(UUID id) {
        super("Note version not found: " + id);
    }
}
