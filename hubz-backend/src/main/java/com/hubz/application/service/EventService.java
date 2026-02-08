package com.hubz.application.service;

import com.hubz.application.dto.request.CreateEventRequest;
import com.hubz.application.dto.request.InviteEventParticipantRequest;
import com.hubz.application.dto.request.RespondEventInvitationRequest;
import com.hubz.application.dto.request.UpdateEventRequest;
import com.hubz.application.dto.response.EventParticipantResponse;
import com.hubz.application.dto.response.EventResponse;
import com.hubz.application.port.out.EventParticipantRepositoryPort;
import com.hubz.application.port.out.EventRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.enums.NotificationType;
import com.hubz.domain.enums.ParticipantStatus;
import com.hubz.domain.enums.RecurrenceType;
import com.hubz.domain.exception.AccessDeniedException;
import com.hubz.domain.exception.EventNotFoundException;
import com.hubz.domain.exception.EventParticipantNotFoundException;
import com.hubz.domain.exception.UserNotFoundException;
import com.hubz.domain.model.Event;
import com.hubz.domain.model.EventParticipant;
import com.hubz.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepositoryPort eventRepository;
    private final EventParticipantRepositoryPort participantRepository;
    private final UserRepositoryPort userRepository;
    private final AuthorizationService authorizationService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<EventResponse> getByOrganization(UUID organizationId, UUID currentUserId) {
        authorizationService.checkOrganizationAccess(organizationId, currentUserId);
        return eventRepository.findByOrganizationId(organizationId).stream()
                .map(this::toResponseWithParticipants)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getPersonalEvents(UUID userId) {
        return eventRepository.findPersonalEvents(userId).stream()
                .map(this::toResponseWithParticipants)
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
                .map(this::toResponseWithParticipants)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getPersonalEventsByTimeRange(
            UUID userId,
            LocalDateTime start,
            LocalDateTime end
    ) {
        return eventRepository.findPersonalEventsByTimeRange(userId, start, end).stream()
                .map(this::toResponseWithParticipants)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventResponse getById(UUID id, UUID currentUserId) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));

        // Check access
        checkEventAccess(event, currentUserId);

        return toResponseWithParticipants(event);
    }

    @Transactional
    public EventResponse create(CreateEventRequest request, UUID organizationId, UUID userId) {
        if (organizationId != null) {
            authorizationService.checkOrganizationAccess(organizationId, userId);
        }

        // Determine recurrence type (default to NONE)
        RecurrenceType recurrenceType = request.getRecurrenceType() != null
                ? request.getRecurrenceType()
                : RecurrenceType.NONE;

        // Default recurrence interval to 1 if recurrence is set
        Integer recurrenceInterval = request.getRecurrenceInterval();
        if (recurrenceType != RecurrenceType.NONE && (recurrenceInterval == null || recurrenceInterval < 1)) {
            recurrenceInterval = 1;
        }

        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .objective(request.getObjective())
                .location(request.getLocation())
                .reminder(request.getReminder())
                .organizationId(organizationId)
                .userId(userId)
                .recurrenceType(recurrenceType)
                .recurrenceInterval(recurrenceInterval)
                .recurrenceEndDate(request.getRecurrenceEndDate())
                .parentEventId(null) // This is a parent event
                .originalDate(null)
                .isRecurrenceException(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Event savedEvent = eventRepository.save(event);

        // Invite participants if provided
        if (request.getParticipantIds() != null && !request.getParticipantIds().isEmpty()) {
            for (UUID participantUserId : request.getParticipantIds()) {
                if (!participantUserId.equals(userId)) { // Don't invite the creator
                    inviteParticipantInternal(savedEvent, participantUserId);
                }
            }
        }

        return toResponseWithParticipants(savedEvent);
    }

    @Transactional
    public EventResponse update(UUID id, UpdateEventRequest request, UUID currentUserId) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));

        // Check access: owner or org member
        checkEventModifyAccess(event, currentUserId);

        // Check if this is part of a recurring series and how to update
        boolean isOccurrence = event.isOccurrence();
        boolean isRecurringParent = event.isRecurring();
        boolean updateAllOccurrences = Boolean.TRUE.equals(request.getUpdateAllOccurrences());

        if (isOccurrence && !updateAllOccurrences) {
            // Update only this single occurrence - mark it as an exception
            return updateSingleOccurrence(event, request, currentUserId);
        } else if (isOccurrence && updateAllOccurrences) {
            // Update the parent and regenerate all occurrences
            Event parentEvent = eventRepository.findById(event.getParentEventId())
                    .orElseThrow(() -> new EventNotFoundException(event.getParentEventId()));
            return updateRecurringEventSeries(parentEvent, request, currentUserId);
        } else if (isRecurringParent && updateAllOccurrences) {
            // Update the parent recurring event and all occurrences
            return updateRecurringEventSeries(event, request, currentUserId);
        } else {
            // Non-recurring event or only update the parent
            return updateSingleEvent(event, request, currentUserId);
        }
    }

    private EventResponse updateSingleEvent(Event event, UpdateEventRequest request, UUID currentUserId) {
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setObjective(request.getObjective());
        event.setLocation(request.getLocation());
        event.setReminder(request.getReminder());

        // Update recurrence settings if provided and this is a parent event
        if (event.isRecurring() || event.getRecurrenceType() == RecurrenceType.NONE) {
            if (request.getRecurrenceType() != null) {
                event.setRecurrenceType(request.getRecurrenceType());
            }
            if (request.getRecurrenceInterval() != null) {
                event.setRecurrenceInterval(request.getRecurrenceInterval());
            }
            if (request.getRecurrenceEndDate() != null) {
                event.setRecurrenceEndDate(request.getRecurrenceEndDate());
            }
        }

        event.setUpdatedAt(LocalDateTime.now());

        Event savedEvent = eventRepository.save(event);

        // Notify participants about the update
        notifyParticipantsAboutUpdate(event);

        return toResponseWithParticipants(savedEvent);
    }

    private EventResponse updateSingleOccurrence(Event occurrence, UpdateEventRequest request, UUID currentUserId) {
        // Mark this occurrence as an exception (modified from parent)
        occurrence.setTitle(request.getTitle());
        occurrence.setDescription(request.getDescription());
        occurrence.setStartTime(request.getStartTime());
        occurrence.setEndTime(request.getEndTime());
        occurrence.setObjective(request.getObjective());
        occurrence.setLocation(request.getLocation());
        occurrence.setReminder(request.getReminder());
        occurrence.setIsRecurrenceException(true);
        occurrence.setUpdatedAt(LocalDateTime.now());

        Event savedEvent = eventRepository.save(occurrence);

        // Notify participants about the update
        notifyParticipantsAboutUpdate(occurrence);

        return toResponseWithParticipants(savedEvent);
    }

    private EventResponse updateRecurringEventSeries(Event parentEvent, UpdateEventRequest request, UUID currentUserId) {
        // Update the parent event
        parentEvent.setTitle(request.getTitle());
        parentEvent.setDescription(request.getDescription());
        parentEvent.setStartTime(request.getStartTime());
        parentEvent.setEndTime(request.getEndTime());
        parentEvent.setObjective(request.getObjective());
        parentEvent.setLocation(request.getLocation());
        parentEvent.setReminder(request.getReminder());

        // Update recurrence settings
        if (request.getRecurrenceType() != null) {
            parentEvent.setRecurrenceType(request.getRecurrenceType());
        }
        if (request.getRecurrenceInterval() != null) {
            parentEvent.setRecurrenceInterval(request.getRecurrenceInterval());
        }
        parentEvent.setRecurrenceEndDate(request.getRecurrenceEndDate());
        parentEvent.setUpdatedAt(LocalDateTime.now());

        Event savedParent = eventRepository.save(parentEvent);

        // Notify participants about the update
        notifyParticipantsAboutUpdate(parentEvent);

        return toResponseWithParticipants(savedParent);
    }

    private void notifyParticipantsAboutUpdate(Event event) {
        List<EventParticipant> participants = participantRepository.findByEventId(event.getId());
        for (EventParticipant participant : participants) {
            if (participant.getStatus() == ParticipantStatus.ACCEPTED) {
                notificationService.createNotification(
                        participant.getUserId(),
                        NotificationType.EVENT_UPDATED,
                        "Evenement modifie",
                        "L'evenement \"" + event.getTitle() + "\" a ete modifie.",
                        getEventLink(event),
                        event.getId(),
                        event.getOrganizationId()
                );
            }
        }
    }

    @Transactional
    public void delete(UUID id, UUID currentUserId) {
        delete(id, currentUserId, false);
    }

    @Transactional
    public void delete(UUID id, UUID currentUserId, boolean deleteAllOccurrences) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));

        // Check access: owner or org admin
        checkEventModifyAccess(event, currentUserId);

        // Handle recurring event deletion
        if (event.isRecurring() && deleteAllOccurrences) {
            // Delete the parent recurring event and all its occurrences
            deleteRecurringEventSeries(event);
        } else if (event.isOccurrence() && deleteAllOccurrences) {
            // Delete the entire series via the parent
            Event parentEvent = eventRepository.findById(event.getParentEventId())
                    .orElseThrow(() -> new EventNotFoundException(event.getParentEventId()));
            deleteRecurringEventSeries(parentEvent);
        } else {
            // Delete single event (or single occurrence)
            deleteSingleEvent(event);
        }
    }

    private void deleteSingleEvent(Event event) {
        // Notify participants about cancellation
        List<EventParticipant> participants = participantRepository.findByEventId(event.getId());
        for (EventParticipant participant : participants) {
            notificationService.createNotification(
                    participant.getUserId(),
                    NotificationType.EVENT_CANCELLED,
                    "Evenement annule",
                    "L'evenement \"" + event.getTitle() + "\" a ete annule.",
                    getEventLink(event),
                    event.getId(),
                    event.getOrganizationId()
            );
        }

        // Delete participants first
        participantRepository.deleteByEventId(event.getId());

        // Delete the event
        eventRepository.delete(event);
    }

    private void deleteRecurringEventSeries(Event parentEvent) {
        // Notify participants about cancellation of the parent
        List<EventParticipant> participants = participantRepository.findByEventId(parentEvent.getId());
        for (EventParticipant participant : participants) {
            notificationService.createNotification(
                    participant.getUserId(),
                    NotificationType.EVENT_CANCELLED,
                    "Serie d'evenements annulee",
                    "La serie d'evenements \"" + parentEvent.getTitle() + "\" a ete annulee.",
                    getEventLink(parentEvent),
                    parentEvent.getId(),
                    parentEvent.getOrganizationId()
            );
        }

        // Delete participants of the parent
        participantRepository.deleteByEventId(parentEvent.getId());

        // Delete the parent event (occurrences are virtual, not stored separately)
        eventRepository.delete(parentEvent);
    }

    // Participant management methods

    @Transactional
    public EventParticipantResponse inviteParticipant(UUID eventId, InviteEventParticipantRequest request, UUID currentUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // Check access: only event owner or org admin can invite
        checkEventModifyAccess(event, currentUserId);

        // Check if user exists
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new UserNotFoundException(request.getUserId()));

        // Check if already invited
        if (participantRepository.existsByEventIdAndUserId(eventId, request.getUserId())) {
            throw new IllegalStateException("User is already invited to this event");
        }

        EventParticipant participant = inviteParticipantInternal(event, request.getUserId());
        return toParticipantResponse(participant, user);
    }

    @Transactional
    public EventParticipantResponse respondToInvitation(UUID eventId, RespondEventInvitationRequest request, UUID currentUserId) {
        // Verify event exists
        if (!eventRepository.findById(eventId).isPresent()) {
            throw new EventNotFoundException(eventId);
        }

        EventParticipant participant = participantRepository.findByEventIdAndUserId(eventId, currentUserId)
                .orElseThrow(() -> new EventParticipantNotFoundException(eventId, currentUserId));

        if (participant.getStatus() != ParticipantStatus.INVITED) {
            throw new IllegalStateException("Invitation already responded to");
        }

        if (request.getStatus() != ParticipantStatus.ACCEPTED && request.getStatus() != ParticipantStatus.DECLINED) {
            throw new IllegalArgumentException("Status must be ACCEPTED or DECLINED");
        }

        participant.setStatus(request.getStatus());
        participant.setRespondedAt(LocalDateTime.now());

        EventParticipant savedParticipant = participantRepository.save(participant);

        // Get user for response
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException(currentUserId));

        return toParticipantResponse(savedParticipant, user);
    }

    @Transactional
    public void removeParticipant(UUID eventId, UUID userId, UUID currentUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // Check access: event owner, org admin, or the participant themselves
        boolean isEventOwner = event.getUserId().equals(currentUserId);
        boolean isOrgAdmin = event.getOrganizationId() != null &&
                authorizationService.isOrganizationAdmin(event.getOrganizationId(), currentUserId);
        boolean isSelf = userId.equals(currentUserId);

        if (!isEventOwner && !isOrgAdmin && !isSelf) {
            throw new AccessDeniedException("Not authorized to remove this participant");
        }

        EventParticipant participant = participantRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new EventParticipantNotFoundException(eventId, userId));

        participantRepository.delete(participant);
    }

    @Transactional(readOnly = true)
    public List<EventParticipantResponse> getParticipants(UUID eventId, UUID currentUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // Check access
        checkEventAccess(event, currentUserId);

        return participantRepository.findByEventId(eventId).stream()
                .map(p -> {
                    User user = userRepository.findById(p.getUserId())
                            .orElse(null);
                    return toParticipantResponse(p, user);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getEventsWhereParticipant(UUID userId) {
        List<EventParticipant> participations = participantRepository.findByUserId(userId);
        List<EventResponse> events = new ArrayList<>();

        for (EventParticipant participation : participations) {
            eventRepository.findById(participation.getEventId())
                    .ifPresent(event -> events.add(toResponseWithParticipants(event)));
        }

        return events;
    }

    // ==================== Recurring Event Methods ====================

    /**
     * Get all occurrences of a recurring event within a time range.
     * This generates virtual occurrences based on the recurrence rule.
     */
    @Transactional(readOnly = true)
    public List<EventResponse> getRecurringEventOccurrences(UUID eventId, LocalDateTime start, LocalDateTime end, UUID currentUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        checkEventAccess(event, currentUserId);

        if (!event.isRecurring()) {
            // Not a recurring event, return just this event if it's in range
            if (!event.getStartTime().isBefore(start) && event.getStartTime().isBefore(end)) {
                return List.of(toResponseWithParticipants(event));
            }
            return List.of();
        }

        return generateOccurrences(event, start, end);
    }

    /**
     * Generate virtual occurrences for a recurring event within a time range.
     */
    private List<EventResponse> generateOccurrences(Event parentEvent, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        List<EventResponse> occurrences = new ArrayList<>();
        RecurrenceType recurrenceType = parentEvent.getRecurrenceType();
        int interval = parentEvent.getRecurrenceInterval() != null ? parentEvent.getRecurrenceInterval() : 1;
        LocalDate recurrenceEndDate = parentEvent.getRecurrenceEndDate();

        // Start from the event's original start time
        LocalDateTime currentStart = parentEvent.getStartTime();
        LocalDateTime currentEnd = parentEvent.getEndTime();
        long durationMinutes = ChronoUnit.MINUTES.between(parentEvent.getStartTime(), parentEvent.getEndTime());

        // Generate occurrences until we exceed the range or recurrence end date
        int maxOccurrences = 365; // Safety limit
        int count = 0;

        while (count < maxOccurrences) {
            // Check if we've passed the recurrence end date
            if (recurrenceEndDate != null && currentStart.toLocalDate().isAfter(recurrenceEndDate)) {
                break;
            }

            // Check if we've passed the query range end
            if (currentStart.isAfter(rangeEnd) || currentStart.isEqual(rangeEnd)) {
                break;
            }

            // If this occurrence is within the query range, add it
            if (!currentStart.isBefore(rangeStart) && currentStart.isBefore(rangeEnd)) {
                // Check if there's an exception for this date
                LocalDate occurrenceDate = currentStart.toLocalDate();
                Event occurrence = createVirtualOccurrence(parentEvent, currentStart, currentEnd, occurrenceDate);
                occurrences.add(toResponseWithParticipants(occurrence));
            }

            // Calculate next occurrence
            currentStart = calculateNextOccurrence(currentStart, recurrenceType, interval);
            currentEnd = currentStart.plusMinutes(durationMinutes);
            count++;
        }

        return occurrences;
    }

    /**
     * Calculate the next occurrence date based on recurrence type and interval.
     */
    private LocalDateTime calculateNextOccurrence(LocalDateTime current, RecurrenceType recurrenceType, int interval) {
        return switch (recurrenceType) {
            case DAILY -> current.plusDays(interval);
            case WEEKLY -> current.plusWeeks(interval);
            case MONTHLY -> current.plusMonths(interval);
            case YEARLY -> current.plusYears(interval);
            default -> current; // NONE - shouldn't happen
        };
    }

    /**
     * Create a virtual occurrence object (not persisted) for display purposes.
     */
    private Event createVirtualOccurrence(Event parent, LocalDateTime start, LocalDateTime end, LocalDate originalDate) {
        return Event.builder()
                .id(parent.getId()) // Use parent ID for virtual occurrences
                .title(parent.getTitle())
                .description(parent.getDescription())
                .startTime(start)
                .endTime(end)
                .objective(parent.getObjective())
                .location(parent.getLocation())
                .reminder(parent.getReminder())
                .organizationId(parent.getOrganizationId())
                .userId(parent.getUserId())
                .recurrenceType(parent.getRecurrenceType())
                .recurrenceInterval(parent.getRecurrenceInterval())
                .recurrenceEndDate(parent.getRecurrenceEndDate())
                .parentEventId(parent.getId())
                .originalDate(originalDate)
                .isRecurrenceException(false)
                .createdAt(parent.getCreatedAt())
                .updatedAt(parent.getUpdatedAt())
                .build();
    }

    /**
     * Get events by organization with recurring event expansion.
     * This expands recurring events into their occurrences within the given time range.
     */
    @Transactional(readOnly = true)
    public List<EventResponse> getByOrganizationWithRecurrence(
            UUID organizationId,
            LocalDateTime start,
            LocalDateTime end,
            UUID currentUserId
    ) {
        authorizationService.checkOrganizationAccess(organizationId, currentUserId);

        List<EventResponse> allEvents = new ArrayList<>();

        // Get non-recurring events in range
        List<Event> regularEvents = eventRepository.findByOrganizationAndTimeRange(organizationId, start, end)
                .stream()
                .filter(e -> e.getRecurrenceType() == null || e.getRecurrenceType() == RecurrenceType.NONE)
                .toList();
        allEvents.addAll(regularEvents.stream().map(this::toResponseWithParticipants).toList());

        // Get recurring events and expand them
        List<Event> recurringEvents = eventRepository.findRecurringEventsByOrganizationId(organizationId);
        for (Event recurringEvent : recurringEvents) {
            allEvents.addAll(generateOccurrences(recurringEvent, start, end));
        }

        // Sort by start time
        allEvents.sort((a, b) -> a.getStartTime().compareTo(b.getStartTime()));

        return allEvents;
    }

    /**
     * Get personal events with recurring event expansion.
     */
    @Transactional(readOnly = true)
    public List<EventResponse> getPersonalEventsWithRecurrence(
            UUID userId,
            LocalDateTime start,
            LocalDateTime end
    ) {
        List<EventResponse> allEvents = new ArrayList<>();

        // Get non-recurring events in range
        List<Event> regularEvents = eventRepository.findPersonalEventsByTimeRange(userId, start, end)
                .stream()
                .filter(e -> e.getRecurrenceType() == null || e.getRecurrenceType() == RecurrenceType.NONE)
                .toList();
        allEvents.addAll(regularEvents.stream().map(this::toResponseWithParticipants).toList());

        // Get recurring events and expand them
        List<Event> recurringEvents = eventRepository.findPersonalRecurringEvents(userId);
        for (Event recurringEvent : recurringEvents) {
            allEvents.addAll(generateOccurrences(recurringEvent, start, end));
        }

        // Sort by start time
        allEvents.sort((a, b) -> a.getStartTime().compareTo(b.getStartTime()));

        return allEvents;
    }

    // ==================== Helper methods ====================

    private EventParticipant inviteParticipantInternal(Event event, UUID userId) {
        EventParticipant participant = EventParticipant.builder()
                .id(UUID.randomUUID())
                .eventId(event.getId())
                .userId(userId)
                .status(ParticipantStatus.INVITED)
                .invitedAt(LocalDateTime.now())
                .build();

        EventParticipant savedParticipant = participantRepository.save(participant);

        // Send notification to the invited user
        notificationService.createNotification(
                userId,
                NotificationType.EVENT_INVITATION,
                "Invitation a un evenement",
                "Vous avez ete invite a l'evenement \"" + event.getTitle() + "\".",
                getEventLink(event),
                event.getId(),
                event.getOrganizationId()
        );

        return savedParticipant;
    }

    private void checkEventAccess(Event event, UUID userId) {
        if (event.getOrganizationId() != null) {
            authorizationService.checkOrganizationAccess(event.getOrganizationId(), userId);
        } else {
            // For personal events, user must be owner or participant
            boolean isOwner = event.getUserId().equals(userId);
            boolean isParticipant = participantRepository.existsByEventIdAndUserId(event.getId(), userId);
            if (!isOwner && !isParticipant) {
                throw new AccessDeniedException("Not authorized to access this event");
            }
        }
    }

    private void checkEventModifyAccess(Event event, UUID userId) {
        if (event.getOrganizationId() != null) {
            authorizationService.checkOrganizationAccess(event.getOrganizationId(), userId);
        } else if (!event.getUserId().equals(userId)) {
            throw new AccessDeniedException("Not authorized to modify this event");
        }
    }

    private String getEventLink(Event event) {
        return event.getOrganizationId() != null
                ? "/org/" + event.getOrganizationId() + "/calendar"
                : "/personal/calendar";
    }

    private EventResponse toResponseWithParticipants(Event event) {
        // For virtual occurrences, get participants from the parent event
        UUID participantEventId = event.getParentEventId() != null ? event.getParentEventId() : event.getId();

        List<EventParticipant> participants = participantRepository.findByEventId(participantEventId);
        List<EventParticipantResponse> participantResponses = participants.stream()
                .map(p -> {
                    User user = userRepository.findById(p.getUserId()).orElse(null);
                    return toParticipantResponse(p, user);
                })
                .toList();

        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .objective(event.getObjective())
                .location(event.getLocation())
                .reminder(event.getReminder())
                .organizationId(event.getOrganizationId())
                .userId(event.getUserId())
                .participants(participantResponses)
                .recurrenceType(event.getRecurrenceType())
                .recurrenceInterval(event.getRecurrenceInterval())
                .recurrenceEndDate(event.getRecurrenceEndDate())
                .parentEventId(event.getParentEventId())
                .originalDate(event.getOriginalDate())
                .isRecurrenceException(event.getIsRecurrenceException())
                .isRecurring(event.isRecurring())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }

    private EventParticipantResponse toParticipantResponse(EventParticipant participant, User user) {
        return EventParticipantResponse.builder()
                .id(participant.getId())
                .eventId(participant.getEventId())
                .userId(participant.getUserId())
                .userEmail(user != null ? user.getEmail() : null)
                .userFirstName(user != null ? user.getFirstName() : null)
                .userLastName(user != null ? user.getLastName() : null)
                .status(participant.getStatus())
                .invitedAt(participant.getInvitedAt())
                .respondedAt(participant.getRespondedAt())
                .build();
    }
}
