package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.EventParticipantRepositoryPort;
import com.hubz.domain.model.EventParticipant;
import com.hubz.infrastructure.persistence.entity.EventParticipantEntity;
import com.hubz.infrastructure.persistence.mapper.EventParticipantMapper;
import com.hubz.infrastructure.persistence.repository.EventParticipantJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EventParticipantRepositoryAdapter implements EventParticipantRepositoryPort {

    private final EventParticipantJpaRepository jpaRepository;
    private final EventParticipantMapper mapper;

    @Override
    public EventParticipant save(EventParticipant participant) {
        EventParticipantEntity entity = mapper.toEntity(participant);
        EventParticipantEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<EventParticipant> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<EventParticipant> findByEventIdAndUserId(UUID eventId, UUID userId) {
        return jpaRepository.findByEventIdAndUserId(eventId, userId).map(mapper::toDomain);
    }

    @Override
    public List<EventParticipant> findByEventId(UUID eventId) {
        return jpaRepository.findByEventId(eventId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<EventParticipant> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void delete(EventParticipant participant) {
        jpaRepository.deleteById(participant.getId());
    }

    @Override
    @Transactional
    public void deleteByEventId(UUID eventId) {
        jpaRepository.deleteByEventId(eventId);
    }

    @Override
    public boolean existsByEventIdAndUserId(UUID eventId, UUID userId) {
        return jpaRepository.existsByEventIdAndUserId(eventId, userId);
    }
}
