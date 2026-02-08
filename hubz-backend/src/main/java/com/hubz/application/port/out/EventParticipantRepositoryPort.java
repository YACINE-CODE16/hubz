package com.hubz.application.port.out;

import com.hubz.domain.model.EventParticipant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventParticipantRepositoryPort {
    EventParticipant save(EventParticipant participant);

    Optional<EventParticipant> findById(UUID id);

    Optional<EventParticipant> findByEventIdAndUserId(UUID eventId, UUID userId);

    List<EventParticipant> findByEventId(UUID eventId);

    List<EventParticipant> findByUserId(UUID userId);

    void delete(EventParticipant participant);

    void deleteByEventId(UUID eventId);

    boolean existsByEventIdAndUserId(UUID eventId, UUID userId);
}
