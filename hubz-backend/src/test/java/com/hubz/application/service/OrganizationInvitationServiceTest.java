package com.hubz.application.service;

import com.hubz.application.dto.request.CreateInvitationRequest;
import com.hubz.application.dto.response.InvitationResponse;
import com.hubz.application.port.out.OrganizationInvitationRepositoryPort;
import com.hubz.application.port.out.OrganizationMemberRepositoryPort;
import com.hubz.application.port.out.OrganizationRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.enums.MemberRole;
import com.hubz.domain.model.Organization;
import com.hubz.domain.model.OrganizationInvitation;
import com.hubz.domain.model.OrganizationMember;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationInvitationService Unit Tests")
class OrganizationInvitationServiceTest {

    @Mock
    private OrganizationInvitationRepositoryPort invitationRepository;

    @Mock
    private OrganizationRepositoryPort organizationRepository;

    @Mock
    private OrganizationMemberRepositoryPort memberRepository;

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private OrganizationInvitationService invitationService;

    private UUID organizationId;
    private UUID userId;
    private UUID invitationId;
    private String invitationToken;
    private Organization testOrganization;
    private OrganizationInvitation testInvitation;
    private User testUser;
    private CreateInvitationRequest createRequest;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        invitationId = UUID.randomUUID();
        invitationToken = UUID.randomUUID().toString();

        testOrganization = Organization.builder()
                .id(organizationId)
                .name("Test Organization")
                .description("Test description")
                .ownerId(userId)
                .createdAt(LocalDateTime.now())
                .build();

        testInvitation = OrganizationInvitation.builder()
                .id(invitationId)
                .organizationId(organizationId)
                .email("invited@example.com")
                .role(MemberRole.MEMBER)
                .token(invitationToken)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .used(false)
                .build();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .password("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createRequest = new CreateInvitationRequest("invited@example.com", MemberRole.MEMBER);
    }

    @Nested
    @DisplayName("Create Invitation Tests")
    class CreateInvitationTests {

        @Test
        @DisplayName("Should successfully create invitation")
        void shouldCreateInvitation() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAdminAccess(organizationId, userId);
            when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(testOrganization));
            when(memberRepository.existsByOrganizationIdAndUserId(organizationId, userId)).thenReturn(true);
            when(invitationRepository.findByOrganizationIdAndEmailAndUsedFalse(organizationId, createRequest.getEmail()))
                    .thenReturn(Optional.empty());
            when(invitationRepository.save(any(OrganizationInvitation.class))).thenReturn(testInvitation);
            doNothing().when(emailService).sendInvitationEmail(anyString(), anyString(), anyString(), anyString());

            // When
            InvitationResponse response = invitationService.createInvitation(organizationId, createRequest, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getEmail()).isEqualTo(testInvitation.getEmail());
            assertThat(response.getRole()).isEqualTo(testInvitation.getRole());
            assertThat(response.getOrganizationId()).isEqualTo(organizationId);

