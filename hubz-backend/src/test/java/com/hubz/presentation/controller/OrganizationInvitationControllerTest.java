package com.hubz.presentation.controller;

import com.hubz.application.dto.request.CreateInvitationRequest;
import com.hubz.application.dto.response.InvitationResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.OrganizationInvitationService;
import com.hubz.domain.enums.MemberRole;
import com.hubz.domain.exception.AccessDeniedException;
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
        value = OrganizationInvitationController.class,
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
@DisplayName("OrganizationInvitationController Unit Tests")
class OrganizationInvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrganizationInvitationService invitationService;

    @MockBean
    private UserRepositoryPort userRepositoryPort;

    private UUID userId;
    private UUID orgId;
    private UUID invitationId;
    private String token;
    private User testUser;
    private Authentication mockAuth;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orgId = UUID.randomUUID();
        invitationId = UUID.randomUUID();
        token = UUID.randomUUID().toString();

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

    private InvitationResponse createInvitationResponse() {
        return InvitationResponse.builder()
                .id(invitationId)
                .organizationId(orgId)
                .email("invite@example.com")
                .role(MemberRole.MEMBER)
                .token(token)
                .invitationUrl("http://localhost:5173/join/" + token)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .used(false)
                .build();
    }

    @Nested
    @DisplayName("POST /api/organizations/{orgId}/invitations - Create Invitation")
    class CreateTests {

        @Test
        @DisplayName("Should return 201 and invitation when creation is successful")
        void shouldCreateInvitation() throws Exception {
            // Given
            InvitationResponse response = createInvitationResponse();
            when(invitationService.createInvitation(eq(orgId), any(CreateInvitationRequest.class), eq(userId)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/organizations/{orgId}/invitations", orgId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "invite@example.com",
                                        "role": "MEMBER"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("invite@example.com"))
                    .andExpect(jsonPath("$.role").value("MEMBER"));

            verify(invitationService).createInvitation(eq(orgId), any(CreateInvitationRequest.class), eq(userId));
        }

        @Test
        @DisplayName("Should return 400 when email is invalid")
        void shouldReturn400WhenEmailInvalid() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/organizations/{orgId}/invitations", orgId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "invalid-email",
                                        "role": "MEMBER"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(invitationService, never()).createInvitation(any(), any(), any());
        }

        @Test
        @DisplayName("Should return 400 when email is missing")
        void shouldReturn400WhenEmailMissing() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/organizations/{orgId}/invitations", orgId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "role": "MEMBER"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(invitationService, never()).createInvitation(any(), any(), any());
        }

        @Test
        @DisplayName("Should return 403 when user is not authorized")
        void shouldReturn403WhenNotAuthorized() throws Exception {
            // Given
            when(invitationService.createInvitation(eq(orgId), any(CreateInvitationRequest.class), eq(userId)))
                    .thenThrow(new AccessDeniedException("Not authorized"));

            // When & Then
            mockMvc.perform(post("/api/organizations/{orgId}/invitations", orgId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "invite@example.com",
                                        "role": "MEMBER"
                                    }
                                    """))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/organizations/{orgId}/invitations - Get Invitations")
    class GetInvitationsTests {

        @Test
        @DisplayName("Should return 200 and list of invitations")
        void shouldGetInvitations() throws Exception {
            // Given
            List<InvitationResponse> responses = List.of(createInvitationResponse());
            when(invitationService.getInvitations(orgId, userId)).thenReturn(responses);

            // When & Then
            mockMvc.perform(get("/api/organizations/{orgId}/invitations", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].email").value("invite@example.com"));

            verify(invitationService).getInvitations(orgId, userId);
        }

        @Test
        @DisplayName("Should return 200 and empty list when no invitations")
        void shouldReturnEmptyList() throws Exception {
            // Given
            when(invitationService.getInvitations(orgId, userId)).thenReturn(List.of());

            // When & Then
            mockMvc.perform(get("/api/organizations/{orgId}/invitations", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());

            verify(invitationService).getInvitations(orgId, userId);
        }
    }

    @Nested
    @DisplayName("GET /api/invitations/{token}/info - Get Invitation Info")
    class GetInvitationInfoTests {

        @Test
        @DisplayName("Should return 200 and invitation info")
        void shouldGetInvitationInfo() throws Exception {
            // Given
            InvitationResponse response = createInvitationResponse();
            when(invitationService.getInvitationByToken(token)).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/invitations/{token}/info", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("invite@example.com"))
                    .andExpect(jsonPath("$.role").value("MEMBER"));

            verify(invitationService).getInvitationByToken(token);
        }

        @Test
        @DisplayName("Should return 400 when token is invalid")
        void shouldReturn400WhenTokenInvalid() throws Exception {
            // Given
            when(invitationService.getInvitationByToken(token))
                    .thenThrow(new RuntimeException("Invalid token"));

            // When & Then
            mockMvc.perform(get("/api/invitations/{token}/info", token))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Invalid token"));
        }
    }

    @Nested
    @DisplayName("POST /api/invitations/{token}/accept - Accept Invitation")
    class AcceptInvitationTests {

        @Test
        @DisplayName("Should return 200 when invitation is accepted")
        void shouldAcceptInvitation() throws Exception {
            // Given
            doNothing().when(invitationService).acceptInvitation(token, userId);

            // When & Then
            mockMvc.perform(post("/api/invitations/{token}/accept", token)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Invitation accepted successfully"));

            verify(invitationService).acceptInvitation(token, userId);
        }

        @Test
        @DisplayName("Should return 400 when invitation is expired")
        void shouldReturn400WhenExpired() throws Exception {
            // Given
            doThrow(new RuntimeException("Invitation has expired"))
                    .when(invitationService).acceptInvitation(token, userId);

            // When & Then
            mockMvc.perform(post("/api/invitations/{token}/accept", token)
                            .principal(mockAuth))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Invitation has expired"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/invitations/{invitationId} - Delete Invitation")
    class DeleteTests {

        @Test
        @DisplayName("Should return 204 when deletion is successful")
        void shouldDeleteInvitation() throws Exception {
            // Given
            doNothing().when(invitationService).deleteInvitation(invitationId, userId);

            // When & Then
            mockMvc.perform(delete("/api/invitations/{invitationId}", invitationId)
                            .principal(mockAuth))
                    .andExpect(status().isNoContent());

            verify(invitationService).deleteInvitation(invitationId, userId);
        }

        @Test
        @DisplayName("Should return 403 when user is not authorized")
        void shouldReturn403WhenNotAuthorized() throws Exception {
            // Given
            doThrow(new AccessDeniedException("Not authorized"))
                    .when(invitationService).deleteInvitation(invitationId, userId);

            // When & Then
            mockMvc.perform(delete("/api/invitations/{invitationId}", invitationId)
                            .principal(mockAuth))
                    .andExpect(status().isForbidden());
        }
    }
}
