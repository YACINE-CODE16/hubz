package com.hubz.presentation.controller;

import com.hubz.application.dto.request.CreateOrganizationRequest;
import com.hubz.application.dto.request.UpdateOrganizationRequest;
import com.hubz.application.dto.response.MemberResponse;
import com.hubz.application.dto.response.OrganizationResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.OrganizationService;
import com.hubz.domain.enums.MemberRole;
import com.hubz.domain.exception.AccessDeniedException;
import com.hubz.domain.exception.MemberAlreadyExistsException;
import com.hubz.domain.exception.OrganizationNotFoundException;
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
        value = OrganizationController.class,
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
@DisplayName("OrganizationController Unit Tests")
class OrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrganizationService organizationService;

    @MockBean
    private UserRepositoryPort userRepositoryPort;

    private UUID userId;
    private UUID orgId;
    private User testUser;
    private Authentication mockAuth;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orgId = UUID.randomUUID();

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

    private OrganizationResponse createOrgResponse() {
        return OrganizationResponse.builder()
                .id(orgId)
                .name("Test Organization")
                .description("Test description")
                .icon("icon.png")
                .color("#FF5733")
                .ownerId(userId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("POST /api/organizations - Create Organization")
    class CreateTests {

        @Test
        @DisplayName("Should return 201 and organization when creation is successful")
        void shouldCreateOrganization() throws Exception {
            // Given
            OrganizationResponse response = createOrgResponse();
            when(organizationService.create(any(CreateOrganizationRequest.class), eq(userId)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/organizations")
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "Test Organization",
                                        "description": "Test description",
                                        "icon": "icon.png",
                                        "color": "#FF5733"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Test Organization"))
                    .andExpect(jsonPath("$.description").value("Test description"));

            verify(organizationService).create(any(CreateOrganizationRequest.class), eq(userId));
        }

        @Test
        @DisplayName("Should return 400 when name is blank")
        void shouldReturn400WhenNameBlank() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/organizations")
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "",
                                        "description": "Test description"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(organizationService, never()).create(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when name is missing")
        void shouldReturn400WhenNameMissing() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/organizations")
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "description": "Test description"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(organizationService, never()).create(any(), any());
        }
    }

    @Nested
    @DisplayName("GET /api/organizations - Get All Organizations")
    class GetAllTests {

        @Test
        @DisplayName("Should return 200 and list of organizations")
        void shouldGetAllOrganizations() throws Exception {
            // Given
            List<OrganizationResponse> responses = List.of(createOrgResponse());
            when(organizationService.getAll()).thenReturn(responses);

            // When & Then
            mockMvc.perform(get("/api/organizations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("Test Organization"));

            verify(organizationService).getAll();
        }

        @Test
        @DisplayName("Should return 200 and empty list when no organizations")
        void shouldReturnEmptyList() throws Exception {
            // Given
            when(organizationService.getAll()).thenReturn(List.of());

            // When & Then
            mockMvc.perform(get("/api/organizations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());

            verify(organizationService).getAll();
        }
    }

    @Nested
    @DisplayName("GET /api/organizations/{id} - Get Organization By ID")
    class GetByIdTests {

        @Test
        @DisplayName("Should return 200 and organization when found")
        void shouldGetOrganizationById() throws Exception {
            // Given
            OrganizationResponse response = createOrgResponse();
            when(organizationService.getById(orgId)).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/organizations/{id}", orgId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(orgId.toString()))
                    .andExpect(jsonPath("$.name").value("Test Organization"));

            verify(organizationService).getById(orgId);
        }

        @Test
        @DisplayName("Should return 404 when organization not found")
        void shouldReturn404WhenNotFound() throws Exception {
            // Given
            when(organizationService.getById(orgId))
                    .thenThrow(new OrganizationNotFoundException(orgId));

            // When & Then
            mockMvc.perform(get("/api/organizations/{id}", orgId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/organizations/{id} - Update Organization")
    class UpdateTests {

        @Test
        @DisplayName("Should return 200 and updated organization when successful")
        void shouldUpdateOrganization() throws Exception {
            // Given
            OrganizationResponse response = OrganizationResponse.builder()
                    .id(orgId)
                    .name("Updated Organization")
                    .description("Updated description")
                    .ownerId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(organizationService.update(eq(orgId), any(UpdateOrganizationRequest.class), eq(userId)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(put("/api/organizations/{id}", orgId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "Updated Organization",
                                        "description": "Updated description"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Organization"));

            verify(organizationService).update(eq(orgId), any(UpdateOrganizationRequest.class), eq(userId));
        }

        @Test
        @DisplayName("Should return 404 when organization not found")
        void shouldReturn404WhenNotFound() throws Exception {
            // Given
            when(organizationService.update(eq(orgId), any(UpdateOrganizationRequest.class), eq(userId)))
                    .thenThrow(new OrganizationNotFoundException(orgId));

            // When & Then
            mockMvc.perform(put("/api/organizations/{id}", orgId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "Updated Organization"
                                    }
                                    """))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when user is not admin")
        void shouldReturn403WhenNotAdmin() throws Exception {
            // Given
            when(organizationService.update(eq(orgId), any(UpdateOrganizationRequest.class), eq(userId)))
                    .thenThrow(new AccessDeniedException("Not authorized"));

            // When & Then
            mockMvc.perform(put("/api/organizations/{id}", orgId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "Updated Organization"
                                    }
                                    """))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/organizations/{id} - Delete Organization")
    class DeleteTests {

        @Test
        @DisplayName("Should return 204 when deletion is successful")
        void shouldDeleteOrganization() throws Exception {
            // Given
            doNothing().when(organizationService).delete(orgId, userId);

            // When & Then
            mockMvc.perform(delete("/api/organizations/{id}", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isNoContent());

            verify(organizationService).delete(orgId, userId);
        }

        @Test
        @DisplayName("Should return 404 when organization not found")
        void shouldReturn404WhenNotFound() throws Exception {
            // Given
            doThrow(new OrganizationNotFoundException(orgId))
                    .when(organizationService).delete(orgId, userId);

            // When & Then
            mockMvc.perform(delete("/api/organizations/{id}", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when user is not admin")
        void shouldReturn403WhenNotAdmin() throws Exception {
            // Given
            doThrow(new AccessDeniedException("Not authorized"))
                    .when(organizationService).delete(orgId, userId);

            // When & Then
            mockMvc.perform(delete("/api/organizations/{id}", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/organizations/{id}/members - Get Members")
    class GetMembersTests {

        @Test
        @DisplayName("Should return 200 and list of members")
        void shouldGetMembers() throws Exception {
            // Given
            MemberResponse member = MemberResponse.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .firstName("John")
                    .lastName("Doe")
                    .email("test@example.com")
                    .role(MemberRole.OWNER)
                    .joinedAt(LocalDateTime.now())
                    .build();

            when(organizationService.getMembers(orgId, userId)).thenReturn(List.of(member));

            // When & Then
            mockMvc.perform(get("/api/organizations/{id}/members", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].email").value("test@example.com"))
                    .andExpect(jsonPath("$[0].role").value("OWNER"));

            verify(organizationService).getMembers(orgId, userId);
        }

        @Test
        @DisplayName("Should return 404 when organization not found")
        void shouldReturn404WhenOrgNotFound() throws Exception {
            // Given
            when(organizationService.getMembers(orgId, userId))
                    .thenThrow(new OrganizationNotFoundException(orgId));

            // When & Then
            mockMvc.perform(get("/api/organizations/{id}/members", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/organizations/{id}/members - Add Member")
    class AddMemberTests {

        @Test
        @DisplayName("Should return 201 when member is added successfully")
        void shouldAddMember() throws Exception {
            // Given
            UUID newMemberId = UUID.randomUUID();
            MemberResponse member = MemberResponse.builder()
                    .id(UUID.randomUUID())
                    .userId(newMemberId)
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("jane@example.com")
                    .role(MemberRole.MEMBER)
                    .joinedAt(LocalDateTime.now())
                    .build();

            when(organizationService.addMember(eq(orgId), eq(newMemberId), eq(MemberRole.MEMBER), eq(userId)))
                    .thenReturn(member);

            // When & Then
            mockMvc.perform(post("/api/organizations/{id}/members", orgId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "userId": "%s",
                                        "role": "MEMBER"
                                    }
                                    """, newMemberId)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("jane@example.com"))
                    .andExpect(jsonPath("$.role").value("MEMBER"));

            verify(organizationService).addMember(eq(orgId), eq(newMemberId), eq(MemberRole.MEMBER), eq(userId));
        }

        @Test
        @DisplayName("Should return 409 when member already exists")
        void shouldReturn409WhenMemberExists() throws Exception {
            // Given
            UUID newMemberId = UUID.randomUUID();
            when(organizationService.addMember(eq(orgId), eq(newMemberId), eq(MemberRole.MEMBER), eq(userId)))
                    .thenThrow(new MemberAlreadyExistsException(newMemberId, orgId));

            // When & Then
            mockMvc.perform(post("/api/organizations/{id}/members", orgId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "userId": "%s",
                                        "role": "MEMBER"
                                    }
                                    """, newMemberId)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("DELETE /api/organizations/{id}/members/{userId} - Remove Member")
    class RemoveMemberTests {

        @Test
        @DisplayName("Should return 204 when member is removed successfully")
        void shouldRemoveMember() throws Exception {
            // Given
            UUID memberToRemove = UUID.randomUUID();
            doNothing().when(organizationService).removeMember(orgId, memberToRemove, userId);

            // When & Then
            mockMvc.perform(delete("/api/organizations/{id}/members/{userId}", orgId, memberToRemove)
                            .principal(mockAuth))
                    .andExpect(status().isNoContent());

            verify(organizationService).removeMember(orgId, memberToRemove, userId);
        }

        @Test
        @DisplayName("Should return 403 when user is not admin")
        void shouldReturn403WhenNotAdmin() throws Exception {
            // Given
            UUID memberToRemove = UUID.randomUUID();
            doThrow(new AccessDeniedException("Not authorized"))
                    .when(organizationService).removeMember(orgId, memberToRemove, userId);

            // When & Then
            mockMvc.perform(delete("/api/organizations/{id}/members/{userId}", orgId, memberToRemove)
                            .principal(mockAuth))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PATCH /api/organizations/{id}/members/{userId}/role - Change Member Role")
    class ChangeMemberRoleTests {

        @Test
        @DisplayName("Should return 200 when role is changed successfully")
        void shouldChangeMemberRole() throws Exception {
            // Given
            UUID memberUserId = UUID.randomUUID();
            MemberResponse member = MemberResponse.builder()
                    .id(UUID.randomUUID())
                    .userId(memberUserId)
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("jane@example.com")
                    .role(MemberRole.ADMIN)
                    .joinedAt(LocalDateTime.now())
                    .build();

            when(organizationService.changeMemberRole(eq(orgId), eq(memberUserId), eq(MemberRole.ADMIN), eq(userId)))
                    .thenReturn(member);

            // When & Then
            mockMvc.perform(patch("/api/organizations/{id}/members/{userId}/role", orgId, memberUserId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "role": "ADMIN"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role").value("ADMIN"));

            verify(organizationService).changeMemberRole(eq(orgId), eq(memberUserId), eq(MemberRole.ADMIN), eq(userId));
        }
    }

    @Nested
    @DisplayName("POST /api/organizations/{id}/transfer-ownership/{newOwnerId} - Transfer Ownership")
    class TransferOwnershipTests {

        @Test
        @DisplayName("Should return 200 when ownership is transferred successfully")
        void shouldTransferOwnership() throws Exception {
            // Given
            UUID newOwnerId = UUID.randomUUID();
            doNothing().when(organizationService).transferOwnership(orgId, newOwnerId, userId);

            // When & Then
            mockMvc.perform(post("/api/organizations/{id}/transfer-ownership/{newOwnerId}", orgId, newOwnerId)
                            .principal(mockAuth))
                    .andExpect(status().isOk());

            verify(organizationService).transferOwnership(orgId, newOwnerId, userId);
        }

        @Test
        @DisplayName("Should return 403 when user is not current owner")
        void shouldReturn403WhenNotOwner() throws Exception {
            // Given
            UUID newOwnerId = UUID.randomUUID();
            doThrow(new AccessDeniedException("Only the owner can transfer ownership"))
                    .when(organizationService).transferOwnership(orgId, newOwnerId, userId);

            // When & Then
            mockMvc.perform(post("/api/organizations/{id}/transfer-ownership/{newOwnerId}", orgId, newOwnerId)
                            .principal(mockAuth))
                    .andExpect(status().isForbidden());
        }
    }
}
