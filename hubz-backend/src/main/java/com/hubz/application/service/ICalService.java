package com.hubz.application.service;

import com.hubz.application.port.out.EventParticipantRepositoryPort;
import com.hubz.application.port.out.EventRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.enums.ParticipantStatus;
import com.hubz.domain.exception.EventNotFoundException;
import com.hubz.domain.model.Event;
import com.hubz.domain.model.EventParticipant;
import com.hubz.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for exporting events to iCal (.ics) format.
 */
@Service
@RequiredArgsConstructor
public class ICalService {

    private final EventRepositoryPort eventRepository;
    private final EventParticipantRepositoryPort participantRepository;
    private final UserRepositoryPort userRepository;
    private final AuthorizationService authorizationService;

    private static final DateTimeFormatter ICAL_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final String CRLF = "\r\n";
    private static final String PRODID = "-//Hubz//Event Calendar//EN";

    /**
     * Export a single event to iCal format.
     */
    @Transactional(readOnly = true)
    public String exportEventToICal(UUID eventId, UUID currentUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // Check access
        checkEventAccess(event, currentUserId);

        StringBuilder ical = new StringBuilder();
        ical.append("BEGIN:VCALENDAR").append(CRLF);
        ical.append("VERSION:2.0").append(CRLF);
        ical.append("PRODID:").append(PRODID).append(CRLF);
        ical.append("CALSCALE:GREGORIAN").append(CRLF);
        ical.append("METHOD:PUBLISH").append(CRLF);

        appendEvent(ical, event);

        ical.append("END:VCALENDAR").append(CRLF);

        return ical.toString();
    }

    /**
     * Export all personal events for a user to iCal format.
     */
    @Transactional(readOnly = true)
    public String exportPersonalEventsToICal(UUID userId) {
        List<Event> events = eventRepository.findPersonalEvents(userId);

        // Also include events where user is a participant
        List<EventParticipant> participations = participantRepository.findByUserId(userId);
        List<Event> participatingEvents = new ArrayList<>();
        for (EventParticipant participation : participations) {
            if (participation.getStatus() == ParticipantStatus.ACCEPTED) {
                eventRepository.findById(participation.getEventId())
                        .ifPresent(participatingEvents::add);
            }
        }

        return buildICalFromEvents(events, participatingEvents, userId);
    }

    /**
     * Export all events for a user (personal + all organizations + participations) to iCal format.
     */
    @Transactional(readOnly = true)
    public String exportAllEventsToICal(UUID userId) {
        // Get all events where user is the organizer
        List<Event> ownEvents = eventRepository.findAllByUserId(userId);

        // Get all events where user is a participant (and accepted)
        List<EventParticipant> participations = participantRepository.findByUserId(userId);
        List<Event> participatingEvents = new ArrayList<>();
        for (EventParticipant participation : participations) {
            if (participation.getStatus() == ParticipantStatus.ACCEPTED) {
                eventRepository.findById(participation.getEventId())
                        .ifPresent(e -> {
                            // Avoid duplicates (if user is both organizer and participant)
                            if (!e.getUserId().equals(userId)) {
                                participatingEvents.add(e);
                            }
                        });
            }
        }

        return buildICalFromEvents(ownEvents, participatingEvents, userId);
    }

    private String buildICalFromEvents(List<Event> ownEvents, List<Event> participatingEvents, UUID userId) {
        StringBuilder ical = new StringBuilder();
        ical.append("BEGIN:VCALENDAR").append(CRLF);
        ical.append("VERSION:2.0").append(CRLF);
        ical.append("PRODID:").append(PRODID).append(CRLF);
        ical.append("CALSCALE:GREGORIAN").append(CRLF);
        ical.append("METHOD:PUBLISH").append(CRLF);
        ical.append("X-WR-CALNAME:Hubz Events").append(CRLF);

        // Add own events
        for (Event event : ownEvents) {
            appendEvent(ical, event);
        }

        // Add participating events
        for (Event event : participatingEvents) {
            appendEvent(ical, event);
        }

        ical.append("END:VCALENDAR").append(CRLF);

        return ical.toString();
    }

