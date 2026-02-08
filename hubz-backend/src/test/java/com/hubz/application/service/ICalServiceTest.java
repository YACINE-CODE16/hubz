package com.hubz.application.service;

import com.hubz.application.port.out.EventParticipantRepositoryPort;
import com.hubz.application.port.out.EventRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.enums.EventReminder;
import com.hubz.domain.enums.ParticipantStatus;
import com.hubz.domain.exception.AccessDeniedException;
import com.hubz.domain.exception.EventNotFoundException;
import com.hubz.domain.model.Event;
import com.hubz.domain.model.EventParticipant;
import com.hubz.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ICalService Unit Tests")
class ICalServiceTest {

    @Mock
    private EventRepositoryPort eventRepository;

    @Mock
    private EventParticipantRepositoryPort participantRepository;

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private ICalService iCalService;

    private UUID userId;
    private UUID eventId;
    private UUID organizationId;
    private Event testEvent;
    private User testUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        organizationId = UUID.randomUUID();

        testEvent = Event.builder()
                .id(eventId)
                .title("Test Meeting")
                .description("A test meeting description")
                .startTime(LocalDateTime.of(2024, 6, 15, 10, 0))
                .endTime(LocalDateTime.of(2024, 6, 15, 11, 0))
                .objective("Discuss project status")
                .location("Conference Room A")
                .reminder(EventReminder.FIFTEEN_MINUTES)
                .organizationId(organizationId)
                .userId(userId)
                .createdAt(LocalDateTime.of(2024, 6, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2024, 6, 1, 9, 0))
                .build();

        testUser = User.builder()
                .id(userId)
                .email("organizer@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    @Nested
    @DisplayName("Export Single Event Tests")
    class ExportSingleEventTests {

        @Test
        @DisplayName("Should export event to iCal format")
        void shouldExportEventToICal() {
            // Given
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(participantRepository.findByEventId(eventId)).thenReturn(List.of());

            // When
            String ical = iCalService.exportEventToICal(eventId, userId);

            // Then
            assertThat(ical).isNotNull();
            assertThat(ical).contains("BEGIN:VCALENDAR");
            assertThat(ical).contains("VERSION:2.0");
            assertThat(ical).contains("BEGIN:VEVENT");
            assertThat(ical).contains("SUMMARY:Test Meeting");
            assertThat(ical).contains("DESCRIPTION:A test meeting description");
            assertThat(ical).contains("LOCATION:Conference Room A");
            assertThat(ical).contains("END:VEVENT");
            assertThat(ical).contains("END:VCALENDAR");
        }

        @Test
        @DisplayName("Should include reminder/alarm in iCal")
        void shouldIncludeReminderInICal() {
            // Given
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(participantRepository.findByEventId(eventId)).thenReturn(List.of());

            // When
            String ical = iCalService.exportEventToICal(eventId, userId);

            // Then
            assertThat(ical).contains("BEGIN:VALARM");
            assertThat(ical).contains("TRIGGER:-PT15M"); // 15 minutes before
            assertThat(ical).contains("ACTION:DISPLAY");
            assertThat(ical).contains("END:VALARM");
        }

        @Test
        @DisplayName("Should include organizer in iCal")
        void shouldIncludeOrganizerInICal() {
            // Given
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(participantRepository.findByEventId(eventId)).thenReturn(List.of());

            // When
            String ical = iCalService.exportEventToICal(eventId, userId);

            // Then
            assertThat(ical).contains("ORGANIZER;CN=John Doe:mailto:organizer@example.com");
        }

        @Test
        @DisplayName("Should include attendees in iCal")
        void shouldIncludeAttendeesInICal() {
            // Given
            UUID attendeeId = UUID.randomUUID();
            User attendee = User.builder()
                    .id(attendeeId)
                    .email("attendee@example.com")
                    .firstName("Jane")
                    .lastName("Smith")
                    .build();
            EventParticipant participant = EventParticipant.builder()
                    .id(UUID.randomUUID())
                    .eventId(eventId)
                    .userId(attendeeId)
                    .status(ParticipantStatus.ACCEPTED)
                    .build();

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.findById(attendeeId)).thenReturn(Optional.of(attendee));
            when(participantRepository.findByEventId(eventId)).thenReturn(List.of(participant));

            // When
            String ical = iCalService.exportEventToICal(eventId, userId);

            // Then
            assertThat(ical).contains("ATTENDEE;PARTSTAT=ACCEPTED;CN=Jane Smith:mailto:attendee@example.com");
        }

        @Test
        @DisplayName("Should throw exception when event not found")
        void shouldThrowExceptionWhenEventNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(eventRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> iCalService.exportEventToICal(nonExistentId, userId))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when user has no access")
        void shouldThrowExceptionWhenNoAccess() {
            // Given
            UUID otherUserId = UUID.randomUUID();
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
            doThrow(new AccessDeniedException("No access"))
                    .when(authorizationService).checkOrganizationAccess(organizationId, otherUserId);

            // When & Then
            assertThatThrownBy(() -> iCalService.exportEventToICal(eventId, otherUserId))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Should escape special characters in iCal")
        void shouldEscapeSpecialCharacters() {
            // Given
            Event eventWithSpecialChars = Event.builder()
                    .id(eventId)
                    .title("Meeting; with, special\\chars")
                    .description("Line1\nLine2")
                    .startTime(LocalDateTime.of(2024, 6, 15, 10, 0))
                    .endTime(LocalDateTime.of(2024, 6, 15, 11, 0))
                    .organizationId(organizationId)
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(eventWithSpecialChars));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(participantRepository.findByEventId(eventId)).thenReturn(List.of());

            // When
            String ical = iCalService.exportEventToICal(eventId, userId);

            // Then
            assertThat(ical).contains("SUMMARY:Meeting\\; with\\, special\\\\chars");
            assertThat(ical).contains("DESCRIPTION:Line1\\nLine2");
        }
    }

