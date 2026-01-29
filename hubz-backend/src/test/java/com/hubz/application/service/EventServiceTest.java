package com.hubz.application.service;

import com.hubz.application.dto.request.CreateEventRequest;
import com.hubz.application.dto.request.UpdateEventRequest;
import com.hubz.application.dto.response.EventResponse;
import com.hubz.application.port.out.EventRepositoryPort;
import com.hubz.domain.exception.AccessDeniedException;
import com.hubz.domain.exception.EventNotFoundException;
import com.hubz.domain.model.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private AuthorizationService authorizationService;

    @InjectMocks
    private EventService eventService;

    private UUID organizationId;
    private UUID userId;
    private UUID eventId;
    private Event testOrgEvent;
    private Event testPersonalEvent;
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
                .organizationId(null)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createRequest = new CreateEventRequest();
        createRequest.setTitle("Test Event");
        createRequest.setDescription("Test description");
        createRequest.setStartTime(startTime);
        createRequest.setEndTime(endTime);
        createRequest.setObjective("Test objective");

        updateRequest = new UpdateEventRequest();
        updateRequest.setTitle("Updated Event");
        updateRequest.setDescription("Updated description");
        updateRequest.setStartTime(startTime.plusDays(1));
        updateRequest.setEndTime(endTime.plusDays(1));
        updateRequest.setObjective("Updated objective");
    }

    @Nested
    @DisplayName("Get Events By Organization Tests")
    class GetByOrganizationTests {

        @Test
        @DisplayName("Should successfully get events by organization")
        void shouldGetEventsByOrganization() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(eventRepository.findByOrganizationId(organizationId)).thenReturn(List.of(testOrgEvent));

            // When
            List<EventResponse> events = eventService.getByOrganization(organizationId, userId);

            // Then
            assertThat(events).hasSize(1);
            assertThat(events.get(0).getTitle()).isEqualTo(testOrgEvent.getTitle());
            assertThat(events.get(0).getOrganizationId()).isEqualTo(organizationId);
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

        @Test
        @DisplayName("Should throw exception when user has no access")
        void shouldThrowExceptionWhenNoAccess() {
            // Given
            doThrow(new RuntimeException("No access"))
                    .when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When & Then
            assertThatThrownBy(() -> eventService.getByOrganization(organizationId, userId))
                    .isInstanceOf(RuntimeException.class);
            verify(eventRepository, never()).findByOrganizationId(any());
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

            // When
            List<EventResponse> events = eventService.getPersonalEvents(userId);

            // Then
            assertThat(events).hasSize(1);
            assertThat(events.get(0).getTitle()).isEqualTo(testPersonalEvent.getTitle());
            assertThat(events.get(0).getOrganizationId()).isNull();
            verify(eventRepository).findPersonalEvents(userId);
        }

        @Test
        @DisplayName("Should return empty list when no personal events exist")
        void shouldReturnEmptyListWhenNoPersonalEvents() {
            // Given
            when(eventRepository.findPersonalEvents(userId)).thenReturn(List.of());

            // When
            List<EventResponse> events = eventService.getPersonalEvents(userId);

            // Then
            assertThat(events).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Events By Time Range Tests")
    class GetByTimeRangeTests {

        @Test
        @DisplayName("Should successfully get organization events by time range")
        void shouldGetOrgEventsByTimeRange() {
            // Given
            LocalDateTime rangeStart = LocalDateTime.now();
            LocalDateTime rangeEnd = LocalDateTime.now().plusDays(7);

            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(eventRepository.findByOrganizationAndTimeRange(organizationId, rangeStart, rangeEnd))
                    .thenReturn(List.of(testOrgEvent));

            // When
            List<EventResponse> events = eventService.getByOrganizationAndTimeRange(
                    organizationId, rangeStart, rangeEnd, userId);

            // Then
            assertThat(events).hasSize(1);
            verify(eventRepository).findByOrganizationAndTimeRange(organizationId, rangeStart, rangeEnd);
        }

        @Test
        @DisplayName("Should successfully get personal events by time range")
        void shouldGetPersonalEventsByTimeRange() {
            // Given
            LocalDateTime rangeStart = LocalDateTime.now();
            LocalDateTime rangeEnd = LocalDateTime.now().plusDays(7);

            when(eventRepository.findPersonalEventsByTimeRange(userId, rangeStart, rangeEnd))
                    .thenReturn(List.of(testPersonalEvent));

            // When
            List<EventResponse> events = eventService.getPersonalEventsByTimeRange(userId, rangeStart, rangeEnd);

            // Then
            assertThat(events).hasSize(1);
            verify(eventRepository).findPersonalEventsByTimeRange(userId, rangeStart, rangeEnd);
        }
    }

    @Nested
    @DisplayName("Create Event Tests")
    class CreateTests {

        @Test
        @DisplayName("Should successfully create organization event")
        void shouldCreateOrganizationEvent() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(eventRepository.save(any(Event.class))).thenReturn(testOrgEvent);

            // When
            EventResponse response = eventService.create(createRequest, organizationId, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTitle()).isEqualTo(testOrgEvent.getTitle());
            assertThat(response.getOrganizationId()).isEqualTo(organizationId);

            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(eventRepository).save(any(Event.class));
        }

        @Test
        @DisplayName("Should successfully create personal event without authorization check")
        void shouldCreatePersonalEvent() {
            // Given
            when(eventRepository.save(any(Event.class))).thenReturn(testPersonalEvent);

            // When
            EventResponse response = eventService.create(createRequest, null, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getOrganizationId()).isNull();

            verify(authorizationService, never()).checkOrganizationAccess(any(), any());
            verify(eventRepository).save(any(Event.class));
        }

        @Test
        @DisplayName("Should set timestamps when creating event")
        void shouldSetTimestamps() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            when(eventRepository.save(eventCaptor.capture())).thenReturn(testOrgEvent);

            // When
            eventService.create(createRequest, organizationId, userId);

            // Then
            Event savedEvent = eventCaptor.getValue();
            assertThat(savedEvent.getCreatedAt()).isNotNull();
            assertThat(savedEvent.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should set all event properties from request")
        void shouldSetAllPropertiesFromRequest() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            when(eventRepository.save(eventCaptor.capture())).thenReturn(testOrgEvent);

            // When
            eventService.create(createRequest, organizationId, userId);

            // Then
            Event savedEvent = eventCaptor.getValue();
            assertThat(savedEvent.getTitle()).isEqualTo(createRequest.getTitle());
            assertThat(savedEvent.getDescription()).isEqualTo(createRequest.getDescription());
            assertThat(savedEvent.getStartTime()).isEqualTo(createRequest.getStartTime());
            assertThat(savedEvent.getEndTime()).isEqualTo(createRequest.getEndTime());
            assertThat(savedEvent.getObjective()).isEqualTo(createRequest.getObjective());
        }
    }

    @Nested
    @DisplayName("Update Event Tests")
    class UpdateTests {

        @Test
        @DisplayName("Should successfully update organization event")
        void shouldUpdateOrganizationEvent() {
            // Given
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(testOrgEvent));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(eventRepository.save(any(Event.class))).thenReturn(testOrgEvent);

            // When
            EventResponse response = eventService.update(eventId, updateRequest, userId);

            // Then
            assertThat(response).isNotNull();
            verify(eventRepository).findById(eventId);
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(eventRepository).save(any(Event.class));
        }

        @Test
        @DisplayName("Should successfully update personal event by owner")
        void shouldUpdatePersonalEventByOwner() {
            // Given
            when(eventRepository.findById(testPersonalEvent.getId())).thenReturn(Optional.of(testPersonalEvent));
            when(eventRepository.save(any(Event.class))).thenReturn(testPersonalEvent);

            // When
            EventResponse response = eventService.update(testPersonalEvent.getId(), updateRequest, userId);

            // Then
            assertThat(response).isNotNull();
            verify(authorizationService, never()).checkOrganizationAccess(any(), any());
            verify(eventRepository).save(any(Event.class));
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
            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when updating personal event by non-owner")
        void shouldThrowExceptionWhenNonOwnerUpdatesPersonalEvent() {
            // Given
            UUID otherUserId = UUID.randomUUID();
            when(eventRepository.findById(testPersonalEvent.getId())).thenReturn(Optional.of(testPersonalEvent));

            // When & Then
            assertThatThrownBy(() -> eventService.update(testPersonalEvent.getId(), updateRequest, otherUserId))
                    .isInstanceOf(AccessDeniedException.class);
            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should update updatedAt timestamp")
        void shouldUpdateTimestamp() {
            // Given
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(testOrgEvent));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            when(eventRepository.save(eventCaptor.capture())).thenReturn(testOrgEvent);

            // When
            eventService.update(eventId, updateRequest, userId);

            // Then
            Event updatedEvent = eventCaptor.getValue();
            assertThat(updatedEvent.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Delete Event Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should successfully delete organization event")
        void shouldDeleteOrganizationEvent() {
            // Given
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(testOrgEvent));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            doNothing().when(eventRepository).delete(testOrgEvent);

            // When
            eventService.delete(eventId, userId);

            // Then
            verify(eventRepository).findById(eventId);
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(eventRepository).delete(testOrgEvent);
        }

        @Test
        @DisplayName("Should successfully delete personal event by owner")
        void shouldDeletePersonalEventByOwner() {
            // Given
            when(eventRepository.findById(testPersonalEvent.getId())).thenReturn(Optional.of(testPersonalEvent));
            doNothing().when(eventRepository).delete(testPersonalEvent);

            // When
            eventService.delete(testPersonalEvent.getId(), userId);

            // Then
            verify(authorizationService, never()).checkOrganizationAccess(any(), any());
            verify(eventRepository).delete(testPersonalEvent);
        }

        @Test
        @DisplayName("Should throw exception when event not found")
        void shouldThrowExceptionWhenEventNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(eventRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> eventService.delete(nonExistentId, userId))
                    .isInstanceOf(EventNotFoundException.class);
            verify(eventRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw exception when deleting personal event by non-owner")
        void shouldThrowExceptionWhenNonOwnerDeletesPersonalEvent() {
            // Given
            UUID otherUserId = UUID.randomUUID();
            when(eventRepository.findById(testPersonalEvent.getId())).thenReturn(Optional.of(testPersonalEvent));

            // When & Then
            assertThatThrownBy(() -> eventService.delete(testPersonalEvent.getId(), otherUserId))
                    .isInstanceOf(AccessDeniedException.class);
            verify(eventRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Response Mapping Tests")
    class ResponseMappingTests {

        @Test
        @DisplayName("Should correctly map event to response")
        void shouldCorrectlyMapEventToResponse() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(eventRepository.findByOrganizationId(organizationId)).thenReturn(List.of(testOrgEvent));

            // When
            List<EventResponse> events = eventService.getByOrganization(organizationId, userId);

            // Then
            EventResponse response = events.get(0);
            assertThat(response.getId()).isEqualTo(testOrgEvent.getId());
            assertThat(response.getTitle()).isEqualTo(testOrgEvent.getTitle());
            assertThat(response.getDescription()).isEqualTo(testOrgEvent.getDescription());
            assertThat(response.getStartTime()).isEqualTo(testOrgEvent.getStartTime());
            assertThat(response.getEndTime()).isEqualTo(testOrgEvent.getEndTime());
            assertThat(response.getObjective()).isEqualTo(testOrgEvent.getObjective());
            assertThat(response.getOrganizationId()).isEqualTo(testOrgEvent.getOrganizationId());
            assertThat(response.getUserId()).isEqualTo(testOrgEvent.getUserId());
            assertThat(response.getCreatedAt()).isEqualTo(testOrgEvent.getCreatedAt());
            assertThat(response.getUpdatedAt()).isEqualTo(testOrgEvent.getUpdatedAt());
        }
    }
}
