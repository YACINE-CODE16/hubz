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
import com.hubz.domain.enums.EventReminder;
import com.hubz.domain.enums.ParticipantStatus;
import com.hubz.domain.enums.RecurrenceType;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventService Unit Tests")
class EventServiceTest {

    @Mock
    private EventRepositoryPort eventRepository;

    @Mock
    private EventParticipantRepositoryPort participantRepository;

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private EventService eventService;

    private UUID organizationId;
    private UUID userId;
    private UUID eventId;
    private Event testOrgEvent;
    private Event testPersonalEvent;
    private User testUser;
    private CreateEventRequest createRequest;
    private UpdateEventRequest updateRequest;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        startTime = LocalDateTime.now().plusDays(1);
        endTime = LocalDateTime.now().plusDays(1).plusHours(2);

        testOrgEvent = Event.builder()
                .id(eventId)
                .title("Organization Meeting")
                .description("Weekly sync meeting")
                .startTime(startTime)
                .endTime(endTime)
                .objective("Sync on project progress")
                .location("Conference Room A")
                .reminder(EventReminder.FIFTEEN_MINUTES)
                .organizationId(organizationId)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testPersonalEvent = Event.builder()
                .id(UUID.randomUUID())
                .title("Personal Event")
                .description("Personal event description")
                .startTime(startTime)
                .endTime(endTime)
                .objective("Personal objective")
                .location("https://zoom.us/meeting")
                .reminder(EventReminder.ONE_HOUR)
                .organizationId(null)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        createRequest = CreateEventRequest.builder()
                .title("Test Event")
                .description("Test description")
                .startTime(startTime)
                .endTime(endTime)
                .objective("Test objective")
                .location("Test Location")
                .reminder(EventReminder.THIRTY_MINUTES)
                .build();

