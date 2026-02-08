package com.hubz.domain.exception;

import java.util.UUID;

public class NoteTagNotFoundException extends RuntimeException {
    public NoteTagNotFoundException(UUID id) {
        super("Note tag not found with id: " + id);
    }
}
