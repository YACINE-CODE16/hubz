package com.hubz.domain.exception;

import java.util.UUID;

public class BackgroundJobNotFoundException extends RuntimeException {
    public BackgroundJobNotFoundException(UUID id) {
        super("Background job not found with id: " + id);
    }
}
