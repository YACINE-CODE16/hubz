package com.hubz.application.service;

import com.hubz.application.dto.request.CreateEventRequest;
import com.hubz.application.dto.request.UpdateEventRequest;
import com.hubz.application.dto.response.EventResponse;
import com.hubz.application.port.out.EventRepositoryPort;
import com.hubz.domain.exception.EventNotFoundException;
import com.hubz.domain.model.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepositoryPort eventRepository;
    private final AuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public List<EventResponse> getByOrganization(UUID organizationId, UUID currentUserId) {
        authorizationService.checkOrganizationAccess(organizationId, currentUserId);
        return eventRepository.findByOrganizationId(organizationId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getPersonalEvents(UUID userId) {
        return eventRepository.findPersonalEvents(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getByOrganizationAndTimeRange(
            UUID organizationId,
            LocalDateTime start,
            LocalDateTime end,
            UUID currentUserId
    ) {
        authorizationService.checkOrganizationAccess(organizationId, currentUserId);
        return eventRepository.findByOrganizationAndTimeRange(organizationId, start, end).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getPersonalEventsByTimeRange(
            UUID userId,
            LocalDateTime start,
            LocalDateTime end
    ) {
        return eventRepository.findPersonalEventsByTimeRange(userId, start, end).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public EventResponse create(CreateEventRequest request, UUID organizationId, UUID userId) {
        if (organizationId != null) {
            authorizationService.checkOrganizationAccess(organizationId, userId);
        }

        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .objective(request.getObjective())
                .organizationId(organizationId)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public EventResponse update(UUID id, UpdateEventRequest request, UUID currentUserId) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));

        // Check access: owner or org member
        if (event.getOrganizationId() != null) {
            authorizationService.checkOrganizationAccess(event.getOrganizationId(), currentUserId);
        } else if (!event.getUserId().equals(currentUserId)) {
            throw new com.hubz.domain.exception.AccessDeniedException("Not authorized to update this event");
        }

        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setObjective(request.getObjective());
        event.setUpdatedAt(LocalDateTime.now());

        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public void delete(UUID id, UUID currentUserId) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));

        // Check access: owner or org member
        if (event.getOrganizationId() != null) {
            authorizationService.checkOrganizationAccess(event.getOrganizationId(), currentUserId);
        } else if (!event.getUserId().equals(currentUserId)) {
            throw new com.hubz.domain.exception.AccessDeniedException("Not authorized to delete this event");
        }

        eventRepository.delete(event);
    }

    private EventResponse toResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .objective(event.getObjective())
                .organizationId(event.getOrganizationId())
                .userId(event.getUserId())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}
