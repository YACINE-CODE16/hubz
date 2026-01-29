package com.hubz.application.service;

import com.hubz.application.dto.request.CreateOrganizationRequest;
import com.hubz.application.dto.request.UpdateOrganizationRequest;
import com.hubz.application.dto.response.MemberResponse;
import com.hubz.application.dto.response.OrganizationResponse;
import com.hubz.application.port.out.OrganizationMemberRepositoryPort;
import com.hubz.application.port.out.OrganizationRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.enums.MemberRole;
import com.hubz.domain.exception.MemberAlreadyExistsException;
import com.hubz.domain.exception.OrganizationNotFoundException;
import com.hubz.domain.exception.UserNotFoundException;
import com.hubz.domain.model.Organization;
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
@DisplayName("OrganizationService Unit Tests")
class OrganizationServiceTest {

    @Mock
    private OrganizationRepositoryPort organizationRepository;

    @Mock
    private OrganizationMemberRepositoryPort memberRepository;

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private OrganizationService organizationService;

    private UUID ownerId;
    private UUID userId;
    private Organization testOrg;
    private User testUser;
    private OrganizationMember testMember;
    private CreateOrganizationRequest createRequest;
    private UpdateOrganizationRequest updateRequest;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testOrg = Organization.builder()
                .id(UUID.randomUUID())
                .name("Test Organization")
                .description("Test description")
                .icon("icon.png")
                .color("#FF5733")
                .ownerId(ownerId)
                .createdAt(LocalDateTime.now())
                .build();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .password("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .description("Test user")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testMember = OrganizationMember.builder()
                .id(UUID.randomUUID())
                .organizationId(testOrg.getId())
                .userId(userId)
                .role(MemberRole.MEMBER)
                .joinedAt(LocalDateTime.now())
                .build();

        createRequest = new CreateOrganizationRequest(
                "Test Organization",
                "Test description",
                "icon.png",
                "#FF5733"
        );

        updateRequest = new UpdateOrganizationRequest(
                "Updated Organization",
                "Updated description",
                "newicon.png",
                "#00FF00",
                "# README"
        );
    }

    @Nested
    @DisplayName("Create Organization Tests")
    class CreateTests {

        @Test
        @DisplayName("Should successfully create organization")
        void shouldCreateOrganization() {
            // Given
            when(organizationRepository.save(any(Organization.class))).thenReturn(testOrg);
            when(memberRepository.save(any(OrganizationMember.class))).thenReturn(testMember);

            // When
            OrganizationResponse response = organizationService.create(createRequest, ownerId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getName()).isEqualTo(testOrg.getName());
            assertThat(response.getDescription()).isEqualTo(testOrg.getDescription());
            assertThat(response.getIcon()).isEqualTo(testOrg.getIcon());
            assertThat(response.getColor()).isEqualTo(testOrg.getColor());
            assertThat(response.getOwnerId()).isEqualTo(ownerId);

            verify(organizationRepository).save(any(Organization.class));
            verify(memberRepository).save(any(OrganizationMember.class));
        }