        updateRequest = UpdateEventRequest.builder()
                .title("Updated Event")
                .description("Updated description")
                .startTime(startTime.plusDays(1))
                .endTime(endTime.plusDays(1))
                .objective("Updated objective")
                .location("New Location")
                .reminder(EventReminder.ONE_DAY)
                .build();
    }

    @Nested
    @DisplayName("Get Events By Organization Tests")
    class GetByOrganizationTests {

        @Test
        @DisplayName("Should successfully get events by organization with participants")
        void shouldGetEventsByOrganizationWithParticipants() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(eventRepository.findByOrganizationId(organizationId)).thenReturn(List.of(testOrgEvent));
            when(participantRepository.findByEventId(eventId)).thenReturn(List.of());

            // When
            List<EventResponse> events = eventService.getByOrganization(organizationId, userId);

            // Then
            assertThat(events).hasSize(1);
            assertThat(events.get(0).getTitle()).isEqualTo(testOrgEvent.getTitle());
            assertThat(events.get(0).getOrganizationId()).isEqualTo(organizationId);
            assertThat(events.get(0).getLocation()).isEqualTo(testOrgEvent.getLocation());
            assertThat(events.get(0).getReminder()).isEqualTo(testOrgEvent.getReminder());
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(eventRepository).findByOrganizationId(organizationId);
        }

        @Test
        @DisplayName("Should return empty list when no events exist")
        void shouldReturnEmptyListWhenNoEvents() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(eventRepository.findByOrganizationId(organizationId)).thenReturn(List.of());

            // When
            List<EventResponse> events = eventService.getByOrganization(organizationId, userId);

            // Then
            assertThat(events).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Personal Events Tests")
    class GetPersonalEventsTests {

        @Test
        @DisplayName("Should successfully get personal events")
        void shouldGetPersonalEvents() {
            // Given
            when(eventRepository.findPersonalEvents(userId)).thenReturn(List.of(testPersonalEvent));
            when(participantRepository.findByEventId(testPersonalEvent.getId())).thenReturn(List.of());

            // When
            List<EventResponse> events = eventService.getPersonalEvents(userId);

            // Then
            assertThat(events).hasSize(1);
            assertThat(events.get(0).getTitle()).isEqualTo(testPersonalEvent.getTitle());
            assertThat(events.get(0).getOrganizationId()).isNull();
            verify(eventRepository).findPersonalEvents(userId);
        }
    }

    @Nested
    @DisplayName("Create Event Tests")
    class CreateTests {

        @Test
        @DisplayName("Should successfully create organization event with location and reminder")
        void shouldCreateOrganizationEventWithNewFields() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(eventRepository.save(any(Event.class))).thenReturn(testOrgEvent);
            when(participantRepository.findByEventId(any())).thenReturn(List.of());

            // When
            EventResponse response = eventService.create(createRequest, organizationId, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTitle()).isEqualTo(testOrgEvent.getTitle());
            assertThat(response.getLocation()).isEqualTo(testOrgEvent.getLocation());
            assertThat(response.getReminder()).isEqualTo(testOrgEvent.getReminder());

            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(eventRepository).save(any(Event.class));
        }

        @Test
        @DisplayName("Should create event and invite participants")
        void shouldCreateEventAndInviteParticipants() {
            // Given
            UUID participantId = UUID.randomUUID();
            createRequest.setParticipantIds(List.of(participantId));

            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(eventRepository.save(any(Event.class))).thenReturn(testOrgEvent);
            when(participantRepository.save(any(EventParticipant.class))).thenAnswer(i -> i.getArgument(0));
            when(participantRepository.findByEventId(any())).thenReturn(List.of());

            // When
            EventResponse response = eventService.create(createRequest, organizationId, userId);

            // Then
            assertThat(response).isNotNull();
            verify(participantRepository).save(any(EventParticipant.class));
            verify(notificationService).createNotification(eq(participantId), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should set location and reminder from request")
        void shouldSetLocationAndReminderFromRequest() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            when(eventRepository.save(eventCaptor.capture())).thenReturn(testOrgEvent);
            when(participantRepository.findByEventId(any())).thenReturn(List.of());

            // When
            eventService.create(createRequest, organizationId, userId);

            // Then
            Event savedEvent = eventCaptor.getValue();
            assertThat(savedEvent.getLocation()).isEqualTo(createRequest.getLocation());
            assertThat(savedEvent.getReminder()).isEqualTo(createRequest.getReminder());
        }
    }

    @Nested
    @DisplayName("Update Event Tests")
    class UpdateTests {

        @Test
        @DisplayName("Should update event with location and reminder")
        void shouldUpdateEventWithNewFields() {
            // Given
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(testOrgEvent));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(participantRepository.findByEventId(eventId)).thenReturn(List.of());
            when(eventRepository.save(any(Event.class))).thenReturn(testOrgEvent);

            // When
            EventResponse response = eventService.update(eventId, updateRequest, userId);

            // Then
            assertThat(response).isNotNull();

            ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            verify(eventRepository).save(eventCaptor.capture());
            Event updatedEvent = eventCaptor.getValue();
            assertThat(updatedEvent.getLocation()).isEqualTo(updateRequest.getLocation());
            assertThat(updatedEvent.getReminder()).isEqualTo(updateRequest.getReminder());
        }

        @Test
        @DisplayName("Should notify accepted participants when event is updated")
        void shouldNotifyParticipantsOnUpdate() {
            // Given
            UUID participantUserId = UUID.randomUUID();
            EventParticipant participant = EventParticipant.builder()
                    .id(UUID.randomUUID())
                    .eventId(eventId)
                    .userId(participantUserId)
                    .status(ParticipantStatus.ACCEPTED)
                    .build();

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(testOrgEvent));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(participantRepository.findByEventId(eventId)).thenReturn(List.of(participant));
            when(eventRepository.save(any(Event.class))).thenReturn(testOrgEvent);

            // When
            eventService.update(eventId, updateRequest, userId);

            // Then
            verify(notificationService).createNotification(eq(participantUserId), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should throw exception when event not found")
        void shouldThrowExceptionWhenEventNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(eventRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> eventService.update(nonExistentId, updateRequest, userId))
                    .isInstanceOf(EventNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Delete Event Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should delete event and notify participants")
        void shouldDeleteEventAndNotifyParticipants() {
            // Given
            UUID participantUserId = UUID.randomUUID();
            EventParticipant participant = EventParticipant.builder()
                    .id(UUID.randomUUID())
                    .eventId(eventId)
                    .userId(participantUserId)
                    .status(ParticipantStatus.ACCEPTED)
                    .build();

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(testOrgEvent));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(participantRepository.findByEventId(eventId)).thenReturn(List.of(participant));
            doNothing().when(participantRepository).deleteByEventId(eventId);
            doNothing().when(eventRepository).delete(testOrgEvent);

            // When
            eventService.delete(eventId, userId);

            // Then
            verify(notificationService).createNotification(eq(participantUserId), any(), any(), any(), any(), any(), any());
            verify(participantRepository).deleteByEventId(eventId);
            verify(eventRepository).delete(testOrgEvent);
        }

        @Test
        @DisplayName("Should throw exception when non-owner deletes personal event")
        void shouldThrowExceptionWhenNonOwnerDeletesPersonalEvent() {
            // Given
            UUID otherUserId = UUID.randomUUID();
            when(eventRepository.findById(testPersonalEvent.getId())).thenReturn(Optional.of(testPersonalEvent));

            // When & Then
            assertThatThrownBy(() -> eventService.delete(testPersonalEvent.getId(), otherUserId))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("Participant Management Tests")
    class ParticipantManagementTests {

        @Test
        @DisplayName("Should invite participant to event")
        void shouldInviteParticipantToEvent() {
            // Given
            UUID inviteeId = UUID.randomUUID();
            User invitee = User.builder()
                    .id(inviteeId)
                    .email("invitee@example.com")
                    .firstName("Jane")
                    .lastName("Doe")
                    .build();

            InviteEventParticipantRequest request = InviteEventParticipantRequest.builder()
                    .userId(inviteeId)
                    .build();

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(testOrgEvent));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(userRepository.findById(inviteeId)).thenReturn(Optional.of(invitee));
            when(participantRepository.existsByEventIdAndUserId(eventId, inviteeId)).thenReturn(false);
            when(participantRepository.save(any(EventParticipant.class))).thenAnswer(i -> {
                EventParticipant p = i.getArgument(0);
                return p;
            });

            // When
            EventParticipantResponse response = eventService.inviteParticipant(eventId, request, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(inviteeId);
            assertThat(response.getStatus()).isEqualTo(ParticipantStatus.INVITED);
            verify(notificationService).createNotification(eq(inviteeId), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should throw exception when inviting already invited user")
        void shouldThrowExceptionWhenInvitingAlreadyInvitedUser() {
            // Given
            UUID inviteeId = UUID.randomUUID();
            User invitee = User.builder().id(inviteeId).email("test@example.com").build();
            InviteEventParticipantRequest request = InviteEventParticipantRequest.builder()
                    .userId(inviteeId)
                    .build();

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(testOrgEvent));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(userRepository.findById(inviteeId)).thenReturn(Optional.of(invitee));
            when(participantRepository.existsByEventIdAndUserId(eventId, inviteeId)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> eventService.inviteParticipant(eventId, request, userId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already invited");
        }

        @Test
        @DisplayName("Should accept event invitation")
        void shouldAcceptEventInvitation() {
            // Given
            EventParticipant participant = EventParticipant.builder()
                    .id(UUID.randomUUID())
                    .eventId(eventId)
                    .userId(userId)
                    .status(ParticipantStatus.INVITED)
                    .invitedAt(LocalDateTime.now())
                    .build();

            RespondEventInvitationRequest request = RespondEventInvitationRequest.builder()
                    .status(ParticipantStatus.ACCEPTED)
                    .build();

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(testOrgEvent));
            when(participantRepository.findByEventIdAndUserId(eventId, userId)).thenReturn(Optional.of(participant));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(participantRepository.save(any(EventParticipant.class))).thenAnswer(i -> i.getArgument(0));

            // When
            EventParticipantResponse response = eventService.respondToInvitation(eventId, request, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(ParticipantStatus.ACCEPTED);
        }

        @Test
        @DisplayName("Should decline event invitation")
        void shouldDeclineEventInvitation() {
            // Given
            EventParticipant participant = EventParticipant.builder()
                    .id(UUID.randomUUID())
                    .eventId(eventId)
                    .userId(userId)
                    .status(ParticipantStatus.INVITED)
                    .invitedAt(LocalDateTime.now())
                    .build();

            RespondEventInvitationRequest request = RespondEventInvitationRequest.builder()
                    .status(ParticipantStatus.DECLINED)
                    .build();

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(testOrgEvent));
            when(participantRepository.findByEventIdAndUserId(eventId, userId)).thenReturn(Optional.of(participant));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(participantRepository.save(any(EventParticipant.class))).thenAnswer(i -> i.getArgument(0));

            // When
            EventParticipantResponse response = eventService.respondToInvitation(eventId, request, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(ParticipantStatus.DECLINED);
        }

        @Test
        @DisplayName("Should throw exception when responding to already responded invitation")
        void shouldThrowExceptionWhenRespondingAgain() {
            // Given
            EventParticipant participant = EventParticipant.builder()
                    .id(UUID.randomUUID())
                    .eventId(eventId)
                    .userId(userId)
                    .status(ParticipantStatus.ACCEPTED) // Already responded
                    .invitedAt(LocalDateTime.now())
                    .build();

            RespondEventInvitationRequest request = RespondEventInvitationRequest.builder()
                    .status(ParticipantStatus.DECLINED)
                    .build();

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(testOrgEvent));
            when(participantRepository.findByEventIdAndUserId(eventId, userId)).thenReturn(Optional.of(participant));

            // When & Then
            assertThatThrownBy(() -> eventService.respondToInvitation(eventId, request, userId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already responded");
        }

        @Test
        @DisplayName("Should remove participant from event")
        void shouldRemoveParticipantFromEvent() {
            // Given
            UUID participantUserId = UUID.randomUUID();
            EventParticipant participant = EventParticipant.builder()
                    .id(UUID.randomUUID())
                    .eventId(eventId)
                    .userId(participantUserId)
                    .status(ParticipantStatus.ACCEPTED)
                    .build();

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(testOrgEvent));
            when(participantRepository.findByEventIdAndUserId(eventId, participantUserId))
                    .thenReturn(Optional.of(participant));
            doNothing().when(participantRepository).delete(participant);

            // When
            eventService.removeParticipant(eventId, participantUserId, userId);

            // Then
            verify(participantRepository).delete(participant);
        }

        @Test
        @DisplayName("Should allow participant to remove themselves")
        void shouldAllowParticipantToRemoveThemselves() {
            // Given
            UUID participantUserId = UUID.randomUUID();
            Event personalEvent = Event.builder()
                    .id(eventId)
                    .title("Test")
                    .organizationId(null)
                    .userId(UUID.randomUUID()) // Different owner
                    .build();
            EventParticipant participant = EventParticipant.builder()
                    .id(UUID.randomUUID())
                    .eventId(eventId)
                    .userId(participantUserId)
                    .status(ParticipantStatus.ACCEPTED)
                    .build();

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(personalEvent));
            when(participantRepository.findByEventIdAndUserId(eventId, participantUserId))
                    .thenReturn(Optional.of(participant));
            doNothing().when(participantRepository).delete(participant);

            // When - participant removes themselves
            eventService.removeParticipant(eventId, participantUserId, participantUserId);

            // Then
            verify(participantRepository).delete(participant);
        }

        @Test
        @DisplayName("Should throw exception when non-authorized user removes participant")
        void shouldThrowExceptionWhenUnauthorizedRemoval() {
            // Given
            UUID participantUserId = UUID.randomUUID();
            UUID unauthorizedUserId = UUID.randomUUID();
            Event personalEvent = Event.builder()
                    .id(eventId)
                    .title("Test")
                    .organizationId(null)
                    .userId(UUID.randomUUID()) // Different owner
                    .build();

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(personalEvent));

            // When & Then
            assertThatThrownBy(() -> eventService.removeParticipant(eventId, participantUserId, unauthorizedUserId))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Should get participants list")
        void shouldGetParticipantsList() {
            // Given
            EventParticipant participant = EventParticipant.builder()
                    .id(UUID.randomUUID())
                    .eventId(eventId)
                    .userId(userId)
                    .status(ParticipantStatus.ACCEPTED)
                    .invitedAt(LocalDateTime.now())
                    .build();

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(testOrgEvent));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(participantRepository.findByEventId(eventId)).thenReturn(List.of(participant));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            List<EventParticipantResponse> participants = eventService.getParticipants(eventId, userId);

            // Then
            assertThat(participants).hasSize(1);
            assertThat(participants.get(0).getUserEmail()).isEqualTo(testUser.getEmail());
        }
    }

    @Nested
    @DisplayName("Get By Id Tests")
    class GetByIdTests {

        @Test
        @DisplayName("Should get event by id with participants")
        void shouldGetEventByIdWithParticipants() {
            // Given
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(testOrgEvent));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(participantRepository.findByEventId(eventId)).thenReturn(List.of());

            // When
            EventResponse response = eventService.getById(eventId, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(eventId);
            assertThat(response.getParticipants()).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when event not found")
        void shouldThrowExceptionWhenEventNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(eventRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> eventService.getById(nonExistentId, userId))
                    .isInstanceOf(EventNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Recurring Event Tests")
    class RecurringEventTests {

        private Event recurringEvent;

        @BeforeEach
        void setUpRecurring() {
            recurringEvent = Event.builder()
                    .id(eventId)
                    .title("Weekly Meeting")
                    .description("Recurring weekly meeting")
                    .startTime(LocalDateTime.of(2024, 1, 1, 10, 0))
                    .endTime(LocalDateTime.of(2024, 1, 1, 11, 0))
                    .objective("Weekly sync")
                    .location("Conference Room")
                    .reminder(EventReminder.FIFTEEN_MINUTES)
                    .organizationId(organizationId)
                    .userId(userId)
                    .recurrenceType(RecurrenceType.WEEKLY)
                    .recurrenceInterval(1)
                    .recurrenceEndDate(LocalDate.of(2024, 3, 31))
                    .parentEventId(null)
                    .isRecurrenceException(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        }

        @Test
        @DisplayName("Should create recurring event with recurrence settings")
        void shouldCreateRecurringEvent() {
            // Given
            CreateEventRequest request = CreateEventRequest.builder()
                    .title("Weekly Meeting")
                    .description("Recurring weekly meeting")
                    .startTime(startTime)
                    .endTime(endTime)
                    .recurrenceType(RecurrenceType.WEEKLY)
                    .recurrenceInterval(1)
                    .recurrenceEndDate(LocalDate.now().plusMonths(3))
                    .build();

            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(eventRepository.save(any(Event.class))).thenAnswer(i -> {
                Event e = i.getArgument(0);
                e.setId(eventId);
                return e;
            });
            when(participantRepository.findByEventId(any())).thenReturn(List.of());

            // When
            EventResponse response = eventService.create(request, organizationId, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getRecurrenceType()).isEqualTo(RecurrenceType.WEEKLY);
            assertThat(response.getRecurrenceInterval()).isEqualTo(1);
            assertThat(response.getIsRecurring()).isTrue();

            ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            verify(eventRepository).save(eventCaptor.capture());
            Event savedEvent = eventCaptor.getValue();
            assertThat(savedEvent.getRecurrenceType()).isEqualTo(RecurrenceType.WEEKLY);
            assertThat(savedEvent.getRecurrenceInterval()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should default recurrence interval to 1 if not provided")
        void shouldDefaultRecurrenceIntervalToOne() {
            // Given
            CreateEventRequest request = CreateEventRequest.builder()
                    .title("Daily Standup")
                    .startTime(startTime)
                    .endTime(endTime)
                    .recurrenceType(RecurrenceType.DAILY)
                    .recurrenceInterval(null) // Not provided
                    .build();

            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(eventRepository.save(any(Event.class))).thenAnswer(i -> {
                Event e = i.getArgument(0);
                e.setId(eventId);
                return e;
            });
            when(participantRepository.findByEventId(any())).thenReturn(List.of());

            // When
            eventService.create(request, organizationId, userId);

            // Then
            ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            verify(eventRepository).save(eventCaptor.capture());
            Event savedEvent = eventCaptor.getValue();
            assertThat(savedEvent.getRecurrenceInterval()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should generate occurrences for weekly recurring event")
        void shouldGenerateWeeklyOccurrences() {
            // Given
            LocalDateTime rangeStart = LocalDateTime.of(2024, 1, 1, 0, 0);
            LocalDateTime rangeEnd = LocalDateTime.of(2024, 1, 31, 23, 59);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(recurringEvent));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(participantRepository.findByEventId(any())).thenReturn(List.of());

            // When
            List<EventResponse> occurrences = eventService.getRecurringEventOccurrences(
                    eventId, rangeStart, rangeEnd, userId
            );

            // Then
            // January 2024 should have 5 occurrences: Jan 1, 8, 15, 22, 29
            assertThat(occurrences).hasSize(5);
            assertThat(occurrences.get(0).getStartTime().toLocalDate()).isEqualTo(LocalDate.of(2024, 1, 1));
            assertThat(occurrences.get(1).getStartTime().toLocalDate()).isEqualTo(LocalDate.of(2024, 1, 8));
            assertThat(occurrences.get(2).getStartTime().toLocalDate()).isEqualTo(LocalDate.of(2024, 1, 15));
            assertThat(occurrences.get(3).getStartTime().toLocalDate()).isEqualTo(LocalDate.of(2024, 1, 22));
            assertThat(occurrences.get(4).getStartTime().toLocalDate()).isEqualTo(LocalDate.of(2024, 1, 29));
        }

        @Test
        @DisplayName("Should generate occurrences for daily recurring event")
        void shouldGenerateDailyOccurrences() {
            // Given
            Event dailyEvent = Event.builder()
                    .id(eventId)
                    .title("Daily Standup")
                    .startTime(LocalDateTime.of(2024, 1, 1, 9, 0))
                    .endTime(LocalDateTime.of(2024, 1, 1, 9, 15))
                    .organizationId(organizationId)
                    .userId(userId)
                    .recurrenceType(RecurrenceType.DAILY)
                    .recurrenceInterval(1)
                    .recurrenceEndDate(LocalDate.of(2024, 1, 10))
                    .build();

            LocalDateTime rangeStart = LocalDateTime.of(2024, 1, 1, 0, 0);
            LocalDateTime rangeEnd = LocalDateTime.of(2024, 1, 15, 23, 59);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(dailyEvent));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(participantRepository.findByEventId(any())).thenReturn(List.of());

            // When
            List<EventResponse> occurrences = eventService.getRecurringEventOccurrences(
                    eventId, rangeStart, rangeEnd, userId
            );

            // Then - Should have 10 occurrences (Jan 1-10)
            assertThat(occurrences).hasSize(10);
        }

        @Test
        @DisplayName("Should generate occurrences with interval > 1")
        void shouldGenerateOccurrencesWithInterval() {
            // Given - Event every 2 weeks
            Event biweeklyEvent = Event.builder()
                    .id(eventId)
                    .title("Biweekly Review")
                    .startTime(LocalDateTime.of(2024, 1, 1, 14, 0))
                    .endTime(LocalDateTime.of(2024, 1, 1, 15, 0))
                    .organizationId(organizationId)
                    .userId(userId)
                    .recurrenceType(RecurrenceType.WEEKLY)
                    .recurrenceInterval(2) // Every 2 weeks
                    .recurrenceEndDate(LocalDate.of(2024, 3, 31))
                    .build();

            LocalDateTime rangeStart = LocalDateTime.of(2024, 1, 1, 0, 0);
            LocalDateTime rangeEnd = LocalDateTime.of(2024, 1, 31, 23, 59);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(biweeklyEvent));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(participantRepository.findByEventId(any())).thenReturn(List.of());

            // When
            List<EventResponse> occurrences = eventService.getRecurringEventOccurrences(
                    eventId, rangeStart, rangeEnd, userId
            );

            // Then - Should have 3 occurrences in January: Jan 1, 15, 29
            assertThat(occurrences).hasSize(3);
            assertThat(occurrences.get(0).getStartTime().toLocalDate()).isEqualTo(LocalDate.of(2024, 1, 1));
            assertThat(occurrences.get(1).getStartTime().toLocalDate()).isEqualTo(LocalDate.of(2024, 1, 15));
            assertThat(occurrences.get(2).getStartTime().toLocalDate()).isEqualTo(LocalDate.of(2024, 1, 29));
        }

        @Test
        @DisplayName("Should respect recurrence end date")
        void shouldRespectRecurrenceEndDate() {
            // Given - Event with end date
            Event limitedEvent = Event.builder()
                    .id(eventId)
                    .title("Limited Recurring")
                    .startTime(LocalDateTime.of(2024, 1, 1, 10, 0))
                    .endTime(LocalDateTime.of(2024, 1, 1, 11, 0))
                    .organizationId(organizationId)
                    .userId(userId)
                    .recurrenceType(RecurrenceType.WEEKLY)
                    .recurrenceInterval(1)
                    .recurrenceEndDate(LocalDate.of(2024, 1, 15)) // Ends mid-month
                    .build();

            LocalDateTime rangeStart = LocalDateTime.of(2024, 1, 1, 0, 0);
            LocalDateTime rangeEnd = LocalDateTime.of(2024, 1, 31, 23, 59);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(limitedEvent));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(participantRepository.findByEventId(any())).thenReturn(List.of());

            // When
            List<EventResponse> occurrences = eventService.getRecurringEventOccurrences(
                    eventId, rangeStart, rangeEnd, userId
            );

            // Then - Should only have 3 occurrences: Jan 1, 8, 15 (end date inclusive)
            assertThat(occurrences).hasSize(3);
        }

        @Test
        @DisplayName("Should return single event for non-recurring event")
        void shouldReturnSingleEventForNonRecurring() {
            // Given
            Event nonRecurringEvent = Event.builder()
                    .id(eventId)
                    .title("Single Event")
                    .startTime(LocalDateTime.of(2024, 1, 15, 10, 0))
                    .endTime(LocalDateTime.of(2024, 1, 15, 11, 0))
                    .organizationId(organizationId)
                    .userId(userId)
                    .recurrenceType(RecurrenceType.NONE)
                    .build();

            LocalDateTime rangeStart = LocalDateTime.of(2024, 1, 1, 0, 0);
            LocalDateTime rangeEnd = LocalDateTime.of(2024, 1, 31, 23, 59);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(nonRecurringEvent));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(participantRepository.findByEventId(any())).thenReturn(List.of());

            // When
            List<EventResponse> occurrences = eventService.getRecurringEventOccurrences(
                    eventId, rangeStart, rangeEnd, userId
            );

            // Then
            assertThat(occurrences).hasSize(1);
            assertThat(occurrences.get(0).getStartTime()).isEqualTo(nonRecurringEvent.getStartTime());
        }

        @Test
        @DisplayName("Should generate monthly occurrences correctly")
        void shouldGenerateMonthlyOccurrences() {
            // Given
            Event monthlyEvent = Event.builder()
                    .id(eventId)
                    .title("Monthly Review")
                    .startTime(LocalDateTime.of(2024, 1, 15, 14, 0))
                    .endTime(LocalDateTime.of(2024, 1, 15, 16, 0))
                    .organizationId(organizationId)
                    .userId(userId)
                    .recurrenceType(RecurrenceType.MONTHLY)
                    .recurrenceInterval(1)
                    .recurrenceEndDate(LocalDate.of(2024, 12, 31))
                    .build();

            LocalDateTime rangeStart = LocalDateTime.of(2024, 1, 1, 0, 0);
            LocalDateTime rangeEnd = LocalDateTime.of(2024, 6, 30, 23, 59);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(monthlyEvent));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(participantRepository.findByEventId(any())).thenReturn(List.of());

            // When
            List<EventResponse> occurrences = eventService.getRecurringEventOccurrences(
                    eventId, rangeStart, rangeEnd, userId
            );

            // Then - Should have 6 occurrences (Jan-Jun 15th)
            assertThat(occurrences).hasSize(6);
            assertThat(occurrences.get(0).getStartTime().toLocalDate()).isEqualTo(LocalDate.of(2024, 1, 15));
            assertThat(occurrences.get(5).getStartTime().toLocalDate()).isEqualTo(LocalDate.of(2024, 6, 15));
        }

        @Test
        @DisplayName("Should delete recurring event series")
        void shouldDeleteRecurringEventSeries() {
            // Given
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(recurringEvent));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(participantRepository.findByEventId(eventId)).thenReturn(List.of());
            doNothing().when(participantRepository).deleteByEventId(eventId);
            doNothing().when(eventRepository).delete(recurringEvent);

            // When
            eventService.delete(eventId, userId, true);

            // Then
            verify(participantRepository).deleteByEventId(eventId);
            verify(eventRepository).delete(recurringEvent);
        }

        @Test
        @DisplayName("Should include isRecurring flag in response")
        void shouldIncludeIsRecurringFlag() {
            // Given
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(recurringEvent));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(participantRepository.findByEventId(eventId)).thenReturn(List.of());

            // When
            EventResponse response = eventService.getById(eventId, userId);

            // Then
            assertThat(response.getIsRecurring()).isTrue();
            assertThat(response.getRecurrenceType()).isEqualTo(RecurrenceType.WEEKLY);
        }
    }
}
