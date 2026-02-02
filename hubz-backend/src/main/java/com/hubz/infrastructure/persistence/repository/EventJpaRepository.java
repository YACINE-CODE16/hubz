package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
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
}
