package com.hubz.application.port.out;

import com.hubz.domain.model.Event;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepositoryPort {
    Event save(Event event);
    List<Event> saveAll(List<Event> events);
    Optional<Event> findById(UUID id);
    List<Event> findByOrganizationId(UUID organizationId);
    List<Event> findPersonalEvents(UUID userId);
    List<Event> findByOrganizationAndTimeRange(UUID organizationId, LocalDateTime start, LocalDateTime end);
    List<Event> findPersonalEventsByTimeRange(UUID userId, LocalDateTime start, LocalDateTime end);
    void delete(Event event);
    void deleteAll(List<Event> events);

    List<Event> searchByTitleOrDescription(String query, List<UUID> organizationIds, UUID userId);

    /**
     * Find events with reminders that should be sent within the given time window.
     * This is used by a scheduled job to send event reminders.
     */
    List<Event> findEventsWithRemindersInTimeWindow(LocalDateTime start, LocalDateTime end);

    /**
     * Find all events for a user (personal + organization events).
     */
    List<Event> findAllByUserId(UUID userId);

    /**
     * Find all occurrences of a recurring event by parent event ID.
     */
    List<Event> findByParentEventId(UUID parentEventId);

    /**
     * Find occurrence exceptions for a recurring event on a specific date.
     */
    Optional<Event> findExceptionByParentAndOriginalDate(UUID parentEventId, LocalDate originalDate);

    /**
     * Delete all occurrences of a recurring event.
     */
    void deleteByParentEventId(UUID parentEventId);

    /**
     * Find all recurring parent events by organization.
     */
    List<Event> findRecurringEventsByOrganizationId(UUID organizationId);

    /**
     * Find all personal recurring parent events.
     */
    List<Event> findPersonalRecurringEvents(UUID userId);
}
