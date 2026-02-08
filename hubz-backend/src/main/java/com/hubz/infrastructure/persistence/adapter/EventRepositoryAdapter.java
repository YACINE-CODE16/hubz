package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.EventRepositoryPort;
import com.hubz.domain.model.Event;
import com.hubz.infrastructure.persistence.entity.EventEntity;
import com.hubz.infrastructure.persistence.mapper.EventMapper;
import com.hubz.infrastructure.persistence.repository.EventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    public List<Event> saveAll(List<Event> events) {
        List<EventEntity> entities = events.stream()
                .map(mapper::toEntity)
                .toList();
        List<EventEntity> saved = jpaRepository.saveAll(entities);
        return saved.stream()
                .map(mapper::toDomain)
                .toList();
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

    @Override
    public void deleteAll(List<Event> events) {
        List<UUID> ids = events.stream().map(Event::getId).toList();
        jpaRepository.deleteAllById(ids);
    }

    @Override
    public List<Event> searchByTitleOrDescription(String query, List<UUID> organizationIds, UUID userId) {
        return jpaRepository.searchByTitleOrDescription(query, organizationIds, userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Event> findEventsWithRemindersInTimeWindow(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findEventsWithRemindersInTimeWindow(start, end).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Event> findAllByUserId(UUID userId) {
        return jpaRepository.findAllByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    // ==================== Recurring Events ====================

    @Override
    public List<Event> findByParentEventId(UUID parentEventId) {
        return jpaRepository.findByParentEventIdOrderByStartTimeAsc(parentEventId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Event> findExceptionByParentAndOriginalDate(UUID parentEventId, LocalDate originalDate) {
        return jpaRepository.findExceptionByParentAndOriginalDate(parentEventId, originalDate)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void deleteByParentEventId(UUID parentEventId) {
        jpaRepository.deleteByParentEventId(parentEventId);
    }

    @Override
    public List<Event> findRecurringEventsByOrganizationId(UUID organizationId) {
        return jpaRepository.findRecurringEventsByOrganizationId(organizationId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Event> findPersonalRecurringEvents(UUID userId) {
        return jpaRepository.findPersonalRecurringEvents(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