        @Test
        @DisplayName("Should create owner member when creating organization")
        void shouldCreateOwnerMember() {
            // Given
            when(organizationRepository.save(any(Organization.class))).thenReturn(testOrg);
            ArgumentCaptor<OrganizationMember> memberCaptor = ArgumentCaptor.forClass(OrganizationMember.class);

            // When
            organizationService.create(createRequest, ownerId);

            // Then
            verify(memberRepository).save(memberCaptor.capture());
            OrganizationMember savedMember = memberCaptor.getValue();
            assertThat(savedMember.getOrganizationId()).isEqualTo(testOrg.getId());
            assertThat(savedMember.getUserId()).isEqualTo(ownerId);
            assertThat(savedMember.getRole()).isEqualTo(MemberRole.OWNER);
            assertThat(savedMember.getJoinedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should set createdAt timestamp")
        void shouldSetCreatedAt() {
            // Given
            when(organizationRepository.save(any(Organization.class))).thenReturn(testOrg);
            ArgumentCaptor<Organization> orgCaptor = ArgumentCaptor.forClass(Organization.class);

            // When
            organizationService.create(createRequest, ownerId);

            // Then
            verify(organizationRepository).save(orgCaptor.capture());
            Organization savedOrg = orgCaptor.getValue();
            assertThat(savedOrg.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should generate UUIDs for organization and member")
        void shouldGenerateUUIDs() {
            // Given
            when(organizationRepository.save(any(Organization.class))).thenReturn(testOrg);
            ArgumentCaptor<Organization> orgCaptor = ArgumentCaptor.forClass(Organization.class);
            ArgumentCaptor<OrganizationMember> memberCaptor = ArgumentCaptor.forClass(OrganizationMember.class);

            // When
            organizationService.create(createRequest, ownerId);

            // Then
            verify(organizationRepository).save(orgCaptor.capture());
            verify(memberRepository).save(memberCaptor.capture());
            assertThat(orgCaptor.getValue().getId()).isNotNull();
            assertThat(memberCaptor.getValue().getId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Get All Organizations Tests")
    class GetAllTests {

        @Test
        @DisplayName("Should return list of all organizations")
        void shouldGetAllOrganizations() {
            // Given
            List<Organization> orgs = List.of(testOrg);
            when(organizationRepository.findAll()).thenReturn(orgs);

            // When
            List<OrganizationResponse> responses = organizationService.getAll();

            // Then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getName()).isEqualTo(testOrg.getName());
            verify(organizationRepository).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no organizations exist")
        void shouldReturnEmptyList() {
            // Given
            when(organizationRepository.findAll()).thenReturn(List.of());

            // When
            List<OrganizationResponse> responses = organizationService.getAll();

            // Then
            assertThat(responses).isEmpty();
            verify(organizationRepository).findAll();
        }
    }

    @Nested
    @DisplayName("Get Organization By ID Tests")
    class GetByIdTests {

        @Test
        @DisplayName("Should successfully get organization by ID")
        void shouldGetOrganizationById() {
            // Given
            when(organizationRepository.findById(testOrg.getId())).thenReturn(Optional.of(testOrg));

            // When
            OrganizationResponse response = organizationService.getById(testOrg.getId());

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(testOrg.getId());
            assertThat(response.getName()).isEqualTo(testOrg.getName());
            verify(organizationRepository).findById(testOrg.getId());
        }

        @Test
        @DisplayName("Should throw OrganizationNotFoundException when not found")
        void shouldThrowExceptionWhenNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(organizationRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> organizationService.getById(nonExistentId))
                    .isInstanceOf(OrganizationNotFoundException.class);
            verify(organizationRepository).findById(nonExistentId);
        }
    }

    @Nested
    @DisplayName("Update Organization Tests")
    class UpdateTests {

        @Test
        @DisplayName("Should successfully update organization")
        void shouldUpdateOrganization() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAdminAccess(testOrg.getId(), ownerId);
            when(organizationRepository.findById(testOrg.getId())).thenReturn(Optional.of(testOrg));
            when(organizationRepository.save(any(Organization.class))).thenReturn(testOrg);

            // When
            OrganizationResponse response = organizationService.update(testOrg.getId(), updateRequest, ownerId);

            // Then
            assertThat(response).isNotNull();
            verify(authorizationService).checkOrganizationAdminAccess(testOrg.getId(), ownerId);
            verify(organizationRepository).findById(testOrg.getId());
            verify(organizationRepository).save(any(Organization.class));
        }

        @Test
        @DisplayName("Should throw exception when user is not admin")
        void shouldThrowExceptionWhenNotAdmin() {
            // Given
            UUID nonAdminUserId = UUID.randomUUID();
            doThrow(new RuntimeException("Not admin"))
                    .when(authorizationService).checkOrganizationAdminAccess(testOrg.getId(), nonAdminUserId);

            // When & Then
            assertThatThrownBy(() -> organizationService.update(testOrg.getId(), updateRequest, nonAdminUserId))
                    .isInstanceOf(RuntimeException.class);
            verify(authorizationService).checkOrganizationAdminAccess(testOrg.getId(), nonAdminUserId);
            verify(organizationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when organization not found")
        void shouldThrowExceptionWhenOrganizationNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            doNothing().when(authorizationService).checkOrganizationAdminAccess(nonExistentId, ownerId);
            when(organizationRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> organizationService.update(nonExistentId, updateRequest, ownerId))
                    .isInstanceOf(OrganizationNotFoundException.class);
            verify(organizationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete Organization Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should successfully delete organization")
        void shouldDeleteOrganization() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAdminAccess(testOrg.getId(), ownerId);
            when(organizationRepository.findById(testOrg.getId())).thenReturn(Optional.of(testOrg));
            doNothing().when(organizationRepository).deleteById(testOrg.getId());

            // When
            organizationService.delete(testOrg.getId(), ownerId);

            // Then
            verify(authorizationService).checkOrganizationAdminAccess(testOrg.getId(), ownerId);
            verify(organizationRepository).findById(testOrg.getId());
            verify(organizationRepository).deleteById(testOrg.getId());
        }

        @Test
        @DisplayName("Should throw exception when user is not admin")
        void shouldThrowExceptionWhenNotAdmin() {
            // Given
            UUID nonAdminUserId = UUID.randomUUID();
            doThrow(new RuntimeException("Not admin"))
                    .when(authorizationService).checkOrganizationAdminAccess(testOrg.getId(), nonAdminUserId);

            // When & Then
            assertThatThrownBy(() -> organizationService.delete(testOrg.getId(), nonAdminUserId))
                    .isInstanceOf(RuntimeException.class);
            verify(organizationRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should throw exception when organization not found")
        void shouldThrowExceptionWhenOrganizationNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            doNothing().when(authorizationService).checkOrganizationAdminAccess(nonExistentId, ownerId);
            when(organizationRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> organizationService.delete(nonExistentId, ownerId))
                    .isInstanceOf(OrganizationNotFoundException.class);
            verify(organizationRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("Get Members Tests")
    class GetMembersTests {

        @Test
        @DisplayName("Should successfully get organization members")
        void shouldGetMembers() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(testOrg.getId(), ownerId);
            when(organizationRepository.findById(testOrg.getId())).thenReturn(Optional.of(testOrg));
            when(memberRepository.findByOrganizationId(testOrg.getId())).thenReturn(List.of(testMember));
            when(userRepository.findById(testMember.getUserId())).thenReturn(Optional.of(testUser));

            // When
            List<MemberResponse> members = organizationService.getMembers(testOrg.getId(), ownerId);

            // Then
            assertThat(members).hasSize(1);
            assertThat(members.get(0).getUserId()).isEqualTo(testUser.getId());
            assertThat(members.get(0).getEmail()).isEqualTo(testUser.getEmail());
            verify(authorizationService).checkOrganizationAccess(testOrg.getId(), ownerId);
            verify(memberRepository).findByOrganizationId(testOrg.getId());
        }

        @Test
        @DisplayName("Should throw exception when organization not found")
        void shouldThrowExceptionWhenOrganizationNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            doNothing().when(authorizationService).checkOrganizationAccess(nonExistentId, ownerId);
            when(organizationRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> organizationService.getMembers(nonExistentId, ownerId))
                    .isInstanceOf(OrganizationNotFoundException.class);
            verify(memberRepository, never()).findByOrganizationId(any());
        }
    }

    @Nested
    @DisplayName("Add Member Tests")
    class AddMemberTests {

        @Test
        @DisplayName("Should successfully add member to organization")
        void shouldAddMember() {
            // Given
            UUID newUserId = UUID.randomUUID();
            doNothing().when(authorizationService).checkOrganizationAdminAccess(testOrg.getId(), ownerId);
            when(organizationRepository.findById(testOrg.getId())).thenReturn(Optional.of(testOrg));
            when(memberRepository.existsByOrganizationIdAndUserId(testOrg.getId(), newUserId)).thenReturn(false);
            when(userRepository.findById(newUserId)).thenReturn(Optional.of(testUser));
            when(memberRepository.save(any(OrganizationMember.class))).thenReturn(testMember);

            // When
            MemberResponse response = organizationService.addMember(testOrg.getId(), newUserId, MemberRole.MEMBER, ownerId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(testUser.getId());
            assertThat(response.getRole()).isEqualTo(MemberRole.MEMBER);
            verify(authorizationService).checkOrganizationAdminAccess(testOrg.getId(), ownerId);
            verify(memberRepository).save(any(OrganizationMember.class));
        }

        @Test
        @DisplayName("Should throw exception when member already exists")
        void shouldThrowExceptionWhenMemberAlreadyExists() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAdminAccess(testOrg.getId(), ownerId);
            when(organizationRepository.findById(testOrg.getId())).thenReturn(Optional.of(testOrg));
            when(memberRepository.existsByOrganizationIdAndUserId(testOrg.getId(), userId)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> organizationService.addMember(testOrg.getId(), userId, MemberRole.MEMBER, ownerId))
                    .isInstanceOf(MemberAlreadyExistsException.class);
            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            UUID nonExistentUserId = UUID.randomUUID();
            doNothing().when(authorizationService).checkOrganizationAdminAccess(testOrg.getId(), ownerId);
            when(organizationRepository.findById(testOrg.getId())).thenReturn(Optional.of(testOrg));
            when(memberRepository.existsByOrganizationIdAndUserId(testOrg.getId(), nonExistentUserId)).thenReturn(false);
            when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> organizationService.addMember(testOrg.getId(), nonExistentUserId, MemberRole.MEMBER, ownerId))
                    .isInstanceOf(UserNotFoundException.class);
            verify(memberRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Remove Member Tests")
    class RemoveMemberTests {

        @Test
        @DisplayName("Should successfully remove member from organization")
        void shouldRemoveMember() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAdminAccess(testOrg.getId(), ownerId);
            doNothing().when(memberRepository).deleteByOrganizationIdAndUserId(testOrg.getId(), userId);

            // When
            organizationService.removeMember(testOrg.getId(), userId, ownerId);

            // Then
            verify(authorizationService).checkOrganizationAdminAccess(testOrg.getId(), ownerId);
            verify(memberRepository).deleteByOrganizationIdAndUserId(testOrg.getId(), userId);
        }

        @Test
        @DisplayName("Should throw exception when user is not admin")
        void shouldThrowExceptionWhenNotAdmin() {
            // Given
            UUID nonAdminUserId = UUID.randomUUID();
            doThrow(new RuntimeException("Not admin"))
                    .when(authorizationService).checkOrganizationAdminAccess(testOrg.getId(), nonAdminUserId);

            // When & Then
            assertThatThrownBy(() -> organizationService.removeMember(testOrg.getId(), userId, nonAdminUserId))
                    .isInstanceOf(RuntimeException.class);
            verify(memberRepository, never()).deleteByOrganizationIdAndUserId(any(), any());
        }
    }
}