            verify(authorizationService).checkOrganizationAdminAccess(organizationId, userId);
            verify(invitationRepository).save(any(OrganizationInvitation.class));
        }

        @Test
        @DisplayName("Should generate unique token for invitation")
        void shouldGenerateUniqueToken() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAdminAccess(organizationId, userId);
            when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(testOrganization));
            when(memberRepository.existsByOrganizationIdAndUserId(organizationId, userId)).thenReturn(true);
            when(invitationRepository.findByOrganizationIdAndEmailAndUsedFalse(organizationId, createRequest.getEmail()))
                    .thenReturn(Optional.empty());
            ArgumentCaptor<OrganizationInvitation> invCaptor = ArgumentCaptor.forClass(OrganizationInvitation.class);
            when(invitationRepository.save(invCaptor.capture())).thenReturn(testInvitation);
            doNothing().when(emailService).sendInvitationEmail(anyString(), anyString(), anyString(), anyString());

            // When
            invitationService.createInvitation(organizationId, createRequest, userId);

            // Then
            OrganizationInvitation savedInvitation = invCaptor.getValue();
            assertThat(savedInvitation.getToken()).isNotNull();
            assertThat(savedInvitation.getToken()).isNotEmpty();
        }

        @Test
        @DisplayName("Should set expiration date 7 days in the future")
        void shouldSetExpirationDate() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAdminAccess(organizationId, userId);
            when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(testOrganization));
            when(memberRepository.existsByOrganizationIdAndUserId(organizationId, userId)).thenReturn(true);
            when(invitationRepository.findByOrganizationIdAndEmailAndUsedFalse(organizationId, createRequest.getEmail()))
                    .thenReturn(Optional.empty());
            ArgumentCaptor<OrganizationInvitation> invCaptor = ArgumentCaptor.forClass(OrganizationInvitation.class);
            when(invitationRepository.save(invCaptor.capture())).thenReturn(testInvitation);
            doNothing().when(emailService).sendInvitationEmail(anyString(), anyString(), anyString(), anyString());

            // When
            invitationService.createInvitation(organizationId, createRequest, userId);

            // Then
            OrganizationInvitation savedInvitation = invCaptor.getValue();
            assertThat(savedInvitation.getExpiresAt()).isAfter(LocalDateTime.now());
            assertThat(savedInvitation.getExpiresAt()).isBefore(LocalDateTime.now().plusDays(8));
        }

        @Test
        @DisplayName("Should throw exception when user is not admin")
        void shouldThrowExceptionWhenNotAdmin() {
            // Given
            doThrow(new RuntimeException("Not admin"))
                    .when(authorizationService).checkOrganizationAdminAccess(organizationId, userId);

            // When & Then
            assertThatThrownBy(() -> invitationService.createInvitation(organizationId, createRequest, userId))
                    .isInstanceOf(RuntimeException.class);
            verify(invitationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when organization not found")
        void shouldThrowExceptionWhenOrganizationNotFound() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAdminAccess(organizationId, userId);
            when(organizationRepository.findById(organizationId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> invitationService.createInvitation(organizationId, createRequest, userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Organization not found");
        }

        @Test
        @DisplayName("Should throw exception when pending invitation already exists")
        void shouldThrowExceptionWhenPendingInvitationExists() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAdminAccess(organizationId, userId);
            when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(testOrganization));
            when(memberRepository.existsByOrganizationIdAndUserId(organizationId, userId)).thenReturn(true);
            when(invitationRepository.findByOrganizationIdAndEmailAndUsedFalse(organizationId, createRequest.getEmail()))
                    .thenReturn(Optional.of(testInvitation));

            // When & Then
            assertThatThrownBy(() -> invitationService.createInvitation(organizationId, createRequest, userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("invitation has already been sent");
        }

        @Test
        @DisplayName("Should still create invitation when email fails")
        void shouldCreateInvitationWhenEmailFails() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAdminAccess(organizationId, userId);
            when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(testOrganization));
            when(memberRepository.existsByOrganizationIdAndUserId(organizationId, userId)).thenReturn(true);
            when(invitationRepository.findByOrganizationIdAndEmailAndUsedFalse(organizationId, createRequest.getEmail()))
                    .thenReturn(Optional.empty());
            when(invitationRepository.save(any(OrganizationInvitation.class))).thenReturn(testInvitation);
            doThrow(new RuntimeException("Email failed"))
                    .when(emailService).sendInvitationEmail(anyString(), anyString(), anyString(), anyString());

            // When
            InvitationResponse response = invitationService.createInvitation(organizationId, createRequest, userId);

            // Then
            assertThat(response).isNotNull();
            verify(invitationRepository).save(any(OrganizationInvitation.class));
        }
    }

    @Nested
    @DisplayName("Get Invitations Tests")
    class GetInvitationsTests {

        @Test
        @DisplayName("Should successfully get invitations")
        void shouldGetInvitations() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAdminAccess(organizationId, userId);
            when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(testOrganization));
            when(invitationRepository.findByOrganizationId(organizationId)).thenReturn(List.of(testInvitation));

            // When
            List<InvitationResponse> invitations = invitationService.getInvitations(organizationId, userId);

            // Then
            assertThat(invitations).hasSize(1);
            assertThat(invitations.get(0).getEmail()).isEqualTo(testInvitation.getEmail());
            verify(authorizationService).checkOrganizationAdminAccess(organizationId, userId);
        }

        @Test
        @DisplayName("Should return empty list when no invitations exist")
        void shouldReturnEmptyListWhenNoInvitations() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAdminAccess(organizationId, userId);
            when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(testOrganization));
            when(invitationRepository.findByOrganizationId(organizationId)).thenReturn(List.of());

            // When
            List<InvitationResponse> invitations = invitationService.getInvitations(organizationId, userId);

            // Then
            assertThat(invitations).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when user is not admin")
        void shouldThrowExceptionWhenNotAdmin() {
            // Given
            doThrow(new RuntimeException("Not admin"))
                    .when(authorizationService).checkOrganizationAdminAccess(organizationId, userId);

            // When & Then
            assertThatThrownBy(() -> invitationService.getInvitations(organizationId, userId))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("Accept Invitation Tests")
    class AcceptInvitationTests {

        @Test
        @DisplayName("Should successfully accept invitation")
        void shouldAcceptInvitation() {
            // Given
            when(invitationRepository.findByToken(invitationToken)).thenReturn(Optional.of(testInvitation));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(memberRepository.existsByOrganizationIdAndUserId(organizationId, userId)).thenReturn(false);
            when(memberRepository.save(any(OrganizationMember.class))).thenReturn(mock(OrganizationMember.class));
            when(invitationRepository.save(any(OrganizationInvitation.class))).thenReturn(testInvitation);

            // When
            invitationService.acceptInvitation(invitationToken, userId);

            // Then
            verify(memberRepository).save(any(OrganizationMember.class));
            verify(invitationRepository).save(any(OrganizationInvitation.class));
        }

        @Test
        @DisplayName("Should mark invitation as used after accepting")
        void shouldMarkInvitationAsUsed() {
            // Given
            when(invitationRepository.findByToken(invitationToken)).thenReturn(Optional.of(testInvitation));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(memberRepository.existsByOrganizationIdAndUserId(organizationId, userId)).thenReturn(false);
            when(memberRepository.save(any(OrganizationMember.class))).thenReturn(mock(OrganizationMember.class));
            ArgumentCaptor<OrganizationInvitation> invCaptor = ArgumentCaptor.forClass(OrganizationInvitation.class);
            when(invitationRepository.save(invCaptor.capture())).thenReturn(testInvitation);

            // When
            invitationService.acceptInvitation(invitationToken, userId);

            // Then
            OrganizationInvitation savedInvitation = invCaptor.getValue();
            assertThat(savedInvitation.getUsed()).isTrue();
            assertThat(savedInvitation.getAcceptedBy()).isEqualTo(userId);
            assertThat(savedInvitation.getAcceptedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should create member with correct role from invitation")
        void shouldCreateMemberWithCorrectRole() {
            // Given
            when(invitationRepository.findByToken(invitationToken)).thenReturn(Optional.of(testInvitation));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(memberRepository.existsByOrganizationIdAndUserId(organizationId, userId)).thenReturn(false);
            ArgumentCaptor<OrganizationMember> memberCaptor = ArgumentCaptor.forClass(OrganizationMember.class);
            when(memberRepository.save(memberCaptor.capture())).thenReturn(mock(OrganizationMember.class));
            when(invitationRepository.save(any(OrganizationInvitation.class))).thenReturn(testInvitation);

            // When
            invitationService.acceptInvitation(invitationToken, userId);

            // Then
            OrganizationMember savedMember = memberCaptor.getValue();
            assertThat(savedMember.getRole()).isEqualTo(testInvitation.getRole());
            assertThat(savedMember.getOrganizationId()).isEqualTo(organizationId);
            assertThat(savedMember.getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Should throw exception when invitation not found")
        void shouldThrowExceptionWhenInvitationNotFound() {
            // Given
            when(invitationRepository.findByToken(invitationToken)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> invitationService.acceptInvitation(invitationToken, userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Invalid invitation token");
        }

        @Test
        @DisplayName("Should throw exception when invitation already used")
        void shouldThrowExceptionWhenInvitationAlreadyUsed() {
            // Given
            OrganizationInvitation usedInvitation = OrganizationInvitation.builder()
                    .id(invitationId)
                    .organizationId(organizationId)
                    .email("invited@example.com")
                    .role(MemberRole.MEMBER)
                    .token(invitationToken)
                    .createdBy(userId)
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .used(true)
                    .build();

            when(invitationRepository.findByToken(invitationToken)).thenReturn(Optional.of(usedInvitation));

            // When & Then
            assertThatThrownBy(() -> invitationService.acceptInvitation(invitationToken, userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("already been used");
        }

        @Test
        @DisplayName("Should throw exception when invitation expired")
        void shouldThrowExceptionWhenInvitationExpired() {
            // Given
            OrganizationInvitation expiredInvitation = OrganizationInvitation.builder()
                    .id(invitationId)
                    .organizationId(organizationId)
                    .email("invited@example.com")
                    .role(MemberRole.MEMBER)
                    .token(invitationToken)
                    .createdBy(userId)
                    .createdAt(LocalDateTime.now().minusDays(10))
                    .expiresAt(LocalDateTime.now().minusDays(3))
                    .used(false)
                    .build();

            when(invitationRepository.findByToken(invitationToken)).thenReturn(Optional.of(expiredInvitation));

            // When & Then
            assertThatThrownBy(() -> invitationService.acceptInvitation(invitationToken, userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            when(invitationRepository.findByToken(invitationToken)).thenReturn(Optional.of(testInvitation));
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> invitationService.acceptInvitation(invitationToken, userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("Should throw exception when user is already a member")
        void shouldThrowExceptionWhenAlreadyMember() {
            // Given
            when(invitationRepository.findByToken(invitationToken)).thenReturn(Optional.of(testInvitation));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(memberRepository.existsByOrganizationIdAndUserId(organizationId, userId)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> invitationService.acceptInvitation(invitationToken, userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("already a member");
        }
    }

    @Nested
    @DisplayName("Get Invitation By Token Tests")
    class GetInvitationByTokenTests {

        @Test
        @DisplayName("Should successfully get invitation by token")
        void shouldGetInvitationByToken() {
            // Given
            when(invitationRepository.findByToken(invitationToken)).thenReturn(Optional.of(testInvitation));
            when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(testOrganization));

            // When
            InvitationResponse response = invitationService.getInvitationByToken(invitationToken);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getEmail()).isEqualTo(testInvitation.getEmail());
            assertThat(response.getOrganizationId()).isEqualTo(organizationId);
        }

        @Test
        @DisplayName("Should throw exception when invitation not found")
        void shouldThrowExceptionWhenInvitationNotFound() {
            // Given
            when(invitationRepository.findByToken(invitationToken)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> invitationService.getInvitationByToken(invitationToken))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Invalid invitation token");
        }
    }

    @Nested
    @DisplayName("Delete Invitation Tests")
    class DeleteInvitationTests {

        @Test
        @DisplayName("Should successfully delete invitation")
        void shouldDeleteInvitation() {
            // Given
            when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(testInvitation));
            doNothing().when(authorizationService).checkOrganizationAdminAccess(organizationId, userId);
            doNothing().when(invitationRepository).deleteById(invitationId);

            // When
            invitationService.deleteInvitation(invitationId, userId);

            // Then
            verify(invitationRepository).findById(invitationId);
            verify(authorizationService).checkOrganizationAdminAccess(organizationId, userId);
            verify(invitationRepository).deleteById(invitationId);
        }

        @Test
        @DisplayName("Should throw exception when invitation not found")
        void shouldThrowExceptionWhenInvitationNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(invitationRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> invitationService.deleteInvitation(nonExistentId, userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Invitation not found");
        }

        @Test
        @DisplayName("Should throw exception when user is not admin")
        void shouldThrowExceptionWhenNotAdmin() {
            // Given
            when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(testInvitation));
            doThrow(new RuntimeException("Not admin"))
                    .when(authorizationService).checkOrganizationAdminAccess(organizationId, userId);

            // When & Then
            assertThatThrownBy(() -> invitationService.deleteInvitation(invitationId, userId))
                    .isInstanceOf(RuntimeException.class);
            verify(invitationRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("Response Mapping Tests")
    class ResponseMappingTests {

        @Test
        @DisplayName("Should correctly map invitation to response")
        void shouldCorrectlyMapInvitationToResponse() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAdminAccess(organizationId, userId);
            when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(testOrganization));
            when(invitationRepository.findByOrganizationId(organizationId)).thenReturn(List.of(testInvitation));

            // When
            List<InvitationResponse> invitations = invitationService.getInvitations(organizationId, userId);

            // Then
            InvitationResponse response = invitations.get(0);
            assertThat(response.getId()).isEqualTo(testInvitation.getId());
            assertThat(response.getOrganizationId()).isEqualTo(testInvitation.getOrganizationId());
            assertThat(response.getEmail()).isEqualTo(testInvitation.getEmail());
            assertThat(response.getRole()).isEqualTo(testInvitation.getRole());
            assertThat(response.getToken()).isEqualTo(testInvitation.getToken());
            assertThat(response.getCreatedBy()).isEqualTo(testInvitation.getCreatedBy());
            assertThat(response.getCreatedAt()).isEqualTo(testInvitation.getCreatedAt());
            assertThat(response.getExpiresAt()).isEqualTo(testInvitation.getExpiresAt());
            assertThat(response.getUsed()).isEqualTo(testInvitation.getUsed());
        }

        @Test
        @DisplayName("Should generate invitation URL")
        void shouldGenerateInvitationUrl() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAdminAccess(organizationId, userId);
            when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(testOrganization));
            when(invitationRepository.findByOrganizationId(organizationId)).thenReturn(List.of(testInvitation));

            // When
            List<InvitationResponse> invitations = invitationService.getInvitations(organizationId, userId);

            // Then
            InvitationResponse response = invitations.get(0);
            assertThat(response.getInvitationUrl()).isEqualTo("/join/" + testInvitation.getToken());
        }
    }
}
