package com.hubz.presentation.controller;

import com.hubz.application.dto.request.CreateTeamRequest;
import com.hubz.application.dto.request.UpdateTeamRequest;
import com.hubz.application.dto.response.TeamMemberResponse;
import com.hubz.application.dto.response.TeamResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.TeamService;
import com.hubz.domain.exception.AccessDeniedException;
import com.hubz.domain.exception.TeamNotFoundException;
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
        value = TeamController.class,
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
@DisplayName("TeamController Unit Tests")
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeamService teamService;

    @MockBean
    private UserRepositoryPort userRepositoryPort;

    private UUID userId;
    private UUID orgId;
    private UUID teamId;
    private User testUser;
    private Authentication mockAuth;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orgId = UUID.randomUUID();
        teamId = UUID.randomUUID();

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

    private TeamResponse createTeamResponse() {
        return TeamResponse.builder()
                .id(teamId)
                .name("Test Team")
                .description("Test description")
                .organizationId(orgId)
                .createdAt(LocalDateTime.now())
                .memberCount(3)
                .build();
    }

    @Nested
    @DisplayName("GET /api/organizations/{orgId}/teams - Get Teams By Organization")
    class GetByOrganizationTests {

        @Test
        @DisplayName("Should return 200 and list of teams")
        void shouldGetTeamsByOrganization() throws Exception {
            // Given
            List<TeamResponse> responses = List.of(createTeamResponse());
            when(teamService.getByOrganization(orgId, userId)).thenReturn(responses);

            // When & Then
            mockMvc.perform(get("/api/organizations/{orgId}/teams", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("Test Team"))
                    .andExpect(jsonPath("$[0].memberCount").value(3));

            verify(teamService).getByOrganization(orgId, userId);
        }

        @Test
        @DisplayName("Should return 200 and empty list when no teams")
        void shouldReturnEmptyList() throws Exception {
            // Given
            when(teamService.getByOrganization(orgId, userId)).thenReturn(List.of());

            // When & Then
            mockMvc.perform(get("/api/organizations/{orgId}/teams", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());

            verify(teamService).getByOrganization(orgId, userId);
        }
    }

    @Nested
    @DisplayName("POST /api/organizations/{orgId}/teams - Create Team")
    class CreateTests {

        @Test
        @DisplayName("Should return 201 and team when creation is successful")
        void shouldCreateTeam() throws Exception {
            // Given
            TeamResponse response = createTeamResponse();
            when(teamService.create(any(CreateTeamRequest.class), eq(orgId), eq(userId)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/organizations/{orgId}/teams", orgId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "Test Team",
                                        "description": "Test description"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Test Team"));

            verify(teamService).create(any(CreateTeamRequest.class), eq(orgId), eq(userId));
        }

        @Test
        @DisplayName("Should return 400 when name is blank")
        void shouldReturn400WhenNameBlank() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/organizations/{orgId}/teams", orgId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "",
                                        "description": "Test description"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(teamService, never()).create(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("PUT /api/teams/{id} - Update Team")
    class UpdateTests {

        @Test
        @DisplayName("Should return 200 and updated team when successful")
        void shouldUpdateTeam() throws Exception {
            // Given
            TeamResponse response = TeamResponse.builder()
                    .id(teamId)
                    .name("Updated Team")
                    .description("Updated description")
                    .organizationId(orgId)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(teamService.update(eq(teamId), any(UpdateTeamRequest.class), eq(userId)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(put("/api/teams/{id}", teamId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "Updated Team",
                                        "description": "Updated description"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Team"));

            verify(teamService).update(eq(teamId), any(UpdateTeamRequest.class), eq(userId));
        }

        @Test
        @DisplayName("Should return 404 when team not found")
        void shouldReturn404WhenNotFound() throws Exception {
            // Given
            when(teamService.update(eq(teamId), any(UpdateTeamRequest.class), eq(userId)))
                    .thenThrow(new TeamNotFoundException(teamId));

            // When & Then
            mockMvc.perform(put("/api/teams/{id}", teamId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "Updated Team"
                                    }
                                    """))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/teams/{id} - Delete Team")
    class DeleteTests {

        @Test
        @DisplayName("Should return 204 when deletion is successful")
        void shouldDeleteTeam() throws Exception {
            // Given
            doNothing().when(teamService).delete(teamId, userId);

            // When & Then
            mockMvc.perform(delete("/api/teams/{id}", teamId)
                            .principal(mockAuth))
                    .andExpect(status().isNoContent());

            verify(teamService).delete(teamId, userId);
        }

        @Test
        @DisplayName("Should return 404 when team not found")
        void shouldReturn404WhenNotFound() throws Exception {
            // Given
            doThrow(new TeamNotFoundException(teamId))
                    .when(teamService).delete(teamId, userId);

            // When & Then
            mockMvc.perform(delete("/api/teams/{id}", teamId)
                            .principal(mockAuth))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when user is not authorized")
        void shouldReturn403WhenNotAuthorized() throws Exception {
            // Given
            doThrow(new AccessDeniedException("Not authorized"))
                    .when(teamService).delete(teamId, userId);

            // When & Then
            mockMvc.perform(delete("/api/teams/{id}", teamId)
                            .principal(mockAuth))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/teams/{teamId}/members - Get Team Members")
    class GetMembersTests {

        @Test
        @DisplayName("Should return 200 and list of members")
        void shouldGetTeamMembers() throws Exception {
            // Given
            TeamMemberResponse member = TeamMemberResponse.builder()
                    .id(UUID.randomUUID())
                    .teamId(teamId)
                    .userId(userId)
                    .firstName("John")
                    .lastName("Doe")
                    .email("test@example.com")
                    .build();

            when(teamService.getTeamMembers(teamId, userId)).thenReturn(List.of(member));

            // When & Then
            mockMvc.perform(get("/api/teams/{teamId}/members", teamId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].email").value("test@example.com"));

            verify(teamService).getTeamMembers(teamId, userId);
        }
    }

    @Nested
    @DisplayName("POST /api/teams/{teamId}/members/{userId} - Add Team Member")
    class AddMemberTests {

        @Test
        @DisplayName("Should return 201 when member is added successfully")
        void shouldAddTeamMember() throws Exception {
            // Given
            UUID newMemberId = UUID.randomUUID();
            doNothing().when(teamService).addMember(teamId, newMemberId, userId);

            // When & Then
            mockMvc.perform(post("/api/teams/{teamId}/members/{userId}", teamId, newMemberId)
                            .principal(mockAuth))
                    .andExpect(status().isCreated());

            verify(teamService).addMember(teamId, newMemberId, userId);
        }
    }

    @Nested
    @DisplayName("DELETE /api/teams/{teamId}/members/{userId} - Remove Team Member")
    class RemoveMemberTests {

        @Test
        @DisplayName("Should return 204 when member is removed successfully")
        void shouldRemoveTeamMember() throws Exception {
            // Given
            UUID memberToRemove = UUID.randomUUID();
            doNothing().when(teamService).removeMember(teamId, memberToRemove, userId);

            // When & Then
            mockMvc.perform(delete("/api/teams/{teamId}/members/{userId}", teamId, memberToRemove)
                            .principal(mockAuth))
                    .andExpect(status().isNoContent());

            verify(teamService).removeMember(teamId, memberToRemove, userId);
        }
    }
}
