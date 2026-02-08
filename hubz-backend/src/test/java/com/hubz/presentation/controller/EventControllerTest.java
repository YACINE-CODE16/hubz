package com.hubz.presentation.controller;

import com.hubz.application.dto.request.CreateEventRequest;
import com.hubz.application.dto.request.UpdateEventRequest;
import com.hubz.application.dto.response.EventResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.EventService;
import com.hubz.application.service.ICalService;
import com.hubz.domain.exception.AccessDeniedException;
import com.hubz.domain.exception.EventNotFoundException;
import com.hubz.domain.model.User;
import com.hubz.infrastructure.config.CorsProperties;
import com.hubz.infrastructure.security.JwtAuthenticationFilter;
import com.hubz.infrastructure.security.JwtService;
import com.hubz.presentation.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = EventController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, JwtService.class, CorsProperties.class}
        )
)
@Import(GlobalExceptionHandler.class)
@DisplayName("EventController Unit Tests")
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @MockBean
    private ICalService iCalService;

    @MockBean
    private UserRepositoryPort userRepositoryPort;

    private UUID userId;
    private UUID orgId;
    private UUID eventId;
    private User testUser;
    private Authentication mockAuth;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orgId = UUID.randomUUID();
        eventId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        mockAuth = mock(Authentication.class);
        when(mockAuth.getName()).thenReturn("test@example.com");
        when(userRepositoryPort.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    }

    private EventResponse createEventResponse() {
        return EventResponse.builder()
                .id(eventId)
                .title("Test Event")
                .description("Test description")
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .objective("Test objective")
                .organizationId(orgId)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("GET /api/organizations/{orgId}/events - Get Organization Events")
    class GetByOrganizationTests {

        @Test
        @DisplayName("Should return 200 and list of events")
        void shouldGetEventsByOrganization() throws Exception {
            // Given
            List<EventResponse> responses = List.of(createEventResponse());
            when(eventService.getByOrganization(orgId, userId)).thenReturn(responses);

            // When & Then
            mockMvc.perform(get("/api/organizations/{orgId}/events", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].title").value("Test Event"));

            verify(eventService).getByOrganization(orgId, userId);
        }

        @Test
        @DisplayName("Should return 200 with time range filter (expandRecurring default true)")
        void shouldGetEventsWithTimeRange() throws Exception {
            // Given
            LocalDateTime start = LocalDateTime.now();
            LocalDateTime end = LocalDateTime.now().plusDays(7);
            List<EventResponse> responses = List.of(createEventResponse());
            // By default, expandRecurring=true, so it uses getByOrganizationWithRecurrence
            when(eventService.getByOrganizationWithRecurrence(eq(orgId), any(), any(), eq(userId)))
                    .thenReturn(responses);

            // When & Then
            mockMvc.perform(get("/api/organizations/{orgId}/events", orgId)
                            .principal(mockAuth)
                            .param("start", start.toString())
                            .param("end", end.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].title").value("Test Event"));

            verify(eventService).getByOrganizationWithRecurrence(eq(orgId), any(), any(), eq(userId));
        }

        @Test
        @DisplayName("Should return 200 with time range filter without expanding recurring")
        void shouldGetEventsWithTimeRangeWithoutExpandingRecurring() throws Exception {
            // Given
            LocalDateTime start = LocalDateTime.now();
            LocalDateTime end = LocalDateTime.now().plusDays(7);
            List<EventResponse> responses = List.of(createEventResponse());
            when(eventService.getByOrganizationAndTimeRange(eq(orgId), any(), any(), eq(userId)))
                    .thenReturn(responses);

            // When & Then
            mockMvc.perform(get("/api/organizations/{orgId}/events", orgId)
                            .principal(mockAuth)
                            .param("start", start.toString())
                            .param("end", end.toString())
                            .param("expandRecurring", "false"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].title").value("Test Event"));

            verify(eventService).getByOrganizationAndTimeRange(eq(orgId), any(), any(), eq(userId));
        }
    }

    @Nested
    @DisplayName("POST /api/organizations/{orgId}/events - Create Organization Event")
    class CreateOrganizationEventTests {

        @Test
        @DisplayName("Should return 201 and event when creation is successful")
        void shouldCreateEvent() throws Exception {
            // Given
            EventResponse response = createEventResponse();
            when(eventService.create(any(CreateEventRequest.class), eq(orgId), eq(userId)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/organizations/{orgId}/events", orgId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "Test Event",
                                        "description": "Test description",
                                        "startTime": "2026-02-15T10:00:00",
                                        "endTime": "2026-02-15T12:00:00",
                                        "objective": "Test objective"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("Test Event"));

            verify(eventService).create(any(CreateEventRequest.class), eq(orgId), eq(userId));
        }

        @Test
        @DisplayName("Should return 400 when title is blank")
        void shouldReturn400WhenTitleBlank() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/organizations/{orgId}/events", orgId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "",
                                        "startTime": "2026-02-15T10:00:00",
                                        "endTime": "2026-02-15T12:00:00"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(eventService, never()).create(any(), any(), any());
        }

        @Test
        @DisplayName("Should return 400 when startTime is missing")
        void shouldReturn400WhenStartTimeMissing() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/organizations/{orgId}/events", orgId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "Test Event",
                                        "endTime": "2026-02-15T12:00:00"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(eventService, never()).create(any(), any(), any());
        }

        @Test
        @DisplayName("Should return 400 when endTime is missing")
        void shouldReturn400WhenEndTimeMissing() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/organizations/{orgId}/events", orgId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "Test Event",
                                        "startTime": "2026-02-15T10:00:00"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(eventService, never()).create(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("GET /api/users/me/events - Get Personal Events")
    class GetPersonalEventsTests {

        @Test
        @DisplayName("Should return 200 and list of personal events")
        void shouldGetPersonalEvents() throws Exception {
            // Given
            EventResponse response = EventResponse.builder()
                    .id(eventId)
                    .title("Personal Event")
                    .userId(userId)
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                    .createdAt(LocalDateTime.now())
                    .build();

            when(eventService.getPersonalEvents(userId)).thenReturn(List.of(response));

            // When & Then
            mockMvc.perform(get("/api/users/me/events")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].title").value("Personal Event"));

            verify(eventService).getPersonalEvents(userId);
        }

        @Test
        @DisplayName("Should return 200 with time range filter (expandRecurring default true)")
        void shouldGetPersonalEventsWithTimeRange() throws Exception {
            // Given
            LocalDateTime start = LocalDateTime.now();
            LocalDateTime end = LocalDateTime.now().plusDays(7);
            EventResponse response = createEventResponse();
            // By default, expandRecurring=true, so it uses getPersonalEventsWithRecurrence
            when(eventService.getPersonalEventsWithRecurrence(eq(userId), any(), any()))
                    .thenReturn(List.of(response));

            // When & Then
            mockMvc.perform(get("/api/users/me/events")
                            .principal(mockAuth)
                            .param("start", start.toString())
                            .param("end", end.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].title").value("Test Event"));

            verify(eventService).getPersonalEventsWithRecurrence(eq(userId), any(), any());
        }

        @Test
        @DisplayName("Should return 200 with time range filter without expanding recurring")
        void shouldGetPersonalEventsWithTimeRangeWithoutExpandingRecurring() throws Exception {
            // Given
            LocalDateTime start = LocalDateTime.now();
            LocalDateTime end = LocalDateTime.now().plusDays(7);
            EventResponse response = createEventResponse();
            when(eventService.getPersonalEventsByTimeRange(eq(userId), any(), any()))
                    .thenReturn(List.of(response));

            // When & Then
            mockMvc.perform(get("/api/users/me/events")
                            .principal(mockAuth)
                            .param("start", start.toString())
                            .param("end", end.toString())
                            .param("expandRecurring", "false"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].title").value("Test Event"));

            verify(eventService).getPersonalEventsByTimeRange(eq(userId), any(), any());
        }
    }

    @Nested
    @DisplayName("POST /api/users/me/events - Create Personal Event")
    class CreatePersonalEventTests {

        @Test
        @DisplayName("Should return 201 and personal event when creation is successful")
        void shouldCreatePersonalEvent() throws Exception {
            // Given
            EventResponse response = EventResponse.builder()
                    .id(eventId)
                    .title("Personal Event")
                    .userId(userId)
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                    .createdAt(LocalDateTime.now())
                    .build();

            when(eventService.create(any(CreateEventRequest.class), eq(null), eq(userId)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/users/me/events")
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "Personal Event",
                                        "startTime": "2026-02-15T10:00:00",
                                        "endTime": "2026-02-15T11:00:00"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("Personal Event"));

            verify(eventService).create(any(CreateEventRequest.class), eq(null), eq(userId));
        }
    }

    @Nested
    @DisplayName("PUT /api/events/{id} - Update Event")
    class UpdateTests {

        @Test
        @DisplayName("Should return 200 and updated event when successful")
        void shouldUpdateEvent() throws Exception {
            // Given
            EventResponse response = EventResponse.builder()
                    .id(eventId)
                    .title("Updated Event")
                    .userId(userId)
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                    .createdAt(LocalDateTime.now())
                    .build();

            when(eventService.update(eq(eventId), any(UpdateEventRequest.class), eq(userId)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(put("/api/events/{id}", eventId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "Updated Event",
                                        "startTime": "2026-02-15T10:00:00",
                                        "endTime": "2026-02-15T12:00:00"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated Event"));

            verify(eventService).update(eq(eventId), any(UpdateEventRequest.class), eq(userId));
        }

        @Test
        @DisplayName("Should return 404 when event not found")
        void shouldReturn404WhenNotFound() throws Exception {
            // Given
            when(eventService.update(eq(eventId), any(UpdateEventRequest.class), eq(userId)))
                    .thenThrow(new EventNotFoundException(eventId));

            // When & Then
            mockMvc.perform(put("/api/events/{id}", eventId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "Updated Event",
                                        "startTime": "2026-02-15T10:00:00",
                                        "endTime": "2026-02-15T12:00:00"
                                    }
                                    """))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/events/{id} - Delete Event")
    class DeleteTests {

        @Test
        @DisplayName("Should return 204 when deletion is successful")
        void shouldDeleteEvent() throws Exception {
            // Given
            doNothing().when(eventService).delete(eventId, userId, false);

            // When & Then
            mockMvc.perform(delete("/api/events/{id}", eventId)
                            .principal(mockAuth))
                    .andExpect(status().isNoContent());

            verify(eventService).delete(eventId, userId, false);
        }

        @Test
        @DisplayName("Should return 204 when deleting all occurrences")
        void shouldDeleteAllOccurrences() throws Exception {
            // Given
            doNothing().when(eventService).delete(eventId, userId, true);

            // When & Then
            mockMvc.perform(delete("/api/events/{id}", eventId)
                            .principal(mockAuth)
                            .param("deleteAllOccurrences", "true"))
                    .andExpect(status().isNoContent());

            verify(eventService).delete(eventId, userId, true);
        }

        @Test
        @DisplayName("Should return 404 when event not found")
        void shouldReturn404WhenNotFound() throws Exception {
            // Given
            doThrow(new EventNotFoundException(eventId))
                    .when(eventService).delete(eventId, userId, false);

            // When & Then
            mockMvc.perform(delete("/api/events/{id}", eventId)
                            .principal(mockAuth))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when user is not authorized")
        void shouldReturn403WhenNotAuthorized() throws Exception {
            // Given
            doThrow(new AccessDeniedException("Not authorized"))
                    .when(eventService).delete(eventId, userId, false);

            // When & Then
            mockMvc.perform(delete("/api/events/{id}", eventId)
                            .principal(mockAuth))
                    .andExpect(status().isForbidden());
        }
    }
}
