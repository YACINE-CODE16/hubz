package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventJpaRepository extends JpaRepository<EventEntity, UUID> {

    List<EventEntity> findByOrganizationIdOrderByStartTimeAsc(UUID organizationId);

    @Query("SELECT e FROM EventEntity e WHERE e.organizationId IS NULL AND e.userId = :userId ORDER BY e.startTime ASC")
    List<EventEntity> findPersonalEvents(@Param("userId") UUID userId);

    @Query("SELECT e FROM EventEntity e WHERE e.organizationId = :orgId AND e.startTime >= :start AND e.startTime < :end ORDER BY e.startTime ASC")
    List<EventEntity> findByOrganizationAndTimeRange(
            @Param("orgId") UUID organizationId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT e FROM EventEntity e WHERE e.organizationId IS NULL AND e.userId = :userId AND e.startTime >= :start AND e.startTime < :end ORDER BY e.startTime ASC")
    List<EventEntity> findPersonalEventsByTimeRange(
            @Param("userId") UUID userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT e FROM EventEntity e WHERE (e.organizationId IN :orgIds OR (e.organizationId IS NULL AND e.userId = :userId)) AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(e.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<EventEntity> searchByTitleOrDescription(@Param("query") String query, @Param("orgIds") List<UUID> organizationIds, @Param("userId") UUID userId);

    /**
     * Find events with reminders that should be sent within the given time window.
     * The reminder time is calculated by subtracting the reminder minutes from the start time.
     */
    @Query("SELECT e FROM EventEntity e WHERE e.reminder IS NOT NULL AND e.startTime >= :start AND e.startTime < :end ORDER BY e.startTime ASC")
    List<EventEntity> findEventsWithRemindersInTimeWindow(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /**
     * Find all events for a user (personal + as organizer).
     */
    @Query("SELECT e FROM EventEntity e WHERE e.userId = :userId ORDER BY e.startTime ASC")
    List<EventEntity> findAllByUserId(@Param("userId") UUID userId);

    // ==================== Recurring Events ====================

    /**
     * Find all occurrences of a recurring event by parent event ID.
     */
    List<EventEntity> findByParentEventIdOrderByStartTimeAsc(UUID parentEventId);

    /**
     * Find an exception occurrence for a specific original date.
     */
    @Query("SELECT e FROM EventEntity e WHERE e.parentEventId = :parentId AND e.originalDate = :originalDate")
    Optional<EventEntity> findExceptionByParentAndOriginalDate(
            @Param("parentId") UUID parentEventId,
            @Param("originalDate") LocalDate originalDate
    );

    /**
     * Delete all occurrences of a recurring event.
     */
    @Modifying
    @Query("DELETE FROM EventEntity e WHERE e.parentEventId = :parentId")
    void deleteByParentEventId(@Param("parentId") UUID parentEventId);

    /**
     * Find recurring parent events for an organization (recurrenceType != NONE and parentEventId is null).
     */
    @Query("SELECT e FROM EventEntity e WHERE e.organizationId = :orgId AND e.recurrenceType IS NOT NULL AND e.recurrenceType != 'NONE' AND e.parentEventId IS NULL ORDER BY e.startTime ASC")
    List<EventEntity> findRecurringEventsByOrganizationId(@Param("orgId") UUID organizationId);

    /**
     * Find personal recurring parent events.
     */
    @Query("SELECT e FROM EventEntity e WHERE e.organizationId IS NULL AND e.userId = :userId AND e.recurrenceType IS NOT NULL AND e.recurrenceType != 'NONE' AND e.parentEventId IS NULL ORDER BY e.startTime ASC")
    List<EventEntity> findPersonalRecurringEvents(@Param("userId") UUID userId);
}
