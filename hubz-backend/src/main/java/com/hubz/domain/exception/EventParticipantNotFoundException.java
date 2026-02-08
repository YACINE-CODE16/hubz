package com.hubz.domain.exception;

import java.util.UUID;

public class EventParticipantNotFoundException extends RuntimeException {
    public EventParticipantNotFoundException(UUID id) {
        super("Event participant not found: " + id);
    }

    public EventParticipantNotFoundException(UUID eventId, UUID userId) {
        super("Participant not found for event " + eventId + " and user " + userId);
    }
}