    private void appendEvent(StringBuilder ical, Event event) {
        ical.append("BEGIN:VEVENT").append(CRLF);
        ical.append("UID:").append(event.getId().toString()).append("@hubz.app").append(CRLF);
        ical.append("DTSTAMP:").append(formatDateTime(LocalDateTime.now())).append(CRLF);
        ical.append("DTSTART:").append(formatDateTime(event.getStartTime())).append(CRLF);
        ical.append("DTEND:").append(formatDateTime(event.getEndTime())).append(CRLF);
        ical.append("SUMMARY:").append(escapeICalText(event.getTitle())).append(CRLF);

        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            ical.append("DESCRIPTION:").append(escapeICalText(event.getDescription())).append(CRLF);
        }

        if (event.getLocation() != null && !event.getLocation().isEmpty()) {
            ical.append("LOCATION:").append(escapeICalText(event.getLocation())).append(CRLF);
        }

        // Add organizer
        User organizer = userRepository.findById(event.getUserId()).orElse(null);
        if (organizer != null) {
            ical.append("ORGANIZER;CN=").append(escapeICalText(organizer.getFirstName() + " " + organizer.getLastName()))
                    .append(":mailto:").append(organizer.getEmail()).append(CRLF);
        }

        // Add attendees
        List<EventParticipant> participants = participantRepository.findByEventId(event.getId());
        for (EventParticipant participant : participants) {
            User user = userRepository.findById(participant.getUserId()).orElse(null);
            if (user != null) {
                String partstat = mapParticipantStatus(participant.getStatus());
                ical.append("ATTENDEE;PARTSTAT=").append(partstat)
                        .append(";CN=").append(escapeICalText(user.getFirstName() + " " + user.getLastName()))
                        .append(":mailto:").append(user.getEmail()).append(CRLF);
            }
        }

        // Add reminder/alarm if set
        if (event.getReminder() != null && event.getReminder().getMinutesBefore() > 0) {
            ical.append("BEGIN:VALARM").append(CRLF);
            ical.append("TRIGGER:-PT").append(event.getReminder().getMinutesBefore()).append("M").append(CRLF);
            ical.append("ACTION:DISPLAY").append(CRLF);
            ical.append("DESCRIPTION:Reminder: ").append(escapeICalText(event.getTitle())).append(CRLF);
            ical.append("END:VALARM").append(CRLF);
        }

        ical.append("CREATED:").append(formatDateTime(event.getCreatedAt())).append(CRLF);
        ical.append("LAST-MODIFIED:").append(formatDateTime(event.getUpdatedAt())).append(CRLF);
        ical.append("END:VEVENT").append(CRLF);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        // Convert to UTC
        return dateTime.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneId.of("UTC"))
                .format(ICAL_DATE_FORMAT);
    }

    private String escapeICalText(String text) {
        if (text == null) {
            return "";
        }
        // Escape special characters according to RFC 5545
        return text
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private String mapParticipantStatus(ParticipantStatus status) {
        return switch (status) {
            case INVITED -> "NEEDS-ACTION";
            case ACCEPTED -> "ACCEPTED";
            case DECLINED -> "DECLINED";
        };
    }

    private void checkEventAccess(Event event, UUID userId) {
        if (event.getOrganizationId() != null) {
            authorizationService.checkOrganizationAccess(event.getOrganizationId(), userId);
        } else {
            // For personal events, user must be owner or participant
            boolean isOwner = event.getUserId().equals(userId);
            boolean isParticipant = participantRepository.existsByEventIdAndUserId(event.getId(), userId);
            if (!isOwner && !isParticipant) {
                throw new com.hubz.domain.exception.AccessDeniedException("Not authorized to access this event");
            }
        }
    }
}
