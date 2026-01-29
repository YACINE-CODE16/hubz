package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.EventRepositoryPort;
import com.hubz.domain.model.Event;
import com.hubz.infrastructure.persistence.entity.EventEntity;
import com.hubz.infrastructure.persistence.mapper.EventMapper;
import com.hubz.infrastructure.persistence.repository.EventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EventRepositoryAdapter implements EventRepositoryPort {

    private final EventJpaRepository jpaRepository;
    private final EventMapper mapper;

    @Override
    public Event save(Event event) {
        EventEntity entity = mapper.toEntity(event);
        EventEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Event> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Event> findByOrganizationId(UUID organizationId) {
        return jpaRepository.findByOrganizationIdOrderByStartTimeAsc(organizationId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Event> findPersonalEvents(UUID userId) {
        return jpaRepository.findPersonalEvents(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Event> findByOrganizationAndTimeRange(UUID organizationId, LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findByOrganizationAndTimeRange(organizationId, start, end).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Event> findPersonalEventsByTimeRange(UUID userId, LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findPersonalEventsByTimeRange(userId, start, end).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void delete(Event event) {
        jpaRepository.deleteById(event.getId());
    }
}
