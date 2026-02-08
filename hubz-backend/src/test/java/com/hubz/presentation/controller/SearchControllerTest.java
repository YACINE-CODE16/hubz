package com.hubz.presentation.controller;

import com.hubz.application.dto.response.SearchResultResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.SearchService;
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
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = SearchController.class,
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
@DisplayName("SearchController Unit Tests")
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchService searchService;

    @MockBean
    private UserRepositoryPort userRepositoryPort;

    private UUID userId;
    private User testUser;
    private Authentication mockAuth;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

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

    @Nested
    @DisplayName("GET /api/search - Search")
    class SearchTests {

        @Test
        @DisplayName("Should return 200 and search results")
        void shouldReturnSearchResults() throws Exception {
            // Given
            SearchResultResponse response = SearchResultResponse.builder()
                    .organizations(List.of(
                            SearchResultResponse.OrganizationSearchResult.builder()
                                    .id(UUID.randomUUID().toString())
                                    .name("Test Organization")
                                    .description("A test organization")
                                    .matchedField("name")
                                    .build()
                    ))
                    .tasks(List.of(
                            SearchResultResponse.TaskSearchResult.builder()
                                    .id(UUID.randomUUID().toString())
                                    .title("Test Task")
                                    .description("A test task")
                                    .status("TODO")
                                    .priority("HIGH")
                                    .matchedField("title")
                                    .build()
                    ))
                    .goals(List.of())
                    .events(List.of())
                    .notes(List.of())
                    .members(List.of())
                    .totalResults(2)
                    .build();

            when(searchService.search("test", userId)).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/search")
                            .param("q", "test")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalResults").value(2))
                    .andExpect(jsonPath("$.organizations[0].name").value("Test Organization"))
                    .andExpect(jsonPath("$.tasks[0].title").value("Test Task"));

            verify(searchService).search("test", userId);
        }

        @Test
        @DisplayName("Should return 200 and empty results when no matches")
        void shouldReturnEmptyResults() throws Exception {
            // Given
            SearchResultResponse response = SearchResultResponse.builder()
                    .organizations(List.of())
                    .tasks(List.of())
                    .goals(List.of())
                    .events(List.of())
                    .notes(List.of())
                    .members(List.of())
                    .totalResults(0)
                    .build();

            when(searchService.search("nonexistent", userId)).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/search")
                            .param("q", "nonexistent")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalResults").value(0))
                    .andExpect(jsonPath("$.organizations").isEmpty())
                    .andExpect(jsonPath("$.tasks").isEmpty());

            verify(searchService).search("nonexistent", userId);
        }

        @Test
        @DisplayName("Should return 200 with empty query")
        void shouldHandleEmptyQuery() throws Exception {
            // Given
            SearchResultResponse response = SearchResultResponse.builder()
                    .organizations(List.of())
                    .tasks(List.of())
                    .goals(List.of())
                    .events(List.of())
                    .notes(List.of())
                    .members(List.of())
                    .totalResults(0)
                    .build();

            when(searchService.search("", userId)).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/search")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalResults").value(0));

            verify(searchService).search("", userId);
        }

        @Test
        @DisplayName("Should return search results with all entity types")
        void shouldReturnAllEntityTypes() throws Exception {
            // Given
            SearchResultResponse response = SearchResultResponse.builder()
                    .organizations(List.of(
                            SearchResultResponse.OrganizationSearchResult.builder()
                                    .id(UUID.randomUUID().toString())
                                    .name("Acme Corp")
                                    .matchedField("name")
                                    .build()
                    ))
                    .tasks(List.of(
                            SearchResultResponse.TaskSearchResult.builder()
                                    .id(UUID.randomUUID().toString())
                                    .title("Review Acme proposal")
                                    .matchedField("title")
                                    .build()
                    ))
                    .goals(List.of(
                            SearchResultResponse.GoalSearchResult.builder()
                                    .id(UUID.randomUUID().toString())
                                    .title("Complete Acme onboarding")
                                    .matchedField("title")
                                    .build()
                    ))
                    .events(List.of(
                            SearchResultResponse.EventSearchResult.builder()
                                    .id(UUID.randomUUID().toString())
                                    .title("Acme kickoff meeting")
                                    .matchedField("title")
                                    .build()
                    ))
                    .notes(List.of(
                            SearchResultResponse.NoteSearchResult.builder()
                                    .id(UUID.randomUUID().toString())
                                    .title("Notes about Acme")
                                    .matchedField("title")
                                    .build()
                    ))
                    .members(List.of(
                            SearchResultResponse.MemberSearchResult.builder()
                                    .id(UUID.randomUUID().toString())
                                    .firstName("Acme")
                                    .lastName("Admin")
                                    .email("admin@acme.com")
                                    .matchedField("email")
                                    .build()
                    ))
                    .totalResults(6)
                    .build();

            when(searchService.search("acme", userId)).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/search")
                            .param("q", "acme")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalResults").value(6))
                    .andExpect(jsonPath("$.organizations").isNotEmpty())
                    .andExpect(jsonPath("$.tasks").isNotEmpty())
                    .andExpect(jsonPath("$.goals").isNotEmpty())
                    .andExpect(jsonPath("$.events").isNotEmpty())
                    .andExpect(jsonPath("$.notes").isNotEmpty())
                    .andExpect(jsonPath("$.members").isNotEmpty());

            verify(searchService).search("acme", userId);
        }
    }
}
