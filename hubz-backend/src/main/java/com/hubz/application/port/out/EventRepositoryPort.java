package com.hubz.application.port.out;

import com.hubz.domain.model.Event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepositoryPort {
    Event save(Event event);
    Optional<Event> findById(UUID id);
    List<Event> findByOrganizationId(UUID organizationId);
    List<Event> findPersonalEvents(UUID userId);
    List<Event> findByOrganizationAndTimeRange(UUID organizationId, LocalDateTime start, LocalDateTime end);
    List<Event> findPersonalEventsByTimeRange(UUID userId, LocalDateTime start, LocalDateTime end);
    void delete(Event event);
}