    @Nested
    @DisplayName("Export Personal Events Tests")
    class ExportPersonalEventsTests {

        @Test
        @DisplayName("Should export personal events to iCal")
        void shouldExportPersonalEventsToICal() {
            // Given
            Event personalEvent = Event.builder()
                    .id(eventId)
                    .title("Personal Meeting")
                    .startTime(LocalDateTime.of(2024, 6, 15, 10, 0))
                    .endTime(LocalDateTime.of(2024, 6, 15, 11, 0))
                    .organizationId(null)
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(eventRepository.findPersonalEvents(userId)).thenReturn(List.of(personalEvent));
            when(participantRepository.findByUserId(userId)).thenReturn(List.of());
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(participantRepository.findByEventId(eventId)).thenReturn(List.of());

            // When
            String ical = iCalService.exportPersonalEventsToICal(userId);

            // Then
            assertThat(ical).contains("BEGIN:VCALENDAR");
            assertThat(ical).contains("X-WR-CALNAME:Hubz Events");
            assertThat(ical).contains("SUMMARY:Personal Meeting");
            assertThat(ical).contains("END:VCALENDAR");
        }

        @Test
        @DisplayName("Should include events where user is participant")
        void shouldIncludeEventsWhereUserIsParticipant() {
            // Given
            UUID otherEventId = UUID.randomUUID();
            Event participatingEvent = Event.builder()
                    .id(otherEventId)
                    .title("Team Meeting")
                    .startTime(LocalDateTime.of(2024, 6, 16, 14, 0))
                    .endTime(LocalDateTime.of(2024, 6, 16, 15, 0))
                    .organizationId(organizationId)
                    .userId(UUID.randomUUID()) // Different owner
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            EventParticipant participation = EventParticipant.builder()
                    .id(UUID.randomUUID())
                    .eventId(otherEventId)
                    .userId(userId)
                    .status(ParticipantStatus.ACCEPTED)
                    .build();

            when(eventRepository.findPersonalEvents(userId)).thenReturn(List.of());
            when(participantRepository.findByUserId(userId)).thenReturn(List.of(participation));
            when(eventRepository.findById(otherEventId)).thenReturn(Optional.of(participatingEvent));
            when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
            when(participantRepository.findByEventId(otherEventId)).thenReturn(List.of());

            // When
            String ical = iCalService.exportPersonalEventsToICal(userId);

            // Then
            assertThat(ical).contains("SUMMARY:Team Meeting");
        }

        @Test
        @DisplayName("Should not include declined events")
        void shouldNotIncludeDeclinedEvents() {
            // Given
            UUID otherEventId = UUID.randomUUID();
            EventParticipant declinedParticipation = EventParticipant.builder()
                    .id(UUID.randomUUID())
                    .eventId(otherEventId)
                    .userId(userId)
                    .status(ParticipantStatus.DECLINED)
                    .build();

            when(eventRepository.findPersonalEvents(userId)).thenReturn(List.of());
            when(participantRepository.findByUserId(userId)).thenReturn(List.of(declinedParticipation));

            // When
            iCalService.exportPersonalEventsToICal(userId);

            // Then
            verify(eventRepository, never()).findById(otherEventId);
        }
    }

