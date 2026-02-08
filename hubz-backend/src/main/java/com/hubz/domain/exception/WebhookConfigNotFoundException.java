package com.hubz.domain.exception;

import java.util.UUID;

public class WebhookConfigNotFoundException extends RuntimeException {

    public WebhookConfigNotFoundException(UUID id) {
        super("Webhook config not found with id: " + id);
    }
}
