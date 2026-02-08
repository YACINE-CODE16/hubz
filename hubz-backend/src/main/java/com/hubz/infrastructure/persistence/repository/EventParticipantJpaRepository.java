package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.EventParticipantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventParticipantJpaRepository extends JpaRepository<EventParticipantEntity, UUID> {

    List<EventParticipantEntity> findByEventId(UUID eventId);

    List<EventParticipantEntity> findByUserId(UUID userId);

    Optional<EventParticipantEntity> findByEventIdAndUserId(UUID eventId, UUID userId);

    boolean existsByEventIdAndUserId(UUID eventId, UUID userId);

    void deleteByEventId(UUID eventId);
}