    @Nested
    @DisplayName("Export All Events Tests")
    class ExportAllEventsTests {

        @Test
        @DisplayName("Should export all user events to iCal")
        void shouldExportAllEventsToICal() {
            // Given
            when(eventRepository.findAllByUserId(userId)).thenReturn(List.of(testEvent));
            when(participantRepository.findByUserId(userId)).thenReturn(List.of());
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(participantRepository.findByEventId(eventId)).thenReturn(List.of());

            // When
            String ical = iCalService.exportAllEventsToICal(userId);

            // Then
            assertThat(ical).contains("BEGIN:VCALENDAR");
            assertThat(ical).contains("SUMMARY:Test Meeting");
            assertThat(ical).contains("END:VCALENDAR");
        }

        @Test
        @DisplayName("Should return valid iCal even with no events")
        void shouldReturnValidICalWithNoEvents() {
            // Given
            when(eventRepository.findAllByUserId(userId)).thenReturn(List.of());
            when(participantRepository.findByUserId(userId)).thenReturn(List.of());

            // When
            String ical = iCalService.exportAllEventsToICal(userId);

            // Then
            assertThat(ical).contains("BEGIN:VCALENDAR");
            assertThat(ical).contains("END:VCALENDAR");
            assertThat(ical).doesNotContain("BEGIN:VEVENT");
        }
    }

    @Nested
    @DisplayName("iCal Format Validation Tests")
    class ICalFormatTests {

        @Test
        @DisplayName("Should use CRLF line endings")
        void shouldUseCRLFLineEndings() {
            // Given
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(participantRepository.findByEventId(eventId)).thenReturn(List.of());

            // When
            String ical = iCalService.exportEventToICal(eventId, userId);

            // Then
            assertThat(ical).contains("\r\n");
        }

        @Test
        @DisplayName("Should include required iCal properties")
        void shouldIncludeRequiredProperties() {
            // Given
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(participantRepository.findByEventId(eventId)).thenReturn(List.of());

            // When
            String ical = iCalService.exportEventToICal(eventId, userId);

            // Then
            assertThat(ical).contains("PRODID:");
            assertThat(ical).contains("CALSCALE:GREGORIAN");
            assertThat(ical).contains("METHOD:PUBLISH");
            assertThat(ical).contains("UID:" + eventId + "@hubz.app");
            assertThat(ical).contains("DTSTAMP:");
            assertThat(ical).contains("DTSTART:");
            assertThat(ical).contains("DTEND:");
            assertThat(ical).contains("CREATED:");
            assertThat(ical).contains("LAST-MODIFIED:");
        }

        @Test
        @DisplayName("Should map participant status correctly")
        void shouldMapParticipantStatusCorrectly() {
            // Given
            UUID attendee1Id = UUID.randomUUID();
            UUID attendee2Id = UUID.randomUUID();
            UUID attendee3Id = UUID.randomUUID();

            User attendee1 = User.builder().id(attendee1Id).email("a1@test.com").firstName("A").lastName("1").build();
            User attendee2 = User.builder().id(attendee2Id).email("a2@test.com").firstName("A").lastName("2").build();
            User attendee3 = User.builder().id(attendee3Id).email("a3@test.com").firstName("A").lastName("3").build();

            List<EventParticipant> participants = List.of(
                    EventParticipant.builder().eventId(eventId).userId(attendee1Id).status(ParticipantStatus.INVITED).build(),
                    EventParticipant.builder().eventId(eventId).userId(attendee2Id).status(ParticipantStatus.ACCEPTED).build(),
                    EventParticipant.builder().eventId(eventId).userId(attendee3Id).status(ParticipantStatus.DECLINED).build()
            );

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.findById(attendee1Id)).thenReturn(Optional.of(attendee1));
            when(userRepository.findById(attendee2Id)).thenReturn(Optional.of(attendee2));
            when(userRepository.findById(attendee3Id)).thenReturn(Optional.of(attendee3));
            when(participantRepository.findByEventId(eventId)).thenReturn(participants);

            // When
            String ical = iCalService.exportEventToICal(eventId, userId);

            // Then
            assertThat(ical).contains("PARTSTAT=NEEDS-ACTION");
            assertThat(ical).contains("PARTSTAT=ACCEPTED");
            assertThat(ical).contains("PARTSTAT=DECLINED");
        }
    }
}
